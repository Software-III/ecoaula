package com.ecoaula.repository;

import com.ecoaula.model.EventoAprendizaje;
import com.ecoaula.model.enums.TipoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Repositorio de la bitácora de eventos del aprendiz (analítica del aprendizaje).
 */
@Repository
public interface EventoAprendizajeRepository extends JpaRepository<EventoAprendizaje, Long> {

    List<EventoAprendizaje> findByTipoAndFechaGreaterThanEqualOrderByFechaAsc(TipoEvento tipo, LocalDateTime desde);

    List<EventoAprendizaje> findByTipoInOrderByFechaAsc(Collection<TipoEvento> tipos);

    boolean existsBySesionAndTipo(String sesion, TipoEvento tipo);

    @Query("SELECT COUNT(DISTINCT e.correo) FROM EventoAprendizaje e WHERE e.correo IS NOT NULL AND e.fecha >= :desde")
    long contarAprendicesActivos(@Param("desde") LocalDateTime desde);
}
