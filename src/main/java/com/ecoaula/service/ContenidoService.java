package com.ecoaula.service;

import com.ecoaula.exception.AccesoDenegadoException;
import com.ecoaula.exception.ReglaNegocioException;
import com.ecoaula.exception.RecursoNoEncontradoException;
import com.ecoaula.model.Aprendiz;
import com.ecoaula.model.AvanceLeccion;
import com.ecoaula.model.Contenido;
import com.ecoaula.model.ContenidoVersion;
import com.ecoaula.model.RecursoImagen;
import com.ecoaula.model.enums.EstadoContenido;
import com.ecoaula.repository.AprendizRepository;
import com.ecoaula.repository.AvanceLeccionRepository;
import com.ecoaula.repository.ContenidoRepository;
import com.ecoaula.repository.ContenidoVersionRepository;
import com.ecoaula.repository.MatriculaRepository;
import com.ecoaula.repository.RecursoImagenRepository;
import com.ecoaula.repository.RevisionAccesibilidadRepository;
import com.ecoaula.service.dto.AvanceCurso;
import com.ecoaula.storage.AlmacenamientoContenido;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio del consumo de contenido por el aprendiz (Proceso 2, ADR-08): entrega la versión exacta que
 * inició, registra su avance validado, sirve imágenes con alternativa textual y controla el acceso a los
 * archivos. El flujo de producción, revisión y publicación vive en GestionContenidoService y
 * RevisionContenidoService.
 */
@Service
public class ContenidoService {

    private final ContenidoRepository contenidoRepository;
    private final ContenidoVersionRepository versionRepository;
    private final AvanceLeccionRepository avanceRepository;
    private final AprendizRepository aprendizRepository;
    private final AlmacenamientoContenido almacenamientoContenido;
    private final RevisionAccesibilidadRepository revisionRepository;
    private final RecursoImagenRepository imagenRepository;
    private final MatriculaRepository matriculaRepository;

    /** Estados de una versión que el aprendiz puede consumir: la vigente o la que ya había iniciado. */
    private static final Set<EstadoContenido> ESTADOS_VISIBLES_PARA_APRENDIZ =
            EnumSet.of(EstadoContenido.PUBLICADA, EstadoContenido.RETIRADA);

    public ContenidoService(ContenidoRepository contenidoRepository,
                            ContenidoVersionRepository versionRepository,
                            AvanceLeccionRepository avanceRepository,
                            AprendizRepository aprendizRepository,
                            AlmacenamientoContenido almacenamientoContenido,
                            RevisionAccesibilidadRepository revisionRepository,
                            RecursoImagenRepository imagenRepository,
                            MatriculaRepository matriculaRepository) {
        this.contenidoRepository = contenidoRepository;
        this.versionRepository = versionRepository;
        this.avanceRepository = avanceRepository;
        this.aprendizRepository = aprendizRepository;
        this.almacenamientoContenido = almacenamientoContenido;
        this.revisionRepository = revisionRepository;
        this.imagenRepository = imagenRepository;
        this.matriculaRepository = matriculaRepository;
    }

    @Transactional(readOnly = true)
    public Contenido obtenerLeccionPorOrden(Long cursoId, Integer orden) {
        return contenidoRepository.findByCursoIdAndOrden(cursoId, orden)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la lección orden " + orden + " en el curso " + cursoId));
    }

    @Transactional(readOnly = true)
    public long contarLeccionesEnCurso(Long cursoId) {
        return contenidoRepository.countByCursoId(cursoId);
    }

