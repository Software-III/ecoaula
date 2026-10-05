package com.ecoaula.repository;

import com.ecoaula.model.Certificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad Certificado (ADR-06).
 */
@Repository
public interface CertificadoRepository extends JpaRepository<Certificado, Long> {

    Optional<Certificado> findByCodigoVerificacion(String codigoVerificacion);

    Optional<Certificado> findByMatriculaId(Long matriculaId);

    boolean existsByCodigoVerificacion(String codigoVerificacion);
}
