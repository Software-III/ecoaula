package com.ecoaula.security;

import com.ecoaula.model.Aprendiz;
import com.ecoaula.model.Certificado;
import com.ecoaula.model.Curso;
import com.ecoaula.model.Matricula;
import com.ecoaula.service.CertificacionService;
import com.ecoaula.service.MatriculaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SeguridadAccesoMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatriculaService matriculaService;

    @MockBean
    private CertificacionService certificacionService;

    @Test
    @DisplayName("Debe rechazar con 403 cuando un aprendiz autenticado intenta ver matrícula de otro usuario")
    @WithMockUser(username = "intruso@ecoaula.local", roles = "APRENDIZ")
    void debeDenegarAccesoAMatriculaAjena() throws Exception {
        when(matriculaService.obtenerPorIdYValidarPropiedad(999L, "intruso@ecoaula.local"))
                .thenThrow(new com.ecoaula.exception.AccesoDenegadoException("No tiene autorización"));

        mockMvc.perform(get("/matriculas/999/confirmacion"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Debe rechazar con 403 cuando un aprendiz intenta acceder al certificado de otro usuario")
    @WithMockUser(username = "intruso@ecoaula.local", roles = "APRENDIZ")
    void debeDenegarAccesoACertificadoAjeno() throws Exception {
        when(certificacionService.obtenerPorIdYValidarPropiedad(888L, "intruso@ecoaula.local"))
                .thenThrow(new com.ecoaula.exception.AccesoDenegadoException("No tiene autorización"));

        mockMvc.perform(get("/certificados/888"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Debe permitir acceso público sin sesión al endpoint de verificación REST (ADR-06)")
    void debePermitirVerificacionPublicaSinSesion() throws Exception {
        when(certificacionService.verificarValidez("EA-7K3M-Q9XW-2D4F-8HNP")).thenReturn(true);

        mockMvc.perform(get("/api/v1/certificados/EA-7K3M-Q9XW-2D4F-8HNP/verificacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido").value(true));
    }
}
