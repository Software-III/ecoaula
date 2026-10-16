package com.ecoaula.integration;

import com.ecoaula.model.Aprendiz;
import com.ecoaula.model.Contenido;
import com.ecoaula.model.ContenidoVersion;
import com.ecoaula.model.Curso;
import com.ecoaula.model.PerfilAccesibilidad;
import com.ecoaula.model.enums.CriterioAccesibilidad;
import com.ecoaula.model.enums.EstadoContenido;
import com.ecoaula.model.enums.Rol;
import com.ecoaula.repository.AprendizRepository;
import com.ecoaula.repository.AvanceLeccionRepository;
import com.ecoaula.repository.ContenidoRepository;
import com.ecoaula.repository.ContenidoVersionRepository;
import com.ecoaula.repository.CursoRepository;
import com.ecoaula.repository.MatriculaRepository;
import com.ecoaula.repository.RecursoImagenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba de integración del consumo de contenido (Procesos 1 y 2): archivos protegidos con soporte Range,
 * retiro y republicación de cursos, continuidad del curso y exactitud del registro de avance.
 * Usa un aprendiz propio para no interferir con el recorrido del aprendiz de demostración.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev-h2")
class FlujoCatalogoYArchivosIntegrationTest {

    private static final String CORREO_LECTOR = "lector@ecoaula.local";
    private static final RequestPostProcessor LECTOR = user(CORREO_LECTOR).roles("APRENDIZ");
    private static final RequestPostProcessor GESTOR = user("gestor@ecoaula.local").roles("GESTOR_CONTENIDO");
    private static final RequestPostProcessor REVISOR = user("revisor@ecoaula.local").roles("REVISOR_ACCESIBILIDAD");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AprendizRepository aprendizRepository;

    @Autowired
    private CursoRepository cursoRepository;

    @Autowired
    private ContenidoRepository contenidoRepository;

    @Autowired
    private ContenidoVersionRepository versionRepository;

    @Autowired
    private MatriculaRepository matriculaRepository;

    @Autowired
    private AvanceLeccionRepository avanceRepository;

    @Autowired
    private RecursoImagenRepository imagenRepository;

    @BeforeEach
    void crearLector() {
        if (!aprendizRepository.existsByCorreo(CORREO_LECTOR)) {
            crearAprendiz(CORREO_LECTOR);
        }
    }

    @Test
    @DisplayName("Los archivos exigen sesión, solo se entregan a matriculados y aceptan peticiones Range")
    void archivosProtegidosConRange() throws Exception {
        Curso ofimatica = curso("Ofimática");
        String audio = versionVigente(ofimatica, 1).getRutaAudio();

        // Sin sesión: redirige al login
        mockMvc.perform(get("/audio/{a}", audio)).andExpect(status().is3xxRedirection());

        // Con sesión pero sin matrícula: no existe para el aprendiz
        mockMvc.perform(get("/audio/{a}", audio).with(user("ajeno@ecoaula.local").roles("APRENDIZ")))
                .andExpect(status().isNotFound());

        // Matriculado: se entrega, y una petición Range responde 206 con el fragmento pedido
        asegurarMatricula(ofimatica);
        mockMvc.perform(get("/audio/{a}", audio).with(LECTOR))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/wav"));
        mockMvc.perform(get("/audio/{a}", audio).with(LECTOR).header(HttpHeaders.RANGE, "bytes=0-9"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, "10"));
    }

