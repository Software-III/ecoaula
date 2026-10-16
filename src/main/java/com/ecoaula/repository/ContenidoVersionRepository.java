package com.ecoaula.repository;

import com.ecoaula.model.ContenidoVersion;
import com.ecoaula.model.enums.EstadoContenido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    List<ContenidoVersion> findByContenidoIdAndEstadoIn(Long contenidoId, Collection<EstadoContenido> estados);

    boolean existsByContenidoIdAndEstado(Long contenidoId, EstadoContenido estado);

    boolean existsByContenidoCursoIdAndEstado(Long cursoId, EstadoContenido estado);

    boolean existsByRutaAudio(String rutaAudio);

    /** Versión con lo que muestran las vistas del gestor y del revisor (open-in-view está desactivado). */
    @EntityGraph(attributePaths = {"contenido", "contenido.curso", "creadoPor", "versionOrigen"})
    Optional<ContenidoVersion> findDetalleById(Long id);

    @EntityGraph(attributePaths = {"contenido", "contenido.curso", "creadoPor", "versionOrigen"})
    List<ContenidoVersion> findConDetalleByContenidoIdOrderByNumeroVersionDesc(Long contenidoId);

    @EntityGraph(attributePaths = {"contenido", "contenido.curso", "creadoPor", "versionOrigen"})
    List<ContenidoVersion> findConDetalleByEstadoOrderByEnviadaARevisionEnAsc(EstadoContenido estado);

    /**
     * El aprendiz solo puede escuchar audios de versiones ya publicadas (vigentes o retiradas)
     * de cursos en los que está matriculado; los borradores no le son accesibles.
     */
    @Query("SELECT COUNT(v) > 0 FROM ContenidoVersion v, Matricula m " +
           "WHERE v.rutaAudio = :ruta AND m.curso = v.contenido.curso " +
           "AND m.aprendiz.correo = :correo AND v.estado IN :estados")
    boolean aprendizPuedeEscuchar(@Param("ruta") String ruta, @Param("correo") String correo,
                                  @Param("estados") Collection<EstadoContenido> estados);

    @Query("SELECT cv FROM ContenidoVersion cv WHERE cv.contenido.curso.id = :cursoId ORDER BY cv.contenido.orden ASC, cv.numeroVersion DESC")
    List<ContenidoVersion> findByCursoId(@Param("cursoId") Long cursoId);
}
