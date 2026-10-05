package com.ecoaula.model;

import com.ecoaula.model.enums.EstadoContenido;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad que representa una versión inmutable de una lección (ADR-08).
 * El contenido nunca se sobrescribe; solo existe una versión PUBLICADA a la vez por contenido.
 */
@Entity
@Table(name = "contenido_versiones")
public class ContenidoVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contenido_id", nullable = false)
    private Contenido contenido;

    @Column(nullable = false)
    private Integer numeroVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoContenido estado = EstadoContenido.BORRADOR;

    @Column(nullable = false)
    private String rutaAudio;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String transcripcion;

    private LocalDateTime publicadaEn;

    public ContenidoVersion() {
    }

    public ContenidoVersion(Contenido contenido, Integer numeroVersion, EstadoContenido estado,
                            String rutaAudio, String transcripcion, LocalDateTime publicadaEn) {
        this.contenido = contenido;
        this.numeroVersion = numeroVersion;
        this.estado = estado;
        this.rutaAudio = rutaAudio;
        this.transcripcion = transcripcion;
        this.publicadaEn = publicadaEn;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Contenido getContenido() {
        return contenido;
    }

    public void setContenido(Contenido contenido) {
        this.contenido = contenido;
    }

    public Integer getNumeroVersion() {
        return numeroVersion;
    }

    public void setNumeroVersion(Integer numeroVersion) {
        this.numeroVersion = numeroVersion;
    }

    public EstadoContenido getEstado() {
        return estado;
    }

    public void setEstado(EstadoContenido estado) {
        this.estado = estado;
    }

    public String getRutaAudio() {
        return rutaAudio;
    }

    public void setRutaAudio(String rutaAudio) {
        this.rutaAudio = rutaAudio;
    }

    public String getTranscripcion() {
        return transcripcion;
    }

    public void setTranscripcion(String transcripcion) {
        this.transcripcion = transcripcion;
    }

    public LocalDateTime getPublicadaEn() {
        return publicadaEn;
    }

    public void setPublicadaEn(LocalDateTime publicadaEn) {
        this.publicadaEn = publicadaEn;
    }
}
