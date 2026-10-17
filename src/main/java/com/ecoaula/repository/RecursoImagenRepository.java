package com.ecoaula.repository;

import com.ecoaula.model.RecursoImagen;
import com.ecoaula.model.enums.EstadoContenido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para las imágenes (con texto alternativo) de las versiones de lecciones.
 */
@Repository
public interface RecursoImagenRepository extends JpaRepository<RecursoImagen, Long> {

    List<RecursoImagen> findByContenidoVersionIdOrderByOrdenAsc(Long contenidoVersionId);

    Optional<RecursoImagen> findTopByContenidoVersionIdOrderByOrdenDesc(Long contenidoVersionId);

    boolean existsByRutaArchivo(String rutaArchivo);

    void deleteByContenidoVersionContenidoId(Long contenidoId);

    /**
     * El aprendiz solo puede ver imágenes de versiones ya publicadas (vigentes o retiradas)
     * de cursos en los que está matriculado.
     */
    @Query("SELECT COUNT(i) > 0 FROM RecursoImagen i, Matricula m " +
           "WHERE i.rutaArchivo = :ruta AND m.curso = i.contenidoVersion.contenido.curso " +
           "AND m.aprendiz.correo = :correo AND i.contenidoVersion.estado IN :estados")
    boolean aprendizPuedeVer(@Param("ruta") String ruta, @Param("correo") String correo,
                             @Param("estados") Collection<EstadoContenido> estados);
}
