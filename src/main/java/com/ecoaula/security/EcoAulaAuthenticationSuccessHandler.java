package com.ecoaula.security;

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

    public EcoAulaAuthenticationSuccessHandler() {
        setDefaultTargetUrl("/cursos");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
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
        }

        getRedirectStrategy().sendRedirect(request, response, "/cursos");
    }
}
