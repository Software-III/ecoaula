package com.ecoaula.service;

import com.ecoaula.controller.dto.RegistroForm;
import com.ecoaula.exception.EcoAulaException;
import com.ecoaula.exception.RecursoNoEncontradoException;
import com.ecoaula.model.Aprendiz;
import com.ecoaula.model.PerfilAccesibilidad;
import com.ecoaula.model.enums.Rol;
import com.ecoaula.notification.NotificacionService;
import com.ecoaula.repository.AprendizRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio encargado del registro y gestión de aprendices en EcoAula.
 */
@Service
public class AprendizService {

    private final AprendizRepository aprendizRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificacionService notificacionService;

    public AprendizService(AprendizRepository aprendizRepository,
                           PasswordEncoder passwordEncoder,
                           NotificacionService notificacionService) {
        this.aprendizRepository = aprendizRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public Aprendiz registrar(RegistroForm form) {
        if (aprendizRepository.existsByCorreo(form.getCorreo())) {
            throw new EcoAulaException("Ya existe una cuenta registrada con el correo: " + form.getCorreo());
        }

        Aprendiz aprendiz = new Aprendiz();
        aprendiz.setNombreCompleto(form.getNombreCompleto());
        aprendiz.setCorreo(form.getCorreo());
        aprendiz.setTelefono(form.getTelefono());
        aprendiz.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        aprendiz.setRol(Rol.APRENDIZ);
        aprendiz.setCondicionVisual(form.getCondicionVisual());
        aprendiz.setConsentimientoDatos(form.isConsentimientoDatos());
        aprendiz.setFechaConsentimiento(LocalDateTime.now());

        PerfilAccesibilidad perfil = new PerfilAccesibilidad(aprendiz);
        perfil.setVelocidadAudio(form.getVelocidadAudio());
        perfil.setModoVisual(form.getModoVisual());
        perfil.setTamanoTexto(form.getTamanoTexto());
        perfil.setSoloAudio(form.isSoloAudio());
        aprendiz.setPerfilAccesibilidad(perfil);

        Aprendiz guardado = aprendizRepository.save(aprendiz);
        notificacionService.notificarRegistro(guardado.getCorreo(), guardado.getNombreCompleto());
        return guardado;
    }

    @Transactional(readOnly = true)
    public Aprendiz buscarPorCorreo(String correo) {
        return aprendizRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el aprendiz con correo: " + correo));
    }
}
