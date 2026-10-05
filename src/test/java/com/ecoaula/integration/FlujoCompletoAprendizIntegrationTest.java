package com.ecoaula.integration;

import com.ecoaula.model.Certificado;
import com.ecoaula.model.Matricula;
import com.ecoaula.repository.CertificadoRepository;
import com.ecoaula.repository.MatriculaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba de integración de extremo a extremo que recorre el viaje completo del aprendiz:
 * Catálogo -> Matrícula -> Confirmación -> Lecciones con audio nativo -> Evaluación -> Certificado -> Verificación pública.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev-h2")
class FlujoCompletoAprendizIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MatriculaRepository matriculaRepository;

    @Autowired
    private CertificadoRepository certificadoRepository;

    @Test
    @DisplayName("Recorrido completo del aprendiz de punta a punta")
    @WithUserDetails("aprendiz@ecoaula.local")
    void flujoCompletoDePuntaAPunta() throws Exception {
        MockHttpSession session = new MockHttpSession();

        // 1. Visitar catálogo de cursos (Pantalla 2)
        mockMvc.perform(get("/cursos").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Catálogo de Cursos Accesibles")))
                .andExpect(content().string(containsString("Demostración: Fundamentos de Ofimática Accesible")));

        // 2. Modificar preferencias en Mi Perfil (Pantalla 1)
        mockMvc.perform(post("/perfil")
                        .session(session)
                        .with(csrf())
                        .param("velocidadAudio", "1.5")
                        .param("modoVisual", "ALTO_CONTRASTE")
                        .param("tamanoTexto", "GRANDE")
                        .param("soloAudio", "false"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("modo-alto-contraste")))
                .andExpect(content().string(containsString("texto-grande")));

        // 3. Matricularse en el Curso 1 (Fundamentos de Ofimática)
        mockMvc.perform(post("/cursos/1/matricular")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        Matricula matricula = matriculaRepository.findByAprendizCorreoAndCursoId("aprendiz@ecoaula.local", 1L)
                .orElseThrow();
        Long matriculaId = matricula.getId();

        // 4. Ver pantalla de Confirmación de Matrícula (Pantalla 3)
        mockMvc.perform(get("/matriculas/" + matriculaId + "/confirmacion").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Matrícula Confirmada")))
                .andExpect(content().string(containsString("Comenzar curso")));

        // 5. Consumir Lección 1 (Pantalla 4)
        mockMvc.perform(get("/cursos/1/lecciones/1").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Progreso: Lección")))
                .andExpect(content().string(containsString("<audio controls")))
                .andExpect(content().string(containsString("Transcripción de la lección")));

        // Descargar transcripción .txt
        mockMvc.perform(get("/cursos/1/lecciones/1/transcripcion/descargar").session(session))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString("COMPLETA DEL AUDIO")));

        // Completar Lección 1 (versión 1)
        mockMvc.perform(post("/cursos/1/lecciones/1/completar")
                        .session(session)
                        .with(csrf())
                        .param("versionId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cursos/1/lecciones/2"));

        // Completar Lección 2 (versión 2)
        mockMvc.perform(post("/cursos/1/lecciones/2/completar")
                        .session(session)
                        .with(csrf())
                        .param("versionId", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cursos/1/lecciones/3"));

        // Completar Lección 3 (versión 3 - última lección, redirige a evaluación)
        mockMvc.perform(post("/cursos/1/lecciones/3/completar")
                        .session(session)
                        .with(csrf())
                        .param("versionId", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matriculas/" + matriculaId + "/evaluacion"));

        // 6. Presentar Evaluación (Pantalla 5) - 5 preguntas
        for (int p = 1; p <= 4; p++) {
            mockMvc.perform(get("/matriculas/" + matriculaId + "/evaluacion?p=" + p).session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Progreso de evaluación: Pregunta")));

            // Responder opción correcta (la primera opción de cada pregunta de demostración)
            long opcionId = ((p - 1) * 4) + 1;
            mockMvc.perform(post("/matriculas/" + matriculaId + "/evaluacion")
                            .session(session)
                            .with(csrf())
                            .param("intentoId", "1")
                            .param("preguntaId", String.valueOf(p))
                            .param("opcionId", String.valueOf(opcionId))
                            .param("indice", String.valueOf(p))
                            .param("esUltima", "false"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/matriculas/" + matriculaId + "/evaluacion?p=" + (p + 1)));
        }

        // Pregunta 5 (última): enviar y calificar
        long opcionPregunta5 = 17;
        mockMvc.perform(post("/matriculas/" + matriculaId + "/evaluacion")
                        .session(session)
                        .with(csrf())
                        .param("intentoId", "1")
                        .param("preguntaId", "5")
                        .param("opcionId", String.valueOf(opcionPregunta5))
                        .param("indice", "5")
                        .param("esUltima", "true"))
                .andExpect(status().is3xxRedirection());

        // 7. Verificar emisión de Certificado (Pantalla 6)
        Certificado certificado = certificadoRepository.findByMatriculaId(matriculaId).orElseThrow();
        assertNotNull(certificado.getCodigoVerificacion());
        assertTrue(certificado.getCodigoVerificacion().startsWith("EA-"));

        mockMvc.perform(get("/certificados/" + certificado.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Certificado de Aprobaci")))
                .andExpect(content().string(containsString(certificado.getCodigoVerificacion())))
                .andExpect(content().string(containsString("Fundamentos de")));

        // Descargar certificado .txt
        mockMvc.perform(get("/certificados/" + certificado.getId() + "/descargar").session(session))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString(certificado.getCodigoVerificacion())));

        // 8. Verificación pública en la web
        mockMvc.perform(get("/verificar?codigo=" + certificado.getCodigoVerificacion()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Certificado Aut")));

        // 9. Verificación pública REST sin sesión (ADR-06)
        mockMvc.perform(get("/api/v1/certificados/" + certificado.getCodigoVerificacion() + "/verificacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido").value(true));
    }
}
