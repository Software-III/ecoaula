package com.ecoaula.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Formulario para agregar una nueva lección a un curso.
 */
public class LeccionForm {

    @NotBlank(message = "El título de la lección es obligatorio")
    @Size(min = 3, max = 255, message = "El título debe tener entre 3 y 255 caracteres")
    private String titulo;

    @Min(value = 1, message = "El número de orden debe ser positivo")
    private Integer orden;

    public LeccionForm() {
    }

    public LeccionForm(String titulo, Integer orden) {
        this.titulo = titulo;
        this.orden = orden;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }
}
