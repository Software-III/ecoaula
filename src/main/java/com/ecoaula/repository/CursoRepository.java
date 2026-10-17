package com.ecoaula.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;

import com.ecoaula.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Curso.
 */
@Repository
public interface CursoRepository extends JpaRepository<Curso, Long> {

    List<Curso> findByActivoTrue();

    /**
     * Bloquea el curso mientras se valida el cupo y se registra la matrícula, para que dos
     * matrículas simultáneas no superen el cupo máximo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Curso c WHERE c.id = :id")
    Optional<Curso> findByIdParaMatricular(@Param("id") Long id);

    @Query("SELECT c FROM Curso c WHERE c.activo = true AND " +
           "(LOWER(c.titulo) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           " LOWER(c.area) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           " LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :termino, '%')))")
    List<Curso> buscarPorTermino(@Param("termino") String termino);
}