    @Transactional(readOnly = true)
    public ContenidoVersion obtenerVersionParaAprendiz(Long contenidoId, String correoAprendiz) {
        if (correoAprendiz != null) {
            Optional<Aprendiz> aprendizOpt = aprendizRepository.findByCorreo(correoAprendiz);
            if (aprendizOpt.isPresent()) {
                Optional<AvanceLeccion> avanceOpt = avanceRepository.findByAprendizIdAndContenidoId(aprendizOpt.get().getId(), contenidoId);
                if (avanceOpt.isPresent()) {
                    // Conserva la versión exacta que ya había iniciado/completado (ADR-08).
                    // La relación del avance es LAZY: se carga aquí, dentro de la transacción, porque la vista
                    // la lee después (open-in-view está desactivado).
                    return (ContenidoVersion) Hibernate.unproxy(avanceOpt.get().getContenidoVersion());
                }
            }
        }

        // Si no la ha iniciado, entrega la versión vigente (PUBLICADA)
        return versionRepository.findByContenidoIdAndEstado(contenidoId, EstadoContenido.PUBLICADA)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe versión publicada para el contenido ID: " + contenidoId));
    }

    @Transactional
    public AvanceLeccion registrarAvance(String correoAprendiz, Long versionId) {
        Aprendiz aprendiz = aprendizRepository.findByCorreo(correoAprendiz)
                .orElseThrow(() -> new RecursoNoEncontradoException("Aprendiz no encontrado: " + correoAprendiz));

        ContenidoVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Versión de contenido no encontrada: " + versionId));

        return avanceRepository.findByAprendizIdAndContenidoVersionId(aprendiz.getId(), version.getId())
                .orElseGet(() -> {
                    AvanceLeccion nuevoAvance = new AvanceLeccion(aprendiz, version);
                    return avanceRepository.save(nuevoAvance);
                });
    }

    /**
     * Registra el avance de una lección validando que el aprendiz esté matriculado y que la versión
     * recibida sea exactamente la que se le entregó (KPI "Exactitud del registro de avance", meta 100 %).
     */
    @Transactional
    public AvanceLeccion registrarAvanceEnLeccion(String correoAprendiz, Long cursoId, Integer orden, Long versionId) {
        verificarMatricula(correoAprendiz, cursoId);
        Contenido leccion = obtenerLeccionPorOrden(cursoId, orden);
        ContenidoVersion versionEntregada = obtenerVersionParaAprendiz(leccion.getId(), correoAprendiz);
        if (!versionEntregada.getId().equals(versionId)) {
            throw new ReglaNegocioException("La versión indicada no corresponde a la lección que está estudiando. "
                    + "Vuelva a abrir la lección e intente de nuevo.");
        }
        return registrarAvance(correoAprendiz, versionId);
    }

    @Transactional(readOnly = true)
    public void verificarMatricula(String correoAprendiz, Long cursoId) {
        if (!matriculaRepository.existsByAprendizCorreoAndCursoId(correoAprendiz, cursoId)) {
            throw new AccesoDenegadoException("Debe matricularse en el curso para acceder a sus lecciones.");
        }
    }

    @Transactional(readOnly = true)
    public boolean estaLeccionCompletada(String correoAprendiz, Long contenidoId) {
        return aprendizRepository.findByCorreo(correoAprendiz)
                .flatMap(a -> avanceRepository.findByAprendizIdAndContenidoId(a.getId(), contenidoId))
                .isPresent();
    }

    @Transactional(readOnly = true)
    public AvanceCurso obtenerAvanceCurso(String correoAprendiz, Long cursoId) {
        Set<Long> completadas = Set.copyOf(avanceRepository.findLeccionesCompletadasEnCurso(correoAprendiz, cursoId));
        return new AvanceCurso(contenidoRepository.findByCursoIdOrderByOrdenAsc(cursoId).stream()
                .map(l -> new AvanceCurso.Leccion(l.getOrden(), l.getTitulo(), completadas.contains(l.getId())))
                .toList());
    }

    @Transactional(readOnly = true)
    public boolean todasLeccionesCompletadas(String correoAprendiz, Long cursoId) {
        Aprendiz aprendiz = aprendizRepository.findByCorreo(correoAprendiz)
                .orElseThrow(() -> new RecursoNoEncontradoException("Aprendiz no encontrado: " + correoAprendiz));

        long totalLecciones = contenidoRepository.countByCursoId(cursoId);
        if (totalLecciones == 0) {
            return false;
        }

        long completadas = avanceRepository.contarLeccionesCompletadasEnCurso(aprendiz.getId(), cursoId);
        return completadas >= totalLecciones;
    }

