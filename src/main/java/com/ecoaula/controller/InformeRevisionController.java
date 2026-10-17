package com.ecoaula.controller;

import com.ecoaula.model.RevisionAccesibilidad;
import com.ecoaula.model.enums.CriterioAccesibilidad;
import com.ecoaula.service.RevisionContenidoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Informes de revisión de accesibilidad (salida del Proceso 2): listado e informe imprimible con el
 * resultado de cada criterio del checklist. Disponible para el gestor de contenido y el revisor (ADR-09).
 */
@Controller
@RequestMapping("/informes-revision")
public class InformeRevisionController {

    private final RevisionContenidoService revisionContenidoService;

    public InformeRevisionController(RevisionContenidoService revisionContenidoService) {
        this.revisionContenidoService = revisionContenidoService;
    }

    @GetMapping
    public String listado(Model model) {
        model.addAttribute("titulo", "Informes de Revisión de Accesibilidad");
        model.addAttribute("informes", revisionContenidoService.listarInformes());
        model.addAttribute("totalCriterios", CriterioAccesibilidad.todos().size());
        return "informes/listado";
    }

    @GetMapping("/{informeId}")
    public String detalle(@PathVariable("informeId") Long informeId, Model model) {
        RevisionAccesibilidad informe = revisionContenidoService.obtenerInforme(informeId);
        model.addAttribute("titulo", "Informe de revisión: " + informe.getVersion().getContenido().getTitulo()
                + " v" + informe.getVersion().getNumeroVersion());
        model.addAttribute("informe", informe);
        model.addAttribute("criterios", CriterioAccesibilidad.todos());
        return "informes/detalle";
    }
}
