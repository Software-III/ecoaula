package com.ecoaula.security;

import com.ecoaula.model.Aprendiz;
import com.ecoaula.repository.AprendizRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Servicio de carga de detalles de usuario para Spring Security.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AprendizRepository aprendizRepository;

    public CustomUserDetailsService(AprendizRepository aprendizRepository) {
        this.aprendizRepository = aprendizRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Aprendiz aprendiz = aprendizRepository.findByCorreo(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con correo: " + correo));

        return new User(
                aprendiz.getCorreo(),
                aprendiz.getPasswordHash(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + aprendiz.getRol().name()))
        );
    }
}
