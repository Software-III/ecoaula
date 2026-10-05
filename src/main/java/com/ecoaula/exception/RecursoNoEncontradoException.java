package com.ecoaula.exception;

/**
 * Excepción lanzada cuando no se encuentra un recurso solicitado (curso, matrícula, lección, etc.).
 */
public class RecursoNoEncontradoException extends EcoAulaException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
