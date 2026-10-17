package com.ecoaula.repository;

import com.ecoaula.model.RespuestaIntento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para respuestas registradas durante un intento de evaluación.
 */
@Repository
public interface RespuestaIntentoRepository extends JpaRepository<RespuestaIntento, Long> {

    List<RespuestaIntento> findByIntentoId(Long intentoId);

    Optional<RespuestaIntento> findByIntentoIdAndPreguntaId(Long intentoId, Long preguntaId);
}
