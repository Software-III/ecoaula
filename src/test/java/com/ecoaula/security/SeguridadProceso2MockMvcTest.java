package com.ecoaula.security;

import com.ecoaula.service.GestionContenidoService;
import com.ecoaula.service.RevisionContenidoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadProceso2MockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GestionContenidoService gestionContenidoService;

    @MockBean
    private RevisionContenidoService revisionContenidoService;

    @Test
    @DisplayName("Sin sesión: debe redirigir a /login al intentar acceder a rutas del gestor")
    void sinSesionRedirigeAlLoginEnRutasGestor() throws Exception {
        mockMvc.perform(get("/gestor/cursos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Sin sesión: debe redirigir a /login al intentar acceder a rutas del revisor")
    void sinSesionRedirigeAlLoginEnRutasRevisor() throws Exception {
        mockMvc.perform(get("/revisor/cola"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Rol APRENDIZ: debe recibir 403 Forbidden al intentar acceder a /gestor/**")
    @WithMockUser(username = "aprendiz@ecoaula.local", roles = "APRENDIZ")
    void aprendizRecibe403EnGestor() throws Exception {
        mockMvc.perform(get("/gestor/cursos"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Rol APRENDIZ: debe recibir 403 Forbidden al intentar acceder a /revisor/**")
    @WithMockUser(username = "aprendiz@ecoaula.local", roles = "APRENDIZ")
    void aprendizRecibe403EnRevisor() throws Exception {
        mockMvc.perform(get("/revisor/cola"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Rol GESTOR_CONTENIDO: debe recibir 403 Forbidden al intentar acceder a /revisor/**")
    @WithMockUser(username = "gestor@ecoaula.local", roles = "GESTOR_CONTENIDO")
    void gestorRecibe403EnRevisor() throws Exception {
        mockMvc.perform(get("/revisor/cola"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Rol GESTOR_CONTENIDO: debe recibir 200 OK en /gestor/cursos")
    @WithMockUser(username = "gestor@ecoaula.local", roles = "GESTOR_CONTENIDO")
    void gestorAccedeExitosamenteAGestor() throws Exception {
        when(gestionContenidoService.listarTodosLosCursos()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/gestor/cursos"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Rol REVISOR_ACCESIBILIDAD: debe recibir 403 Forbidden al intentar acceder a /gestor/**")
    @WithMockUser(username = "revisor@ecoaula.local", roles = "REVISOR_ACCESIBILIDAD")
    void revisorRecibe403EnGestor() throws Exception {
        mockMvc.perform(get("/gestor/cursos"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Rol REVISOR_ACCESIBILIDAD: debe recibir 200 OK en /revisor/cola")
    @WithMockUser(username = "revisor@ecoaula.local", roles = "REVISOR_ACCESIBILIDAD")
    void revisorAccedeExitosamenteARevisor() throws Exception {
        when(revisionContenidoService.obtenerColaRevision()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/revisor/cola"))
                .andExpect(status().isOk());
    }
}
