package com.ecoaula.repository;

import com.ecoaula.model.Matricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Matricula.
 */
@Repository
public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    Optional<Matricula> findByAprendizIdAndCursoId(Long aprendizId, Long cursoId);

    Optional<Matricula> findByAprendizCorreoAndCursoId(String correo, Long cursoId);

    boolean existsByAprendizIdAndCursoId(Long aprendizId, Long cursoId);

    boolean existsByAprendizCorreoAndCursoId(String correo, Long cursoId);

    List<Matricula> findByAprendizId(Long aprendizId);

    List<Matricula> findByAprendizCorreo(String correo);
}
