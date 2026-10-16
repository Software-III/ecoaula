package com.ecoaula.model;

import com.ecoaula.model.enums.EstadoIntento;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que registra un intento de presentación de evaluación por parte del aprendiz.
 */
@Entity
@Table(name = "intentos_evaluacion")
public class IntentoEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matricula_id", nullable = false)
    private Matricula matricula;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoIntento estado = EstadoIntento.EN_CURSO;

    private Double puntaje;

    private Boolean aprobado;

    @Column(nullable = false)
    private LocalDateTime fechaInicio;

    private LocalDateTime fechaFin;

    @OneToMany(mappedBy = "intento", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RespuestaIntento> respuestas = new ArrayList<>();

    public IntentoEvaluacion() {
    }

    public IntentoEvaluacion(Matricula matricula) {
        this.matricula = matricula;
        this.estado = EstadoIntento.EN_CURSO;
        this.fechaInicio = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Matricula getMatricula() {
        return matricula;
    }

    public void setMatricula(Matricula matricula) {
        this.matricula = matricula;
    }

    public EstadoIntento getEstado() {
        return estado;
    }

    public void setEstado(EstadoIntento estado) {
        this.estado = estado;
    }

    public Double getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(Double puntaje) {
        this.puntaje = puntaje;
    }

    public Boolean getAprobado() {
        return aprobado;
    }

    public void setAprobado(Boolean aprobado) {
        this.aprobado = aprobado;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public List<RespuestaIntento> getRespuestas() {
        return respuestas;
    }

    public void setRespuestas(List<RespuestaIntento> respuestas) {
        this.respuestas = respuestas;
    }
}
