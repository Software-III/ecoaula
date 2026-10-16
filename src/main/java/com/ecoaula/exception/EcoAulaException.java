package com.ecoaula.exception;

/**
 * Excepción base de la plataforma EcoAula.
 * Toda excepción de dominio debe extender de esta clase.
 */
public class EcoAulaException extends RuntimeException {

    public EcoAulaException(String mensaje) {
        super(mensaje);
    }

    public EcoAulaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
