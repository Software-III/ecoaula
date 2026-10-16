package com.ecoaula.repository;

import com.ecoaula.model.Contenido;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @EntityGraph(attributePaths = {"curso"})
    List<Contenido> findAllByOrderByCursoIdAscOrdenAsc();

    @EntityGraph(attributePaths = {"curso"})
    Optional<Contenido> findConCursoById(Long id);

    /**
     * Bloquea la lección mientras se cambia su versión vigente, para que dos operaciones
     * concurrentes no dejen dos versiones PUBLICADAS (ADR-08).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Contenido c WHERE c.id = :id")
    Optional<Contenido> findByIdParaActualizar(@Param("id") Long id);
}
