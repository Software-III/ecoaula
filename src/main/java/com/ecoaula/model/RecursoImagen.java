package com.ecoaula.model;

import jakarta.persistence.*;

/**
 * Imagen de una versión de lección con su alternativa textual (WCAG 1.1.1).
 * El texto alternativo es obligatorio; la descripción detallada se usa para gráficos, tablas o
 * diagramas que no caben en una frase. Pertenece a una versión inmutable (ADR-08).
 */
@Entity
@Table(name = "recursos_imagen")
public class RecursoImagen {

    public static final int MAX_TEXTO_ALTERNATIVO = 250;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contenido_version_id", nullable = false)
    private ContenidoVersion contenidoVersion;

    @Column(nullable = false)
    private String rutaArchivo;

    @Column(nullable = false, length = MAX_TEXTO_ALTERNATIVO)
    private String textoAlternativo;

    @Column(columnDefinition = "TEXT")
    private String descripcionDetallada;

    @Column(nullable = false)
    private Integer orden;

    public RecursoImagen() {
    }

    public RecursoImagen(ContenidoVersion contenidoVersion, String rutaArchivo, String textoAlternativo,
                         String descripcionDetallada, Integer orden) {
        this.contenidoVersion = contenidoVersion;
        this.rutaArchivo = rutaArchivo;
        this.textoAlternativo = textoAlternativo;
        this.descripcionDetallada = descripcionDetallada;
        this.orden = orden;
    }

    /** Copia la imagen a otra versión; el archivo se comparte porque es inmutable. */
    public RecursoImagen copiarPara(ContenidoVersion otraVersion) {
        return new RecursoImagen(otraVersion, rutaArchivo, textoAlternativo, descripcionDetallada, orden);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ContenidoVersion getContenidoVersion() {
        return contenidoVersion;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }

    public String getTextoAlternativo() {
        return textoAlternativo;
    }

    public String getDescripcionDetallada() {
        return descripcionDetallada;
    }

    public Integer getOrden() {
        return orden;
    }
}
