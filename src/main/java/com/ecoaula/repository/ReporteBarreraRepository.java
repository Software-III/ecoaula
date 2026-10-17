package com.ecoaula.repository;

import com.ecoaula.model.ReporteBarrera;
import com.ecoaula.model.enums.EstadoReporte;
import com.ecoaula.model.enums.EtapaReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio de los reportes de barreras de accesibilidad.
 */
@Repository
public interface ReporteBarreraRepository extends JpaRepository<ReporteBarrera, Long> {

    List<ReporteBarrera> findAllByOrderByCreadoEnDesc();

    List<ReporteBarrera> findByCorreoAprendizOrderByCreadoEnDesc(String correoAprendiz);

    List<ReporteBarrera> findByEtapaAndCreadoEnGreaterThanEqual(EtapaReporte etapa, LocalDateTime desde);

    List<ReporteBarrera> findByEstadoAndResueltoEnGreaterThanEqual(EstadoReporte estado, LocalDateTime desde);

    long countByEstadoNot(EstadoReporte estado);
}
