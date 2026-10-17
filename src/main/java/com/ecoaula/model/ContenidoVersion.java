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

    @Column(nullable = true)
    private String rutaAudio;

    @Column(nullable = true, columnDefinition = "LONGTEXT")
    private String transcripcion;

    private LocalDateTime publicadaEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_id", nullable = true)
    private Aprendiz creadoPor;

    @Column(nullable = false)
    private LocalDateTime creadoEn;

    private LocalDateTime enviadaARevisionEn;

    @Column(columnDefinition = "TEXT")
    private String motivoRechazo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_origen_id", nullable = true)
    private ContenidoVersion versionOrigen;

    // Material fuente con licencia o autorización de uso (Ley 23 de 1982)
    @Column(columnDefinition = "TEXT")
    private String fuenteMaterial;

    private LocalDateTime licenciaDeclaradaEn;

    @PrePersist
    protected void onPrePersist() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
        if (estado == null) {
            estado = EstadoContenido.BORRADOR;
        }
    }

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
        this.creadoEn = LocalDateTime.now();
    }

    public ContenidoVersion(Contenido contenido, Integer numeroVersion, Aprendiz creadoPor) {
        this.contenido = contenido;
        this.numeroVersion = numeroVersion;
        this.creadoPor = creadoPor;
        this.estado = EstadoContenido.BORRADOR;
        this.creadoEn = LocalDateTime.now();
    }

    public boolean tieneAudioYTranscripcion() {
        return rutaAudio != null && !rutaAudio.isBlank() &&
               transcripcion != null && !transcripcion.isBlank();
    }

    public boolean esBorrador() {
        return estado == EstadoContenido.BORRADOR;
    }

    public boolean esEnRevision() {
        return estado == EstadoContenido.EN_REVISION;
    }

    public boolean esPublicada() {
        return estado == EstadoContenido.PUBLICADA;
    }

    public boolean esRechazada() {
        return estado == EstadoContenido.RECHAZADA;
    }

    public boolean esRetirada() {
        return estado == EstadoContenido.RETIRADA;
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

    public Aprendiz getCreadoPor() {
        return creadoPor;
    }

    public void setCreadoPor(Aprendiz creadoPor) {
        this.creadoPor = creadoPor;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public LocalDateTime getEnviadaARevisionEn() {
        return enviadaARevisionEn;
    }

    public void setEnviadaARevisionEn(LocalDateTime enviadaARevisionEn) {
        this.enviadaARevisionEn = enviadaARevisionEn;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public ContenidoVersion getVersionOrigen() {
        return versionOrigen;
    }

    public void setVersionOrigen(ContenidoVersion versionOrigen) {
        this.versionOrigen = versionOrigen;
    }

    public String getFuenteMaterial() {
        return fuenteMaterial;
    }

    public void setFuenteMaterial(String fuenteMaterial) {
        this.fuenteMaterial = fuenteMaterial;
    }

    public LocalDateTime getLicenciaDeclaradaEn() {
        return licenciaDeclaradaEn;
    }

    public void setLicenciaDeclaradaEn(LocalDateTime licenciaDeclaradaEn) {
        this.licenciaDeclaradaEn = licenciaDeclaradaEn;
    }
}
