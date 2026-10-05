package com.ecoaula.controller;

import com.ecoaula.model.Contenido;
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
import java.util.List;

/**
 * Controlador para la visualización accesible de lecciones y avance por versión (Pantalla 4).
 */
@Controller
public class ContenidoController {

    private final CursoService cursoService;
    private final ContenidoService contenidoService;
    private final MatriculaService matriculaService;

    public ContenidoController(CursoService cursoService,
                               ContenidoService contenidoService,
                               MatriculaService matriculaService) {
        this.cursoService = cursoService;
        this.contenidoService = contenidoService;
        this.matriculaService = matriculaService;
    }

    @GetMapping("/cursos/{id}/lecciones/{n}")
    public String verLeccion(@PathVariable("id") Long cursoId,
                             @PathVariable("n") Integer orden,
                             Principal principal,
                             Model model) {
        // Validar que el usuario esté matriculado
        if (!matriculaService.estaMatriculado(principal.getName(), cursoId)) {
            // Si no está matriculado, matricularlo automáticamente o redirigir
            matriculaService.matricular(principal.getName(), cursoId);
        }

        Curso curso = cursoService.obtenerPorId(cursoId);
        Contenido leccion = contenidoService.obtenerLeccionPorOrden(cursoId, orden);
        ContenidoVersion version = contenidoService.obtenerVersionParaAprendiz(leccion.getId(), principal.getName());
        long totalLecciones = contenidoService.contarLeccionesEnCurso(cursoId);
        boolean completada = contenidoService.estaLeccionCompletada(principal.getName(), leccion.getId());
        boolean esUltimaLeccion = (orden >= totalLecciones);

        model.addAttribute("titulo", "Lección " + orden + ": " + leccion.getTitulo() + " — " + curso.getTitulo());
        model.addAttribute("curso", curso);
        model.addAttribute("leccion", leccion);
        model.addAttribute("version", version);
        model.addAttribute("orden", orden);
        model.addAttribute("totalLecciones", totalLecciones);
        model.addAttribute("completada", completada);
        model.addAttribute("esUltimaLeccion", esUltimaLeccion);

        return "cursos/leccion";
    }

    @PostMapping("/cursos/{id}/lecciones/{n}/completar")
    public String completarLeccion(@PathVariable("id") Long cursoId,
                                   @PathVariable("n") Integer orden,
                                   @RequestParam("versionId") Long versionId,
                                   Principal principal) {
        // Registrar avance ligado a la versión exacta (ADR-08)
        contenidoService.registrarAvance(principal.getName(), versionId);

        long totalLecciones = contenidoService.contarLeccionesEnCurso(cursoId);
        if (orden >= totalLecciones) {
            // Al completar la última lección, redirigir a la evaluación del curso
            List<Matricula> matriculas = matriculaService.listarPorAprendiz(principal.getName());
            Matricula matricula = matriculas.stream()
                    .filter(m -> m.getCurso().getId().equals(cursoId))
                    .findFirst()
                    .orElseThrow();
            return "redirect:/matriculas/" + matricula.getId() + "/evaluacion";
        }

        // Si quedan lecciones, avanzar a la siguiente
        return "redirect:/cursos/" + cursoId + "/lecciones/" + (orden + 1);
    }

    @GetMapping("/cursos/{id}/lecciones/{n}/transcripcion/descargar")
    public ResponseEntity<byte[]> descargarTranscripcion(@PathVariable("id") Long cursoId,
                                                         @PathVariable("n") Integer orden,
                                                         Principal principal) {
        Curso curso = cursoService.obtenerPorId(cursoId);
        Contenido leccion = contenidoService.obtenerLeccionPorOrden(cursoId, orden);
        ContenidoVersion version = contenidoService.obtenerVersionParaAprendiz(leccion.getId(), principal.getName());

        byte[] contenidoTxt = contenidoService.generarArchivoTranscripcion(
                curso.getTitulo(), leccion.getTitulo(), orden, version.getTranscripcion()
        );

        String nombreDescarga = "transcripcion-leccion-" + orden + ".txt";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreDescarga + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(contenidoTxt);
    }
}
