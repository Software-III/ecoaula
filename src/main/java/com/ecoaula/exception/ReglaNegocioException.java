package com.ecoaula.exception;

/**
 * Excepción lanzada cuando se infringe una regla de negocio o transición de estado en EcoAula.
 */
public class ReglaNegocioException extends EcoAulaException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
