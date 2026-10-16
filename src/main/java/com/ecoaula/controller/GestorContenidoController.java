package com.ecoaula.controller;

import com.ecoaula.exception.EcoAulaException;
import com.ecoaula.model.RecursoImagen;
import com.ecoaula.service.ContenidoService;
import com.ecoaula.service.RevisionContenidoService;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.UncheckedIOException;
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
    private final ContenidoService contenidoService;
    private final RevisionContenidoService revisionContenidoService;

    public GestorContenidoController(GestionContenidoService gestionContenidoService, ContenidoService contenidoService,
                                     RevisionContenidoService revisionContenidoService) {
        this.gestionContenidoService = gestionContenidoService;
        this.contenidoService = contenidoService;
        this.revisionContenidoService = revisionContenidoService;
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

        Map<Long, Long> matriculadosPorCurso = new HashMap<>();
        Map<Long, Boolean> evaluacionPorCurso = new HashMap<>();
        for (Curso curso : cursos) {
            matriculadosPorCurso.put(curso.getId(), gestionContenidoService.contarMatriculados(curso.getId()));
            evaluacionPorCurso.put(curso.getId(), gestionContenidoService.tieneEvaluacion(curso.getId()));
        }
        model.addAttribute("titulo", "Gestión de Cursos y Contenidos");
        model.addAttribute("cursos", cursos);
        model.addAttribute("matriculadosPorCurso", matriculadosPorCurso);
        model.addAttribute("evaluacionPorCurso", evaluacionPorCurso);
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
                form.getCupoMaximo()
        );
        redirectAttributes.addFlashAttribute("mensajeExito", "Curso '" + nuevoCurso.getTitulo()
                + "' creado fuera del catálogo. Agréguele lecciones y publíquelo cuando todas estén aprobadas.");
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
        form.setCupoMaximo(curso.getCupoMaximo());

        model.addAttribute("titulo", "Editar Curso: " + curso.getTitulo());
        model.addAttribute("cursoForm", form);
        model.addAttribute("cursoId", id);
        model.addAttribute("curso", curso);
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
            model.addAttribute("curso", gestionContenidoService.obtenerCurso(id));
            model.addAttribute("esEdicion", true);
            return "gestor/curso-form";
        }
        try {
            gestionContenidoService.actualizarCurso(
                    id,
                    form.getTitulo(),
                    form.getDescripcion(),
                    form.getHoras(),
                    form.getArea(),
                    form.getModalidad(),
                    form.getCupoMaximo()
            );
        } catch (EcoAulaException ex) {
            model.addAttribute("titulo", "Editar Curso");
            model.addAttribute("cursoId", id);
            model.addAttribute("curso", gestionContenidoService.obtenerCurso(id));
            model.addAttribute("esEdicion", true);
            model.addAttribute("mensajeError", ex.getMessage());
            return "gestor/curso-form";
        }

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
        model.addAttribute("matriculados", gestionContenidoService.contarMatriculados(id));
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
            model.addAttribute("matriculados", gestionContenidoService.contarMatriculados(cursoId));
            return "gestor/lecciones";
        }
        Contenido leccionCreada;
        try {
            leccionCreada = gestionContenidoService.agregarLeccion(
                    cursoId,
                    form.getTitulo(),
                    form.getOrden(),
                    principal.getName()
            );
        } catch (EcoAulaException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
            return "redirect:/gestor/cursos/" + cursoId + "/lecciones";
        }

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
        // Informe de revisión de cada versión revisada (el más reciente)
        Map<Long, Long> informePorVersion = new HashMap<>();
        revisionContenidoService.historialDeLeccion(contenidoId)
                .forEach(r -> informePorVersion.put(r.getVersion().getId(), r.getId()));
        model.addAttribute("informePorVersion", informePorVersion);
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
        ContenidoVersion version = gestionContenidoService.obtenerVersionDetalle(versionId);
        BorradorVersionForm form = new BorradorVersionForm(version.getTranscripcion());
        form.setFuenteMaterial(version.getFuenteMaterial());
        form.setLicenciaDeclarada(version.getLicenciaDeclaradaEn() != null);
        model.addAttribute("imagenes", contenidoService.listarImagenes(versionId));
        model.addAttribute("maxTextoAlternativo", RecursoImagen.MAX_TEXTO_ALTERNATIVO);

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
        try {
            gestionContenidoService.actualizarBorrador(
                    versionId,
                    form.getTranscripcion(),
                    form.getArchivoAudio(),
                    form.getFuenteMaterial(),
                    form.isLicenciaDeclarada(),
                    principal.getName()
            );
        } catch (EcoAulaException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
            return "redirect:/gestor/versiones/" + versionId + "/editar";
        }
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
        ContenidoVersion version;
        try {
            version = gestionContenidoService.enviarARevision(versionId, principal.getName());
        } catch (EcoAulaException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
            return "redirect:/gestor/versiones/" + versionId + "/editar#seccion-envio-revision";
        }
        redirectAttributes.addFlashAttribute("mensajeExito", "La versión " + version.getNumeroVersion() +
                " ha sido enviada a revisión técnica de accesibilidad.");
        return "redirect:/gestor/lecciones/" + version.getContenido().getId() + "/versiones";
    }

    // ------------------------------------------------------------------
    // Imágenes con alternativa textual (WCAG 1.1.1)
    // ------------------------------------------------------------------

    @PostMapping("/versiones/{versionId}/imagenes")
    public String agregarImagen(@PathVariable("versionId") Long versionId,
                                @RequestParam(value = "imagen", required = false) MultipartFile imagen,
                                @RequestParam(value = "textoAlternativo", required = false) String textoAlternativo,
                                @RequestParam(value = "descripcionDetallada", required = false) String descripcionDetallada,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            contenidoService.agregarImagen(versionId, principal.getName(),
                    imagen == null ? null : imagen.getOriginalFilename(), bytes(imagen), textoAlternativo, descripcionDetallada);
            redirectAttributes.addFlashAttribute("mensajeExito", "Imagen agregada con su texto alternativo.");
        } catch (EcoAulaException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
            redirectAttributes.addFlashAttribute("textoAlternativo", textoAlternativo);
            redirectAttributes.addFlashAttribute("descripcionDetallada", descripcionDetallada);
        }
        return "redirect:/gestor/versiones/" + versionId + "/editar#seccion-imagenes";
    }

    @PostMapping("/versiones/{versionId}/imagenes/{imagenId}/quitar")
    public String quitarImagen(@PathVariable("versionId") Long versionId,
                               @PathVariable("imagenId") Long imagenId,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        try {
            contenidoService.quitarImagen(versionId, imagenId, principal.getName());
            redirectAttributes.addFlashAttribute("mensajeExito", "Imagen quitada de la versión.");
        } catch (EcoAulaException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/gestor/versiones/" + versionId + "/editar#seccion-imagenes";
    }

    // ------------------------------------------------------------------
    // Catálogo: publicar, retirar y eliminar (cursos y lecciones)
    // ------------------------------------------------------------------

    @PostMapping("/cursos/{id}/publicar")
    public String publicar(@PathVariable("id") Long cursoId, RedirectAttributes redirectAttributes) {
        try {
            Curso curso = gestionContenidoService.publicarEnCatalogo(cursoId);
            redirectAttributes.addFlashAttribute("mensajeExito", "Curso '" + curso.getTitulo()
                    + "' publicado en el catálogo. Ya está disponible para matrícula.");
        } catch (EcoAulaException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/gestor/cursos#curso-" + cursoId;
    }

    @PostMapping("/cursos/{id}/retirar")
    public String retirar(@PathVariable("id") Long cursoId, RedirectAttributes redirectAttributes) {
        Curso curso = gestionContenidoService.retirarDelCatalogo(cursoId);
        redirectAttributes.addFlashAttribute("mensajeExito", "Curso '" + curso.getTitulo()
                + "' retirado del catálogo. No recibe matrículas nuevas; los aprendices matriculados conservan su acceso.");
        return "redirect:/gestor/cursos#curso-" + cursoId;
    }

    @PostMapping("/cursos/{id}/eliminar")
    public String eliminarCurso(@PathVariable("id") Long cursoId,
                                @RequestParam(value = "confirmar", defaultValue = "false") boolean confirmar,
                                RedirectAttributes redirectAttributes) {
        if (!confirmar) {
            redirectAttributes.addFlashAttribute("mensajeError", "Marque la casilla de confirmación para eliminar el curso.");
            return "redirect:/gestor/cursos/" + cursoId + "/editar#seccion-eliminar";
        }
        try {
            String titulo = gestionContenidoService.obtenerCurso(cursoId).getTitulo();
            gestionContenidoService.eliminarCurso(cursoId);
            redirectAttributes.addFlashAttribute("mensajeExito", "Curso '" + titulo + "' eliminado.");
            return "redirect:/gestor/cursos";
        } catch (EcoAulaException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
            return "redirect:/gestor/cursos/" + cursoId + "/editar#seccion-eliminar";
        }
    }

    @PostMapping("/lecciones/{contenidoId}/titulo")
    public String renombrarLeccion(@PathVariable("contenidoId") Long contenidoId,
                                   @RequestParam(value = "titulo", required = false) String titulo,
                                   RedirectAttributes redirectAttributes) {
        Long cursoId = gestionContenidoService.obtenerLeccion(contenidoId).getCurso().getId();
        try {
            gestionContenidoService.actualizarTituloLeccion(contenidoId, titulo);
            redirectAttributes.addFlashAttribute("mensajeExito", "Título de la lección actualizado.");
        } catch (EcoAulaException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/gestor/cursos/" + cursoId + "/lecciones";
    }

    @PostMapping("/lecciones/{contenidoId}/eliminar")
    public String eliminarLeccion(@PathVariable("contenidoId") Long contenidoId,
                                  @RequestParam(value = "confirmar", defaultValue = "false") boolean confirmar,
                                  RedirectAttributes redirectAttributes) {
        Contenido leccion = gestionContenidoService.obtenerLeccion(contenidoId);
        Long cursoId = leccion.getCurso().getId();
        if (!confirmar) {
            redirectAttributes.addFlashAttribute("mensajeError", "Marque la casilla de confirmación para eliminar la lección.");
            return "redirect:/gestor/cursos/" + cursoId + "/lecciones";
        }
        try {
            gestionContenidoService.eliminarLeccion(contenidoId);
            redirectAttributes.addFlashAttribute("mensajeExito", "Lección '" + leccion.getTitulo()
                    + "' eliminada. Las lecciones siguientes se renumeraron.");
        } catch (EcoAulaException ex) {
            redirectAttributes.addFlashAttribute("mensajeError", ex.getMessage());
        }
        return "redirect:/gestor/cursos/" + cursoId + "/lecciones";
    }

    private static byte[] bytes(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo adjunto", e);
        }
    }
}
