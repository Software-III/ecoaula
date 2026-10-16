package com.ecoaula.model.enums;

/**
 * Eventos del recorrido del aprendiz que alimentan los indicadores del Plan de Gestión de la Calidad
 * y la detección de inactividad (componente de analítica del aprendizaje).
 */
public enum TipoEvento {
    /** Abrió el formulario de registro (Proceso 1). */
    REGISTRO_INICIADO,
    /** Creó su cuenta (Proceso 1). */
    REGISTRO_COMPLETADO,
    /** Inició sesión. */
    INICIO_SESION,
    /** Confirmó su matrícula en un curso (Proceso 1). */
    MATRICULA_COMPLETADA,
    /** Abrió una lección (Proceso 2). */
    LECCION_ABIERTA,
    /** Comenzó a reproducir el audio de una lección (Proceso 2). */
    AUDIO_REPRODUCIDO,
    /** Marcó una lección como completada (Proceso 2). */
    LECCION_COMPLETADA
}
