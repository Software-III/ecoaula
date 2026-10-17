package com.ecoaula.model.enums;

/**
 * Etapa del recorrido en la que el aprendiz encontró una barrera de accesibilidad;
 * corresponde a cada proceso de negocio.
 */
public enum EtapaReporte {
    REGISTRO_MATRICULA("Registro, inicio de sesión o matrícula"),
    CONTENIDO("Contenido de las lecciones"),
    EVALUACION_CERTIFICADO("Evaluación o certificado");

    private final String descripcion;

    EtapaReporte(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
