package com.ecoaula.controller.dto;

import com.ecoaula.model.enums.ModoVisual;
import com.ecoaula.model.enums.TamanoTexto;
import jakarta.validation.constraints.*;

/**
 * Formulario DTO para el registro de nuevos aprendices y configuración inicial de su perfil accesible.
 */
public class RegistroForm {

    @NotBlank(message = "El nombre completo es obligatorio")
    private String nombreCompleto;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    private String correo;

    @NotBlank(message = "El número de teléfono es obligatorio")
    private String telefono;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener como mínimo 8 caracteres")
    private String password;

    private String condicionVisual;

    @AssertTrue(message = "Debe autorizar expresamente el tratamiento de datos para continuar (Ley 1581 de 2012)")
    private boolean consentimientoDatos = false;

    // Preferencias de accesibilidad
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

    public RegistroForm() {
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCondicionVisual() {
        return condicionVisual;
    }

    public void setCondicionVisual(String condicionVisual) {
        this.condicionVisual = condicionVisual;
    }

    public boolean isConsentimientoDatos() {
        return consentimientoDatos;
    }

    public void setConsentimientoDatos(boolean consentimientoDatos) {
        this.consentimientoDatos = consentimientoDatos;
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
