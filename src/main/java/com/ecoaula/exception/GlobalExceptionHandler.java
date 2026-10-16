package com.ecoaula.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Manejador global de excepciones para renderizar páginas de error accesibles.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MatriculaDuplicadaException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String manejarMatriculaDuplicada(MatriculaDuplicadaException ex, Model model) {
        log.warn("Conflicto de matrícula: {}", ex.getMessage());
        model.addAttribute("codigoError", "409");
        model.addAttribute("tituloError", "Matrícula ya existente");
        model.addAttribute("mensajeError", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(EvaluacionNoHabilitadaException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String manejarEvaluacionNoHabilitada(EvaluacionNoHabilitadaException ex, Model model) {
        log.warn("Evaluación no habilitada: {}", ex.getMessage());
        model.addAttribute("codigoError", "400");
        model.addAttribute("tituloError", "Evaluación no disponible");
        model.addAttribute("mensajeError", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String manejarRecursoNoEncontrado(RecursoNoEncontradoException ex, Model model) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        model.addAttribute("codigoError", "404");
        model.addAttribute("tituloError", "Recurso no encontrado");
        model.addAttribute("mensajeError", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(ReglaNegocioException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String manejarReglaNegocio(ReglaNegocioException ex, Model model) {
        log.warn("Violación de regla de negocio: {}", ex.getMessage());
        model.addAttribute("codigoError", "400");
        model.addAttribute("tituloError", "Acción no permitida");
        model.addAttribute("mensajeError", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String manejarIllegalArgument(IllegalArgumentException ex, Model model) {
        log.warn("Parámetro inválido: {}", ex.getMessage());
        model.addAttribute("codigoError", "400");
        model.addAttribute("tituloError", "Solicitud inválida");
        model.addAttribute("mensajeError", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String manejarAccesoDenegado(AccesoDenegadoException ex, Model model) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        model.addAttribute("codigoError", "403");
        model.addAttribute("tituloError", "Acceso no autorizado");
        model.addAttribute("mensajeError", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String manejarArchivoDemasiadoGrande(MaxUploadSizeExceededException ex, Model model) {
        log.warn("Archivo adjunto supera el tamaño máximo: {}", ex.getMessage());
        model.addAttribute("codigoError", "413");
        model.addAttribute("tituloError", "Archivo demasiado grande");
        model.addAttribute("mensajeError", "El archivo adjunto supera el tamaño máximo permitido.");
        return "error";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String manejarRutaInexistente(NoResourceFoundException ex, Model model) {
        model.addAttribute("codigoError", "404");
        model.addAttribute("tituloError", "Página no encontrada");
        model.addAttribute("mensajeError", "La dirección solicitada no existe. Vuelva al catálogo de cursos para continuar.");
        return "error";
    }

    @ExceptionHandler(EcoAulaException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String manejarErrorDeDominio(EcoAulaException ex, Model model) {
        log.warn("Operación rechazada: {}", ex.getMessage());
        model.addAttribute("codigoError", "400");
        model.addAttribute("tituloError", "No se pudo completar la operación");
        model.addAttribute("mensajeError", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String manejarExcepcionGeneral(Exception ex, Model model) {
        log.error("Error no controlado en la aplicación", ex);
        model.addAttribute("codigoError", "500");
        model.addAttribute("tituloError", "Error interno del servidor");
        model.addAttribute("mensajeError", "Ha ocurrido un error inesperado al procesar la solicitud. Por favor intente nuevamente.");
        return "error";
    }
}
