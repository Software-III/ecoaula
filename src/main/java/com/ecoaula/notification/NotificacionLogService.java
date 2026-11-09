package com.ecoaula.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementación de NotificacionService que registra las notificaciones en el log del sistema.
 * <p>
 * Ley 1581 de 2012 (principio de finalidad y de seguridad): el log no guarda datos personales. Se registra solo
 * el tipo de evento y el curso; no el nombre, el correo ni el código de verificación del certificado.
 */
@Service
public class NotificacionLogService implements NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionLogService.class);

    @Override
    public void notificarRegistro(String correo, String nombre) {
        log.info("[NOTIFICACION REGISTRO] Cuenta nueva registrada; se envía la bienvenida.");
    }

    @Override
    public void notificarMatricula(String correo, String nombreCurso) {
        log.info("[NOTIFICACION MATRICULA] Matrícula confirmada en el curso '{}'.", nombreCurso);
    }

    @Override
    public void notificarContenidoActualizado(String correo, String nombreCurso, String tituloLeccion, int numeroVersion) {
        log.info("[NOTIFICACION CONTENIDO] La lección '{}' del curso '{}' tiene una versión actualizada (v{}). "
                + "Quien ya la había iniciado conserva la versión con la que comenzó.", tituloLeccion, nombreCurso, numeroVersion);
    }

    @Override
    public void notificarEmisionCertificado(String correo, String nombreCurso, String codigoVerificacion) {
        log.info("[NOTIFICACION CERTIFICADO] Certificado emitido en el curso '{}'.", nombreCurso);
    }
}
