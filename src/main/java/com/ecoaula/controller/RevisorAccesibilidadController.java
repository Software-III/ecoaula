package com.ecoaula.controller;

import com.ecoaula.model.ContenidoVersion;
import com.ecoaula.model.enums.CriterioAccesibilidad;
import com.ecoaula.service.RevisionContenidoService;
import com.ecoaula.service.dto.IndicadoresCalidad;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controlador web accesible para el Revisor de Accesibilidad (/revisor/**).
 * Gestiona la cola de revisión, auditoría técnica con lista de chequeo WCAG/NTC 5854
 * y el panel de indicadores de calidad.
 */
@Controller
@RequestMapping("/revisor")
public class RevisorAccesibilidadController {

    private final RevisionContenidoService revisionContenidoService;

    public RevisorAccesibilidadController(RevisionContenidoService revisionContenidoService) {
        this.revisionContenidoService = revisionContenidoService;
    }

    /**
     * Pantalla 1: Cola de versiones EN_REVISION (más antiguas primero).
     */
    @GetMapping({"", "/cola"})
    public String colaRevision(Model model) {
        List<ContenidoVersion> cola = revisionContenidoService.obtenerColaRevision();
        model.addAttribute("titulo", "Cola de Revisión Técnica de Accesibilidad");
        model.addAttribute("cola", cola);
        return "revisor/cola";
    }

    /**
     * Pantalla 2: Detalle de revisión técnica (audio nativo, transcripción, lista de chequeo WCAG).
     */
    @GetMapping("/versiones/{versionId}/revisar")
    public String detalleRevision(@PathVariable("versionId") Long versionId, Model model) {
        ContenidoVersion version = revisionContenidoService.obtenerVersionParaRevision(versionId);

        model.addAttribute("titulo", "Auditar Accesibilidad — Versión " + version.getNumeroVersion() + " (" + version.getContenido().getTitulo() + ")");
        model.addAttribute("version", version);
        model.addAttribute("criterios", CriterioAccesibilidad.todos());
        return "revisor/detalle";
    }

    /**
     * Acción: Aprobar y Publicar.
     */
    @PostMapping("/versiones/{versionId}/aprobar")
    public String aprobarVersion(@PathVariable("versionId") Long versionId,
                                 @RequestParam(value = "criterios", required = false) Set<String> criterios,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        if (criterios == null) {
            criterios = new HashSet<>();
        }

        revisionContenidoService.aprobarYPublicar(versionId, principal.getName(), criterios);

        redirectAttributes.addFlashAttribute("mensajeExito", "La versión ha sido APROBADA y PUBLICADA exitosamente. Los aprendices ya pueden acceder a ella.");
        return "redirect:/revisor/cola";
    }

    /**
     * Acción: Rechazar con motivo obligatorio.
     */
    @PostMapping("/versiones/{versionId}/rechazar")
    public String rechazarVersion(@PathVariable("versionId") Long versionId,
                                  @RequestParam(value = "motivo", required = false) String motivo,
                                  @RequestParam(value = "criterios", required = false) Set<String> criterios,
                                  Principal principal,
                                  RedirectAttributes redirectAttributes) {
        if (criterios == null) {
            criterios = new HashSet<>();
        }

        revisionContenidoService.rechazar(versionId, principal.getName(), motivo, criterios);

        redirectAttributes.addFlashAttribute("mensajeExito", "La versión ha sido RECHAZADA. Se ha registrado el motivo para que el gestor proceda con la subsanación.");
        return "redirect:/revisor/cola";
    }

    /**
     * Pantalla 4: Indicadores del Plan de Calidad de Accesibilidad.
     */
    @GetMapping("/indicadores")
    public String verIndicadores(Model model) {
        IndicadoresCalidad indicadores = revisionContenidoService.calcularIndicadores();
        model.addAttribute("titulo", "Indicadores de Calidad de Accesibilidad");
        model.addAttribute("indicadores", indicadores);
        return "revisor/indicadores";
    }
}
