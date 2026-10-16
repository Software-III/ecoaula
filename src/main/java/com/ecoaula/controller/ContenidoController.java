package com.ecoaula.controller;

import com.ecoaula.analitica.RegistroEventosService;
import com.ecoaula.model.Contenido;
import com.ecoaula.model.enums.TipoEvento;
import jakarta.servlet.http.HttpSession;
import com.ecoaula.model.ContenidoVersion;
import com.ecoaula.model.Curso;
import com.ecoaula.model.Matricula;
import com.ecoaula.service.ContenidoService;
import com.ecoaula.service.CursoService;
import com.ecoaula.service.MatriculaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

/**
 * Controlador para la visualización accesible de lecciones y avance por versión (Pantalla 4).
 */
@Controller
public class ContenidoController {

    private final CursoService cursoService;
    private final ContenidoService contenidoService;
    private final MatriculaService matriculaService;
    private final RegistroEventosService registroEventos;

    public ContenidoController(CursoService cursoService,
                               ContenidoService contenidoService,
                               MatriculaService matriculaService,
                             RegistroEventosService registroEventos) {
        this.cursoService = cursoService;
        this.contenidoService = contenidoService;
        this.matriculaService = matriculaService;
        this.registroEventos = registroEventos;
    }

    @GetMapping("/cursos/{id}/lecciones/{n}")
    public String verLeccion(@PathVariable("id") Long cursoId,
                             @PathVariable("n") Integer orden,
                             Principal principal,
                             HttpSession session,
                             Model model) {
        // Solo los aprendices matriculados acceden a las lecciones; la matrícula se hace desde el catálogo
        contenidoService.verificarMatricula(principal.getName(), cursoId);

        Curso curso = cursoService.obtenerPorId(cursoId);
        Contenido leccion = contenidoService.obtenerLeccionPorOrden(cursoId, orden);
        ContenidoVersion version = contenidoService.obtenerVersionParaAprendiz(leccion.getId(), principal.getName());
        long totalLecciones = contenidoService.contarLeccionesEnCurso(cursoId);
        boolean completada = contenidoService.estaLeccionCompletada(principal.getName(), leccion.getId());
        boolean esUltimaLeccion = (orden >= totalLecciones);

        registroEventos.registrar(TipoEvento.LECCION_ABIERTA, session, principal.getName(), cursoId, leccion.getId());

        model.addAttribute("titulo", "Lección " + orden + ": " + leccion.getTitulo() + " — " + curso.getTitulo());
        model.addAttribute("curso", curso);
        model.addAttribute("leccion", leccion);
        model.addAttribute("version", version);
        model.addAttribute("imagenes", contenidoService.listarImagenes(version.getId()));
        model.addAttribute("orden", orden);
        model.addAttribute("totalLecciones", totalLecciones);
        model.addAttribute("completada", completada);
        model.addAttribute("esUltimaLeccion", esUltimaLeccion);
        model.addAttribute("matricula", matriculaService.obtenerPorAprendizYCurso(principal.getName(), cursoId));

        return "cursos/leccion";
    }

    @PostMapping("/cursos/{id}/lecciones/{n}/completar")
    public String completarLeccion(@PathVariable("id") Long cursoId,
                                   @PathVariable("n") Integer orden,
                                   @RequestParam("versionId") Long versionId,
                                   Principal principal,
                                   HttpSession session) {
        // Registrar avance ligado a la versión exacta que se le entregó al aprendiz (ADR-08)
        contenidoService.registrarAvanceEnLeccion(principal.getName(), cursoId, orden, versionId);
        registroEventos.registrar(TipoEvento.LECCION_COMPLETADA, session, principal.getName(), cursoId,
                contenidoService.obtenerLeccionPorOrden(cursoId, orden).getId());

        long totalLecciones = contenidoService.contarLeccionesEnCurso(cursoId);
        if (orden >= totalLecciones) {
            // Al completar la última lección, redirigir a la evaluación del curso
            Matricula matricula = matriculaService.obtenerPorAprendizYCurso(principal.getName(), cursoId);
            return "redirect:/matriculas/" + matricula.getId() + "/evaluacion";
        }

        // Si quedan lecciones, avanzar a la siguiente
        return "redirect:/cursos/" + cursoId + "/lecciones/" + (orden + 1);
    }

    @GetMapping("/cursos/{id}/lecciones/{n}/transcripcion/descargar")
    public ResponseEntity<byte[]> descargarTranscripcion(@PathVariable("id") Long cursoId,
                                                         @PathVariable("n") Integer orden,
                                                         Principal principal) {
        contenidoService.verificarMatricula(principal.getName(), cursoId);
        Curso curso = cursoService.obtenerPorId(cursoId);
        Contenido leccion = contenidoService.obtenerLeccionPorOrden(cursoId, orden);
        ContenidoVersion version = contenidoService.obtenerVersionParaAprendiz(leccion.getId(), principal.getName());

        byte[] contenidoTxt = contenidoService.generarArchivoTranscripcion(
                curso.getTitulo(), leccion.getTitulo(), orden, version.getTranscripcion(),
                contenidoService.listarImagenes(version.getId())
        );

        String nombreDescarga = "transcripcion-leccion-" + orden + ".txt";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreDescarga + "\"")
                .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                .body(contenidoTxt);
    }
}
