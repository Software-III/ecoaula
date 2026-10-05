package com.ecoaula.repository;

import com.ecoaula.model.PerfilAccesibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para el perfil de accesibilidad.
 */
@Repository
public interface PerfilAccesibilidadRepository extends JpaRepository<PerfilAccesibilidad, Long> {

    Optional<PerfilAccesibilidad> findByAprendizId(Long aprendizId);

    Optional<PerfilAccesibilidad> findByAprendizCorreo(String correo);
}
