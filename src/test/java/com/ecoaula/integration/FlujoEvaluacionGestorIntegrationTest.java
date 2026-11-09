package com.ecoaula.integration;

import com.ecoaula.model.Curso;
import com.ecoaula.repository.CursoRepository;
import com.ecoaula.service.GestionEvaluacionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proceso 3 / NC-01: el gestor crea la evaluación final de un curso nuevo desde la interfaz y, con ella,
 * el curso puede cumplir la regla de publicación. Solo el gestor entra a la pantalla.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlujoEvaluacionGestorIntegrationTest {

    private static final RequestPostProcessor GESTOR = user("gestor@ecoaula.local").roles("GESTOR_CONTENIDO");
    private static final RequestPostProcessor APRENDIZ = user("aprendiz@ecoaula.local").roles("APRENDIZ");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CursoRepository cursoRepository;

    @Autowired
    private GestionEvaluacionService evaluacionService;

    private Curso cursoNuevo() {
        Curso curso = new Curso("Curso con evaluación " + System.nanoTime(), "Descripción", 16, "Tecnología", "Virtual");
        curso.setActivo(false);
        return cursoRepository.save(curso);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder agregar(Long cursoId, String enunciado,
                                                                                                String correcta) {
        return post("/gestor/cursos/{id}/evaluacion/preguntas", cursoId).with(GESTOR).with(csrf())
                .param("enunciado", enunciado)
                .param("opcion1", "Respuesta A").param("opcion2", "Respuesta B")
                .param("opcion3", "Respuesta C").param("opcion4", "")
                .param("correcta", correcta);
    }

    @Test
    @DisplayName("El gestor crea las preguntas, ve el avance hacia las 3 mínimas y puede eliminar una")
    void gestorCreaYEliminaPreguntas() throws Exception {
        Curso curso = cursoNuevo();

        mockMvc.perform(get("/gestor/cursos/{id}/evaluacion", curso.getId()).with(GESTOR))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("todavía no tiene preguntas")));

        mockMvc.perform(agregar(curso.getId(), "Primera pregunta", "1")).andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensajeExito", containsString("Pregunta agregada")));
        mockMvc.perform(agregar(curso.getId(), "Segunda pregunta", "2")).andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/gestor/cursos/{id}/evaluacion", curso.getId()).with(GESTOR))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Primera pregunta")))
                .andExpect(content().string(containsString("Segunda pregunta")))
                .andExpect(content().string(containsString("(respuesta correcta)")))
                .andExpect(content().string(containsString("Faltan")));

        // Con 2 preguntas el curso aún no se publica
        mockMvc.perform(post("/gestor/cursos/{id}/publicar", curso.getId()).with(GESTOR).with(csrf()))
                .andExpect(flash().attribute("mensajeError", containsString("necesita al menos 3")));

        mockMvc.perform(agregar(curso.getId(), "Tercera pregunta", "3")).andExpect(status().is3xxRedirection());
        assertEquals(3, evaluacionService.listarPreguntas(curso.getId()).size());

        Long primera = evaluacionService.listarPreguntas(curso.getId()).get(0).getId();
        mockMvc.perform(post("/gestor/cursos/{id}/evaluacion/preguntas/{pid}/eliminar", curso.getId(), primera)
                        .with(GESTOR).with(csrf()).param("confirmar", "true"))
                .andExpect(flash().attribute("mensajeExito", containsString("Pregunta eliminada")));
        assertEquals(2, evaluacionService.listarPreguntas(curso.getId()).size());
        assertEquals(1, evaluacionService.listarPreguntas(curso.getId()).get(0).getOrden());
    }

    @Test
    @DisplayName("Sin marcar la respuesta correcta la pregunta se rechaza y se conserva lo escrito")
    void rechazaSinCorrecta() throws Exception {
        Curso curso = cursoNuevo();
        mockMvc.perform(post("/gestor/cursos/{id}/evaluacion/preguntas", curso.getId()).with(GESTOR).with(csrf())
                        .param("enunciado", "Pregunta sin correcta").param("opcion1", "A").param("opcion2", "B"))
                .andExpect(flash().attribute("mensajeError", containsString("correcta")))
                .andExpect(flash().attribute("enunciadoPrevio", "Pregunta sin correcta"));
        assertEquals(0, evaluacionService.listarPreguntas(curso.getId()).size());
    }

    @Test
    @DisplayName("Eliminar una pregunta exige marcar la confirmación")
    void eliminarExigeConfirmacion() throws Exception {
        Curso curso = cursoNuevo();
        mockMvc.perform(agregar(curso.getId(), "Pregunta única", "1"));
        Long id = evaluacionService.listarPreguntas(curso.getId()).get(0).getId();
        mockMvc.perform(post("/gestor/cursos/{id}/evaluacion/preguntas/{pid}/eliminar", curso.getId(), id)
                        .with(GESTOR).with(csrf()))
                .andExpect(flash().attribute("mensajeError", containsString("confirmación")));
        assertEquals(1, evaluacionService.listarPreguntas(curso.getId()).size());
    }

    @Test
    @DisplayName("Solo el gestor accede: el aprendiz recibe 403 y sin sesión se pide iniciar sesión")
    void soloElGestorAccede() throws Exception {
        Curso curso = cursoNuevo();
        mockMvc.perform(get("/gestor/cursos/{id}/evaluacion", curso.getId()).with(APRENDIZ))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/gestor/cursos/{id}/evaluacion/preguntas", curso.getId()).with(APRENDIZ).with(csrf())
                        .param("enunciado", "x"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/gestor/cursos/{id}/evaluacion", curso.getId()))
                .andExpect(status().is3xxRedirection());
    }
}
