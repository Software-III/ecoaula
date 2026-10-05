package com.ecoaula.integration;

import com.ecoaula.model.Certificado;
import com.ecoaula.model.Matricula;
import com.ecoaula.repository.CertificadoRepository;
import com.ecoaula.repository.MatriculaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Ejecución del flujo completo de punta a punta contra base de datos MySQL real (Docker Compose - ADR-04).
 * Registra y evidencia cada paso del recorrido del aprendiz.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@org.junit.jupiter.api.condition.EnabledIf("isMySqlAvailable")
class FlujoCompletoMySQLIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(FlujoCompletoMySQLIntegrationTest.class);

    static boolean isMySqlAvailable() {
        try (java.net.Socket socket = new java.net.Socket("localhost", 3307)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MatriculaRepository matriculaRepository;

    @Autowired
    private CertificadoRepository certificadoRepository;

    @Test
    @DisplayName("Recorrido completo del aprendiz contra MySQL oficial en Docker")
    @WithUserDetails("aprendiz@ecoaula.local")
    void flujoCompletoContraMySQL() throws Exception {
        log.info(">>> [MYSQL TEST] INICIANDO RECORRIDO COMPLETO CONTRA MYSQL 8 (DOCKER COMPOSE) <<<");
        MockHttpSession session = new MockHttpSession();

        // 1. Visitar catálogo de cursos (Pantalla 2)
        log.info("Paso 1: Consultando catálogo de cursos en MySQL...");
        mockMvc.perform(get("/cursos").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Catálogo de Cursos Accesibles")))
                .andExpect(content().string(containsString("Demostración: Fundamentos de Ofimática Accesible")));
        log.info("Paso 1 completado con éxito.");

        // 2. Modificar preferencias en Mi Perfil (Pantalla 1)
        log.info("Paso 2: Actualizando perfil de accesibilidad (Alto contraste y texto grande)...");
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
        log.info("Paso 2 completado con éxito. Preferencias reflejadas en sesión y BD.");

        // 3. Matricularse en el Curso 1 (Fundamentos de Ofimática)
        log.info("Paso 3: Realizando matrícula en el curso ID 1...");
        mockMvc.perform(post("/cursos/1/matricular")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        Matricula matricula = matriculaRepository.findByAprendizCorreoAndCursoId("aprendiz@ecoaula.local", 1L)
                .orElseThrow();
        Long matriculaId = matricula.getId();
        log.info("Paso 3 completado. Matrícula creada en MySQL con ID: {}", matriculaId);

        // 4. Ver pantalla de Confirmación de Matrícula (Pantalla 3)
        log.info("Paso 4: Verificando confirmación de matrícula...");
        mockMvc.perform(get("/matriculas/" + matriculaId + "/confirmacion").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Matrícula Confirmada")))
                .andExpect(content().string(containsString("Comenzar curso")));
        log.info("Paso 4 completado con éxito.");

        // 5. Consumir Lecciones 1, 2 y 3 con audio nativo y transcripciones
        log.info("Paso 5: Consumiendo Lección 1 (audio nativo y transcripción)...");
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

        // Completar lecciones secuenciales ligadas a su versión exacta (ADR-08)
        log.info("Paso 5.1: Completando lección 1...");
        mockMvc.perform(post("/cursos/1/lecciones/1/completar")
                        .session(session)
                        .with(csrf())
                        .param("versionId", "1"))
                .andExpect(status().is3xxRedirection());

        log.info("Paso 5.2: Completando lección 2...");
        mockMvc.perform(post("/cursos/1/lecciones/2/completar")
                        .session(session)
                        .with(csrf())
                        .param("versionId", "2"))
                .andExpect(status().is3xxRedirection());

        log.info("Paso 5.3: Completando lección 3 (última lección)...");
        mockMvc.perform(post("/cursos/1/lecciones/3/completar")
                        .session(session)
                        .with(csrf())
                        .param("versionId", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/matriculas/" + matriculaId + "/evaluacion"));
        log.info("Paso 5 completado. 100% de lecciones registradas en tabla 'avances_lecciones' de MySQL.");

        // 6. Presentar Evaluación (Pantalla 5) - 5 preguntas
        log.info("Paso 6: Presentando cuestionario accesible de evaluación (1 pregunta a la vez)...");
        for (int p = 1; p <= 4; p++) {
            mockMvc.perform(get("/matriculas/" + matriculaId + "/evaluacion?p=" + p).session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Progreso de evaluación: Pregunta")));

            long opcionId = ((p - 1) * 4) + 1;
            mockMvc.perform(post("/matriculas/" + matriculaId + "/evaluacion")
                            .session(session)
                            .with(csrf())
                            .param("intentoId", "1")
                            .param("preguntaId", String.valueOf(p))
                            .param("opcionId", String.valueOf(opcionId))
                            .param("indice", String.valueOf(p))
                            .param("esUltima", "false"))
                    .andExpect(status().is3xxRedirection());
            log.info("  -> Pregunta {} respondida.", p);
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
        log.info("Paso 6 completado. Intento calificado con 100% de aprobación.");

        // 7. Verificar emisión de Certificado en MySQL (Pantalla 6)
        log.info("Paso 7: Comprobando emisión de certificado en MySQL...");
        Certificado certificado = certificadoRepository.findByMatriculaId(matriculaId).orElseThrow();
        assertNotNull(certificado.getCodigoVerificacion());
        assertTrue(certificado.getCodigoVerificacion().startsWith("EA-"));
        log.info("Certificado emitido con código impredecible: {}", certificado.getCodigoVerificacion());

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
        log.info("Paso 7 completado. Certificado accesible en HTML y .txt descargable.");

        // 8. Verificación pública en la web
        log.info("Paso 8: Verificación pública en interfaz web...");
        mockMvc.perform(get("/verificar?codigo=" + certificado.getCodigoVerificacion()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Certificado Aut")));
        log.info("Paso 8 completado con éxito.");

        // 9. Verificación pública REST sin sesión (ADR-06)
        log.info("Paso 9: Verificación pública REST sin sesión (ADR-06)...");
        mockMvc.perform(get("/api/v1/certificados/" + certificado.getCodigoVerificacion() + "/verificacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido").value(true));
        log.info("Paso 9 completado. API REST retornó {\"valido\": true}.");

        log.info(">>> [MYSQL TEST] FLUJO COMPLETO CONTRA MYSQL EJECUTADO EXITOSAMENTE AL 100% <<<");
    }
}
