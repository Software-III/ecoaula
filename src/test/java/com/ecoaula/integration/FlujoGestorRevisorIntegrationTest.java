package com.ecoaula.integration;

import com.ecoaula.model.Contenido;
import com.ecoaula.model.ContenidoVersion;
import com.ecoaula.model.Curso;
import com.ecoaula.model.RevisionAccesibilidad;
import com.ecoaula.model.enums.CriterioAccesibilidad;
import com.ecoaula.model.enums.EstadoContenido;
import com.ecoaula.model.enums.ResultadoRevision;
import com.ecoaula.repository.ContenidoRepository;
import com.ecoaula.repository.ContenidoVersionRepository;
import com.ecoaula.repository.CursoRepository;
import com.ecoaula.repository.RecursoImagenRepository;
import com.ecoaula.repository.RevisionAccesibilidadRepository;
import com.ecoaula.storage.AudioDemoGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Prueba de integración del Proceso 2 sobre el flujo del gestor (/gestor) y del revisor (/revisor):
 * catálogo con publicación validada, licencia del material, imágenes con texto alternativo, rechazo con
 * informe, subsanación inmutable, eliminación con reglas y vistas que cargan sin errores.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev-h2")
class FlujoGestorRevisorIntegrationTest {

    private static final RequestPostProcessor GESTOR = user("gestor@ecoaula.local").roles("GESTOR_CONTENIDO");
    private static final RequestPostProcessor REVISOR = user("revisor@ecoaula.local").roles("REVISOR_ACCESIBILIDAD");
    private static final RequestPostProcessor APRENDIZ = user("aprendiz@ecoaula.local").roles("APRENDIZ");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CursoRepository cursoRepository;

    @Autowired
    private ContenidoRepository contenidoRepository;

    @Autowired
    private ContenidoVersionRepository versionRepository;

    @Autowired
    private RevisionAccesibilidadRepository revisionRepository;

    @Autowired
    private RecursoImagenRepository imagenRepository;

