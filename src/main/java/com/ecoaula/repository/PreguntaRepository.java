package com.ecoaula.repository;

import com.ecoaula.model.Pregunta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para preguntas de evaluación.
 */
@Repository
public interface PreguntaRepository extends JpaRepository<Pregunta, Long> {

    @Override
    @EntityGraph(attributePaths = {"opciones"})
    Optional<Pregunta> findById(Long id);

    @EntityGraph(attributePaths = {"opciones"})
    List<Pregunta> findByEvaluacionIdOrderByOrdenAsc(Long evaluacionId);

    long countByEvaluacionId(Long evaluacionId);
}
