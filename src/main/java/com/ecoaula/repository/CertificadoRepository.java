package com.ecoaula.repository;

import com.ecoaula.model.Certificado;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad Certificado (ADR-06).
 */
@Repository
public interface CertificadoRepository extends JpaRepository<Certificado, Long> {

    @Override
    @EntityGraph(attributePaths = {"matricula", "matricula.aprendiz", "matricula.curso"})
    Optional<Certificado> findById(Long id);

    @EntityGraph(attributePaths = {"matricula", "matricula.aprendiz", "matricula.curso"})
    Optional<Certificado> findByMatriculaId(Long matriculaId);

    Optional<Certificado> findByCodigoVerificacion(String codigoVerificacion);

    boolean existsByCodigoVerificacion(String codigoVerificacion);
}
