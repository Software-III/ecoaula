package com.ecoaula.service;

import com.ecoaula.exception.AccesoDenegadoException;
import com.ecoaula.exception.RecursoNoEncontradoException;
import com.ecoaula.exception.ReglaNegocioException;
import com.ecoaula.model.Aprendiz;
import com.ecoaula.model.Contenido;
import com.ecoaula.model.ContenidoVersion;
import com.ecoaula.model.Curso;
import com.ecoaula.model.enums.EstadoContenido;
import com.ecoaula.model.enums.Rol;
import com.ecoaula.repository.AprendizRepository;
import com.ecoaula.repository.ContenidoRepository;
import com.ecoaula.repository.ContenidoVersionRepository;
import com.ecoaula.repository.CursoRepository;
import com.ecoaula.repository.EvaluacionRepository;
import com.ecoaula.repository.MatriculaRepository;
import com.ecoaula.repository.RecursoImagenRepository;
import com.ecoaula.service.dto.EstadoLeccion;
import com.ecoaula.storage.ValidadorAudioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Servicio para la gestión de cursos, lecciones y versionamiento inmutable
 * del lado del Gestor de Contenido (ADR-08).
 */
@Service
public class GestionContenidoService {

    private final CursoRepository cursoRepository;
    private final ContenidoRepository contenidoRepository;
    private final ContenidoVersionRepository versionRepository;
    private final AprendizRepository aprendizRepository;
    private final ValidadorAudioService validadorAudioService;
    private final MatriculaRepository matriculaRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final RecursoImagenRepository imagenRepository;
    private final ContenidoService contenidoService;

    /** Estados de una versión cuyo trámite no ha terminado. */
    private static final Set<EstadoContenido> ESTADOS_EN_TRAMITE =
            EnumSet.of(EstadoContenido.BORRADOR, EstadoContenido.EN_REVISION);


    public GestionContenidoService(CursoRepository cursoRepository,
                                   ContenidoRepository contenidoRepository,
                                   ContenidoVersionRepository versionRepository,
                                   AprendizRepository aprendizRepository,
                                   ValidadorAudioService validadorAudioService,
                                   MatriculaRepository matriculaRepository,
                                   EvaluacionRepository evaluacionRepository,
                                   RecursoImagenRepository imagenRepository,
                                   ContenidoService contenidoService) {
        this.cursoRepository = cursoRepository;
        this.contenidoRepository = contenidoRepository;
        this.versionRepository = versionRepository;
        this.aprendizRepository = aprendizRepository;
        this.validadorAudioService = validadorAudioService;
        this.matriculaRepository = matriculaRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.imagenRepository = imagenRepository;
        this.contenidoService = contenidoService;
    }

    @Transactional(readOnly = true)
    public List<Curso> listarTodosLosCursos() {
        return cursoRepository.findAll();
    }

