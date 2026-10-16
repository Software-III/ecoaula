package com.ecoaula.model;

import com.ecoaula.model.enums.EstadoReporte;
import com.ecoaula.model.enums.EtapaReporte;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Reporte de una barrera de accesibilidad encontrada por un aprendiz (entrada del Proceso 2 y fuente
 * del indicador "Tasa de incidencias de accesibilidad reportadas"). Puede enviarse sin sesión, porque
 * la barrera puede impedir justamente registrarse o iniciar sesión.
 */
@Entity
@Table(name = "reportes_barrera")
public class ReporteBarrera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Correo de la cuenta que reporta; null si se reportó sin iniciar sesión. */
    private String correoAprendiz;

    /** Correo opcional para responder a quien reporta sin sesión. */
    private String correoContacto;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private EtapaReporte etapa;

    private Long cursoId;

    private Long contenidoId;

    /** Descripción del curso y la lección en el momento del reporte, por si luego se eliminan. */
    private String contexto;

    private String tecnologiaAsistencia;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private EstadoReporte estado = EstadoReporte.ABIERTO;

    @Column(columnDefinition = "TEXT")
    private String respuesta;

    private String atendidoPor;

    @Column(nullable = false)
    private LocalDateTime creadoEn;

    private LocalDateTime resueltoEn;

    public ReporteBarrera() {
    }

    public ReporteBarrera(String correoAprendiz, String correoContacto, EtapaReporte etapa, Long cursoId, Long contenidoId,
                          String contexto, String tecnologiaAsistencia, String descripcion) {
        this.correoAprendiz = correoAprendiz;
        this.correoContacto = correoContacto;
        this.etapa = etapa;
        this.cursoId = cursoId;
        this.contenidoId = contenidoId;
        this.contexto = contexto;
        this.tecnologiaAsistencia = tecnologiaAsistencia;
        this.descripcion = descripcion;
        this.creadoEn = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCorreoAprendiz() {
        return correoAprendiz;
    }

    public String getCorreoContacto() {
        return correoContacto;
    }

    public EtapaReporte getEtapa() {
        return etapa;
    }

    public Long getCursoId() {
        return cursoId;
    }

    public Long getContenidoId() {
        return contenidoId;
    }

    public String getContexto() {
        return contexto;
    }

    public String getTecnologiaAsistencia() {
        return tecnologiaAsistencia;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public EstadoReporte getEstado() {
        return estado;
    }

    public void setEstado(EstadoReporte estado) {
        this.estado = estado;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public void setRespuesta(String respuesta) {
        this.respuesta = respuesta;
    }

    public String getAtendidoPor() {
        return atendidoPor;
    }

    public void setAtendidoPor(String atendidoPor) {
        this.atendidoPor = atendidoPor;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public LocalDateTime getResueltoEn() {
        return resueltoEn;
    }

    public void setResueltoEn(LocalDateTime resueltoEn) {
        this.resueltoEn = resueltoEn;
    }
}