    @Test
    @DisplayName("Curso nuevo: borrador con licencia e imagen, rechazo con informe, subsanación, aprobación y publicación validada")
    void flujoCompletoDeUnCursoNuevo() throws Exception {
        // El curso nace fuera del catálogo y respeta la duración de 8 a 40 horas
        mockMvc.perform(nuevoCurso("Curso inválido", "5")).andExpect(content().string(containsString("La duración mínima")));
        String titulo = "Inglés instrumental " + System.nanoTime();
        mockMvc.perform(nuevoCurso(titulo, "16")).andExpect(status().is3xxRedirection());
        Curso curso = curso(titulo);
        assertFalse(curso.isActivo());
        mockMvc.perform(get("/cursos").with(APRENDIZ)).andExpect(content().string(not(containsString(titulo))));

        // Agregar una lección crea su versión 1 en borrador (diseño de Leonardo)
        mockMvc.perform(post("/gestor/cursos/{id}/lecciones/nueva", curso.getId()).with(GESTOR).with(csrf())
                .param("titulo", "Saludos y presentaciones")).andExpect(status().is3xxRedirection());
        Contenido leccion = contenidoRepository.findByCursoIdAndOrden(curso.getId(), 1).orElseThrow();
        ContenidoVersion v1 = ultimaVersion(leccion);
        assertEquals(EstadoContenido.BORRADOR, v1.getEstado());

        // Publicar incompleto se rechaza con el detalle de lo que falta
        mockMvc.perform(post("/gestor/cursos/{id}/publicar", curso.getId()).with(GESTOR).with(csrf()))
                .andExpect(flash().attribute("mensajeError", containsString("no tiene una versión aprobada")));

        // Audio y transcripción sin licencia: se guarda, pero no se puede enviar a revisión
        mockMvc.perform(multipart("/gestor/versiones/{id}/editar", v1.getId())
                        .file(new MockMultipartFile("archivoAudio", "saludos.wav", "audio/wav", AudioDemoGenerator.generarTonoSuaveWav(1, 440.0)))
                        .with(GESTOR).with(csrf())
                        .param("transcripcion", "Hello significa hola."))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/gestor/versiones/{id}/enviar-revision", v1.getId()).with(GESTOR).with(csrf()))
                .andExpect(flash().attribute("mensajeError", containsString("Ley 23 de 1982")));

        // Imagen sin texto alternativo: rechazada; con texto alternativo: agregada
        mockMvc.perform(multipart("/gestor/versiones/{id}/imagenes", v1.getId()).file(imagen()).with(GESTOR).with(csrf())
                        .param("textoAlternativo", ""))
                .andExpect(flash().attribute("mensajeError", containsString("El texto alternativo es obligatorio")));
        mockMvc.perform(multipart("/gestor/versiones/{id}/imagenes", v1.getId()).file(imagen()).with(GESTOR).with(csrf())
                        .param("textoAlternativo", "Dos personas se dan la mano al presentarse"))
                .andExpect(flash().attribute("mensajeExito", containsString("Imagen agregada")));
        String rutaImagen = imagenRepository.findByContenidoVersionIdOrderByOrdenAsc(v1.getId()).get(0).getRutaArchivo();
        mockMvc.perform(get("/imagenes/{a}", rutaImagen).with(APRENDIZ)).andExpect(status().isNotFound());
        mockMvc.perform(get("/imagenes/{a}", rutaImagen).with(REVISOR)).andExpect(status().isOk());

        // Declarar licencia y enviar
        mockMvc.perform(multipart("/gestor/versiones/{id}/editar", v1.getId()).with(GESTOR).with(csrf())
                .param("transcripcion", "Hello significa hola.").param("fuenteMaterial", "Elaboración propia")
                .param("licenciaDeclarada", "true")).andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/gestor/versiones/{id}/enviar-revision", v1.getId()).with(GESTOR).with(csrf()))
                .andExpect(flash().attribute("mensajeExito", containsString("enviada a revisión")));

        // El revisor ve la versión completa (imagen, fuente) y la rechaza con los criterios que sí se cumplen
        mockMvc.perform(get("/revisor/versiones/{id}/revisar", v1.getId()).with(REVISOR))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dos personas se dan la mano al presentarse")))
                .andExpect(content().string(containsString("Elaboración propia")));
        mockMvc.perform(post("/revisor/versiones/{id}/rechazar", v1.getId()).with(REVISOR).with(csrf())
                        .param("motivo", "Los términos en inglés no están explicados.")
                        .param("criterios", CriterioAccesibilidad.TRANSCRIPCION_FIEL.getCodigo(), CriterioAccesibilidad.AUDIO_INTELIGIBLE.getCodigo()))
                .andExpect(redirectedUrl("/revisor/cola"));
        RevisionAccesibilidad rechazo = revisionRepository.findByVersionIdOrderByFechaAsc(v1.getId()).get(0);
        assertEquals(ResultadoRevision.RECHAZADA, rechazo.getResultado());
        assertEquals(2, rechazo.getItemsChequeados().size());
        mockMvc.perform(get("/informes-revision/{id}", rechazo.getId()).with(GESTOR))
                .andExpect(content().string(containsString("2 de 6 criterios cumplidos")))
                .andExpect(content().string(containsString("No cumple")))
                .andExpect(content().string(containsString("Los términos en inglés no están explicados.")));
        mockMvc.perform(get("/gestor/lecciones/{id}/versiones", leccion.getId()).with(GESTOR))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/informes-revision/" + rechazo.getId())));

        // Subsanación: nueva versión que hereda imagen y fuente; aprobar incompleto vuelve a la auditoría
        mockMvc.perform(post("/gestor/versiones/{id}/corregir", v1.getId()).with(GESTOR).with(csrf())).andExpect(status().is3xxRedirection());
        ContenidoVersion v2 = ultimaVersion(leccion);
        assertEquals(v1.getId(), versionRepository.findDetalleById(v2.getId()).orElseThrow().getVersionOrigen().getId());
        assertEquals(1, imagenRepository.findByContenidoVersionIdOrderByOrdenAsc(v2.getId()).size());
        mockMvc.perform(multipart("/gestor/versiones/{id}/editar", v2.getId()).with(GESTOR).with(csrf())
                .param("transcripcion", "La palabra en inglés hello significa hola.").param("fuenteMaterial", "Elaboración propia")
                .param("licenciaDeclarada", "true")).andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/gestor/versiones/{id}/enviar-revision", v2.getId()).with(GESTOR).with(csrf()));
        mockMvc.perform(post("/revisor/versiones/{id}/aprobar", v2.getId()).with(REVISOR).with(csrf())
                        .param("criterios", CriterioAccesibilidad.TRANSCRIPCION_FIEL.getCodigo()))
                .andExpect(redirectedUrl("/revisor/versiones/" + v2.getId() + "/revisar#seccion-chequeo"))
                .andExpect(flash().attribute("mensajeError", containsString("todos los criterios")));
        mockMvc.perform(post("/revisor/versiones/{id}/aprobar", v2.getId()).with(REVISOR).with(csrf())
                        .param("criterios", todosLosCriterios()))
                .andExpect(redirectedUrl("/revisor/cola"));
        assertEquals(EstadoContenido.PUBLICADA, versionRepository.findById(v2.getId()).orElseThrow().getEstado());

        // Lecciones aprobadas, pero sin evaluación final: aún no se publica
        mockMvc.perform(post("/gestor/cursos/{id}/publicar", curso.getId()).with(GESTOR).with(csrf()))
                .andExpect(flash().attribute("mensajeError", containsString("no tiene evaluación final")))
                .andExpect(flash().attribute("mensajeError", not(containsString("no tiene una versión aprobada"))));
    }

