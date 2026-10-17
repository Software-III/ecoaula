package com.ecoaula.repository;

import com.ecoaula.model.Matricula;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Matricula.
 */
@Repository
public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    @Override
    @EntityGraph(attributePaths = {"aprendiz", "curso", "certificado"})
    Optional<Matricula> findById(Long id);

    @EntityGraph(attributePaths = {"aprendiz", "curso", "certificado"})
    Optional<Matricula> findByAprendizIdAndCursoId(Long aprendizId, Long cursoId);

    @EntityGraph(attributePaths = {"aprendiz", "curso", "certificado"})
    Optional<Matricula> findByAprendizCorreoAndCursoId(String correo, Long cursoId);

    boolean existsByAprendizIdAndCursoId(Long aprendizId, Long cursoId);

    long countByCursoId(Long cursoId);

    boolean existsByAprendizCorreoAndCursoId(String correo, Long cursoId);

    List<Matricula> findByAprendizId(Long aprendizId);

    @EntityGraph(attributePaths = {"curso"})
    List<Matricula> findByAprendizCorreo(String correo);

    @EntityGraph(attributePaths = {"aprendiz"})
    List<Matricula> findByCursoId(Long cursoId);
}
