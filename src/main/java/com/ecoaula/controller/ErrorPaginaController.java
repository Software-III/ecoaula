package com.ecoaula.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Destino de todos los errores que llegan a /error (404 de rutas o archivos, 403 de Spring Security, 405, 500...).
 * <p>
 * Antes se usaba el controlador de errores por defecto de Spring Boot, que responde JSON crudo cuando el
 * cliente no pide HTML (por ejemplo, el reproductor de audio o una descarga). Hallazgo de auditoría
 * (CP-017, observaciones 7.3): un usuario de tecnología de asistencia recibía texto JSON sin estructura.
 * Aquí siempre se devuelve la página de error accesible; solo la API pública de verificación de certificados
 * (/api/**) conserva el JSON. Nunca se muestra el detalle interno de la excepción.
 */
@Controller
public class ErrorPaginaController implements ErrorController {

    @RequestMapping("/error")
    public Object manejar(HttpServletRequest request) {
        HttpStatus estado = estadoDe(request);
        String uriOriginal = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        if (uriOriginal != null && uriOriginal.startsWith("/api/")) {
            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("error", titulo(estado));
            cuerpo.put("estado", estado.value());
            return ResponseEntity.status(estado).body(cuerpo);
        }

        ModelAndView vista = new ModelAndView("error");
        vista.setStatus(estado);
        vista.addObject("codigoError", String.valueOf(estado.value()));
        vista.addObject("tituloError", titulo(estado));
        vista.addObject("mensajeError", mensaje(estado));
        return vista;
    }

    private static HttpStatus estadoDe(HttpServletRequest request) {
        Object codigo = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (codigo instanceof Integer valor) {
            HttpStatus estado = HttpStatus.resolve(valor);
            if (estado != null) {
                return estado;
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    static String titulo(HttpStatus estado) {
        return switch (estado) {
            case BAD_REQUEST -> "Solicitud inválida";
            case UNAUTHORIZED -> "Debe iniciar sesión";
            case FORBIDDEN -> "Acceso no autorizado";
            case NOT_FOUND -> "Página no encontrada";
            case METHOD_NOT_ALLOWED -> "Acción no permitida";
            case PAYLOAD_TOO_LARGE -> "Archivo demasiado grande";
            case TOO_MANY_REQUESTS -> "Demasiadas solicitudes";
            default -> "Error interno del servidor";
        };
    }

    static String mensaje(HttpStatus estado) {
        return switch (estado) {
            case BAD_REQUEST -> "No pudimos entender la solicitud. Revise los datos e intente de nuevo.";
            case UNAUTHORIZED -> "Inicie sesión para continuar.";
            case FORBIDDEN -> "Su rol no tiene permiso para realizar esta acción. Si cree que es un error, contacte a la coordinación.";
            case NOT_FOUND -> "La dirección o el archivo solicitado no existe. Vuelva al catálogo de cursos para continuar.";
            case METHOD_NOT_ALLOWED -> "Esta acción no está disponible desde esa dirección.";
            case PAYLOAD_TOO_LARGE -> "El archivo adjunto supera el tamaño máximo permitido.";
            case TOO_MANY_REQUESTS -> "Realizó demasiadas solicitudes seguidas. Espere un momento e intente de nuevo.";
            default -> "Ha ocurrido un error inesperado al procesar la solicitud. Por favor intente nuevamente.";
        };
    }
}