    @Test
    @DisplayName("Retirar un curso del catálogo bloquea matrículas nuevas pero conserva el acceso de los matriculados")
    void retiroYRepublicacionDeCurso() throws Exception {
        Curso servicio = curso("Atención al Cliente");
        asegurarMatricula(servicio);

        mockMvc.perform(post("/gestor/cursos/{id}/retirar", servicio.getId()).with(GESTOR).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertFalse(cursoRepository.findById(servicio.getId()).orElseThrow().isActivo());

        try {
            // Quien no está matriculado no puede matricularse
            mockMvc.perform(post("/cursos/{id}/matricular", servicio.getId())
                            .with(user("revisor@ecoaula.local").roles("APRENDIZ")).with(csrf()))
                    .andExpect(status().isBadRequest());

            // El matriculado lo sigue viendo en el catálogo, con aviso, y conserva sus lecciones
            mockMvc.perform(get("/cursos").with(LECTOR))
                    .andExpect(content().string(containsString(servicio.getTitulo())))
                    .andExpect(content().string(containsString("retirado del catálogo")));
            mockMvc.perform(get("/cursos/{id}/lecciones/1", servicio.getId()).with(LECTOR))
                    .andExpect(status().isOk());
        } finally {
            mockMvc.perform(post("/gestor/cursos/{id}/publicar", servicio.getId()).with(GESTOR).with(csrf()))
                    .andExpect(flash().attribute("mensajeExito", containsString("publicado en el catálogo")));
        }
        assertTrue(cursoRepository.findById(servicio.getId()).orElseThrow().isActivo());

        // Publicado en el catálogo no admite lecciones nuevas
        mockMvc.perform(post("/gestor/cursos/{id}/lecciones/nueva", servicio.getId()).with(GESTOR).with(csrf())
                        .param("titulo", "Lección extra"))
                .andExpect(flash().attribute("mensajeError", containsString("Retírelo del catálogo para agregarle lecciones")));
    }

    @Test
    @DisplayName("El avance solo se registra con matrícula y con la versión que se le entregó al aprendiz")
    void exactitudDelRegistroDeAvance() throws Exception {
        Curso finanzas = curso("Finanzas");
        Curso ofimatica = curso("Ofimática");

        // Sin matrícula: ni lecciones ni registro de avance (ya no se matricula de forma implícita)
        if (!matriculaRepository.existsByAprendizCorreoAndCursoId(CORREO_LECTOR, finanzas.getId())) {
            mockMvc.perform(get("/cursos/{id}/lecciones/1", finanzas.getId()).with(LECTOR))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/cursos/{id}/lecciones/1/completar", finanzas.getId()).with(LECTOR).with(csrf())
                            .param("versionId", String.valueOf(versionVigente(finanzas, 1).getId())))
                    .andExpect(status().isForbidden());
            assertFalse(matriculaRepository.existsByAprendizCorreoAndCursoId(CORREO_LECTOR, finanzas.getId()));
        }

        // Matriculado, pero enviando la versión de otra lección: se rechaza sin registrar avance
        asegurarMatricula(ofimatica);
        Long lector = aprendizRepository.findByCorreo(CORREO_LECTOR).orElseThrow().getId();
        long avancesAntes = avanceRepository.findByAprendizId(lector).size();
        mockMvc.perform(post("/cursos/{id}/lecciones/1/completar", ofimatica.getId()).with(LECTOR).with(csrf())
                        .param("versionId", String.valueOf(versionVigente(ofimatica, 2).getId())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("no corresponde a la lección")));
        assertEquals(avancesAntes, avanceRepository.findByAprendizId(lector).size());

        // Con la versión correcta sí se registra
        mockMvc.perform(post("/cursos/{id}/lecciones/1/completar", ofimatica.getId()).with(LECTOR).with(csrf())
                        .param("versionId", String.valueOf(versionVigente(ofimatica, 1).getId())))
                .andExpect(redirectedUrl("/cursos/" + ofimatica.getId() + "/lecciones/2"));

        // Volver a abrir una lección ya completada entrega la versión con la que la inició (ADR-08)
        mockMvc.perform(get("/cursos/{id}/lecciones/1", ofimatica.getId()).with(LECTOR))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(versionVigente(ofimatica, 1).getTranscripcion())));
    }

    @Test
    @DisplayName("El aprendiz continúa el curso donde lo dejó y el aviso de matrícula solo aparece al matricularse")
    void continuarCursoDondeLoDejo() throws Exception {
        String correo = "continuidad-" + System.nanoTime() + "@ecoaula.local";
        crearAprendiz(correo);
        RequestPostProcessor aprendiz = user(correo).roles("APRENDIZ");
        Curso ofimatica = curso("Ofimática");

        // Al matricularse: aviso de éxito y botón para comenzar
        String destino = mockMvc.perform(post("/cursos/{id}/matricular", ofimatica.getId()).with(aprendiz).with(csrf()))
                .andExpect(flash().attribute("matriculaRecienCreada", true))
                .andReturn().getResponse().getRedirectedUrl();
        Long matriculaId = matriculaRepository.findByAprendizCorreoAndCursoId(correo, ofimatica.getId()).orElseThrow().getId();
        mockMvc.perform(get(destino).with(aprendiz).flashAttr("matriculaRecienCreada", true))
                .andExpect(content().string(containsString("Su matrícula ha sido registrada con éxito")))
                .andExpect(content().string(containsString("Comenzar curso")));

        // Completa la lección 1 y sale del curso
        mockMvc.perform(post("/cursos/{id}/lecciones/1/completar", ofimatica.getId()).with(aprendiz).with(csrf())
                        .param("versionId", String.valueOf(versionVigente(ofimatica, 1).getId())))
                .andExpect(status().is3xxRedirection());

        // El catálogo lleva a "Mi curso", que continúa en la lección 2 y no repite el aviso de matrícula
        mockMvc.perform(get("/cursos").with(aprendiz))
                .andExpect(content().string(containsString("/matriculas/" + matriculaId + "\"")));
        mockMvc.perform(get("/matriculas/{id}", matriculaId).with(aprendiz))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1 de 3 lecciones completadas")))
                .andExpect(content().string(containsString("Continuar con la lección 2")))
                .andExpect(content().string(containsString("(completada)")))
                .andExpect(content().string(not(containsString("registrada con éxito"))))
                .andExpect(content().string(not(containsString("Comenzar curso"))));
        mockMvc.perform(get("/matriculas/{id}/confirmacion", matriculaId).with(aprendiz))
                .andExpect(content().string(not(containsString("registrada con éxito"))));

        // La lección enlaza de vuelta a su avance
        mockMvc.perform(get("/cursos/{id}/lecciones/2", ofimatica.getId()).with(aprendiz))
                .andExpect(content().string(containsString("Mi avance en el curso")));

        // Al completar todas, el siguiente paso es la evaluación
        for (int n = 2; n <= 3; n++) {
            mockMvc.perform(post("/cursos/{id}/lecciones/{n}/completar", ofimatica.getId(), n).with(aprendiz).with(csrf())
                            .param("versionId", String.valueOf(versionVigente(ofimatica, n).getId())))
                    .andExpect(status().is3xxRedirection());
        }
        mockMvc.perform(get("/matriculas/{id}", matriculaId).with(aprendiz))
                .andExpect(content().string(containsString("3 de 3 lecciones completadas")))
                .andExpect(content().string(containsString("Presentar evaluación final")));
    }

    private void crearAprendiz(String correo) {
        Aprendiz nuevo = new Aprendiz();
        nuevo.setNombreCompleto("Aprendiz de Pruebas");
        nuevo.setCorreo(correo);
        nuevo.setTelefono("3001112233");
        nuevo.setPasswordHash("{noop}no-se-usa");
        nuevo.setRol(Rol.APRENDIZ);
        nuevo.setConsentimientoDatos(true);
        nuevo.setFechaConsentimiento(LocalDateTime.now());
        nuevo.setPerfilAccesibilidad(new PerfilAccesibilidad(nuevo));
        aprendizRepository.save(nuevo);
    }

    private Curso curso(String fragmentoTitulo) {
        return cursoRepository.findAll().stream()
                .filter(c -> c.getTitulo().contains(fragmentoTitulo))
                .findFirst().orElseThrow();
    }

    private ContenidoVersion versionVigente(Curso curso, int orden) {
        Contenido leccion = contenidoRepository.findByCursoIdAndOrden(curso.getId(), orden).orElseThrow();
        return versionRepository.findByContenidoIdAndEstado(leccion.getId(), EstadoContenido.PUBLICADA).orElseThrow();
    }

    private void asegurarMatricula(Curso curso) throws Exception {
        if (!matriculaRepository.existsByAprendizCorreoAndCursoId(CORREO_LECTOR, curso.getId())) {
            mockMvc.perform(post("/cursos/{id}/matricular", curso.getId()).with(LECTOR).with(csrf()))
                    .andExpect(status().is3xxRedirection());
        }
    }

    private static String[] todosLosCriterios() {
        return Arrays.stream(CriterioAccesibilidad.values()).map(Enum::name).toArray(String[]::new);
    }
}
