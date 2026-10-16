package com.ecoaula.repository;

import com.ecoaula.model.ContenidoVersion;
import com.ecoaula.model.enums.EstadoContenido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para versiones de lecciones (ADR-08).
 */
@Repository
public interface ContenidoVersionRepository extends JpaRepository<ContenidoVersion, Long> {

    Optional<ContenidoVersion> findByContenidoIdAndEstado(Long contenidoId, EstadoContenido estado);

    List<ContenidoVersion> findByContenidoIdOrderByNumeroVersionDesc(Long contenidoId);

    List<ContenidoVersion> findByContenidoIdOrderByNumeroVersionAsc(Long contenidoId);

    Optional<ContenidoVersion> findTopByContenidoIdOrderByNumeroVersionDesc(Long contenidoId);

    List<ContenidoVersion> findByEstadoOrderByEnviadaARevisionEnAsc(EstadoContenido estado);

    List<ContenidoVersion> findByEstadoOrderByCreadoEnAsc(EstadoContenido estado);

    List<ContenidoVersion> findByEstado(EstadoContenido estado);

    @Query("SELECT cv FROM ContenidoVersion cv WHERE cv.contenido.curso.id = :cursoId ORDER BY cv.contenido.orden ASC, cv.numeroVersion DESC")
    List<ContenidoVersion> findByCursoId(@Param("cursoId") Long cursoId);
}