    public Curso obtenerCurso(Long cursoId) {
        return cursoRepository.findById(cursoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Curso no encontrado con ID: " + cursoId));
    }

    /**
     * Crea el curso fuera del catálogo: el gestor le agrega lecciones y lo publica cuando está completo
     * (ver {@link #publicarEnCatalogo}).
     */
    @Transactional
    public Curso crearCurso(String titulo, String descripcion, Integer horas, String area, String modalidad, Integer cupoMaximo) {
        if (titulo == null || titulo.isBlank()) {
            throw new ReglaNegocioException("El título del curso es obligatorio.");
        }
        Curso curso = new Curso(titulo.trim(), descripcion, horas, area, modalidad);
        curso.setActivo(false);
        curso.setCupoMaximo(cupoMaximo);
        return cursoRepository.save(curso);
    }

    /**
     * Modifica los datos del curso. Solo con el curso fuera del catálogo: lo publicado no cambia
     * mientras los aprendices lo consultan. El cupo no puede quedar por debajo de los matriculados.
     */
    @Transactional
    public Curso actualizarCurso(Long cursoId, String titulo, String descripcion, Integer horas, String area, String modalidad,
                                 Integer cupoMaximo) {
        Curso curso = obtenerCurso(cursoId);
        exigirFueraDelCatalogo(curso, "modificar sus datos");
        if (titulo == null || titulo.isBlank()) {
            throw new ReglaNegocioException("El título del curso es obligatorio.");
        }
        long matriculados = matriculaRepository.countByCursoId(cursoId);
        if (cupoMaximo != null && cupoMaximo < matriculados) {
            throw new ReglaNegocioException("El cupo máximo no puede ser menor que los " + matriculados + " aprendices ya matriculados.");
        }
        curso.setTitulo(titulo.trim());
        curso.setDescripcion(descripcion);
        curso.setHoras(horas);
        curso.setArea(area);
        curso.setModalidad(modalidad);
        curso.setCupoMaximo(cupoMaximo);
        return cursoRepository.save(curso);
    }

    /**
     * Publica el curso en el catálogo (entrada del Proceso 1) si está completo: al menos una lección,
     * todas con versión PUBLICADA por el revisor, y evaluación final configurada (Proceso 3).
     */
    @Transactional
    public Curso publicarEnCatalogo(Long cursoId) {
        Curso curso = obtenerCurso(cursoId);
        List<String> pendientes = new ArrayList<>();
        List<Contenido> lecciones = contenidoRepository.findByCursoIdOrderByOrdenAsc(cursoId);
        if (lecciones.isEmpty()) {
            pendientes.add("el curso no tiene lecciones");
        }
        lecciones.stream()
                .filter(l -> versionRepository.findByContenidoIdAndEstado(l.getId(), EstadoContenido.PUBLICADA).isEmpty())
                .forEach(l -> pendientes.add("la lección " + l.getOrden() + " (" + l.getTitulo()
                        + ") no tiene una versión aprobada por el revisor de accesibilidad"));
        if (evaluacionRepository.findByCursoId(cursoId).isEmpty()) {
            pendientes.add("el curso no tiene evaluación final configurada");
        }
        if (!pendientes.isEmpty()) {
            throw new ReglaNegocioException("No se puede publicar el curso en el catálogo: " + String.join("; ", pendientes) + ".");
        }
        curso.setActivo(true);
        return cursoRepository.save(curso);
    }

    /**
     * Retira el curso del catálogo: deja de recibir matrículas nuevas, pero los aprendices ya
     * matriculados conservan el acceso a sus lecciones, su evaluación y su certificado.
     */
    @Transactional
    public Curso retirarDelCatalogo(Long cursoId) {
        Curso curso = obtenerCurso(cursoId);
        curso.setActivo(false);
        return cursoRepository.save(curso);
    }

    /**
     * Elimina un curso que nunca llegó a aprendices, con sus lecciones, versiones, imágenes,
     * informes de revisión y evaluación.
     */
    @Transactional
    public void eliminarCurso(Long cursoId) {
        Curso curso = obtenerCurso(cursoId);
        exigirCambioEstructuralPermitido(curso, "eliminarlo");
        if (contenidoService.cursoTieneVersionEnRevision(cursoId)) {
            throw new ReglaNegocioException("No se puede eliminar el curso mientras tenga versiones en revisión de accesibilidad. "
                    + "Espere la decisión del revisor.");
        }
        contenidoRepository.findByCursoIdOrderByOrdenAsc(cursoId)
                .forEach(l -> contenidoService.eliminarDependenciasDeLeccion(l.getId()));
        // Lecciones, versiones y evaluación se eliminan en cascada desde el curso
        cursoRepository.delete(curso);
    }

    @Transactional
    public Contenido actualizarTituloLeccion(Long contenidoId, String titulo) {
        Contenido leccion = obtenerLeccion(contenidoId);
        exigirFueraDelCatalogo(leccion.getCurso(), "modificar sus lecciones");
        if (titulo == null || titulo.isBlank()) {
            throw new ReglaNegocioException("El título de la lección es obligatorio.");
        }
        leccion.setTitulo(titulo.strip());
        return contenidoRepository.save(leccion);
    }

    /**
     * Elimina una lección con sus versiones, imágenes e informes, y renumera las siguientes para que la
     * navegación del aprendiz (lección n → n+1) siga siendo continua.
     */
    @Transactional
    public void eliminarLeccion(Long contenidoId) {
        Contenido leccion = obtenerLeccion(contenidoId);
        Long cursoId = leccion.getCurso().getId();
        exigirCambioEstructuralPermitido(leccion.getCurso(), "eliminar sus lecciones");
        if (contenidoService.tieneVersionEnRevision(contenidoId)) {
            throw new ReglaNegocioException("No se puede eliminar la lección mientras tenga una versión en revisión de accesibilidad. "
                    + "Espere la decisión del revisor.");
        }
        contenidoService.eliminarDependenciasDeLeccion(contenidoId);
        contenidoRepository.delete(leccion);
        contenidoRepository.flush();

        int orden = 1;
        for (Contenido restante : contenidoRepository.findByCursoIdOrderByOrdenAsc(cursoId)) {
            restante.setOrden(orden++);
            contenidoRepository.save(restante);
        }
    }

    @Transactional(readOnly = true)
    public long contarMatriculados(Long cursoId) {
        return matriculaRepository.countByCursoId(cursoId);
    }

    @Transactional(readOnly = true)
    public boolean tieneEvaluacion(Long cursoId) {
        return evaluacionRepository.findByCursoId(cursoId).isPresent();
    }

    @Transactional(readOnly = true)
    public List<Contenido> listarLeccionesDeCurso(Long cursoId) {
        return contenidoRepository.findByCursoIdOrderByOrdenAsc(cursoId);
    }

    public Contenido obtenerLeccion(Long contenidoId) {
        // Con su curso cargado: las vistas lo leen fuera de la transacción (open-in-view desactivado)
        return contenidoRepository.findConCursoById(contenidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Lección no encontrada con ID: " + contenidoId));
    }

    @Transactional
    public Contenido agregarLeccion(Long cursoId, String titulo, Integer orden, String correoGestor) {
        Aprendiz gestor = obtenerYValidarGestor(correoGestor);
        Curso curso = obtenerCurso(cursoId);
        exigirCambioEstructuralPermitido(curso, "agregarle lecciones");
        if (titulo == null || titulo.isBlank()) {
            throw new ReglaNegocioException("El título de la lección es obligatorio.");
        }
        // La lección siempre se agrega al final: la navegación del aprendiz recorre las lecciones 1, 2, 3… sin huecos
        orden = (int) contenidoRepository.countByCursoId(cursoId) + 1;

        Contenido contenido = new Contenido(titulo.trim(), orden, curso);
        contenido = contenidoRepository.save(contenido);

        // Crear automáticamente la versión 1 en estado BORRADOR
        ContenidoVersion v1 = new ContenidoVersion(contenido, 1, gestor);
        v1.setEstado(EstadoContenido.BORRADOR);
        v1.setCreadoEn(LocalDateTime.now());
        versionRepository.save(v1);

        return contenido;
    }

    public ContenidoVersion obtenerVersion(Long versionId) {
        return versionRepository.findById(versionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Versión de contenido no encontrada con ID: " + versionId));
    }

    @Transactional(readOnly = true)
    public List<ContenidoVersion> listarHistorialVersiones(Long contenidoId) {
        return versionRepository.findConDetalleByContenidoIdOrderByNumeroVersionDesc(contenidoId);
    }

    /** Versión con su lección, curso, autor y origen, para las vistas. */
    @Transactional(readOnly = true)
    public ContenidoVersion obtenerVersionDetalle(Long versionId) {
        return versionRepository.findDetalleById(versionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Versión de contenido no encontrada con ID: " + versionId));
    }

    @Transactional
    public ContenidoVersion crearNuevaVersionBorrador(Long contenidoId, String correoGestor) {
        Aprendiz gestor = obtenerYValidarGestor(correoGestor);
        Contenido contenido = obtenerLeccion(contenidoId);
        exigirSinVersionEnTramite(contenidoId);

        int siguienteNumero = versionRepository.findTopByContenidoIdOrderByNumeroVersionDesc(contenidoId)
                .map(v -> v.getNumeroVersion() + 1)
                .orElse(1);

        ContenidoVersion nuevaVersion = new ContenidoVersion(contenido, siguienteNumero, gestor);
        nuevaVersion.setEstado(EstadoContenido.BORRADOR);
        nuevaVersion.setCreadoEn(LocalDateTime.now());

        // Se parte de la versión vigente para corregirla sin volver a cargar todo
        Optional<ContenidoVersion> vigente = versionRepository.findByContenidoIdAndEstado(contenidoId, EstadoContenido.PUBLICADA);
        vigente.ifPresent(v -> {
            nuevaVersion.setTranscripcion(v.getTranscripcion());
            nuevaVersion.setRutaAudio(v.getRutaAudio());
            nuevaVersion.setFuenteMaterial(v.getFuenteMaterial());
        });
        ContenidoVersion guardada = versionRepository.save(nuevaVersion);
        vigente.ifPresent(v -> copiarImagenes(v, guardada));
        return guardada;
    }

    /**
     * Crea un nuevo borrador copiando una versión rechazada para su subsanación (ADR-08).
     */
    @Transactional
    public ContenidoVersion crearVersionDesdeRechazada(Long versionRechazadaId, String correoGestor) {
        Aprendiz gestor = obtenerYValidarGestor(correoGestor);
        ContenidoVersion versionRechazada = obtenerVersion(versionRechazadaId);

        if (versionRechazada.getEstado() != EstadoContenido.RECHAZADA) {
            throw new ReglaNegocioException("Solo se pueden subsanar versiones en estado RECHAZADA.");
        }
        exigirSinVersionEnTramite(versionRechazada.getContenido().getId());

        int siguienteNumero = versionRepository.findTopByContenidoIdOrderByNumeroVersionDesc(versionRechazada.getContenido().getId())
                .map(v -> v.getNumeroVersion() + 1)
                .orElse(versionRechazada.getNumeroVersion() + 1);

        ContenidoVersion nuevoBorrador = new ContenidoVersion(
                versionRechazada.getContenido(),
                siguienteNumero,
                gestor
        );
        nuevoBorrador.setEstado(EstadoContenido.BORRADOR);
        nuevoBorrador.setTranscripcion(versionRechazada.getTranscripcion());
        nuevoBorrador.setRutaAudio(versionRechazada.getRutaAudio());
        nuevoBorrador.setVersionOrigen(versionRechazada);
        nuevoBorrador.setFuenteMaterial(versionRechazada.getFuenteMaterial());
        nuevoBorrador.setCreadoEn(LocalDateTime.now());
        ContenidoVersion guardado = versionRepository.save(nuevoBorrador);
        copiarImagenes(versionRechazada, guardado);
        return guardado;
    }

    /**
     * Actualiza el borrador de una lección (transcripción y/o archivo de audio).
     */
    @Transactional
    public ContenidoVersion actualizarBorrador(Long versionId, String transcripcion,
                                              MultipartFile archivoAudio, String correoGestor) {
        return actualizarBorrador(versionId, transcripcion, archivoAudio, null, null, correoGestor);
    }

    /**
     * Actualiza el borrador incluyendo la fuente del material y la declaración de licencia (Ley 23 de 1982).
     * Si {@code licenciaDeclarada} es null, la fuente y la licencia no se modifican.
     */
    @Transactional
    public ContenidoVersion actualizarBorrador(Long versionId, String transcripcion, MultipartFile archivoAudio,
                                              String fuenteMaterial, Boolean licenciaDeclarada, String correoGestor) {
        obtenerYValidarGestor(correoGestor);
        ContenidoVersion version = obtenerVersion(versionId);

        if (version.getEstado() != EstadoContenido.BORRADOR) {
            throw new ReglaNegocioException("No se puede editar: las versiones en estado " +
                    version.getEstado() + " son de solo lectura (ADR-08).");
        }

        if (transcripcion != null) {
            version.setTranscripcion(transcripcion.trim());
        }

        if (archivoAudio != null && !archivoAudio.isEmpty()) {
            String rutaSaneada = validadorAudioService.validarYGuardar(archivoAudio);
            version.setRutaAudio(rutaSaneada);
        }
        if (licenciaDeclarada != null) {
            version.setFuenteMaterial(fuenteMaterial == null || fuenteMaterial.isBlank() ? null : fuenteMaterial.strip());
            if (!licenciaDeclarada) {
                version.setLicenciaDeclaradaEn(null);
            } else if (version.getLicenciaDeclaradaEn() == null) {
                version.setLicenciaDeclaradaEn(LocalDateTime.now());
            }
        }
        return versionRepository.save(version);
    }

    /**
     * Envía la versión a revisión técnica de accesibilidad.
     * Valida que cumpla con los requisitos mínimos no negociables (audio y transcripción).
     */
    @Transactional
    public ContenidoVersion enviarARevision(Long versionId, String correoGestor) {
        obtenerYValidarGestor(correoGestor);
        ContenidoVersion version = obtenerVersion(versionId);

        if (version.getEstado() != EstadoContenido.BORRADOR) {
            throw new ReglaNegocioException("Solo las versiones en estado BORRADOR pueden ser enviadas a revisión.");
        }

        if (version.getRutaAudio() == null || version.getRutaAudio().isBlank()) {
            throw new ReglaNegocioException("Requisito de accesibilidad no cumplido: La lección debe contar con un archivo de audio antes de ser enviada a revisión.");
        }

        if (version.getTranscripcion() == null || version.getTranscripcion().isBlank()) {
            throw new ReglaNegocioException("Requisito de accesibilidad no cumplido: La lección debe contar con la transcripción completa en texto antes de ser enviada a revisión.");
        }
        if (version.getFuenteMaterial() == null || version.getFuenteMaterial().isBlank() || version.getLicenciaDeclaradaEn() == null) {
            throw new ReglaNegocioException("Antes de enviar a revisión, registre la fuente del material y declare que cuenta con "
                    + "licencia o autorización de uso (Ley 23 de 1982).");
        }

        version.setEstado(EstadoContenido.EN_REVISION);
        version.setEnviadaARevisionEn(LocalDateTime.now());
        return versionRepository.save(version);
    }

    /**
     * Estado de la lección para los paneles: vigente, versiones en trámite y rechazada pendiente de subsanar.
     * Mostrar solo la última versión ocultaba, por ejemplo, una versión en revisión detrás de un borrador más nuevo.
     */
    @Transactional(readOnly = true)
    public EstadoLeccion estadoDeLeccion(Long contenidoId) {
        List<ContenidoVersion> versiones = versionRepository.findByContenidoIdOrderByNumeroVersionDesc(contenidoId);
        ContenidoVersion vigente = versiones.stream()
                .filter(v -> v.getEstado() == EstadoContenido.PUBLICADA).findFirst().orElse(null);
        List<ContenidoVersion> enTramite = versiones.stream()
                .filter(v -> ESTADOS_EN_TRAMITE.contains(v.getEstado()))
                .sorted(Comparator.comparing(ContenidoVersion::getNumeroVersion))
                .toList();
        // Una rechazada espera subsanación si es la versión más reciente de la lección
        ContenidoVersion rechazada = !versiones.isEmpty() && versiones.get(0).getEstado() == EstadoContenido.RECHAZADA
                ? versiones.get(0) : null;
        return new EstadoLeccion(vigente, enTramite, rechazada);
    }

    /**
     * Una lección tiene a lo sumo una versión en trámite (borrador o en revisión): con varias, el gestor
     * no sabe cuál está revisando el revisor y la segunda aprobación reemplazaría a la primera sin decidirlo.
     */
    private void exigirSinVersionEnTramite(Long contenidoId) {
        versionRepository.findByContenidoIdAndEstadoIn(contenidoId, ESTADOS_EN_TRAMITE).stream()
                .findFirst()
                .ifPresent(v -> {
                    throw new ReglaNegocioException("La lección ya tiene la versión " + v.getNumeroVersion() + " en estado "
                            + v.getEstado() + ". Termine ese trámite (o espere la decisión del revisor) antes de crear otra versión.");
                });
    }

    /** Copia las imágenes (con su texto alternativo) a la nueva versión; los archivos se comparten porque son inmutables. */
    private void copiarImagenes(ContenidoVersion origen, ContenidoVersion destino) {
        imagenRepository.findByContenidoVersionIdOrderByOrdenAsc(origen.getId())
                .forEach(imagen -> imagenRepository.save(imagen.copiarPara(destino)));
    }

    private void exigirFueraDelCatalogo(Curso curso, String accion) {
        if (curso.isActivo()) {
            throw new ReglaNegocioException("El curso está publicado en el catálogo. Retírelo del catálogo para " + accion
                    + ". Para corregir el contenido de una lección publicada, cree una nueva versión.");
        }
    }

    /** Agregar o eliminar lecciones, o eliminar el curso, cambia el recorrido de quien ya está matriculado. */
    private void exigirCambioEstructuralPermitido(Curso curso, String accion) {
        exigirFueraDelCatalogo(curso, accion);
        long matriculados = matriculaRepository.countByCursoId(curso.getId());
        if (matriculados > 0) {
            throw new ReglaNegocioException("No se puede " + accion + ": el curso tiene " + matriculados
                    + " aprendiz(es) matriculado(s) y cambiaría su recorrido. Para corregir contenido, cree una nueva versión de la lección.");
        }
    }

    private Aprendiz obtenerYValidarGestor(String correoGestor) {
        Aprendiz usuario = aprendizRepository.findByCorreo(correoGestor)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con correo: " + correoGestor));

        if (usuario.getRol() == Rol.REVISOR_ACCESIBILIDAD) {
            throw new AccesoDenegadoException("Separación de funciones: Un revisor de accesibilidad no puede crear ni editar contenidos.");
        }

        if (usuario.getRol() != Rol.GESTOR_CONTENIDO) {
            throw new AccesoDenegadoException("Acceso restringido: Solo los usuarios con rol GESTOR_CONTENIDO pueden realizar esta operación.");
        }

        return usuario;
    }
}
