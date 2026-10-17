package com.ecoaula.model;

import com.ecoaula.model.enums.EstadoMatricula;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que vincula a un aprendiz con un curso matriculado.
 * Cuenta con restricción única compuesta (aprendiz_id, curso_id) a nivel de base de datos.
 */
@Entity
@Table(name = "matriculas", uniqueConstraints = {
    @UniqueConstraint(name = "uk_matricula_aprendiz_curso", columnNames = {"aprendiz_id", "curso_id"})
})
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprendiz_id", nullable = false)
    private Aprendiz aprendiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoMatricula estado = EstadoMatricula.ACTIVA;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @OneToOne(mappedBy = "matricula", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Certificado certificado;

    @OneToMany(mappedBy = "matricula", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IntentoEvaluacion> intentosEvaluacion = new ArrayList<>();

    public Matricula() {
    }

    public Matricula(Aprendiz aprendiz, Curso curso) {
        this.aprendiz = aprendiz;
        this.curso = curso;
        this.estado = EstadoMatricula.ACTIVA;
        this.fecha = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Aprendiz getAprendiz() {
        return aprendiz;
    }

    public void setAprendiz(Aprendiz aprendiz) {
        this.aprendiz = aprendiz;
    }

    public Curso getCurso() {
        return curso;
    }

    public void setCurso(Curso curso) {
        this.curso = curso;
    }

    public EstadoMatricula getEstado() {
        return estado;
    }

    public void setEstado(EstadoMatricula estado) {
        this.estado = estado;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Certificado getCertificado() {
        return certificado;
    }

    public void setCertificado(Certificado certificado) {
        this.certificado = certificado;
        if (certificado != null) {
            certificado.setMatricula(this);
        }
    }

    public List<IntentoEvaluacion> getIntentosEvaluacion() {
        return intentosEvaluacion;
    }

    public void setIntentosEvaluacion(List<IntentoEvaluacion> intentosEvaluacion) {
        this.intentosEvaluacion = intentosEvaluacion;
    }
}
