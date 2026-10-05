package com.ecoaula.exception;

/**
 * Excepción lanzada cuando un usuario intenta acceder a un recurso que no le pertenece
 * o para el cual no cuenta con autorización suficiente.
 */
public class AccesoDenegadoException extends EcoAulaException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
