package com.ecoaula.service;

import com.ecoaula.exception.RecursoNoEncontradoException;
import com.ecoaula.model.Curso;
import com.ecoaula.repository.CursoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio encargado de la consulta y catálogo de cursos accesibles.
 */
@Service
public class CursoService {

    private final CursoRepository cursoRepository;

    public CursoService(CursoRepository cursoRepository) {
        this.cursoRepository = cursoRepository;
    }

    @Transactional(readOnly = true)
    public List<Curso> listarCursosActivos() {
        return cursoRepository.findByActivoTrue();
    }

    @Transactional(readOnly = true)
    public List<Curso> buscarCursos(String termino) {
        if (termino == null || termino.trim().isEmpty()) {
            return listarCursosActivos();
        }
        return cursoRepository.buscarPorTermino(termino.trim());
    }

    @Transactional(readOnly = true)
    public Curso obtenerPorId(Long id) {
        return cursoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el curso con ID: " + id));
    }
}
