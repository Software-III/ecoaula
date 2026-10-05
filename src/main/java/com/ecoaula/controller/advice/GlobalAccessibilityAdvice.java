package com.ecoaula.controller.advice;

import com.ecoaula.model.PerfilAccesibilidad;
import com.ecoaula.service.AccesibilidadService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Advice global que inyecta el perfil de accesibilidad en todas las vistas Thymeleaf.
 * Utiliza la sesión HTTP para cachear el perfil y evitar consultas a BD en cada solicitud (ADR-02 / ADR-07).
 */
@ControllerAdvice
public class GlobalAccessibilityAdvice {

    public static final String SESSION_PERFIL_KEY = "perfilAccesibilidad";
    private final AccesibilidadService accesibilidadService;

    public GlobalAccessibilityAdvice(AccesibilidadService accesibilidadService) {
        this.accesibilidadService = accesibilidadService;
    }

    @ModelAttribute("perfilAccesibilidad")
    public PerfilAccesibilidad agregarPerfilAccesibilidad(HttpSession session) {
        // 1. Verificar si ya se encuentra en sesión
        PerfilAccesibilidad perfilSesion = (PerfilAccesibilidad) session.getAttribute(SESSION_PERFIL_KEY);
        if (perfilSesion != null) {
            return perfilSesion;
        }

        // 2. Si no está en sesión pero el usuario está autenticado, cargar desde servicio y guardar en sesión
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String correo = auth.getName();
            try {
                PerfilAccesibilidad perfil = accesibilidadService.obtenerPerfilPorCorreo(correo);
                session.setAttribute(SESSION_PERFIL_KEY, perfil);
                return perfil;
            } catch (Exception ignored) {
                // Si ocurre algún problema, se retorna el perfil predeterminado
            }
        }

        // 3. Usuario anónimo o sin perfil persistido: perfil predeterminado
        PerfilAccesibilidad perfilDefecto = new PerfilAccesibilidad();
        return perfilDefecto;
    }
}
