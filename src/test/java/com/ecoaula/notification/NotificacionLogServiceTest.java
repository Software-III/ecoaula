package com.ecoaula.notification;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ley 1581 de 2012: el log de notificaciones no debe guardar nombres, correos ni códigos de certificado.
 */
class NotificacionLogServiceTest {

    private static final String CORREO = "persona.privada@ejemplo.org";
    private static final String NOMBRE = "Persona Privada Ejemplo";
    private static final String CODIGO = "COD-SECRETO-12345";

    private final NotificacionLogService servicio = new NotificacionLogService();
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void engancharLog() {
        logger = (Logger) LoggerFactory.getLogger(NotificacionLogService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void soltarLog() {
        logger.detachAppender(appender);
    }

    @Test
    @DisplayName("Las cuatro notificaciones se registran sin nombre, correo ni código de verificación")
    void noRegistraDatosPersonales() {
        servicio.notificarRegistro(CORREO, NOMBRE);
        servicio.notificarMatricula(CORREO, "Curso de prueba");
        servicio.notificarContenidoActualizado(CORREO, "Curso de prueba", "Lección 1", 2);
        servicio.notificarEmisionCertificado(CORREO, "Curso de prueba", CODIGO);

        List<String> mensajes = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());

        assertEquals(4, mensajes.size());
        String todo = String.join("\n", mensajes);
        assertFalse(todo.contains(CORREO), "El log no debe contener el correo");
        assertFalse(todo.contains("persona.privada"), "El log no debe contener el correo");
        assertFalse(todo.contains(NOMBRE), "El log no debe contener el nombre");
        assertFalse(todo.contains(CODIGO), "El log no debe contener el código de verificación");
        assertTrue(todo.contains("Curso de prueba"), "El curso sí es información no personal y se conserva");
    }
}
