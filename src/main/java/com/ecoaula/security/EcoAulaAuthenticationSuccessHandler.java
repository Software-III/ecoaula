package com.ecoaula.security;

import com.ecoaula.analitica.RegistroEventosService;
import com.ecoaula.model.enums.TipoEvento;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

/**
 * Manejador de éxito en autenticación que redirige de forma accesible
 * según el rol del usuario (aprendiz, gestor o revisor).
 */
@Component
public class EcoAulaAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final RegistroEventosService registroEventos;

    public EcoAulaAuthenticationSuccessHandler(RegistroEventosService registroEventos) {
        this.registroEventos = registroEventos;
        setDefaultTargetUrl("/cursos");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // Solo los aprendices cuentan para la analítica del aprendizaje (no el personal interno)
        boolean esAprendiz = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_APRENDIZ".equals(a.getAuthority()));
        if (esAprendiz) {
            registroEventos.registrar(TipoEvento.INICIO_SESION, request.getSession(), authentication.getName());
        }

        SavedRequest savedRequest = new HttpSessionRequestCache().getRequest(request, response);
        if (savedRequest != null) {
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        for (GrantedAuthority authority : authorities) {
            if ("ROLE_GESTOR_CONTENIDO".equals(authority.getAuthority())) {
                getRedirectStrategy().sendRedirect(request, response, "/gestor/cursos");
                return;
            }
            if ("ROLE_REVISOR_ACCESIBILIDAD".equals(authority.getAuthority())) {
                getRedirectStrategy().sendRedirect(request, response, "/revisor/cola");
                return;
            }
            if ("ROLE_COORDINADOR".equals(authority.getAuthority())) {
                getRedirectStrategy().sendRedirect(request, response, "/reportes-barrera");
                return;
            }
        }

        getRedirectStrategy().sendRedirect(request, response, "/cursos");
    }
}
