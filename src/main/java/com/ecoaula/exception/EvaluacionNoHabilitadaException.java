package com.ecoaula.exception;

/**
 * Excepción lanzada cuando un aprendiz intenta presentar una evaluación sin haber completado el 100% de las lecciones.
 */
public class EvaluacionNoHabilitadaException extends EcoAulaException {

    public EvaluacionNoHabilitadaException(String mensaje) {
        super(mensaje);
    }
}
