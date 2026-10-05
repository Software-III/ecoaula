package com.ecoaula.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementación de NotificacionService que registra las notificaciones en el log del sistema.
 */
@Service
public class NotificacionLogService implements NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionLogService.class);

    @Override
    public void notificarRegistro(String correo, String nombre) {
        log.info("[NOTIFICACION REGISTRO] Bienvenido a EcoAula, {}. Correo registrado: {}", nombre, correo);
    }

    @Override
    public void notificarMatricula(String correo, String nombreCurso) {
        log.info("[NOTIFICACION MATRICULA] Matrícula confirmada para {} en el curso '{}'.", correo, nombreCurso);
    }

    @Override
    public void notificarEmisionCertificado(String correo, String nombreCurso, String codigoVerificacion) {
        log.info("[NOTIFICACION CERTIFICADO] Certificado emitido para {} en el curso '{}'. Código: {}",
                correo, nombreCurso, codigoVerificacion);
    }
}
