package com.ecoaula.notification;

/**
 * Interfaz para el envío de notificaciones y confirmaciones a aprendices.
 */
public interface NotificacionService {

    void notificarRegistro(String correo, String nombre);

    void notificarMatricula(String correo, String nombreCurso);

    void notificarEmisionCertificado(String correo, String nombreCurso, String codigoVerificacion);
}
