package com.ecoaula.repository;

import com.ecoaula.model.Contenido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Contenido (lecciones).
 */
@Repository
public interface ContenidoRepository extends JpaRepository<Contenido, Long> {

    List<Contenido> findByCursoIdOrderByOrdenAsc(Long cursoId);

    Optional<Contenido> findByCursoIdAndOrden(Long cursoId, Integer orden);

    long countByCursoId(Long cursoId);
}
