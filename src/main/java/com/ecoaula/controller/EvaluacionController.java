package com.ecoaula.controller;

import com.ecoaula.model.*;
import com.ecoaula.model.enums.EstadoIntento;
import com.ecoaula.service.EvaluacionService;
import com.ecoaula.service.MatriculaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

/**
 * Controlador para la presentación accesible de la evaluación final, pregunta a pregunta (Pantalla 5).
 */
@Controller
public class EvaluacionController {

    private final EvaluacionService evaluacionService;
    private final MatriculaService matriculaService;

    public EvaluacionController(EvaluacionService evaluacionService,
                                MatriculaService matriculaService) {
        this.evaluacionService = evaluacionService;
        this.matriculaService = matriculaService;
    }

    @GetMapping("/matriculas/{id}/evaluacion")
    public String presentarEvaluacion(@PathVariable("id") Long matriculaId,
                                      @RequestParam(value = "p", defaultValue = "1") int indicePregunta,
                                      Principal principal,
                                      Model model) {
        Matricula matricula = matriculaService.obtenerPorIdYValidarPropiedad(matriculaId, principal.getName());
        IntentoEvaluacion intento = evaluacionService.iniciarOContinuarIntento(matriculaId, principal.getName());

        // Si ya está calificado, redirigir según aprobación
        if (intento.getEstado() == EstadoIntento.CALIFICADO) {
            if (Boolean.TRUE.equals(intento.getAprobado()) && matricula.getCertificado() != null) {
                return "redirect:/certificados/" + matricula.getCertificado().getId();
            }
            return "redirect:/matriculas/" + matriculaId + "/evaluacion/resultado?intentoId=" + intento.getId();
        }

        Evaluacion evaluacion = evaluacionService.obtenerPorCurso(matricula.getCurso().getId());
        List<Pregunta> preguntas = evaluacionService.listarPreguntas(evaluacion.getId());
        int totalPreguntas = preguntas.size();

        if (indicePregunta < 1 || indicePregunta > totalPreguntas) {
            indicePregunta = 1;
        }

        Pregunta preguntaActual = evaluacionService.obtenerPreguntaPorIndice(evaluacion.getId(), indicePregunta);
        Optional<OpcionPregunta> respuestaPrevia = evaluacionService.obtenerRespuestaPrevia(intento.getId(), preguntaActual.getId());

        model.addAttribute("titulo", "Evaluación: Pregunta " + indicePregunta + " de " + totalPreguntas + " — " + matricula.getCurso().getTitulo());
        model.addAttribute("matricula", matricula);
        model.addAttribute("intento", intento);
        model.addAttribute("pregunta", preguntaActual);
        model.addAttribute("indice", indicePregunta);
        model.addAttribute("totalPreguntas", totalPreguntas);
        model.addAttribute("esUltima", indicePregunta >= totalPreguntas);
        model.addAttribute("opcionSeleccionadaId", respuestaPrevia.map(OpcionPregunta::getId).orElse(null));

        return "evaluaciones/pregunta";
    }

    @PostMapping("/matriculas/{id}/evaluacion")
    public String responderPregunta(@PathVariable("id") Long matriculaId,
                                    @RequestParam("intentoId") Long intentoId,
                                    @RequestParam("preguntaId") Long preguntaId,
                                    @RequestParam("opcionId") Long opcionId,
                                    @RequestParam("indice") int indice,
                                    @RequestParam(value = "esUltima", defaultValue = "false") boolean esUltima,
                                    Principal principal) {
        matriculaService.obtenerPorIdYValidarPropiedad(matriculaId, principal.getName());
        evaluacionService.guardarRespuesta(intentoId, preguntaId, opcionId);

        if (esUltima) {
            // Calificar intento y emitir certificado si aprueba
            IntentoEvaluacion calificado = evaluacionService.calificarIntento(intentoId);
            if (Boolean.TRUE.equals(calificado.getAprobado())) {
                Matricula matricula = matriculaService.obtenerPorIdYValidarPropiedad(matriculaId, principal.getName());
                return "redirect:/certificados/" + matricula.getCertificado().getId();
            } else {
                return "redirect:/matriculas/" + matriculaId + "/evaluacion/resultado?intentoId=" + intentoId;
            }
        }

        return "redirect:/matriculas/" + matriculaId + "/evaluacion?p=" + (indice + 1);
    }

    @GetMapping("/matriculas/{id}/evaluacion/resultado")
    public String verResultado(@PathVariable("id") Long matriculaId,
                               @RequestParam("intentoId") Long intentoId,
                               Principal principal,
                               Model model) {
        Matricula matricula = matriculaService.obtenerPorIdYValidarPropiedad(matriculaId, principal.getName());
        IntentoEvaluacion intento = evaluacionService.iniciarOContinuarIntento(matriculaId, principal.getName());

        model.addAttribute("titulo", "Resultado de la Evaluación — " + matricula.getCurso().getTitulo());
        model.addAttribute("matricula", matricula);
        model.addAttribute("intento", intento);
        model.addAttribute("umbral", evaluacionService.getUmbralAprobacion());

        return "evaluaciones/resultado";
    }

    @PostMapping("/matriculas/{id}/evaluacion/reintentar")
    public String reintentarEvaluacion(@PathVariable("id") Long matriculaId,
                                       Principal principal) {
        matriculaService.obtenerPorIdYValidarPropiedad(matriculaId, principal.getName());
        return "redirect:/matriculas/" + matriculaId + "/evaluacion?p=1";
    }
}