    @Test
    @DisplayName("Marcar la licencia y pulsar \"Guardar y enviar\" sin guardar antes envía la versión en un solo paso")
    void guardarYEnviarEnUnSoloPaso() throws Exception {
        String titulo = "Curso guardar y enviar " + System.nanoTime();
        mockMvc.perform(nuevoCurso(titulo, "8")).andExpect(status().is3xxRedirection());
        Curso curso = curso(titulo);
        mockMvc.perform(post("/gestor/cursos/{id}/lecciones/nueva", curso.getId()).with(GESTOR).with(csrf())
                .param("titulo", "Lección única")).andExpect(status().is3xxRedirection());
        ContenidoVersion v1 = ultimaVersion(contenidoRepository.findByCursoIdAndOrden(curso.getId(), 1).orElseThrow());

        // Guardar sin enviar: queda en borrador
        mockMvc.perform(multipart("/gestor/versiones/{id}/editar", v1.getId())
                        .file(new MockMultipartFile("archivoAudio", "a.wav", "audio/wav", AudioDemoGenerator.generarTonoSuaveWav(1, 440.0)))
                        .with(GESTOR).with(csrf()).param("transcripcion", "Texto de la lección."))
                .andExpect(redirectedUrl("/gestor/versiones/" + v1.getId() + "/editar"));
        assertEquals(EstadoContenido.BORRADOR, versionRepository.findById(v1.getId()).orElseThrow().getEstado());

        // Mismo envío que hace el navegador: casilla marcada y botón "Guardar y enviar" (accion=enviar)
        mockMvc.perform(multipart("/gestor/versiones/{id}/editar", v1.getId()).with(GESTOR).with(csrf())
                        .param("transcripcion", "Texto de la lección.")
                        .param("fuenteMaterial", "Elaboración propia")
                        .param("licenciaDeclarada", "true").param("_licenciaDeclarada", "on")
                        .param("accion", "enviar"))
                .andExpect(flash().attribute("mensajeExito", containsString("enviada a revisión")));
        ContenidoVersion enviada = versionRepository.findById(v1.getId()).orElseThrow();
        assertEquals(EstadoContenido.EN_REVISION, enviada.getEstado());
        assertNotNull(enviada.getLicenciaDeclaradaEn());
    }

    @Test
    @DisplayName("Modificar y eliminar: título de lección, eliminación con renumeración, curso completo y reglas del catálogo")
    void modificarYEliminar() throws Exception {
        String titulo = "Curso editable " + System.nanoTime();
        mockMvc.perform(nuevoCurso(titulo, "10")).andExpect(status().is3xxRedirection());
        Curso curso = curso(titulo);
        for (String leccion : List.of("Primera lección", "Segunda lección", "Tercera lección")) {
            mockMvc.perform(post("/gestor/cursos/{id}/lecciones/nueva", curso.getId()).with(GESTOR).with(csrf())
                    .param("titulo", leccion)).andExpect(status().is3xxRedirection());
        }
        mockMvc.perform(get("/gestor/cursos/{id}/lecciones", curso.getId()).with(GESTOR))
                .andExpect(content().string(containsString("Modificar o eliminar la lección 2")));

        Contenido segunda = contenidoRepository.findByCursoIdAndOrden(curso.getId(), 2).orElseThrow();
        mockMvc.perform(post("/gestor/lecciones/{id}/titulo", segunda.getId()).with(GESTOR).with(csrf()).param("titulo", "Segunda corregida"))
                .andExpect(flash().attribute("mensajeExito", containsString("actualizado")));

        Contenido primera = contenidoRepository.findByCursoIdAndOrden(curso.getId(), 1).orElseThrow();
        mockMvc.perform(post("/gestor/lecciones/{id}/eliminar", primera.getId()).with(GESTOR).with(csrf()))
                .andExpect(flash().attribute("mensajeError", containsString("Marque la casilla")));
        mockMvc.perform(post("/gestor/lecciones/{id}/eliminar", primera.getId()).with(GESTOR).with(csrf()).param("confirmar", "true"))
                .andExpect(flash().attribute("mensajeExito", containsString("renumeraron")));
        List<Contenido> restantes = contenidoRepository.findByCursoIdOrderByOrdenAsc(curso.getId());
        assertEquals(List.of("Segunda corregida", "Tercera lección"), restantes.stream().map(Contenido::getTitulo).toList());
        assertEquals(List.of(1, 2), restantes.stream().map(Contenido::getOrden).toList());

        mockMvc.perform(post("/gestor/cursos/{id}/eliminar", curso.getId()).with(GESTOR).with(csrf()).param("confirmar", "true"))
                .andExpect(redirectedUrl("/gestor/cursos"));
        assertFalse(cursoRepository.existsById(curso.getId()));

        // Un curso publicado no se modifica
        Curso ofimatica = curso("Ofimática");
        mockMvc.perform(post("/gestor/cursos/{id}/editar", ofimatica.getId()).with(GESTOR).with(csrf())
                        .param("titulo", "Otro título del curso").param("descripcion", "Otra descripción del curso")
                        .param("horas", "10").param("area", "Otra").param("modalidad", "Virtual accesible"))
                .andExpect(content().string(containsString("Retírelo del catálogo para modificar sus datos")));

        // Con una versión en revisión no se elimina la lección
        Curso enRevision = curso("Prueba: Correo electrónico accesible");
        Contenido leccionEnRevision = contenidoRepository.findByCursoIdAndOrden(enRevision.getId(), 1).orElseThrow();
        mockMvc.perform(post("/gestor/lecciones/{id}/eliminar", leccionEnRevision.getId()).with(GESTOR).with(csrf()).param("confirmar", "true"))
                .andExpect(flash().attribute("mensajeError", containsString("versión en revisión")));
    }

