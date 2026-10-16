package com.ecoaula.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad que representa el certificado de aprobación emitido a una matrícula (ADR-06).
 * Posee un código de verificación único e impredecible generado con SecureRandom.
 */
@Entity
@Table(name = "certificados")
public class Certificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matricula_id", nullable = false, unique = true)
    private Matricula matricula;

    @Column(nullable = false, unique = true, length = 32)
    private String codigoVerificacion;

    @Column(nullable = false)
    private LocalDateTime emitidoEn;

    public Certificado() {
    }

    public Certificado(Matricula matricula, String codigoVerificacion) {
        this.matricula = matricula;
        this.codigoVerificacion = codigoVerificacion;
        this.emitidoEn = LocalDateTime.now();
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

    public String getCodigoVerificacion() {
        return codigoVerificacion;
    }

    public void setCodigoVerificacion(String codigoVerificacion) {
        this.codigoVerificacion = codigoVerificacion;
    }

    public LocalDateTime getEmitidoEn() {
        return emitidoEn;
    }

    public void setEmitidoEn(LocalDateTime emitidoEn) {
        this.emitidoEn = emitidoEn;
    }
}
