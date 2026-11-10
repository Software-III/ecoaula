package com.ecoaula.controller;

import com.ecoaula.model.Contenido;
import com.ecoaula.model.ContenidoVersion;
import com.ecoaula.model.Curso;
import com.ecoaula.model.enums.EstadoContenido;
import com.ecoaula.service.ContenidoService;
import com.ecoaula.service.GestionContenidoService;
import com.ecoaula.service.RevisionContenidoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Hallazgo de auditoría (6.4.10): el editor del gestor abría versiones RETIRADAS aunque el servidor bloqueaba el guardado.
 */
class GestorEditorVersionRetiradaTest {

    private final GestionContenidoService gestion = mock(GestionContenidoService.class);
    private final GestorContenidoController controlador =
            new GestorContenidoController(gestion, mock(ContenidoService.class), mock(RevisionContenidoService.class));

    private ContenidoVersion version(EstadoContenido estado) {
        Curso curso = new Curso("Curso", "Desc", 10, "Área", "Virtual");
        curso.setId(1L);
        Contenido leccion = new Contenido("Lección", 1, curso);
        leccion.setId(100L);
        ContenidoVersion version = new ContenidoVersion();
        version.setId(5L);
        version.setContenido(leccion);
        version.setNumeroVersion(2);
        version.setEstado(estado);
        return version;
    }

    @Test
    @DisplayName("Una versión retirada no abre el editor: se vuelve a las versiones con el motivo")
    void versionRetiradaNoAbreElEditor() {
        when(gestion.obtenerVersionDetalle(5L)).thenReturn(version(EstadoContenido.RETIRADA));
        RedirectAttributesModelMap flash = new RedirectAttributesModelMap();

        String vista = controlador.editarBorradorForm(5L, new ConcurrentModel(), flash);

        assertEquals("redirect:/gestor/lecciones/100/versiones", vista);
        assertTrue(flash.getFlashAttributes().get("mensajeError").toString().contains("retirada"));
    }
}
