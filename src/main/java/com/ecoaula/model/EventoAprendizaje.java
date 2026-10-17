package com.ecoaula.model;

import com.ecoaula.model.enums.TipoEvento;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Registro de un evento del recorrido del aprendiz (componente de analítica del aprendizaje).
 * Guarda identificadores sin llaves foráneas para que la bitácora se conserve aunque se elimine
 * un curso o una lección. No guarda datos sensibles: solo el correo de la cuenta, si la hay.
 */
@Entity
@Table(name = "eventos_aprendizaje", indexes = {
    @Index(name = "idx_evento_tipo_fecha", columnList = "tipo, fecha"),
    @Index(name = "idx_evento_correo", columnList = "correo")
})
public class EventoAprendizaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 40)
    private TipoEvento tipo;

    /** Correo de la cuenta; null mientras la persona aún no tiene cuenta (registro iniciado). */
    private String correo;

    /** Identificador de la visita, para relacionar eventos de una misma sesión de navegación. */
    @Column(nullable = false, length = 36)
    private String sesion;

    private Long cursoId;

    private Long contenidoId;

    @Column(nullable = false)
    private LocalDateTime fecha;

    public EventoAprendizaje() {
    }

    public EventoAprendizaje(TipoEvento tipo, String correo, String sesion, Long cursoId, Long contenidoId) {
        this.tipo = tipo;
        this.correo = correo;
        this.sesion = sesion;
        this.cursoId = cursoId;
        this.contenidoId = contenidoId;
        this.fecha = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public String getCorreo() {
        return correo;
    }

    public String getSesion() {
        return sesion;
    }

    public Long getCursoId() {
        return cursoId;
    }

    public Long getContenidoId() {
        return contenidoId;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