    public byte[] generarArchivoTranscripcion(String tituloCurso, String tituloLeccion, int orden, String textoTranscripcion,
                                              List<RecursoImagen> imagenes) {
        StringBuilder sb = new StringBuilder();
        sb.append("=================================================================\n");
        sb.append("EcoAula — Plataforma de Aprendizaje Accesible\n");
        sb.append("Curso: ").append(tituloCurso).append("\n");
        sb.append("Lección ").append(orden).append(": ").append(tituloLeccion).append("\n");
        sb.append("=================================================================\n\n");
        sb.append("TRANSCRIPCIÓN COMPLETA DEL AUDIO:\n\n");
        sb.append(textoTranscripcion).append("\n\n");
        if (imagenes != null && !imagenes.isEmpty()) {
            sb.append("DESCRIPCIÓN DE LAS IMÁGENES DE LA LECCIÓN:\n\n");
            int n = 1;
            for (RecursoImagen imagen : imagenes) {
                sb.append("Imagen ").append(n++).append(": ").append(imagen.getTextoAlternativo()).append("\n");
                if (imagen.getDescripcionDetallada() != null) {
                    sb.append(imagen.getDescripcionDetallada()).append("\n");
                }
                sb.append("\n");
            }
        }
        sb.append("-----------------------------------------------------------------\n");
        sb.append("Documento generado para lectura accesible y estudio sin conexión.\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }


    // ------------------------------------------------------------------
    // Versión vigente, imágenes con alternativa textual (WCAG 1.1.1) y acceso a archivos
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Optional<ContenidoVersion> obtenerVersionVigente(Long contenidoId) {
        return versionRepository.findByContenidoIdAndEstado(contenidoId, EstadoContenido.PUBLICADA);
    }

    @Transactional(readOnly = true)
    public List<RecursoImagen> listarImagenes(Long versionId) {
        return imagenRepository.findByContenidoVersionIdOrderByOrdenAsc(versionId);
    }

    @Transactional
    public RecursoImagen agregarImagen(Long versionId, String correoGestor, String nombreArchivo, byte[] imagen,
                                       String textoAlternativo, String descripcionDetallada) {
        ContenidoVersion version = obtenerBorrador(versionId);

        if (imagen == null || imagen.length == 0) {
            throw new ReglaNegocioException("Seleccione el archivo de imagen que desea agregar.");
        }
        if (textoAlternativo == null || textoAlternativo.isBlank()) {
            throw new ReglaNegocioException("El texto alternativo es obligatorio: describe la imagen a quien no puede verla (WCAG 1.1.1).");
        }
        String alt = textoAlternativo.strip();
        if (alt.length() > RecursoImagen.MAX_TEXTO_ALTERNATIVO) {
            throw new ReglaNegocioException("El texto alternativo admite máximo " + RecursoImagen.MAX_TEXTO_ALTERNATIVO
                    + " caracteres. Si la imagen necesita más explicación (gráficos, tablas), use la descripción detallada.");
        }

        String extension = extensionImagen(nombreArchivo, imagen);
        String ruta = almacenamientoContenido.guardarArchivo("imagen-" + version.getContenido().getId() + "-v"
                + version.getNumeroVersion() + "-" + UUID.randomUUID().toString().substring(0, 8) + "." + extension, imagen);
        int orden = imagenRepository.findTopByContenidoVersionIdOrderByOrdenDesc(versionId)
                .map(i -> i.getOrden() + 1)
                .orElse(1);

        String descripcion = descripcionDetallada == null || descripcionDetallada.isBlank() ? null : descripcionDetallada.strip();
        return imagenRepository.save(new RecursoImagen(version, ruta, alt, descripcion, orden));
    }

    @Transactional
    public void quitarImagen(Long versionId, Long imagenId, String correoGestor) {
        obtenerBorrador(versionId);
        RecursoImagen imagen = imagenRepository.findById(imagenId)
                .filter(i -> i.getContenidoVersion().getId().equals(versionId))
                .orElseThrow(() -> new RecursoNoEncontradoException("La imagen no pertenece a esta versión."));

        imagenRepository.delete(imagen);
    }

    @Transactional(readOnly = true)
    public void verificarAccesoAudio(String rutaArchivo, String correo, boolean esPersonalDeContenido) {
        boolean permitido = esPersonalDeContenido
                ? versionRepository.existsByRutaAudio(rutaArchivo)
                : versionRepository.aprendizPuedeEscuchar(rutaArchivo, correo, ESTADOS_VISIBLES_PARA_APRENDIZ);
        if (!permitido) {
            throw new RecursoNoEncontradoException("Archivo de audio no encontrado: " + rutaArchivo);
        }
    }

    @Transactional(readOnly = true)
    public void verificarAccesoImagen(String rutaArchivo, String correo, boolean esPersonalDeContenido) {
        boolean permitido = esPersonalDeContenido
                ? imagenRepository.existsByRutaArchivo(rutaArchivo)
                : imagenRepository.aprendizPuedeVer(rutaArchivo, correo, ESTADOS_VISIBLES_PARA_APRENDIZ);
        if (!permitido) {
            throw new RecursoNoEncontradoException("Imagen no encontrada: " + rutaArchivo);
        }
    }

    /**
     * Elimina lo que depende de las versiones de una lección y no se borra en cascada
     * (imágenes e informes de revisión). Lo usa el catálogo antes de eliminar una lección o un curso
     * que nunca llegó a aprendices.
     */
    @Transactional
    public void eliminarDependenciasDeLeccion(Long contenidoId) {
        imagenRepository.deleteByContenidoVersionContenidoId(contenidoId);
        revisionRepository.deleteByVersionContenidoId(contenidoId);
    }

    @Transactional(readOnly = true)
    public boolean tieneVersionEnRevision(Long contenidoId) {
        return versionRepository.existsByContenidoIdAndEstado(contenidoId, EstadoContenido.EN_REVISION);
    }

    @Transactional(readOnly = true)
    public boolean cursoTieneVersionEnRevision(Long cursoId) {
        return versionRepository.existsByContenidoCursoIdAndEstado(cursoId, EstadoContenido.EN_REVISION);
    }

    private ContenidoVersion obtenerVersion(Long versionId) {
        return versionRepository.findById(versionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Versión de contenido no encontrada: " + versionId));
    }

    /**
     * Acepta solo PNG, JPEG o WebP, verificando tanto la extensión como la firma del archivo.
     */
    private String extensionImagen(String nombreOriginal, byte[] imagen) {
        String nombre = nombreOriginal == null ? "" : nombreOriginal.toLowerCase(Locale.ROOT);
        boolean firmaPng = imagen.length >= 8 && (imagen[0] & 0xFF) == 0x89 && imagen[1] == 'P' && imagen[2] == 'N' && imagen[3] == 'G';
        boolean firmaJpeg = imagen.length >= 3 && (imagen[0] & 0xFF) == 0xFF && (imagen[1] & 0xFF) == 0xD8 && (imagen[2] & 0xFF) == 0xFF;
        boolean firmaWebp = imagen.length >= 12 && imagen[0] == 'R' && imagen[1] == 'I' && imagen[2] == 'F' && imagen[3] == 'F'
                && imagen[8] == 'W' && imagen[9] == 'E' && imagen[10] == 'B' && imagen[11] == 'P';

        if (nombre.endsWith(".png") && firmaPng) {
            return "png";
        }
        if ((nombre.endsWith(".jpg") || nombre.endsWith(".jpeg")) && firmaJpeg) {
            return "jpg";
        }
        if (nombre.endsWith(".webp") && firmaWebp) {
            return "webp";
        }
        throw new ReglaNegocioException("La imagen debe ser un archivo PNG, JPEG o WebP válido.");
    }

    private ContenidoVersion obtenerBorrador(Long versionId) {
        ContenidoVersion version = obtenerVersion(versionId);
        if (version.getEstado() != EstadoContenido.BORRADOR) {
            throw new ReglaNegocioException("Solo se pueden modificar las imágenes de una versión en BORRADOR (ADR-08).");
        }
        return version;
    }
}
