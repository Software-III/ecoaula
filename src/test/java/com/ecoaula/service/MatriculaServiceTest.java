package com.ecoaula.service;

import com.ecoaula.exception.AccesoDenegadoException;
import com.ecoaula.exception.MatriculaDuplicadaException;
import com.ecoaula.model.Aprendiz;
import com.ecoaula.model.Curso;
import com.ecoaula.model.Matricula;
import com.ecoaula.notification.NotificacionService;
import com.ecoaula.repository.AprendizRepository;
import com.ecoaula.repository.CursoRepository;
import com.ecoaula.repository.MatriculaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatriculaServiceTest {

    @Mock
    private MatriculaRepository matriculaRepository;

    @Mock
    private AprendizRepository aprendizRepository;

    @Mock
    private CursoRepository cursoRepository;

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private MatriculaService matriculaService;

    private Aprendiz aprendiz;
    private Curso curso;

    @BeforeEach
    void setUp() {
        aprendiz = new Aprendiz();
        aprendiz.setId(1L);
        aprendiz.setCorreo("test@ecoaula.local");
        aprendiz.setNombreCompleto("Estudiante Prueba");

        curso = new Curso("Ofimática Accesible", "Curso demo", 10, "Tecnología", "Virtual");
        curso.setId(10L);
    }

    @Test
    @DisplayName("Debe matricular exitosamente al aprendiz si no tiene matrícula previa")
    void debeMatricularExitosamente() {
        when(aprendizRepository.findByCorreo(aprendiz.getCorreo())).thenReturn(Optional.of(aprendiz));
        when(cursoRepository.findById(curso.getId())).thenReturn(Optional.of(curso));
        when(matriculaRepository.existsByAprendizIdAndCursoId(aprendiz.getId(), curso.getId())).thenReturn(false);

        Matricula guardada = new Matricula(aprendiz, curso);
        guardada.setId(100L);
        when(matriculaRepository.save(any(Matricula.class))).thenReturn(guardada);

        Matricula resultado = matriculaService.matricular(aprendiz.getCorreo(), curso.getId());

        assertNotNull(resultado);
        assertEquals(100L, resultado.getId());
        verify(notificacionService, times(1)).notificarMatricula(aprendiz.getCorreo(), curso.getTitulo());
    }

    @Test
    @DisplayName("Debe rechazar con MatriculaDuplicadaException si el aprendiz ya está matriculado en el curso")
    void debeRechazarMatriculaDuplicada() {
        when(aprendizRepository.findByCorreo(aprendiz.getCorreo())).thenReturn(Optional.of(aprendiz));
        when(cursoRepository.findById(curso.getId())).thenReturn(Optional.of(curso));
        when(matriculaRepository.existsByAprendizIdAndCursoId(aprendiz.getId(), curso.getId())).thenReturn(true);

        assertThrows(MatriculaDuplicadaException.class, () ->
                matriculaService.matricular(aprendiz.getCorreo(), curso.getId()));

        verify(matriculaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe rechazar con AccesoDenegadoException si un aprendiz intenta acceder a la matrícula de otro")
    void debeRechazarAccesoAMatriculaAjena() {
        Matricula matriculaAjena = new Matricula(aprendiz, curso);
        matriculaAjena.setId(200L);

        when(matriculaRepository.findById(200L)).thenReturn(Optional.of(matriculaAjena));

        assertThrows(AccesoDenegadoException.class, () ->
                matriculaService.obtenerPorIdYValidarPropiedad(200L, "otro.usuario@ecoaula.local"));
    }
}
