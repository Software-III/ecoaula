package com.ecoaula.repository;

import com.ecoaula.model.Aprendiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad Aprendiz.
 */
@Repository
public interface AprendizRepository extends JpaRepository<Aprendiz, Long> {

    Optional<Aprendiz> findByCorreo(String correo);

    boolean existsByCorreo(String correo);
}
