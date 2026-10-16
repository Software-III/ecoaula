package com.ecoaula.controller;

import com.ecoaula.controller.dto.ReporteBarreraForm;
import com.ecoaula.exception.EcoAulaException;
import com.ecoaula.model.ReporteBarrera;
import com.ecoaula.model.enums.EstadoReporte;
import com.ecoaula.model.enums.EtapaReporte;
import com.ecoaula.service.ReporteBarreraService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

/**
 * Reporte de barreras de accesibilidad. Cualquier persona puede reportar (incluso sin sesión, porque la
 * barrera puede impedir registrarse); el aprendiz consulta sus reportes y el personal interno los atiende.
 */
@Controller
public class ReporteBarreraController {

    private final ReporteBarreraService reporteService;

    public ReporteBarreraController(ReporteBarreraService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/reportar-barrera")
    public String formulario(@RequestParam(value = "etapa", required = false) EtapaReporte etapa,
                             @RequestParam(value = "curso", required = false) Long cursoId,
                             @RequestParam(value = "leccion", required = false) Long contenidoId,
                             Model model) {
        ReporteBarreraForm form = new ReporteBarreraForm();
        form.setEtapa(etapa);
        form.setCursoId(cursoId);
        form.setContenidoId(contenidoId);
        prepararFormulario(model, form);
        return "reportes/formulario";
    }

    @PostMapping("/reportar-barrera")
    public String reportar(@Valid @ModelAttribute("form") ReporteBarreraForm form,
                           BindingResult bindingResult,
                           Principal principal,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            prepararFormulario(model, form);
            return "reportes/formulario";
        }
        ReporteBarrera reporte = reporteService.crear(principal == null ? null : principal.getName(), form);
        redirectAttributes.addFlashAttribute("numeroReporte", reporte.getId());
        return "redirect:/reportar-barrera/enviado";
    }

    @GetMapping("/reportar-barrera/enviado")
    public String enviado(Model model) {
        model.addAttribute("titulo", "Reporte enviado");
        return "reportes/enviado";
    }

    @GetMapping("/mis-reportes")
    public String misReportes(Principal principal, Model model) {
        model.addAttribute("titulo", "Mis reportes de barreras");
        model.addAttribute("reportes", reporteService.listarDeAprendiz(principal.getName()));
        return "reportes/mis-reportes";
    }

    @GetMapping("/reportes-barrera")
    public String listado(@RequestParam(value = "estado", required = false) EstadoReporte estado,
                          @RequestParam(value = "etapa", required = false) EtapaReporte etapa,
                          Model model) {
        List<ReporteBarrera> reportes = reporteService.listarTodos().stream()
                .filter(r -> estado == null || r.getEstado() == estado)
                .filter(r -> etapa == null || r.getEtapa() == etapa)
                .toList();
        model.addAttribute("titulo", "Reportes de barreras de accesibilidad");
        model.addAttribute("reportes", reportes);
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("etapaFiltro", etapa);
        model.addAttribute("estados", EstadoReporte.values());
        model.addAttribute("etapas", EtapaReporte.values());
        return "reportes/listado";
    }

    @GetMapping("/reportes-barrera/{id}")
    public String detalle(@PathVariable("id") Long id, Model model) {
        prepararDetalle(model, reporteService.obtener(id));
        return "reportes/detalle";
    }

    @PostMapping("/reportes-barrera/{id}")
    public String atender(@PathVariable("id") Long id,
                          @RequestParam(value = "estado", required = false) EstadoReporte estado,
                          @RequestParam(value = "respuesta", required = false) String respuesta,
                          Principal principal,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        try {
            reporteService.atender(id, estado, respuesta, principal.getName());
            redirectAttributes.addFlashAttribute("mensajeExito", "Reporte número " + id + " actualizado.");
            return "redirect:/reportes-barrera";
        } catch (EcoAulaException ex) {
            prepararDetalle(model, reporteService.obtener(id));
            model.addAttribute("mensajeError", ex.getMessage());
            model.addAttribute("respuestaPendiente", respuesta);
            return "reportes/detalle";
        }
    }

    private void prepararFormulario(Model model, ReporteBarreraForm form) {
        model.addAttribute("titulo", "Reportar una barrera de accesibilidad");
        model.addAttribute("form", form);
        model.addAttribute("etapas", EtapaReporte.values());
        model.addAttribute("contexto", reporteService.describirContexto(form.getCursoId(), form.getContenidoId()));
    }

    private void prepararDetalle(Model model, ReporteBarrera reporte) {
        model.addAttribute("titulo", "Reporte número " + reporte.getId());
        model.addAttribute("reporte", reporte);
        model.addAttribute("estados", EstadoReporte.values());
    }
}
