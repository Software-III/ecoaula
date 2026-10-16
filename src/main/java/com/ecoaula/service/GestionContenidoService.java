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
import com.ecoaula.storage.ValidadorAudioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

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

    public GestionContenidoService(CursoRepository cursoRepository,
                                   ContenidoRepository contenidoRepository,
                                   ContenidoVersionRepository versionRepository,
                                   AprendizRepository aprendizRepository,
                                   ValidadorAudioService validadorAudioService) {
        this.cursoRepository = cursoRepository;
        this.contenidoRepository = contenidoRepository;
        this.versionRepository = versionRepository;
        this.aprendizRepository = aprendizRepository;
        this.validadorAudioService = validadorAudioService;
    }

    @Transactional(readOnly = true)
    public List<Curso> listarTodosLosCursos() {
        return cursoRepository.findAll();
    }

    public Curso obtenerCurso(Long cursoId) {
        return cursoRepository.findById(cursoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Curso no encontrado con ID: " + cursoId));
    }

    @Transactional
    public Curso crearCurso(String titulo, String descripcion, Integer horas, String area, String modalidad, boolean activo) {
        if (titulo == null || titulo.isBlank()) {
            throw new ReglaNegocioException("El título del curso es obligatorio.");
        }
        Curso curso = new Curso(titulo.trim(), descripcion, horas, area, modalidad);
        curso.setActivo(activo);
        return cursoRepository.save(curso);
    }

    @Transactional
    public Curso actualizarCurso(Long cursoId, String titulo, String descripcion, Integer horas, String area, String modalidad, boolean activo) {
        Curso curso = obtenerCurso(cursoId);
        if (titulo == null || titulo.isBlank()) {
            throw new ReglaNegocioException("El título del curso es obligatorio.");
        }
        curso.setTitulo(titulo.trim());
        curso.setDescripcion(descripcion);
        curso.setHoras(horas);
        curso.setArea(area);
        curso.setModalidad(modalidad);
        curso.setActivo(activo);
        return cursoRepository.save(curso);
    }

    @Transactional(readOnly = true)
    public List<Contenido> listarLeccionesDeCurso(Long cursoId) {
        return contenidoRepository.findByCursoIdOrderByOrdenAsc(cursoId);
    }

    public Contenido obtenerLeccion(Long contenidoId) {
        return contenidoRepository.findById(contenidoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Lección no encontrada con ID: " + contenidoId));
    }

    @Transactional
    public Contenido agregarLeccion(Long cursoId, String titulo, Integer orden, String correoGestor) {
        Aprendiz gestor = obtenerYValidarGestor(correoGestor);
        Curso curso = obtenerCurso(cursoId);

        if (titulo == null || titulo.isBlank()) {
            throw new ReglaNegocioException("El título de la lección es obligatorio.");
        }
        if (orden == null || orden < 1) {
            orden = (int) contenidoRepository.countByCursoId(cursoId) + 1;
        }

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
        return versionRepository.findByContenidoIdOrderByNumeroVersionDesc(contenidoId);
    }

    @Transactional
    public ContenidoVersion crearNuevaVersionBorrador(Long contenidoId, String correoGestor) {
        Aprendiz gestor = obtenerYValidarGestor(correoGestor);
        Contenido contenido = obtenerLeccion(contenidoId);

        int siguienteNumero = versionRepository.findTopByContenidoIdOrderByNumeroVersionDesc(contenidoId)
                .map(v -> v.getNumeroVersion() + 1)
                .orElse(1);

        ContenidoVersion nuevaVersion = new ContenidoVersion(contenido, siguienteNumero, gestor);
        nuevaVersion.setEstado(EstadoContenido.BORRADOR);
        nuevaVersion.setCreadoEn(LocalDateTime.now());
        return versionRepository.save(nuevaVersion);
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
        nuevoBorrador.setCreadoEn(LocalDateTime.now());

        return versionRepository.save(nuevoBorrador);
    }

    /**
     * Actualiza el borrador de una lección (transcripción y/o archivo de audio).
     */
    @Transactional
    public ContenidoVersion actualizarBorrador(Long versionId, String transcripcion,
                                              MultipartFile archivoAudio, String correoGestor) {
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

        version.setEstado(EstadoContenido.EN_REVISION);
        version.setEnviadaARevisionEn(LocalDateTime.now());
        return versionRepository.save(version);
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
