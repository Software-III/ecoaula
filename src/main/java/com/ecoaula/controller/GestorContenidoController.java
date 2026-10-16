package com.ecoaula.controller;

import com.ecoaula.controller.dto.BorradorVersionForm;
import com.ecoaula.controller.dto.CursoForm;
import com.ecoaula.controller.dto.LeccionForm;
import com.ecoaula.model.Contenido;
import com.ecoaula.model.ContenidoVersion;
import com.ecoaula.model.Curso;
import com.ecoaula.service.GestionContenidoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador web accesible para el Gestor de Contenido (/gestor/**).
 * Administra cursos, lecciones, edición de borradores y publicación versionada (ADR-08).
 */
@Controller
@RequestMapping("/gestor")
public class GestorContenidoController {

    private final GestionContenidoService gestionContenidoService;

    public GestorContenidoController(GestionContenidoService gestionContenidoService) {
        this.gestionContenidoService = gestionContenidoService;
    }

    /**
     * Pantalla 1: Mis cursos y lecciones, con el estado de la última versión de cada lección.
     */
    @GetMapping({"", "/cursos"})
    public String listarCursos(Model model) {
        List<Curso> cursos = gestionContenidoService.listarTodosLosCursos();
        Map<Long, List<Contenido>> leccionesPorCurso = new HashMap<>();
        Map<Long, ContenidoVersion> ultimasVersiones = new HashMap<>();

        for (Curso curso : cursos) {
            List<Contenido> lecciones = gestionContenidoService.listarLeccionesDeCurso(curso.getId());
            leccionesPorCurso.put(curso.getId(), lecciones);
            for (Contenido leccion : lecciones) {
                List<ContenidoVersion> versiones = gestionContenidoService.listarHistorialVersiones(leccion.getId());
                if (!versiones.isEmpty()) {
                    ultimasVersiones.put(leccion.getId(), versiones.get(0));
                }
            }
        }

        model.addAttribute("titulo", "Gestión de Cursos y Contenidos");
        model.addAttribute("cursos", cursos);
        model.addAttribute("leccionesPorCurso", leccionesPorCurso);
        model.addAttribute("ultimasVersiones", ultimasVersiones);
        return "gestor/cursos";
    }

    /**
     * Formulario para crear un nuevo curso.
     */
    @GetMapping("/cursos/nuevo")
    public String nuevoCursoForm(Model model) {
        model.addAttribute("titulo", "Crear Nuevo Curso Accesible");
        model.addAttribute("cursoForm", new CursoForm());
        model.addAttribute("esEdicion", false);
        return "gestor/curso-form";
    }

    @PostMapping("/cursos/nuevo")
    public String crearCurso(@Valid @ModelAttribute("cursoForm") CursoForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("titulo", "Crear Nuevo Curso Accesible");
            model.addAttribute("esEdicion", false);
            return "gestor/curso-form";
        }

        Curso nuevoCurso = gestionContenidoService.crearCurso(
                form.getTitulo(),
                form.getDescripcion(),
                form.getHoras(),
                form.getArea(),
                form.getModalidad(),
                form.isActivo()
        );

        redirectAttributes.addFlashAttribute("mensajeExito", "Curso '" + nuevoCurso.getTitulo() + "' creado exitosamente. Ahora puede agregar sus lecciones.");
        return "redirect:/gestor/cursos/" + nuevoCurso.getId() + "/lecciones";
    }

    /**
     * Formulario para editar un curso existente.
     */
    @GetMapping("/cursos/{id}/editar")
    public String editarCursoForm(@PathVariable("id") Long id, Model model) {
        Curso curso = gestionContenidoService.obtenerCurso(id);
        CursoForm form = new CursoForm();
        form.setTitulo(curso.getTitulo());
        form.setDescripcion(curso.getDescripcion());
        form.setHoras(curso.getHoras());
        form.setArea(curso.getArea());
        form.setModalidad(curso.getModalidad());
        form.setActivo(curso.isActivo());

        model.addAttribute("titulo", "Editar Curso: " + curso.getTitulo());
        model.addAttribute("cursoForm", form);
        model.addAttribute("cursoId", id);
        model.addAttribute("esEdicion", true);
        return "gestor/curso-form";
    }

    @PostMapping("/cursos/{id}/editar")
    public String actualizarCurso(@PathVariable("id") Long id,
                                  @Valid @ModelAttribute("cursoForm") CursoForm form,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("titulo", "Editar Curso");
            model.addAttribute("cursoId", id);
            model.addAttribute("esEdicion", true);
            return "gestor/curso-form";
        }

        gestionContenidoService.actualizarCurso(
                id,
                form.getTitulo(),
                form.getDescripcion(),
                form.getHoras(),
                form.getArea(),
                form.getModalidad(),
                form.isActivo()
        );

        redirectAttributes.addFlashAttribute("mensajeExito", "Curso actualizado correctamente.");
        return "redirect:/gestor/cursos";
    }

    /**
     * Pantalla 2: Lecciones del curso y adición de nuevas lecciones.
     */
    @GetMapping("/cursos/{id}/lecciones")
    public String listarLecciones(@PathVariable("id") Long id, Model model) {
        Curso curso = gestionContenidoService.obtenerCurso(id);
        List<Contenido> lecciones = gestionContenidoService.listarLeccionesDeCurso(id);
        Map<Long, ContenidoVersion> ultimasVersiones = new HashMap<>();

        for (Contenido leccion : lecciones) {
            List<ContenidoVersion> versiones = gestionContenidoService.listarHistorialVersiones(leccion.getId());
            if (!versiones.isEmpty()) {
                ultimasVersiones.put(leccion.getId(), versiones.get(0));
            }
        }

        int siguienteOrden = lecciones.size() + 1;

        model.addAttribute("titulo", "Lecciones de: " + curso.getTitulo());
        model.addAttribute("curso", curso);
        model.addAttribute("lecciones", lecciones);
        model.addAttribute("ultimasVersiones", ultimasVersiones);
        model.addAttribute("leccionForm", new LeccionForm("", siguienteOrden));
        return "gestor/lecciones";
    }

    @PostMapping("/cursos/{id}/lecciones/nueva")
    public String agregarLeccion(@PathVariable("id") Long cursoId,
                                 @Valid @ModelAttribute("leccionForm") LeccionForm form,
                                 BindingResult bindingResult,
                                 Principal principal,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            Curso curso = gestionContenidoService.obtenerCurso(cursoId);
            List<Contenido> lecciones = gestionContenidoService.listarLeccionesDeCurso(cursoId);
            Map<Long, ContenidoVersion> ultimasVersiones = new HashMap<>();
            for (Contenido leccion : lecciones) {
                List<ContenidoVersion> versiones = gestionContenidoService.listarHistorialVersiones(leccion.getId());
                if (!versiones.isEmpty()) {
                    ultimasVersiones.put(leccion.getId(), versiones.get(0));
                }
            }
            model.addAttribute("titulo", "Lecciones de: " + curso.getTitulo());
            model.addAttribute("curso", curso);
            model.addAttribute("lecciones", lecciones);
            model.addAttribute("ultimasVersiones", ultimasVersiones);
            return "gestor/lecciones";
        }

        Contenido leccionCreada = gestionContenidoService.agregarLeccion(
                cursoId,
                form.getTitulo(),
                form.getOrden(),
                principal.getName()
        );

        redirectAttributes.addFlashAttribute("mensajeExito", "Lección '" + leccionCreada.getTitulo() + "' agregada con éxito. Se ha iniciado el borrador de su primera versión.");
        return "redirect:/gestor/lecciones/" + leccionCreada.getId() + "/versiones";
    }

    /**
     * Pantalla 4: Vista de versiones con su historial y motivo de rechazo.
     */
    @GetMapping("/lecciones/{contenidoId}/versiones")
    public String listarVersiones(@PathVariable("contenidoId") Long contenidoId, Model model) {
        Contenido leccion = gestionContenidoService.obtenerLeccion(contenidoId);
        List<ContenidoVersion> versiones = gestionContenidoService.listarHistorialVersiones(contenidoId);

        model.addAttribute("titulo", "Versiones de la Lección: " + leccion.getTitulo());
        model.addAttribute("leccion", leccion);
        model.addAttribute("versiones", versiones);
        return "gestor/versiones";
    }

    @PostMapping("/lecciones/{contenidoId}/versiones/nueva")
    public String crearNuevaVersion(@PathVariable("contenidoId") Long contenidoId,
                                    Principal principal,
                                    RedirectAttributes redirectAttributes) {
        ContenidoVersion nuevaVersion = gestionContenidoService.crearNuevaVersionBorrador(contenidoId, principal.getName());
        redirectAttributes.addFlashAttribute("mensajeExito", "Se ha creado la versión " + nuevaVersion.getNumeroVersion() + " en estado BORRADOR.");
        return "redirect:/gestor/versiones/" + nuevaVersion.getId() + "/editar";
    }

    @PostMapping("/versiones/{versionId}/corregir")
    public String subsanarVersionRechazada(@PathVariable("versionId") Long versionId,
                                           Principal principal,
                                           RedirectAttributes redirectAttributes) {
        ContenidoVersion nuevoBorrador = gestionContenidoService.crearVersionDesdeRechazada(versionId, principal.getName());
        redirectAttributes.addFlashAttribute("mensajeExito", "Se creó la versión " + nuevoBorrador.getNumeroVersion() +
                " en estado BORRADOR a partir de la versión rechazada. Realice las correcciones necesarias y reenvíe a revisión.");
        return "redirect:/gestor/versiones/" + nuevoBorrador.getId() + "/editar";
    }

    /**
     * Pantalla 3: Crear/editar una versión en borrador (transcripción, subida de audio, vista previa nativa).
     */
    @GetMapping("/versiones/{versionId}/editar")
    public String editarBorradorForm(@PathVariable("versionId") Long versionId, Model model) {
        ContenidoVersion version = gestionContenidoService.obtenerVersion(versionId);

        BorradorVersionForm form = new BorradorVersionForm(version.getTranscripcion());

        model.addAttribute("titulo", "Editar Borrador — Versión " + version.getNumeroVersion() + " (" + version.getContenido().getTitulo() + ")");
        model.addAttribute("version", version);
        model.addAttribute("borradorForm", form);
        return "gestor/version-editar";
    }

    @PostMapping("/versiones/{versionId}/editar")
    public String guardarBorrador(@PathVariable("versionId") Long versionId,
                                  @ModelAttribute("borradorForm") BorradorVersionForm form,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        gestionContenidoService.actualizarBorrador(
                versionId,
                form.getTranscripcion(),
                form.getArchivoAudio(),
                principal.getName()
        );

        redirectAttributes.addFlashAttribute("mensajeExito", "Borrador guardado exitosamente.");
        return "redirect:/gestor/versiones/" + versionId + "/editar";
    }

    /**
     * Botón "Enviar a revisión".
     */
    @PostMapping("/versiones/{versionId}/enviar-revision")
    public String enviarARevision(@PathVariable("versionId") Long versionId,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        ContenidoVersion version = gestionContenidoService.enviarARevision(versionId, principal.getName());
        redirectAttributes.addFlashAttribute("mensajeExito", "La versión " + version.getNumeroVersion() +
                " ha sido enviada a revisión técnica de accesibilidad.");
        return "redirect:/gestor/lecciones/" + version.getContenido().getId() + "/versiones";
    }
}
