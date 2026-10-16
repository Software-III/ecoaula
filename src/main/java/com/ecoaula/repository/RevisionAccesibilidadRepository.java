package com.ecoaula.repository;

import com.ecoaula.model.RevisionAccesibilidad;
import com.ecoaula.model.enums.ResultadoRevision;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para auditorías y revisiones de accesibilidad.
 */
@Repository
public interface RevisionAccesibilidadRepository extends JpaRepository<RevisionAccesibilidad, Long> {

    List<RevisionAccesibilidad> findByVersionIdOrderByFechaDesc(Long versionId);

    List<RevisionAccesibilidad> findByVersionIdOrderByFechaAsc(Long versionId);

    long countByResultado(ResultadoRevision resultado);

    List<RevisionAccesibilidad> findAllByOrderByFechaDesc();

    /** Informes con lo que muestran las vistas (open-in-view está desactivado). */
    @EntityGraph(attributePaths = {"itemsChequeados", "version", "version.contenido", "version.contenido.curso", "version.versionOrigen", "revisor"})
    List<RevisionAccesibilidad> findConDetalleByOrderByFechaDesc();

    @EntityGraph(attributePaths = {"itemsChequeados", "version", "version.contenido", "version.contenido.curso", "version.versionOrigen", "revisor"})
    Optional<RevisionAccesibilidad> findDetalleById(Long id);

    /** Historial de revisiones de una lección, a través de todas sus versiones. */
    @EntityGraph(attributePaths = {"itemsChequeados", "version", "revisor"})
    List<RevisionAccesibilidad> findByVersionContenidoIdOrderByFechaAsc(Long contenidoId);

    void deleteByVersionContenidoId(Long contenidoId);
}
