package com.ecoaula.service;

import com.ecoaula.controller.dto.ReporteBarreraForm;
import com.ecoaula.exception.EcoAulaException;
import com.ecoaula.exception.RecursoNoEncontradoException;
import com.ecoaula.model.Contenido;
import com.ecoaula.model.Curso;
import com.ecoaula.model.ReporteBarrera;
import com.ecoaula.model.enums.EstadoReporte;
import com.ecoaula.repository.ContenidoRepository;
import com.ecoaula.repository.CursoRepository;
import com.ecoaula.repository.ReporteBarreraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reportes de barreras de accesibilidad: los envía el aprendiz (con o sin sesión) y los atiende
 * el personal interno. Son una entrada del Proceso 2 y la fuente del indicador de incidencias.
 */
@Service
public class ReporteBarreraService {

    private final ReporteBarreraRepository reporteRepository;
    private final CursoRepository cursoRepository;
    private final ContenidoRepository contenidoRepository;

    public ReporteBarreraService(ReporteBarreraRepository reporteRepository,
                                 CursoRepository cursoRepository,
                                 ContenidoRepository contenidoRepository) {
        this.reporteRepository = reporteRepository;
        this.cursoRepository = cursoRepository;
        this.contenidoRepository = contenidoRepository;
    }

    /** Descripción legible del lugar del reporte: "Curso: X · Lección 2: Y", o null si no aplica. */
    @Transactional(readOnly = true)
    public String describirContexto(Long cursoId, Long contenidoId) {
        Curso curso = cursoId == null ? null : cursoRepository.findById(cursoId).orElse(null);
        Contenido leccion = leccionDelCurso(curso, contenidoId);
        if (curso == null) {
            return null;
        }
        return "Curso: " + curso.getTitulo()
                + (leccion == null ? "" : " · Lección " + leccion.getOrden() + ": " + leccion.getTitulo());
    }

    @Transactional
    public ReporteBarrera crear(String correoAprendiz, ReporteBarreraForm form) {
        // Solo se conservan un curso existente y una lección que pertenezca a ese curso
        Curso curso = form.getCursoId() == null ? null : cursoRepository.findById(form.getCursoId()).orElse(null);
        Contenido leccion = leccionDelCurso(curso, form.getContenidoId());
        return reporteRepository.save(new ReporteBarrera(correoAprendiz, vacioANulo(form.getCorreoContacto()), form.getEtapa(),
                curso == null ? null : curso.getId(), leccion == null ? null : leccion.getId(),
                describirContexto(form.getCursoId(), form.getContenidoId()),
                vacioANulo(form.getTecnologiaAsistencia()), form.getDescripcion().strip()));
    }

    private Contenido leccionDelCurso(Curso curso, Long contenidoId) {
        if (curso == null || contenidoId == null) {
            return null;
        }
        return contenidoRepository.findById(contenidoId)
                .filter(l -> l.getCurso().getId().equals(curso.getId()))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ReporteBarrera> listarTodos() {
        return reporteRepository.findAllByOrderByCreadoEnDesc();
    }

    @Transactional(readOnly = true)
    public List<ReporteBarrera> listarDeAprendiz(String correo) {
        return reporteRepository.findByCorreoAprendizOrderByCreadoEnDesc(correo);
    }

    @Transactional(readOnly = true)
    public ReporteBarrera obtener(Long id) {
        return reporteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reporte de barrera no encontrado: " + id));
    }

    /**
     * Cambia el estado del reporte. Para marcarlo como resuelto se exige una respuesta, que el
     * aprendiz ve en "Mis reportes".
     */
    @Transactional
    public ReporteBarrera atender(Long id, EstadoReporte estado, String respuesta, String correoPersonal) {
        ReporteBarrera reporte = obtener(id);
        if (estado == null) {
            throw new EcoAulaException("Seleccione el estado del reporte.");
        }
        String texto = vacioANulo(respuesta);
        if (estado == EstadoReporte.RESUELTO && texto == null) {
            throw new EcoAulaException("Escriba la respuesta para el aprendiz antes de marcar el reporte como resuelto.");
        }
        reporte.setEstado(estado);
        reporte.setRespuesta(texto);
        reporte.setAtendidoPor(correoPersonal);
        reporte.setResueltoEn(estado == EstadoReporte.RESUELTO ? LocalDateTime.now() : null);
        return reporteRepository.save(reporte);
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
