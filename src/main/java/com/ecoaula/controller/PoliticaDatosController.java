package com.ecoaula.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página pública de la Política de Tratamiento de Datos Personales (Ley 1581 de 2012, Decreto 1377 de 2013).
 * Es pública para que cualquier persona pueda leerla antes de dar su autorización en el registro.
 */
@Controller
public class PoliticaDatosController {

    private final String correoContacto;

    public PoliticaDatosController(@Value("${ecoaula.privacidad.correo-contacto:}") String correoContacto) {
        this.correoContacto = correoContacto == null ? "" : correoContacto.trim();
    }

    @GetMapping("/politica-datos")
    public String politica(Model model) {
        model.addAttribute("titulo", "Política de Tratamiento de Datos Personales");
        model.addAttribute("correoContacto", correoContacto);
        return "politica-datos";
    }
}
