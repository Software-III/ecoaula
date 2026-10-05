package com.ecoaula.model;

import com.ecoaula.model.enums.ModoVisual;
import com.ecoaula.model.enums.TamanoTexto;
import jakarta.persistence.*;
import java.io.Serializable;

/**
 * Perfil de accesibilidad persistente de la cuenta del aprendiz (ADR-07).
 * Se mantiene en base de datos y se precarga en la sesión HTTP (ADR-02 / ADR-07).
 */
@Entity
@Table(name = "perfiles_accesibilidad")
public class PerfilAccesibilidad implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprendiz_id", nullable = false, unique = true)
    private Aprendiz aprendiz;

    @Column(nullable = false)
    private Double velocidadAudio = 1.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModoVisual modoVisual = ModoVisual.ESTANDAR;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TamanoTexto tamanoTexto = TamanoTexto.NORMAL;

    @Column(nullable = false)
    private boolean soloAudio = false;

    public PerfilAccesibilidad() {
    }

    public PerfilAccesibilidad(Aprendiz aprendiz) {
        this.aprendiz = aprendiz;
        this.velocidadAudio = 1.0;
        this.modoVisual = ModoVisual.ESTANDAR;
        this.tamanoTexto = TamanoTexto.NORMAL;
        this.soloAudio = false;
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

    public Double getVelocidadAudio() {
        return velocidadAudio;
    }

    public void setVelocidadAudio(Double velocidadAudio) {
        this.velocidadAudio = velocidadAudio;
    }

    public ModoVisual getModoVisual() {
        return modoVisual;
    }

    public void setModoVisual(ModoVisual modoVisual) {
        this.modoVisual = modoVisual;
    }

    public TamanoTexto getTamanoTexto() {
        return tamanoTexto;
    }

    public void setTamanoTexto(TamanoTexto tamanoTexto) {
        this.tamanoTexto = tamanoTexto;
    }

    public boolean isSoloAudio() {
        return soloAudio;
    }

    public void setSoloAudio(boolean soloAudio) {
        this.soloAudio = soloAudio;
    }
}
