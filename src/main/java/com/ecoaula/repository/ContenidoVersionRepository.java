package com.ecoaula.repository;

import com.ecoaula.model.ContenidoVersion;
import com.ecoaula.model.enums.EstadoContenido;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
