package com.ecoaula.controller;

import com.ecoaula.analitica.RegistroEventosService;
import com.ecoaula.model.Curso;
import com.ecoaula.model.enums.TipoEvento;
import jakarta.servlet.http.HttpSession;
import com.ecoaula.model.Matricula;
import com.ecoaula.service.ContenidoService;
import com.ecoaula.service.CursoService;
import com.ecoaula.service.MatriculaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
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
    private final ContenidoService contenidoService;
    private final RegistroEventosService registroEventos;

    public MatriculaController(CursoService cursoService,
                               MatriculaService matriculaService,
                               ContenidoService contenidoService,
                               RegistroEventosService registroEventos) {
        this.cursoService = cursoService;
        this.matriculaService = matriculaService;
        this.contenidoService = contenidoService;
        this.registroEventos = registroEventos;
    }

    @GetMapping("/cursos")
    public String catalogoCursos(@RequestParam(value = "q", required = false) String termino,
                                 Principal principal,
                                 Model model) {
        model.addAttribute("titulo", "Catálogo de Cursos Accesibles");
        List<Curso> cursos = new ArrayList<>(cursoService.buscarCursos(termino));
        model.addAttribute("termino", termino);

        // Mapear qué cursos ya están matriculados por el aprendiz autenticado
        Map<Long, Long> matriculasPorCurso = new HashMap<>();
        if (principal != null) {
            List<Matricula> matriculas = matriculaService.listarPorAprendiz(principal.getName());
            for (Matricula m : matriculas) {
                matriculasPorCurso.put(m.getCurso().getId(), m.getId());
                // Un curso retirado del catálogo sigue visible para quien ya está matriculado
                if (!m.getCurso().isActivo() && (termino == null || termino.isBlank())) {
                    cursos.add(m.getCurso());
                }
            }
        }
        model.addAttribute("cursos", cursos);
        model.addAttribute("matriculasPorCurso", matriculasPorCurso);

        // Cupos disponibles de los cursos con límite (los que no tienen límite no aparecen en el mapa)
        Map<Long, Long> cuposDisponibles = new HashMap<>();
        for (Curso curso : cursos) {
            Long disponibles = matriculaService.cuposDisponibles(curso);
            if (disponibles != null) {
                cuposDisponibles.put(curso.getId(), disponibles);
            }
        }
        model.addAttribute("cuposDisponibles", cuposDisponibles);

        return "cursos/catalogo";
    }

    @PostMapping("/cursos/{id}/matricular")
    public String procesarMatricula(@PathVariable("id") Long cursoId,
                                    Principal principal,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        Matricula matricula = matriculaService.matricular(principal.getName(), cursoId);
        registroEventos.registrar(TipoEvento.MATRICULA_COMPLETADA, session, principal.getName(), cursoId, null);
        // El aviso de éxito solo se muestra justo después de matricularse, no al volver al curso
        redirectAttributes.addFlashAttribute("matriculaRecienCreada", true);
        return "redirect:/matriculas/" + matricula.getId() + "/confirmacion";
    }

    /** Pantalla 3: confirmación de matrícula y ruta de aprendizaje habilitada. */
    @GetMapping("/matriculas/{id}/confirmacion")
    public String confirmacionMatricula(@PathVariable("id") Long matriculaId,
                                        Principal principal,
                                        Model model) {
        prepararCurso(model, matriculaId, principal.getName(), true);
        return "matriculas/confirmacion";
    }

    /** Página del curso del aprendiz: avance por lección y acceso directo a la siguiente pendiente. */
    @GetMapping("/matriculas/{id}")
    public String miCurso(@PathVariable("id") Long matriculaId,
                          Principal principal,
                          Model model) {
        prepararCurso(model, matriculaId, principal.getName(), false);
        return "matriculas/confirmacion";
    }

    private void prepararCurso(Model model, Long matriculaId, String correo, boolean esConfirmacion) {
        Matricula matricula = matriculaService.obtenerPorIdYValidarPropiedad(matriculaId, correo);
        Curso curso = matricula.getCurso();
        model.addAttribute("titulo", (esConfirmacion ? "Matrícula Confirmada — " : "Mi curso — ") + curso.getTitulo());
        model.addAttribute("esConfirmacion", esConfirmacion);
        model.addAttribute("matricula", matricula);
        model.addAttribute("curso", curso);
        model.addAttribute("certificado", matricula.getCertificado());
        model.addAttribute("avance", contenidoService.obtenerAvanceCurso(correo, curso.getId()));
    }
}
