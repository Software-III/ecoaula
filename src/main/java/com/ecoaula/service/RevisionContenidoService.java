package com.ecoaula.service;

import com.ecoaula.exception.AccesoDenegadoException;
import com.ecoaula.exception.RecursoNoEncontradoException;
import com.ecoaula.exception.ReglaNegocioException;
import com.ecoaula.model.Aprendiz;
import com.ecoaula.model.ContenidoVersion;
import com.ecoaula.model.RevisionAccesibilidad;
import com.ecoaula.model.enums.CriterioAccesibilidad;
import com.ecoaula.model.enums.EstadoContenido;
import com.ecoaula.model.enums.ResultadoRevision;
import com.ecoaula.model.enums.Rol;
import com.ecoaula.repository.AprendizRepository;
import com.ecoaula.repository.ContenidoVersionRepository;
import com.ecoaula.repository.RevisionAccesibilidadRepository;
import com.ecoaula.service.dto.IndicadoresCalidad;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio encargado de la auditoría técnica de accesibilidad, aprobación/publicación
 * e indicadores de calidad según ADR-08 y WCAG 2.1 AA / NTC 5854.
 */
@Service
public class RevisionContenidoService {

    private final ContenidoVersionRepository versionRepository;
    private final RevisionAccesibilidadRepository revisionRepository;
    private final AprendizRepository aprendizRepository;

    public RevisionContenidoService(ContenidoVersionRepository versionRepository,
                                    RevisionAccesibilidadRepository revisionRepository,
                                    AprendizRepository aprendizRepository) {
        this.versionRepository = versionRepository;
        this.revisionRepository = revisionRepository;
        this.aprendizRepository = aprendizRepository;
    }

    /**
     * Retorna la cola de versiones pendientes de revisión técnica,
     * ordenadas por fecha de envío (más antiguas primero).
     */
    @Transactional(readOnly = true)
    public List<ContenidoVersion> obtenerColaRevision() {
        return versionRepository.findByEstadoOrderByEnviadaARevisionEnAsc(EstadoContenido.EN_REVISION);
    }

