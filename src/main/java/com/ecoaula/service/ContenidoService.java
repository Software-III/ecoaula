package com.ecoaula.service;

import com.ecoaula.exception.RecursoNoEncontradoException;
import com.ecoaula.model.Aprendiz;
import com.ecoaula.model.AvanceLeccion;
import com.ecoaula.model.Contenido;
import com.ecoaula.model.ContenidoVersion;
import com.ecoaula.model.enums.EstadoContenido;
import com.ecoaula.repository.AprendizRepository;
import com.ecoaula.repository.AvanceLeccionRepository;
import com.ecoaula.repository.ContenidoRepository;
import com.ecoaula.repository.ContenidoVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Servicio encargado del consumo y versionamiento inmutable de contenidos (ADR-08).
 * Asegura que el aprendiz conserve la versión que inició y registra la versión exacta consumida.
 */
@Service
public class ContenidoService {

    private final ContenidoRepository contenidoRepository;
    private final ContenidoVersionRepository versionRepository;
    private final AvanceLeccionRepository avanceRepository;
    private final AprendizRepository aprendizRepository;

    public ContenidoService(ContenidoRepository contenidoRepository,
                            ContenidoVersionRepository versionRepository,
                            AvanceLeccionRepository avanceRepository,
                            AprendizRepository aprendizRepository) {
        this.contenidoRepository = contenidoRepository;
        this.versionRepository = versionRepository;
        this.avanceRepository = avanceRepository;
        this.aprendizRepository = aprendizRepository;
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
                    // Conserva la versión exacta que ya había iniciado/completado (ADR-08)
                    return avanceOpt.get().getContenidoVersion();
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

    @Transactional(readOnly = true)
    public boolean estaLeccionCompletada(String correoAprendiz, Long contenidoId) {
        return aprendizRepository.findByCorreo(correoAprendiz)
                .flatMap(a -> avanceRepository.findByAprendizIdAndContenidoId(a.getId(), contenidoId))
                .isPresent();
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

    public byte[] generarArchivoTranscripcion(String tituloCurso, String tituloLeccion, int orden, String textoTranscripcion) {
        StringBuilder sb = new StringBuilder();
        sb.append("=================================================================\n");
        sb.append("EcoAula — Plataforma de Aprendizaje Accesible\n");
        sb.append("Curso: ").append(tituloCurso).append("\n");
        sb.append("Lección ").append(orden).append(": ").append(tituloLeccion).append("\n");
        sb.append("=================================================================\n\n");
        sb.append("TRANSCRIPCIÓN COMPLETA DEL AUDIO:\n\n");
        sb.append(textoTranscripcion).append("\n\n");
        sb.append("-----------------------------------------------------------------\n");
        sb.append("Documento generado para lectura accesible y estudio sin conexión.\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
