package com.ecoaula.controller;

import com.ecoaula.analitica.IndicadoresService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Tablero de indicadores de los Procesos 1 y 2 (Plan de Gestión de la Calidad) y de aprendices en
 * riesgo de abandono. Disponible para el personal interno (ADR-09).
 */
@Controller
public class IndicadoresController {

    private static final List<Integer> PERIODOS = List.of(7, 30, 90, 365);

    private final IndicadoresService indicadoresService;

    public IndicadoresController(IndicadoresService indicadoresService) {
        this.indicadoresService = indicadoresService;
    }

    @GetMapping("/indicadores")
    public String tablero(@RequestParam(value = "dias", defaultValue = "30") int dias, Model model) {
        int periodo = PERIODOS.contains(dias) ? dias : 30;
        model.addAttribute("titulo", "Indicadores de calidad");
        model.addAttribute("tablero", indicadoresService.calcular(periodo));
        model.addAttribute("periodos", PERIODOS);
        return "indicadores/tablero";
    }
}