    @Transactional(readOnly = true)
    public ContenidoVersion obtenerVersionParaRevision(Long versionId) {
        return versionRepository.findById(versionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Versión no encontrada con ID: " + versionId));
    }

    /**
     * Aprueba la versión técnica de accesibilidad y publica el contenido.
     * Si existía una versión previamente PUBLICADA, pasa a RETIRADA inmutablemente (ADR-08).
     */
    @Transactional
    public RevisionAccesibilidad aprobarYPublicar(Long versionId, String correoRevisor, Set<String> itemsChequeados) {
        Aprendiz revisor = obtenerYValidarRevisor(correoRevisor);
        ContenidoVersion version = obtenerVersionParaRevision(versionId);

        validarSeparacionFunciones(version, revisor);

        if (version.getEstado() != EstadoContenido.EN_REVISION) {
            throw new ReglaNegocioException("Solo se pueden aprobar versiones en estado EN_REVISION.");
        }

        if (!CriterioAccesibilidad.cumpleTodos(itemsChequeados)) {
            throw new ReglaNegocioException("No se puede aprobar la publicación: todos los criterios de la lista de chequeo de accesibilidad deben estar marcados como conformes.");
        }

        // ADR-08: Retirar versión publicada anterior si existe
        Optional<ContenidoVersion> versionPublicadaAnterior = versionRepository.findByContenidoIdAndEstado(
                version.getContenido().getId(),
                EstadoContenido.PUBLICADA
        );
        versionPublicadaAnterior.ifPresent(v -> {
            v.setEstado(EstadoContenido.RETIRADA);
            versionRepository.save(v);
        });

        // Publicar la nueva versión
        version.setEstado(EstadoContenido.PUBLICADA);
        version.setPublicadaEn(LocalDateTime.now());
        version.setMotivoRechazo(null);
        versionRepository.save(version);

        // Registrar auditoría de revisión
        RevisionAccesibilidad revision = new RevisionAccesibilidad(
                version,
                revisor,
                ResultadoRevision.APROBADA,
                null,
                itemsChequeados
        );

        return revisionRepository.save(revision);
    }

    /**
     * Rechaza la versión de contenido registrando el motivo obligatorio para el gestor.
     */
    @Transactional
    public RevisionAccesibilidad rechazar(Long versionId, String correoRevisor, String motivo, Set<String> itemsChequeados) {
        Aprendiz revisor = obtenerYValidarRevisor(correoRevisor);
        ContenidoVersion version = obtenerVersionParaRevision(versionId);

        validarSeparacionFunciones(version, revisor);

        if (version.getEstado() != EstadoContenido.EN_REVISION) {
            throw new ReglaNegocioException("Solo se pueden rechazar versiones en estado EN_REVISION.");
        }

        if (motivo == null || motivo.trim().isBlank()) {
            throw new ReglaNegocioException("El motivo de rechazo es obligatorio para orientar al gestor en la subsanación.");
        }

        version.setEstado(EstadoContenido.RECHAZADA);
        version.setMotivoRechazo(motivo.trim());
        versionRepository.save(version);

        RevisionAccesibilidad revision = new RevisionAccesibilidad(
                version,
                revisor,
                ResultadoRevision.RECHAZADA,
                motivo.trim(),
                itemsChequeados
        );

        return revisionRepository.save(revision);
    }

    /**
     * Calcula los dos indicadores del Plan de Calidad y Accesibilidad:
     * 1. Tasa de accesibilidad en la primera revisión
     * 2. Tiempo promedio de publicación
     */
    @Transactional(readOnly = true)
    public IndicadoresCalidad calcularIndicadores() {
        List<RevisionAccesibilidad> todasRevisiones = revisionRepository.findAllByOrderByFechaDesc();

        // Agrupar revisiones por versión (id)
        Map<Long, List<RevisionAccesibilidad>> revisionesPorVersion = todasRevisiones.stream()
                .collect(Collectors.groupingBy(r -> r.getVersion().getId()));

        long totalVersionesRevisadas = revisionesPorVersion.size();
        long aprobadasPrimeraRevision = 0;

        for (List<RevisionAccesibilidad> lista : revisionesPorVersion.values()) {
            // Ordenar por fecha ascendente para obtener la primera revisión
            lista.sort(Comparator.comparing(RevisionAccesibilidad::getFecha));
            if (!lista.isEmpty() && lista.get(0).getResultado() == ResultadoRevision.APROBADA) {
                aprobadasPrimeraRevision++;
            }
        }

        double tasa = totalVersionesRevisadas > 0
                ? ((double) aprobadasPrimeraRevision / totalVersionesRevisadas) * 100.0
                : 0.0;

        // Calcular tiempo promedio de publicación para versiones publicadas o retiradas
        List<ContenidoVersion> publicadas = versionRepository.findByEstado(EstadoContenido.PUBLICADA);
        List<ContenidoVersion> retiradas = versionRepository.findByEstado(EstadoContenido.RETIRADA);

        List<ContenidoVersion> todasPublicadas = new ArrayList<>(publicadas);
        todasPublicadas.addAll(retiradas);

        double sumaHoras = 0.0;
        long contadasParaTiempo = 0;

        for (ContenidoVersion v : todasPublicadas) {
            if (v.getPublicadaEn() != null && v.getCreadoEn() != null) {
                Duration duracion = Duration.between(v.getCreadoEn(), v.getPublicadaEn());
                if (!duracion.isNegative()) {
                    sumaHoras += duracion.toMinutes() / 60.0;
                    contadasParaTiempo++;
                }
            }
        }

        double tiempoPromedioHoras = contadasParaTiempo > 0
                ? (sumaHoras / contadasParaTiempo)
                : 0.0;

        return new IndicadoresCalidad(
                tasa,
                totalVersionesRevisadas,
                aprobadasPrimeraRevision,
                tiempoPromedioHoras,
                todasPublicadas.size()
        );
    }

    private void validarSeparacionFunciones(ContenidoVersion version, Aprendiz revisor) {
        if (version.getCreadoPor() != null && version.getCreadoPor().getId().equals(revisor.getId())) {
            throw new AccesoDenegadoException("Separación de funciones: Un usuario no puede revisar una versión que él mismo creó.");
        }
    }

    private Aprendiz obtenerYValidarRevisor(String correoRevisor) {
        Aprendiz usuario = aprendizRepository.findByCorreo(correoRevisor)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con correo: " + correoRevisor));

        if (usuario.getRol() == Rol.GESTOR_CONTENIDO) {
            throw new AccesoDenegadoException("Separación de funciones: Un gestor de contenido no puede aprobar ni rechazar revisiones.");
        }

        if (usuario.getRol() != Rol.REVISOR_ACCESIBILIDAD) {
            throw new AccesoDenegadoException("Acceso restringido: Solo los usuarios con rol REVISOR_ACCESIBILIDAD pueden auditar lecciones.");
        }

        return usuario;
    }
}
