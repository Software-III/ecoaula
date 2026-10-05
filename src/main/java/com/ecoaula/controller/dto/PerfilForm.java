package com.ecoaula.controller.dto;

import com.ecoaula.model.enums.ModoVisual;
import com.ecoaula.model.enums.TamanoTexto;
import jakarta.validation.constraints.NotNull;

/**
 * Formulario DTO para edición de preferencias de accesibilidad en /perfil.
 */
public class PerfilForm {

    @NotNull(message = "Seleccione la velocidad de audio deseada")
    private Double velocidadAudio = 1.0;

    @NotNull(message = "Seleccione el modo visual")
    private ModoVisual modoVisual = ModoVisual.ESTANDAR;

    @NotNull(message = "Seleccione el tamaño de texto")
    private TamanoTexto tamanoTexto = TamanoTexto.NORMAL;

    private boolean soloAudio = false;

    public PerfilForm() {
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
