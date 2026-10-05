package com.ecoaula.service;

import com.ecoaula.exception.RecursoNoEncontradoException;
import com.ecoaula.model.Aprendiz;
import com.ecoaula.model.PerfilAccesibilidad;
import com.ecoaula.repository.AprendizRepository;
import com.ecoaula.repository.PerfilAccesibilidadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio encargado de la gestión del perfil de accesibilidad del aprendiz (ADR-07).
 */
@Service
public class AccesibilidadService {

    private final PerfilAccesibilidadRepository perfilRepository;
    private final AprendizRepository aprendizRepository;

    public AccesibilidadService(PerfilAccesibilidadRepository perfilRepository,
                                AprendizRepository aprendizRepository) {
        this.perfilRepository = perfilRepository;
        this.aprendizRepository = aprendizRepository;
    }

    @Transactional(readOnly = true)
    public PerfilAccesibilidad obtenerPerfilPorCorreo(String correo) {
        return perfilRepository.findByAprendizCorreo(correo)
                .orElseGet(() -> {
                    // Si no existe perfil asociado, se inicializa uno por defecto
                    Aprendiz aprendiz = aprendizRepository.findByCorreo(correo)
                            .orElseThrow(() -> new RecursoNoEncontradoException("Aprendiz no encontrado con correo: " + correo));
                    PerfilAccesibilidad nuevoPerfil = new PerfilAccesibilidad(aprendiz);
                    return perfilRepository.save(nuevoPerfil);
                });
    }

    @Transactional
    public PerfilAccesibilidad actualizarPerfil(String correo, PerfilAccesibilidad datosActualizados) {
        PerfilAccesibilidad perfil = obtenerPerfilPorCorreo(correo);
        perfil.setVelocidadAudio(datosActualizados.getVelocidadAudio());
        perfil.setModoVisual(datosActualizados.getModoVisual());
        perfil.setTamanoTexto(datosActualizados.getTamanoTexto());
        perfil.setSoloAudio(datosActualizados.isSoloAudio());
        return perfilRepository.save(perfil);
    }
}
