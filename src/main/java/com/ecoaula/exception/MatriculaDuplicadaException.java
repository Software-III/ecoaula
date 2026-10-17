package com.ecoaula.exception;

/**
 * Excepción lanzada cuando un aprendiz intenta matricularse en un curso en el que ya tiene matrícula activa.
 */
public class MatriculaDuplicadaException extends EcoAulaException {

    public MatriculaDuplicadaException(String mensaje) {
        super(mensaje);
    }
}
