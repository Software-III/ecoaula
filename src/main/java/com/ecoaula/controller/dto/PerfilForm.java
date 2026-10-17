package com.ecoaula.controller.dto;

import com.ecoaula.model.enums.ModoVisual;
import com.ecoaula.model.enums.TamanoTexto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Formulario DTO para edición de preferencias de accesibilidad en /perfil.
 */
public class PerfilForm {

    @NotNull(message = "Seleccione la velocidad de audio deseada")
    @DecimalMin(value = "0.5", message = "La velocidad mínima de reproducción es 0.5x")
    @DecimalMax(value = "2.0", message = "La velocidad máxima de reproducción es 2.0x")
    private Double velocidadAudio = 1.0;

    @Size(max = 200, message = "El nombre de la voz admite máximo 200 caracteres")
    private String vozLectura;

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

    public String getVozLectura() {
        return vozLectura;
    }

    public void setVozLectura(String vozLectura) {
        this.vozLectura = vozLectura;
    }

        public boolean isSoloAudio() {
        return soloAudio;
    }

    public void setSoloAudio(boolean soloAudio) {
        this.soloAudio = soloAudio;
    }
}