    @Test
    @DisplayName("Las pantallas del gestor y del revisor cargan con los datos de prueba (sin errores de carga diferida)")
    void pantallasCargan() throws Exception {
        mockMvc.perform(get("/gestor/cursos").with(GESTOR)).andExpect(status().isOk());
        for (Curso curso : cursoRepository.findAll()) {
            mockMvc.perform(get("/gestor/cursos/{id}/lecciones", curso.getId()).with(GESTOR)).andExpect(status().isOk());
            for (Contenido leccion : contenidoRepository.findByCursoIdOrderByOrdenAsc(curso.getId())) {
                mockMvc.perform(get("/gestor/lecciones/{id}/versiones", leccion.getId()).with(GESTOR)).andExpect(status().isOk());
                ContenidoVersion ultima = ultimaVersion(leccion);
                mockMvc.perform(get("/gestor/versiones/{id}/editar", ultima.getId()).with(GESTOR)).andExpect(status().isOk());
            }
        }
        mockMvc.perform(get("/revisor/cola").with(REVISOR))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Adjuntar archivos y organizar la bandeja")));
        for (ContenidoVersion v : versionRepository.findByEstado(EstadoContenido.EN_REVISION)) {
            mockMvc.perform(get("/revisor/versiones/{id}/revisar", v.getId()).with(REVISOR)).andExpect(status().isOk());
        }
        mockMvc.perform(get("/informes-revision").with(REVISOR)).andExpect(status().isOk());
        for (RevisionAccesibilidad r : revisionRepository.findAll()) {
            mockMvc.perform(get("/informes-revision/{id}", r.getId()).with(GESTOR)).andExpect(status().isOk());
        }
        mockMvc.perform(get("/revisor/indicadores").with(REVISOR)).andExpect(redirectedUrl("/indicadores"));
        mockMvc.perform(get("/informes-revision").with(APRENDIZ)).andExpect(status().isForbidden());
        assertNotNull(curso("Prueba: Inglés para el trabajo"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder nuevoCurso(String titulo, String horas) {
        return post("/gestor/cursos/nuevo").with(GESTOR).with(csrf())
                .param("titulo", titulo).param("descripcion", "Vocabulario básico para el trabajo.")
                .param("horas", horas).param("area", "Idiomas").param("modalidad", "Virtual accesible");
    }

    private ContenidoVersion ultimaVersion(Contenido leccion) {
        return versionRepository.findTopByContenidoIdOrderByNumeroVersionDesc(leccion.getId()).orElseThrow();
    }

    private Curso curso(String fragmentoTitulo) {
        return cursoRepository.findAll().stream()
                .filter(c -> c.getTitulo().contains(fragmentoTitulo))
                .findFirst().orElseThrow();
    }

    private static String[] todosLosCriterios() {
        return CriterioAccesibilidad.todos().stream().map(CriterioAccesibilidad::getCodigo).toArray(String[]::new);
    }

    private static MockMultipartFile imagen() throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", salida);
        return new MockMultipartFile("imagen", "saludo.png", "image/png", salida.toByteArray());
    }
}
