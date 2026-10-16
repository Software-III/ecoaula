package com.ecoaula.repository;

import com.ecoaula.model.AvanceLeccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para el avance de lecciones por versión de contenido (ADR-08).
 */
@Repository
public interface AvanceLeccionRepository extends JpaRepository<AvanceLeccion, Long> {

    List<AvanceLeccion> findByAprendizId(Long aprendizId);

    Optional<AvanceLeccion> findByAprendizIdAndContenidoVersionId(Long aprendizId, Long contenidoVersionId);

    @Query("SELECT a FROM AvanceLeccion a WHERE a.aprendiz.id = :aprendizId AND a.contenidoVersion.contenido.id = :contenidoId")
    Optional<AvanceLeccion> findByAprendizIdAndContenidoId(@Param("aprendizId") Long aprendizId, @Param("contenidoId") Long contenidoId);

    @Query("SELECT a.contenidoVersion.contenido.id FROM AvanceLeccion a " +
           "WHERE a.aprendiz.correo = :correo AND a.contenidoVersion.contenido.curso.id = :cursoId")
    List<Long> findLeccionesCompletadasEnCurso(@Param("correo") String correo, @Param("cursoId") Long cursoId);

    @Query("SELECT COUNT(a) FROM AvanceLeccion a WHERE a.aprendiz.id = :aprendizId AND a.contenidoVersion.contenido.curso.id = :cursoId")
    long contarLeccionesCompletadasEnCurso(@Param("aprendizId") Long aprendizId, @Param("cursoId") Long cursoId);
}
