package com.ecoaula.integration;

import com.ecoaula.model.Aprendiz;
import com.ecoaula.model.Curso;
import com.ecoaula.model.PerfilAccesibilidad;
import com.ecoaula.model.enums.Rol;
import com.ecoaula.repository.AprendizRepository;
import com.ecoaula.repository.CursoRepository;
import com.ecoaula.repository.MatriculaRepository;
import com.ecoaula.repository.PerfilAccesibilidadRepository;
import com.ecoaula.repository.ContenidoRepository;
import com.ecoaula.repository.ContenidoVersionRepository;
import com.ecoaula.repository.RecursoImagenRepository;
import com.ecoaula.model.Contenido;
import com.ecoaula.model.ContenidoVersion;
import com.ecoaula.model.RecursoImagen;
import com.ecoaula.model.enums.EstadoContenido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba de integración del Proceso 1: cupo máximo de los cursos y preferencias de velocidad y voz
 * del perfil de accesibilidad aplicadas en el reproductor y en la lectura en voz alta (ADR-07).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev-h2")
class FlujoCupoYPerfilIntegrationTest {

    private static final RequestPostProcessor GESTOR = user("gestor@ecoaula.local").roles("GESTOR_CONTENIDO");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AprendizRepository aprendizRepository;

    @Autowired
    private CursoRepository cursoRepository;

    @Autowired
    private MatriculaRepository matriculaRepository;

    @Autowired
    private PerfilAccesibilidadRepository perfilRepository;

    @Autowired
    private ContenidoRepository contenidoRepository;

    @Autowired
    private ContenidoVersionRepository versionRepository;

    @Autowired
    private RecursoImagenRepository imagenRepository;

    @Test
    @DisplayName("Con el cupo lleno el catálogo no ofrece la matrícula y el servidor la rechaza")
    void cupoMaximoDelCurso() throws Exception {
        Curso curso = curso("Prueba: Inglés para el trabajo");
        if (curso.isActivo()) {
            mockMvc.perform(post("/gestor/cursos/{id}/retirar", curso.getId()).with(GESTOR).with(csrf()));
        }
        long matriculados = matriculaRepository.countByCursoId(curso.getId());

        // Cupo menor que los matriculados: se rechaza
        if (matriculados > 1) {
            mockMvc.perform(editarCurso(curso, String.valueOf(matriculados - 1)))
                    .andExpect(content().string(containsString("no puede ser menor")));
        }

        // Deja exactamente un cupo libre y publica
        mockMvc.perform(editarCurso(curso, String.valueOf(matriculados + 1))).andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/gestor/cursos/{id}/publicar", curso.getId()).with(GESTOR).with(csrf()))
                .andExpect(flash().attribute("mensajeExito", containsString("publicado en el catálogo")));

        RequestPostProcessor primero = aprendizNuevo();
        mockMvc.perform(get("/cursos").with(primero))
                .andExpect(content().string(containsString("Cupos disponibles: 1 de " + (matriculados + 1))));
        mockMvc.perform(post("/cursos/{id}/matricular", curso.getId()).with(primero).with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Sin cupos: el catálogo lo indica y no muestra el botón; forzar la petición también falla
        RequestPostProcessor segundo = aprendizNuevo();
        mockMvc.perform(get("/cursos").with(segundo))
                .andExpect(content().string(containsString("Cupos disponibles: 0 de " + (matriculados + 1))))
                .andExpect(content().string(containsString("no tiene cupos disponibles")));
        mockMvc.perform(post("/cursos/{id}/matricular", curso.getId()).with(segundo).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("no tiene cupos disponibles")));
        assertEquals(matriculados + 1, matriculaRepository.countByCursoId(curso.getId()));

        // El gestor ve la ocupación en el panel
        mockMvc.perform(get("/gestor/cursos").with(GESTOR))
                .andExpect(content().string(containsString((matriculados + 1) + " de " + (matriculados + 1) + " cupos ocupados")));
    }

