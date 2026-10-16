package com.ecoaula.model;

import jakarta.persistence.*;

/**
 * Entidad que representa una opción de respuesta a una pregunta.
 */
@Entity
@Table(name = "opciones_pregunta")
public class OpcionPregunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pregunta_id", nullable = false)
    private Pregunta pregunta;

    @Column(nullable = false)
    private String texto;

    private String descripcionCorta;

    @Column(nullable = false)
    private boolean correcta = false;

    public OpcionPregunta() {
    }

    public OpcionPregunta(Pregunta pregunta, String texto, String descripcionCorta, boolean correcta) {
        this.pregunta = pregunta;
        this.texto = texto;
        this.descripcionCorta = descripcionCorta;
        this.correcta = correcta;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Pregunta getPregunta() {
        return pregunta;
    }

    public void setPregunta(Pregunta pregunta) {
        this.pregunta = pregunta;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getDescripcionCorta() {
        return descripcionCorta;
    }

    public void setDescripcionCorta(String descripcionCorta) {
        this.descripcionCorta = descripcionCorta;
    }

    public boolean isCorrecta() {
        return correcta;
    }

    public void setCorrecta(boolean correcta) {
        this.correcta = correcta;
    }
}
