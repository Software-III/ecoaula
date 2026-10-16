package com.ecoaula.repository;

import com.ecoaula.model.RevisionAccesibilidad;
import com.ecoaula.model.enums.ResultadoRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para auditorías y revisiones de accesibilidad.
 */
@Repository
public interface RevisionAccesibilidadRepository extends JpaRepository<RevisionAccesibilidad, Long> {

    List<RevisionAccesibilidad> findByVersionIdOrderByFechaDesc(Long versionId);

    List<RevisionAccesibilidad> findByVersionIdOrderByFechaAsc(Long versionId);

    long countByResultado(ResultadoRevision resultado);

    List<RevisionAccesibilidad> findAllByOrderByFechaDesc();
}
