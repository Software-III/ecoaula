package com.ecoaula.controller.dto;

import jakarta.validation.constraints.Max;
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
    @Min(value = 8, message = "La duración mínima de un curso complementario es de 8 horas")
    @Max(value = 40, message = "La duración máxima de un curso complementario es de 40 horas")
    private Integer horas;

    @NotBlank(message = "El área de conocimiento es obligatoria")
    private String area;

    @NotBlank(message = "La modalidad es obligatoria")
    private String modalidad = "Virtual accesible";

    /** Número máximo de aprendices; vacío significa sin límite. */
    @Min(value = 1, message = "El cupo máximo debe ser de al menos 1 aprendiz; déjelo vacío para no limitarlo")
    private Integer cupoMaximo;

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

    public Integer getCupoMaximo() {
        return cupoMaximo;
    }

    public void setCupoMaximo(Integer cupoMaximo) {
        this.cupoMaximo = cupoMaximo;
    }
}
