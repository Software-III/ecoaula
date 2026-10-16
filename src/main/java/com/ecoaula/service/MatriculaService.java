package com.ecoaula.service;

import com.ecoaula.exception.AccesoDenegadoException;
import com.ecoaula.exception.EcoAulaException;
import com.ecoaula.exception.MatriculaDuplicadaException;
import com.ecoaula.exception.RecursoNoEncontradoException;
import com.ecoaula.model.Aprendiz;
import com.ecoaula.model.Curso;
import com.ecoaula.model.Matricula;
import com.ecoaula.notification.NotificacionService;
import com.ecoaula.repository.AprendizRepository;
import com.ecoaula.repository.CursoRepository;
import com.ecoaula.repository.MatriculaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio encargado del proceso de matrícula de aprendices en cursos (ADR-04).
 * Valida unicidad de matrícula en capa de servicio y captura restricciones únicas en BD.
 */
@Service
public class MatriculaService {

    private final MatriculaRepository matriculaRepository;
    private final AprendizRepository aprendizRepository;
    private final CursoRepository cursoRepository;
    private final NotificacionService notificacionService;

    public MatriculaService(MatriculaRepository matriculaRepository,
                            AprendizRepository aprendizRepository,
                            CursoRepository cursoRepository,
                            NotificacionService notificacionService) {
        this.matriculaRepository = matriculaRepository;
        this.aprendizRepository = aprendizRepository;
        this.cursoRepository = cursoRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public Matricula matricular(String correoAprendiz, Long cursoId) {
        Aprendiz aprendiz = aprendizRepository.findByCorreo(correoAprendiz)
                .orElseThrow(() -> new RecursoNoEncontradoException("Aprendiz no encontrado con correo: " + correoAprendiz));

        Curso curso = cursoRepository.findByIdParaMatricular(cursoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Curso no encontrado con ID: " + cursoId));

        if (!curso.isActivo()) {
            throw new EcoAulaException("El curso " + curso.getTitulo() + " no está disponible para matrículas nuevas.");
        }

        if (matriculaRepository.existsByAprendizIdAndCursoId(aprendiz.getId(), curso.getId())) {
            throw new MatriculaDuplicadaException("Ya se encuentra matriculado en el curso: " + curso.getTitulo());
        }

        if (curso.getCupoMaximo() != null && matriculaRepository.countByCursoId(curso.getId()) >= curso.getCupoMaximo()) {
            throw new EcoAulaException("El curso " + curso.getTitulo() + " no tiene cupos disponibles.");
        }

        try {
            Matricula matricula = new Matricula(aprendiz, curso);
            Matricula guardada = matriculaRepository.save(matricula);
            notificacionService.notificarMatricula(aprendiz.getCorreo(), curso.getTitulo());
            return guardada;
        } catch (DataIntegrityViolationException e) {
            throw new MatriculaDuplicadaException("Ya existe una matrícula registrada para este curso (restricción en BD).");
        }
    }

    @Transactional(readOnly = true)
    public Matricula obtenerPorIdYValidarPropiedad(Long matriculaId, String correoAprendiz) {
        Matricula matricula = matriculaRepository.findById(matriculaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Matrícula no encontrada con ID: " + matriculaId));

        if (!matricula.getAprendiz().getCorreo().equalsIgnoreCase(correoAprendiz)) {
            throw new AccesoDenegadoException("No tiene autorización para acceder a esta matrícula.");
        }

        return matricula;
    }

    /**
     * Cupos disponibles del curso, o null si no tiene límite.
     */
    @Transactional(readOnly = true)
    public Long cuposDisponibles(Curso curso) {
        if (curso.getCupoMaximo() == null) {
            return null;
        }
        return Math.max(0, curso.getCupoMaximo() - matriculaRepository.countByCursoId(curso.getId()));
    }

    @Transactional(readOnly = true)
    public Matricula obtenerPorAprendizYCurso(String correoAprendiz, Long cursoId) {
        return matriculaRepository.findByAprendizCorreoAndCursoId(correoAprendiz, cursoId)
                .orElseThrow(() -> new AccesoDenegadoException("Debe matricularse en el curso para acceder a sus lecciones."));
    }

    @Transactional(readOnly = true)
    public boolean estaMatriculado(String correoAprendiz, Long cursoId) {
        return matriculaRepository.existsByAprendizCorreoAndCursoId(correoAprendiz, cursoId);
    }

    @Transactional(readOnly = true)
    public List<Matricula> listarPorAprendiz(String correoAprendiz) {
        return matriculaRepository.findByAprendizCorreo(correoAprendiz);
    }
}
