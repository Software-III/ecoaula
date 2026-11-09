package com.ecoaula.controller;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hallazgo de auditoría (CP-017 y 404): los errores se muestran con la página accesible de la plataforma,
 * nunca como JSON crudo, salvo en la API pública /api/**.
 */
class ErrorPaginaControllerTest {

    private final ErrorPaginaController controlador = new ErrorPaginaController();

    private MockHttpServletRequest error(int codigo, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, codigo);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, uri);
        request.addHeader("Accept", "application/json");
        return request;
    }

    @Test
    @DisplayName("Un 403 de seguridad se muestra como página HTML con su estado, aunque el cliente pida JSON")
    void prohibidoEsPaginaHtml() {
        Object resultado = controlador.manejar(error(403, "/revisor/versiones/5/aprobar"));

        ModelAndView vista = assertInstanceOf(ModelAndView.class, resultado);
        assertEquals("error", vista.getViewName());
        assertEquals(HttpStatus.FORBIDDEN, vista.getStatus());
        assertEquals("403", vista.getModel().get("codigoError"));
        assertEquals("Acceso no autorizado", vista.getModel().get("tituloError"));
    }

    @Test
    @DisplayName("Un 404 de una lección o de un audio se muestra como página HTML")
    void noEncontradoEsPaginaHtml() {
        ModelAndView vista = assertInstanceOf(ModelAndView.class, controlador.manejar(error(404, "/archivos/audio/no-existe.mp3")));

        assertEquals(HttpStatus.NOT_FOUND, vista.getStatus());
        assertTrue(vista.getModel().get("mensajeError").toString().contains("catálogo de cursos"));
    }

    @Test
    @DisplayName("La página de error no expone el detalle interno de la excepción")
    void noExponeDetalleInterno() {
        MockHttpServletRequest request = error(500, "/cursos");
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, "could not execute statement; SQL [insert into ...]");
        request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("detalle interno"));

        ModelAndView vista = assertInstanceOf(ModelAndView.class, controlador.manejar(request));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, vista.getStatus());
        assertFalse(vista.getModel().toString().contains("SQL"));
        assertFalse(vista.getModel().toString().contains("detalle interno"));
    }

    @Test
    @DisplayName("Sin código de estado en la solicitud se responde 500")
    void sinCodigoEs500() {
        ModelAndView vista = assertInstanceOf(ModelAndView.class, controlador.manejar(new MockHttpServletRequest()));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, vista.getStatus());
    }

    @Test
    @DisplayName("La API pública de verificación de certificados conserva el error en JSON")
    @SuppressWarnings("unchecked")
    void apiConservaJson() {
        Object resultado = controlador.manejar(error(404, "/api/v1/certificados/XXXX/verificacion"));

        ResponseEntity<Map<String, Object>> respuesta = (ResponseEntity<Map<String, Object>>) assertInstanceOf(ResponseEntity.class, resultado);
        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
        assertEquals(404, respuesta.getBody().get("estado"));
    }
}
