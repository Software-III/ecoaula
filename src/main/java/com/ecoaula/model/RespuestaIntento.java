package com.ecoaula.model;

import jakarta.persistence.*;

/**
 * Entidad que almacena la respuesta seleccionada por el aprendiz para una pregunta durante un intento.
 */
@Entity
@Table(name = "respuestas_intento", uniqueConstraints = {
    @UniqueConstraint(name = "uk_intento_pregunta", columnNames = {"intento_id", "pregunta_id"})
})
public class RespuestaIntento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intento_id", nullable = false)
    private IntentoEvaluacion intento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pregunta_id", nullable = false)
    private Pregunta pregunta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opcion_id")
    private OpcionPregunta opcionSeleccionada;

    public RespuestaIntento() {
    }

    public RespuestaIntento(IntentoEvaluacion intento, Pregunta pregunta, OpcionPregunta opcionSeleccionada) {
        this.intento = intento;
        this.pregunta = pregunta;
        this.opcionSeleccionada = opcionSeleccionada;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public IntentoEvaluacion getIntento() {
        return intento;
    }

    public void setIntento(IntentoEvaluacion intento) {
        this.intento = intento;
    }

    public Pregunta getPregunta() {
        return pregunta;
    }

    public void setPregunta(Pregunta pregunta) {
        this.pregunta = pregunta;
    }

    public OpcionPregunta getOpcionSeleccionada() {
        return opcionSeleccionada;
    }

    public void setOpcionSeleccionada(OpcionPregunta opcionSeleccionada) {
        this.opcionSeleccionada = opcionSeleccionada;
    }
}
