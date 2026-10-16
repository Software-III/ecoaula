package com.ecoaula.controller.dto;

import com.ecoaula.model.enums.EtapaReporte;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Formulario DTO para reportar una barrera de accesibilidad.
 */
public class ReporteBarreraForm {

    @NotNull(message = "Indique en qué parte de la plataforma encontró la barrera")
    private EtapaReporte etapa;

    @NotBlank(message = "Describa la barrera que encontró")
    @Size(max = 4000, message = "La descripción admite máximo 4000 caracteres")
    private String descripcion;

    @Size(max = 200, message = "La tecnología de asistencia admite máximo 200 caracteres")
    private String tecnologiaAsistencia;

    @Email(message = "El formato del correo de contacto no es válido")
    @Size(max = 255, message = "El correo de contacto admite máximo 255 caracteres")
    private String correoContacto;

    private Long cursoId;

    private Long contenidoId;

    public EtapaReporte getEtapa() {
        return etapa;
    }

    public void setEtapa(EtapaReporte etapa) {
        this.etapa = etapa;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTecnologiaAsistencia() {
        return tecnologiaAsistencia;
    }

    public void setTecnologiaAsistencia(String tecnologiaAsistencia) {
        this.tecnologiaAsistencia = tecnologiaAsistencia;
    }

    public String getCorreoContacto() {
        return correoContacto;
    }

    public void setCorreoContacto(String correoContacto) {
        this.correoContacto = correoContacto;
    }

    public Long getCursoId() {
        return cursoId;
    }

    public void setCursoId(Long cursoId) {
        this.cursoId = cursoId;
    }

    public Long getContenidoId() {
        return contenidoId;
    }

    public void setContenidoId(Long contenidoId) {
        this.contenidoId = contenidoId;
    }
}
