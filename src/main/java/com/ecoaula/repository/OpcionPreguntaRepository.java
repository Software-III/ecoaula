package com.ecoaula.repository;

import com.ecoaula.model.OpcionPregunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para opciones de preguntas.
 */
@Repository
public interface OpcionPreguntaRepository extends JpaRepository<OpcionPregunta, Long> {

    List<OpcionPregunta> findByPreguntaId(Long preguntaId);
}
