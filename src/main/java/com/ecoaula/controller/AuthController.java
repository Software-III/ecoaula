package com.ecoaula.controller;

import com.ecoaula.controller.advice.GlobalAccessibilityAdvice;
import com.ecoaula.controller.dto.PerfilForm;
import com.ecoaula.controller.dto.RegistroForm;
import com.ecoaula.model.PerfilAccesibilidad;
import com.ecoaula.service.AccesibilidadService;
import com.ecoaula.service.AprendizService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

/**
 * Controlador para autenticación, registro accesible y configuración de perfil (Pantalla 1).
 */
@Controller
public class AuthController {

    private final AprendizService aprendizService;
    private final AccesibilidadService accesibilidadService;

    public AuthController(AprendizService aprendizService,
                          AccesibilidadService accesibilidadService) {
        this.aprendizService = aprendizService;
        this.accesibilidadService = accesibilidadService;
    }

    @GetMapping("/")
    public String inicio() {
        return "redirect:/cursos";
    }

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        @RequestParam(value = "registrado", required = false) String registrado,
                        Model model) {
        model.addAttribute("titulo", "Iniciar Sesión");
        if (error != null) {
            model.addAttribute("mensajeError", "Correo electrónico o contraseña incorrectos. Por favor verifique sus datos.");
        }
        if (logout != null) {
            model.addAttribute("mensajeExito", "Ha cerrado la sesión de manera segura.");
        }
        if (registrado != null) {
            model.addAttribute("mensajeExito", "¡Cuenta creada exitosamente! Ahora puede iniciar sesión con su correo y contraseña.");
        }
        return "login";
    }

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("titulo", "Registro y Perfil de Accesibilidad");
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegistroForm());
        }
        return "registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@Valid @ModelAttribute("form") RegistroForm form,
                                  BindingResult bindingResult,
                                  Model model) {
        model.addAttribute("titulo", "Registro y Perfil de Accesibilidad");
        if (bindingResult.hasErrors()) {
            return "registro";
        }

        try {
            aprendizService.registrar(form);
            return "redirect:/login?registrado=true";
        } catch (Exception ex) {
            model.addAttribute("mensajeError", ex.getMessage());
            return "registro";
        }
    }

    @GetMapping("/perfil")
    public String mostrarPerfil(Principal principal, Model model) {
        model.addAttribute("titulo", "Mi Perfil de Accesibilidad");
        PerfilAccesibilidad perfil = accesibilidadService.obtenerPerfilPorCorreo(principal.getName());

        PerfilForm form = new PerfilForm();
        form.setVelocidadAudio(perfil.getVelocidadAudio());
        form.setModoVisual(perfil.getModoVisual());
        form.setTamanoTexto(perfil.getTamanoTexto());
        form.setSoloAudio(perfil.isSoloAudio());

        model.addAttribute("form", form);
        return "perfil";
    }

    @PostMapping("/perfil")
    public String guardarPerfil(@Valid @ModelAttribute("form") PerfilForm form,
                                BindingResult bindingResult,
                                Principal principal,
                                HttpSession session,
                                Model model) {
        model.addAttribute("titulo", "Mi Perfil de Accesibilidad");
        if (bindingResult.hasErrors()) {
            return "perfil";
        }

        PerfilAccesibilidad datos = new PerfilAccesibilidad();
        datos.setVelocidadAudio(form.getVelocidadAudio());
        datos.setModoVisual(form.getModoVisual());
        datos.setTamanoTexto(form.getTamanoTexto());
        datos.setSoloAudio(form.isSoloAudio());

        PerfilAccesibilidad actualizado = accesibilidadService.actualizarPerfil(principal.getName(), datos);
        // Actualizar perfil en sesión para aplicar de inmediato sin reconsultar BD (ADR-02 / ADR-07)
        session.setAttribute(GlobalAccessibilityAdvice.SESSION_PERFIL_KEY, actualizado);

        model.addAttribute("mensajeExito", "Preferencias de accesibilidad guardadas y aplicadas correctamente.");
        return "perfil";
    }
}
