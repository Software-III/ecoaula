package com.ecoaula.repository;

import com.ecoaula.model.Pregunta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para preguntas de evaluación.
 */
@Repository
public interface PreguntaRepository extends JpaRepository<Pregunta, Long> {

    List<Pregunta> findByEvaluacionIdOrderByOrdenAsc(Long evaluacionId);

    long countByEvaluacionId(Long evaluacionId);
}
