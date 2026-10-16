package com.ecoaula.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Formulario para creación y edición de cursos por parte del gestor.
 */
public class CursoForm {

    @NotBlank(message = "El título del curso es obligatorio")
    @Size(min = 5, max = 200, message = "El título debe tener entre 5 y 200 caracteres")
    private String titulo;

    @NotBlank(message = "La descripción del curso es obligatoria")
    @Size(min = 10, max = 2000, message = "La descripción debe tener entre 10 y 2000 caracteres")
    private String descripcion;

    @NotNull(message = "La intensidad horaria es obligatoria")
    @Min(value = 1, message = "La duración mínima es de 1 hora")
    private Integer horas;

    @NotBlank(message = "El área de conocimiento es obligatoria")
    private String area;

    @NotBlank(message = "La modalidad es obligatoria")
    private String modalidad = "Virtual accesible";

    private boolean activo = true;

    public CursoForm() {
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getHoras() {
        return horas;
    }

    public void setHoras(Integer horas) {
        this.horas = horas;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getModalidad() {
        return modalidad;
    }

    public void setModalidad(String modalidad) {
        this.modalidad = modalidad;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
