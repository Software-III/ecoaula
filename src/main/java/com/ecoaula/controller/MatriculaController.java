package com.ecoaula.controller;

import com.ecoaula.model.Curso;
import com.ecoaula.model.Matricula;
import com.ecoaula.service.CursoService;
import com.ecoaula.service.MatriculaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador para catálogo de cursos y proceso de matrícula (Pantallas 2 y 3).
 */
@Controller
public class MatriculaController {

    private final CursoService cursoService;
    private final MatriculaService matriculaService;

    public MatriculaController(CursoService cursoService,
                               MatriculaService matriculaService) {
        this.cursoService = cursoService;
        this.matriculaService = matriculaService;
    }

    @GetMapping("/cursos")
    public String catalogoCursos(@RequestParam(value = "q", required = false) String termino,
                                 Principal principal,
                                 Model model) {
        model.addAttribute("titulo", "Catálogo de Cursos Accesibles");
        List<Curso> cursos = cursoService.buscarCursos(termino);
        model.addAttribute("cursos", cursos);
        model.addAttribute("termino", termino);

        // Mapear qué cursos ya están matriculados por el aprendiz autenticado
        Map<Long, Long> matriculasPorCurso = new HashMap<>();
        if (principal != null) {
            List<Matricula> matriculas = matriculaService.listarPorAprendiz(principal.getName());
            for (Matricula m : matriculas) {
                matriculasPorCurso.put(m.getCurso().getId(), m.getId());
            }
        }
        model.addAttribute("matriculasPorCurso", matriculasPorCurso);

        return "cursos/catalogo";
    }

    @PostMapping("/cursos/{id}/matricular")
    public String procesarMatricula(@PathVariable("id") Long cursoId,
                                    Principal principal) {
        Matricula matricula = matriculaService.matricular(principal.getName(), cursoId);
        return "redirect:/matriculas/" + matricula.getId() + "/confirmacion";
    }

    @GetMapping("/matriculas/{id}/confirmacion")
    public String confirmacionMatricula(@PathVariable("id") Long matriculaId,
                                        Principal principal,
                                        Model model) {
        Matricula matricula = matriculaService.obtenerPorIdYValidarPropiedad(matriculaId, principal.getName());
        model.addAttribute("titulo", "Matrícula Confirmada — " + matricula.getCurso().getTitulo());
        model.addAttribute("matricula", matricula);
        model.addAttribute("curso", matricula.getCurso());
        return "matriculas/confirmacion";
    }
}
