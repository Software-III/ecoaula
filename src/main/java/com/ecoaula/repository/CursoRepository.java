package com.ecoaula.repository;

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

    @Query("SELECT c FROM Curso c WHERE c.activo = true AND " +
           "(LOWER(c.titulo) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           " LOWER(c.area) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           " LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :termino, '%')))")
    List<Curso> buscarPorTermino(@Param("termino") String termino);
}
