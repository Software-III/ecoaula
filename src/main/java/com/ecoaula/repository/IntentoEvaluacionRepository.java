package com.ecoaula.repository;

import com.ecoaula.model.IntentoEvaluacion;
import com.ecoaula.model.enums.EstadoIntento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para intentos de evaluación.
 */
@Repository
public interface IntentoEvaluacionRepository extends JpaRepository<IntentoEvaluacion, Long> {

    @Override
    @EntityGraph(attributePaths = {"matricula", "matricula.curso", "matricula.aprendiz"})
    Optional<IntentoEvaluacion> findById(Long id);

    @EntityGraph(attributePaths = {"matricula", "matricula.curso", "matricula.aprendiz"})
    List<IntentoEvaluacion> findByMatriculaIdOrderByFechaInicioDesc(Long matriculaId);

    @EntityGraph(attributePaths = {"matricula", "matricula.curso", "matricula.aprendiz"})
    Optional<IntentoEvaluacion> findByMatriculaIdAndEstado(Long matriculaId, EstadoIntento estado);

    long countByMatriculaIdAndEstado(Long matriculaId, EstadoIntento estado);
}