    @Test
    @DisplayName("La velocidad y la voz del perfil llegan al reproductor y a la lectura en voz alta")
    void preferenciasDeVelocidadYVoz() throws Exception {
        String correo = "perfil-" + System.nanoTime() + "@ecoaula.local";
        RequestPostProcessor aprendiz = aprendizNuevo(correo);

        // Velocidad fuera de rango: se rechaza
        mockMvc.perform(post("/perfil").with(aprendiz).with(csrf())
                        .param("velocidadAudio", "5").param("modoVisual", "ESTANDAR").param("tamanoTexto", "NORMAL"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La velocidad máxima de reproducción es 2.0x")));

        // Velocidad y voz válidas: se guardan en la cuenta (ADR-07)
        mockMvc.perform(post("/perfil").with(aprendiz).with(csrf())
                        .param("velocidadAudio", "1.5").param("vozLectura", "Microsoft Sabina - Spanish (Mexico)")
                        .param("modoVisual", "ESTANDAR").param("tamanoTexto", "NORMAL"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Microsoft Sabina - Spanish (Mexico)")));
        PerfilAccesibilidad perfil = perfilRepository.findByAprendizCorreo(correo).orElseThrow();
        assertEquals(1.5, perfil.getVelocidadAudio());
        assertEquals("Microsoft Sabina - Spanish (Mexico)", perfil.getVozLectura());

        // En la lección: el audio declara la velocidad y el botón de lectura usa la voz y la velocidad
        Curso ofimatica = curso("Ofimática");
        mockMvc.perform(post("/cursos/{id}/matricular", ofimatica.getId()).with(aprendiz).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/cursos/{id}/lecciones/1", ofimatica.getId()).with(aprendiz))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-velocidad=\"1.5\"")))
                .andExpect(content().string(containsString("data-voz=\"Microsoft Sabina - Spanish (Mexico)\"")))
                .andExpect(content().string(containsString("Velocidad de reproducción configurada en su perfil")))
                .andExpect(content().string(containsString("/js/accesibilidad.js")));

        // Voz vacía: vuelve a la predeterminada del navegador
        mockMvc.perform(post("/perfil").with(aprendiz).with(csrf())
                        .param("velocidadAudio", "1.0").param("vozLectura", "  ")
                        .param("modoVisual", "ESTANDAR").param("tamanoTexto", "NORMAL"))
                .andExpect(status().isOk());
        assertNull(perfilRepository.findByAprendizCorreo(correo).orElseThrow().getVozLectura());

        // El script es público, como las hojas de estilo
        mockMvc.perform(get("/js/accesibilidad.js")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Modo solo audio: aviso, imágenes como texto (sin descargarlas) y sin datos técnicos; desactivado, imágenes normales")
    void modoSoloAudio() throws Exception {
        // Lección publicada con una imagen
        Curso servicio = curso("Atención al Cliente");
        Contenido leccion = contenidoRepository.findByCursoIdAndOrden(servicio.getId(), 1).orElseThrow();
        ContenidoVersion vigente = versionRepository.findByContenidoIdAndEstado(leccion.getId(), EstadoContenido.PUBLICADA).orElseThrow();
        if (imagenRepository.findByContenidoVersionIdOrderByOrdenAsc(vigente.getId()).isEmpty()) {
            imagenRepository.save(new RecursoImagen(vigente, "imagen-solo-audio.png", "Persona guiando a otra del brazo",
                    "La persona guía camina medio paso adelante y ofrece el codo.", 1));
        }
        String correo = "solo-audio-" + System.nanoTime() + "@ecoaula.local";
        RequestPostProcessor aprendiz = aprendizNuevo(correo);
        mockMvc.perform(post("/cursos/{id}/matricular", servicio.getId()).with(aprendiz).with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Activado
        mockMvc.perform(post("/perfil").with(aprendiz).with(csrf())
                        .param("velocidadAudio", "1.0").param("modoVisual", "ESTANDAR").param("tamanoTexto", "NORMAL")
                        .param("soloAudio", "true"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/cursos/{id}/lecciones/1", servicio.getId()).with(aprendiz))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"solo-audio \"")))
                .andExpect(content().string(containsString("Modo solo audio activo")))
                .andExpect(content().string(containsString("Descripción de las imágenes de la lección")))
                .andExpect(content().string(containsString("Persona guiando a otra del brazo.")))
                .andExpect(content().string(containsString("Descripción detallada: La persona guía camina medio paso adelante")))
                .andExpect(content().string(not(containsString("/imagenes/imagen-solo-audio.png"))))
                .andExpect(content().string(not(containsString("Versión del contenido consumida"))));

        // Desactivado
        mockMvc.perform(post("/perfil").with(aprendiz).with(csrf())
                        .param("velocidadAudio", "1.0").param("modoVisual", "ESTANDAR").param("tamanoTexto", "NORMAL"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/cursos/{id}/lecciones/1", servicio.getId()).with(aprendiz))
                .andExpect(content().string(not(containsString("Modo solo audio activo"))))
                .andExpect(content().string(containsString("/imagenes/imagen-solo-audio.png")))
                .andExpect(content().string(containsString("alt=\"Persona guiando a otra del brazo\"")))
                .andExpect(content().string(containsString("Versión del contenido consumida")));
    }

    private MockHttpServletRequestBuilder editarCurso(Curso curso, String cupo) {
        return post("/gestor/cursos/{id}/editar", curso.getId()).with(GESTOR).with(csrf())
                .param("titulo", curso.getTitulo()).param("descripcion", curso.getDescripcion())
                .param("horas", String.valueOf(curso.getHoras())).param("area", curso.getArea())
                .param("modalidad", curso.getModalidad())
                .param("cupoMaximo", cupo);
    }

    private RequestPostProcessor aprendizNuevo() {
        return aprendizNuevo("cupo-" + System.nanoTime() + "@ecoaula.local");
    }

    private RequestPostProcessor aprendizNuevo(String correo) {
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
        return user(correo).roles("APRENDIZ");
    }

    private Curso curso(String fragmentoTitulo) {
        return cursoRepository.findAll().stream()
                .filter(c -> c.getTitulo().contains(fragmentoTitulo))
                .findFirst().orElseThrow();
    }
}
