package com.ecoaula.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración centralizada de seguridad para EcoAula (ADR-09).
 * Establece rutas públicas, formulario de login accesible, protección de sesión,
 * autorización estricta por roles (/gestor/**, /revisor/**) y rate limiting.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final RateLimitingFilter rateLimitingFilter;
    private final EcoAulaAuthenticationSuccessHandler authenticationSuccessHandler;

    public SecurityConfig(RateLimitingFilter rateLimitingFilter,
                          EcoAulaAuthenticationSuccessHandler authenticationSuccessHandler) {
        this.rateLimitingFilter = rateLimitingFilter;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/registro",
                    "/login",
                    "/api/v1/certificados/**",
                    "/verificar",
                    "/css/**",
                    "/js/**",
                    "/reportar-barrera",
                    "/reportar-barrera/**",
                    "/error",
                    "/h2-console/**"
                ).permitAll()
                .requestMatchers("/gestor/**").hasRole("GESTOR_CONTENIDO")
                .requestMatchers("/revisor/**").hasRole("REVISOR_ACCESIBILIDAD")
                .requestMatchers("/informes-revision/**").hasAnyRole("GESTOR_CONTENIDO", "REVISOR_ACCESIBILIDAD")
                .requestMatchers("/reportes-barrera/**", "/indicadores").hasAnyRole("GESTOR_CONTENIDO", "REVISOR_ACCESIBILIDAD", "COORDINADOR")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("correo")
                .passwordParameter("password")
                .successHandler(authenticationSuccessHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/v1/certificados/**", "/h2-console/**")
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
