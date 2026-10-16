package com.ecoaula.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad que registra el avance de un aprendiz sobre una versión específica de una lección (ADR-08).
 * Garantiza trazabilidad de la versión exacta vista por el estudiante.
 */
@Entity
@Table(name = "avances_lecciones", uniqueConstraints = {
    @UniqueConstraint(name = "uk_avance_aprendiz_version", columnNames = {"aprendiz_id", "contenido_version_id"})
})
public class AvanceLeccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprendiz_id", nullable = false)
    private Aprendiz aprendiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contenido_version_id", nullable = false)
    private ContenidoVersion contenidoVersion;

    @Column(nullable = false)
    private LocalDateTime completadaEn;

    public AvanceLeccion() {
    }

    public AvanceLeccion(Aprendiz aprendiz, ContenidoVersion contenidoVersion) {
        this.aprendiz = aprendiz;
        this.contenidoVersion = contenidoVersion;
        this.completadaEn = LocalDateTime.now();
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

    public ContenidoVersion getContenidoVersion() {
        return contenidoVersion;
    }

    public void setContenidoVersion(ContenidoVersion contenidoVersion) {
        this.contenidoVersion = contenidoVersion;
    }

    public LocalDateTime getCompletadaEn() {
        return completadaEn;
    }

    public void setCompletadaEn(LocalDateTime completadaEn) {
        this.completadaEn = completadaEn;
    }
}
