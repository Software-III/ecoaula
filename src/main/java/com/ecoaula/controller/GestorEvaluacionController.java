package com.ecoaula.controller;

import com.ecoaula.exception.EcoAulaException;
import com.ecoaula.model.Curso;
import com.ecoaula.service.GestionEvaluacionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

/**
 * Pantalla del gestor para crear la evaluación final de un curso (Proceso 3). Solo el rol GESTOR_CONTENIDO
 * entra a /gestor/** (SecurityConfig). Los formularios son de una sola columna y cada campo tiene su etiqueta.
 */
@Controller
@RequestMapping("/gestor/cursos/{cursoId}/evaluacion")
public class GestorEvaluacionController {

    private final GestionEvaluacionService evaluacionService;

    public GestorEvaluacionController(GestionEvaluacionService evaluacionService) {
        this.evaluacionService = evaluacionService;
    }

    @GetMapping
    public String ver(@PathVariable("cursoId") Long cursoId, Model model) {
        Curso curso = evaluacionService.obtenerCurso(cursoId);
        model.addAttribute("titulo", "Evaluación final de: " + curso.getTitulo());
        model.addAttribute("curso", curso);
        model.addAttribute("preguntas", evaluacionService.listarPreguntas(cursoId));
        model.addAttribute("bloqueo", evaluacionService.motivoDeBloqueo(cursoId));
        model.addAttribute("minPreguntas", GestionEvaluacionService.MIN_PREGUNTAS_PARA_PUBLICAR);
        model.addAttribute("maxEnunciado", GestionEvaluacionService.MAX_ENUNCIADO);
        model.addAttribute("maxOpcion", GestionEvaluacionService.MAX_OPCION);
        return "gestor/evaluacion";
    }

    @PostMapping("/preguntas")
    public String agregar(@PathVariable("cursoId") Long cursoId,
                          @RequestParam(value = "enunciado", required = false) String enunciado,
                          @RequestParam(value = "opcion1", required = false) String opcion1,
                          @RequestParam(value = "opcion2", required = false) String opcion2,
                          @RequestParam(value = "opcion3", required = false) String opcion3,
                          @RequestParam(value = "opcion4", required = false) String opcion4,
                          @RequestParam(value = "correcta", required = false) Integer correcta,
                          RedirectAttributes redirectAttributes) {
        List<String> opciones = Arrays.asList(opcion1, opcion2, opcion3, opcion4);
        try {
            evaluacionService.agregarPregunta(cursoId, enunciado, opciones, correcta == null ? 0 : correcta);
            redirectAttributes.addFlashAttribute("mensajeExito", "Pregunta agregada a la evaluación final.");
        } catch (EcoAulaException ex) {
            // Se devuelve lo escrito para que el gestor corrija sin volver a teclearlo todo
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
            redirectAttributes.addFlashAttribute("enunciadoPrevio", enunciado);
            redirectAttributes.addFlashAttribute("opcion1Previa", opcion1);
            redirectAttributes.addFlashAttribute("opcion2Previa", opcion2);
            redirectAttributes.addFlashAttribute("opcion3Previa", opcion3);
            redirectAttributes.addFlashAttribute("opcion4Previa", opcion4);
            redirectAttributes.addFlashAttribute("correctaPrevia", correcta);
        }
        return "redirect:/gestor/cursos/" + cursoId + "/evaluacion#seccion-nueva-pregunta";
    }

    @PostMapping("/preguntas/{preguntaId}/eliminar")
    public String eliminar(@PathVariable("cursoId") Long cursoId,
                           @PathVariable("preguntaId") Long preguntaId,
                           @RequestParam(value = "confirmar", defaultValue = "false") boolean confirmar,
                           RedirectAttributes redirectAttributes) {
        if (!confirmar) {
            redirectAttributes.addFlashAttribute("mensajeError", "Marque la casilla de confirmación para eliminar la pregunta.");
            return "redirect:/gestor/cursos/" + cursoId + "/evaluacion";
        }
        try {
            evaluacionService.eliminarPregunta(cursoId, preguntaId);
            redirectAttributes.addFlashAttribute("mensajeExito", "Pregunta eliminada. Las siguientes se renumeraron.");
        } catch (EcoAulaException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/gestor/cursos/" + cursoId + "/evaluacion";
    }
}
