package com.ecoaula.integration;

import com.ecoaula.model.Contenido;
import com.ecoaula.model.Curso;
import com.ecoaula.model.EventoAprendizaje;
import com.ecoaula.model.ReporteBarrera;
import com.ecoaula.model.enums.EstadoReporte;
import com.ecoaula.model.enums.TipoEvento;
import com.ecoaula.repository.ContenidoRepository;
import com.ecoaula.repository.CursoRepository;
import com.ecoaula.repository.EventoAprendizajeRepository;
import com.ecoaula.repository.ReporteBarreraRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Comparator;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba de integración del reporte de barreras de accesibilidad y del registro de eventos que
 * alimenta los indicadores de calidad de los Procesos 1 y 2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev-h2")
class FlujoReportesYAnaliticaIntegrationTest {

    private static final RequestPostProcessor COORDINADOR = user("coordinador@ecoaula.local").roles("COORDINADOR");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReporteBarreraRepository reporteRepository;

    @Autowired
    private EventoAprendizajeRepository eventoRepository;

    @Autowired
    private CursoRepository cursoRepository;

    @Autowired
    private ContenidoRepository contenidoRepository;

    @Test
    @DisplayName("Reportar una barrera sin sesión y con sesión, atenderla y que el aprendiz vea la respuesta")
    void reporteDeBarreras() throws Exception {
        // Sin sesión: el formulario es público, porque la barrera puede impedir registrarse
        mockMvc.perform(get("/reportar-barrera").param("etapa", "REGISTRO_MATRICULA"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reportar una barrera de accesibilidad")));
        mockMvc.perform(post("/reportar-barrera").with(csrf()).param("etapa", "REGISTRO_MATRICULA").param("descripcion", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Describa la barrera que encontró")));
        mockMvc.perform(post("/reportar-barrera").with(csrf())
                        .param("etapa", "REGISTRO_MATRICULA")
                        .param("descripcion", "El campo de contraseña no anuncia los requisitos.")
                        .param("correoContacto", "persona@correo.co"))
                .andExpect(redirectedUrl("/reportar-barrera/enviado"))
                .andExpect(flash().attributeExists("numeroReporte"));
        ReporteBarrera anonimo = ultimoReporte();
        assertNull(anonimo.getCorreoAprendiz());
        assertEquals("persona@correo.co", anonimo.getCorreoContacto());

        // Con sesión y desde una lección: queda asociado a la cuenta y al lugar
        Curso ofimatica = curso("Ofimática");
        Contenido leccion = contenidoRepository.findByCursoIdAndOrden(ofimatica.getId(), 2).orElseThrow();
        String correo = "reporta-" + System.nanoTime() + "@ecoaula.local";
        RequestPostProcessor aprendiz = user(correo).roles("APRENDIZ");
        mockMvc.perform(get("/reportar-barrera").with(aprendiz)
                        .param("etapa", "CONTENIDO").param("curso", ofimatica.getId().toString()).param("leccion", leccion.getId().toString()))
                .andExpect(content().string(containsString("Lección 2: " + leccion.getTitulo())));
        mockMvc.perform(post("/reportar-barrera").with(aprendiz).with(csrf())
                        .param("etapa", "CONTENIDO").param("cursoId", ofimatica.getId().toString())
                        .param("contenidoId", leccion.getId().toString())
                        .param("descripcion", "El audio se corta a la mitad.").param("tecnologiaAsistencia", "NVDA con Firefox"))
                .andExpect(redirectedUrl("/reportar-barrera/enviado"));
        ReporteBarrera conSesion = ultimoReporte();
        assertEquals(correo, conSesion.getCorreoAprendiz());
        assertEquals(leccion.getId(), conSesion.getContenidoId());
        assertTrue(conSesion.getContexto().contains("Lección 2"));

        // El aprendiz no atiende reportes; el coordinador sí
        mockMvc.perform(get("/reportes-barrera").with(aprendiz)).andExpect(status().isForbidden());
        mockMvc.perform(get("/reportes-barrera").with(COORDINADOR).param("etapa", "CONTENIDO"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Reporte número " + conSesion.getId())));

        // Resolver exige una respuesta para el aprendiz
        mockMvc.perform(post("/reportes-barrera/{id}", conSesion.getId()).with(COORDINADOR).with(csrf()).param("estado", "RESUELTO"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Escriba la respuesta para el aprendiz")));
        mockMvc.perform(post("/reportes-barrera/{id}", conSesion.getId()).with(COORDINADOR).with(csrf())
                        .param("estado", "RESUELTO").param("respuesta", "Se reemplazó el archivo de audio de la lección."))
                .andExpect(redirectedUrl("/reportes-barrera"));
        ReporteBarrera resuelto = reporteRepository.findById(conSesion.getId()).orElseThrow();
        assertEquals(EstadoReporte.RESUELTO, resuelto.getEstado());
        assertEquals("coordinador@ecoaula.local", resuelto.getAtendidoPor());

        mockMvc.perform(get("/mis-reportes").with(aprendiz))
                .andExpect(content().string(containsString("Se reemplazó el archivo de audio de la lección.")))
                .andExpect(content().string(containsString("resuelto.")));
    }

    @Test
    @DisplayName("El recorrido real del aprendiz deja eventos en la bitácora y el tablero los muestra al personal")
    void eventosDelRecorridoEIndicadores() throws Exception {
        MockHttpSession visita = new MockHttpSession();
        String correo = "recorrido-" + System.nanoTime() + "@ecoaula.local";

        // Proceso 1: abrir el registro (una sola vez por visita), crear la cuenta, iniciar sesión, matricularse
        mockMvc.perform(get("/registro").session(visita)).andExpect(status().isOk());
        mockMvc.perform(get("/registro").session(visita)).andExpect(status().isOk());
        mockMvc.perform(post("/registro").session(visita).with(csrf())
                        .param("nombreCompleto", "Aprendiz Recorrido").param("correo", correo).param("telefono", "3001234567")
                        .param("password", "ClaveSegura123").param("consentimientoDatos", "true")
                        .param("velocidadAudio", "1.0").param("modoVisual", "ESTANDAR").param("tamanoTexto", "NORMAL"))
                .andExpect(redirectedUrl("/login?registrado=true"));
        MockHttpSession sesion = (MockHttpSession) mockMvc.perform(formLogin("/login").user("correo", correo).password("password", "ClaveSegura123"))
                .andExpect(redirectedUrl("/cursos"))
                .andReturn().getRequest().getSession();

        Curso ofimatica = curso("Ofimática");
        Contenido leccion = contenidoRepository.findByCursoIdAndOrden(ofimatica.getId(), 1).orElseThrow();
        mockMvc.perform(post("/cursos/{id}/matricular", ofimatica.getId()).session(sesion).with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Proceso 2: abrir la lección y comenzar a reproducir el audio
        mockMvc.perform(get("/cursos/{id}/lecciones/1", ofimatica.getId()).session(sesion))
                .andExpect(content().string(containsString("data-evento-url=\"/eventos/reproduccion\"")));
        mockMvc.perform(post("/eventos/reproduccion").session(sesion).with(csrf())
                        .param("curso", ofimatica.getId().toString()).param("leccion", leccion.getId().toString()))
                .andExpect(status().isNoContent());

        List<TipoEvento> tipos = eventoRepository.findAll().stream()
                .filter(e -> correo.equals(e.getCorreo()))
                .sorted(Comparator.comparing(EventoAprendizaje::getId))
                .map(EventoAprendizaje::getTipo).toList();
        assertEquals(List.of(TipoEvento.REGISTRO_COMPLETADO, TipoEvento.INICIO_SESION, TipoEvento.MATRICULA_COMPLETADA,
                TipoEvento.LECCION_ABIERTA, TipoEvento.AUDIO_REPRODUCIDO), tipos);

        // El registro iniciado se anotó una sola vez para la visita, aunque se abrió dos veces
        String idVisita = eventoRepository.findAll().stream()
                .filter(e -> correo.equals(e.getCorreo()) && e.getTipo() == TipoEvento.REGISTRO_COMPLETADO)
                .findFirst().orElseThrow().getSesion();
        assertEquals(1, eventoRepository.findAll().stream()
                .filter(e -> e.getTipo() == TipoEvento.REGISTRO_INICIADO && idVisita.equals(e.getSesion())).count());

        // El aviso de reproducción solo se acepta en cursos con matrícula
        Curso finanzas = curso("Finanzas");
        mockMvc.perform(post("/eventos/reproduccion").session(sesion).with(csrf())
                        .param("curso", finanzas.getId().toString()).param("leccion", "1"))
                .andExpect(status().isForbidden());

        // Tablero: el personal lo ve; el aprendiz no
        mockMvc.perform(get("/indicadores").session(sesion)).andExpect(status().isForbidden());
        mockMvc.perform(get("/indicadores").with(COORDINADOR).param("dias", "7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Proceso 1: Inscripción y matrícula accesible")))
                .andExpect(content().string(containsString("Tiempo de finalización de la matrícula")))
                .andExpect(content().string(containsString("Tiempo de acceso al contenido")))
                .andExpect(content().string(containsString("Aprendices en riesgo de abandono")));
    }

    @Test
    @DisplayName("El coordinador inicia sesión en la atención de reportes")
    void inicioDelCoordinador() throws Exception {
        mockMvc.perform(formLogin("/login").user("correo", "coordinador@ecoaula.local").password("password", "CoordinadorEcoAula2026!"))
                .andExpect(redirectedUrl("/reportes-barrera"));
    }

    private ReporteBarrera ultimoReporte() {
        return reporteRepository.findAll().stream().max(Comparator.comparing(ReporteBarrera::getId)).orElseThrow();
    }

    private Curso curso(String fragmentoTitulo) {
        return cursoRepository.findAll().stream()
                .filter(c -> c.getTitulo().contains(fragmentoTitulo))
                .findFirst().orElseThrow();
    }
}
