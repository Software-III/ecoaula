package com.ecoaula.service;

import com.ecoaula.exception.EvaluacionNoHabilitadaException;
import com.ecoaula.model.*;
import com.ecoaula.model.enums.EstadoIntento;
import com.ecoaula.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluacionServiceTest {

    @Mock
    private EvaluacionRepository evaluacionRepository;
    @Mock
    private PreguntaRepository preguntaRepository;
    @Mock
    private OpcionPreguntaRepository opcionRepository;
    @Mock
    private IntentoEvaluacionRepository intentoRepository;
    @Mock
    private RespuestaIntentoRepository respuestaRepository;
    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private ContenidoService contenidoService;
    @Mock
    private CertificacionService certificacionService;

    private EvaluacionService evaluacionService;

    private Aprendiz aprendiz;
    private Curso curso;
    private Matricula matricula;
    private Evaluacion evaluacion;

    @BeforeEach
    void setUp() {
        evaluacionService = new EvaluacionService(
                evaluacionRepository, preguntaRepository, opcionRepository,
                intentoRepository, respuestaRepository, matriculaRepository,
                contenidoService, certificacionService, 70.0
        );

        aprendiz = new Aprendiz();
        aprendiz.setId(1L);
        aprendiz.setCorreo("aprendiz@ecoaula.local");

        curso = new Curso("Ofimática Accesible", "Desc", 10, "Tec", "Virtual");
        curso.setId(20L);

        evaluacion = new Evaluacion(curso);
        evaluacion.setId(30L);
        curso.setEvaluacion(evaluacion);

        matricula = new Matricula(aprendiz, curso);
        matricula.setId(40L);
    }

    @Test
    @DisplayName("Debe lanzar EvaluacionNoHabilitadaException si el aprendiz no ha completado el 100% de lecciones")
    void debeBloquearEvaluacionSiLeccionesIncompletas() {
        when(matriculaRepository.findById(matricula.getId())).thenReturn(Optional.of(matricula));
        when(contenidoService.todasLeccionesCompletadas(aprendiz.getCorreo(), curso.getId())).thenReturn(false);

        assertThrows(EvaluacionNoHabilitadaException.class, () ->
                evaluacionService.iniciarOContinuarIntento(matricula.getId(), aprendiz.getCorreo()));

        verify(intentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe calificar como APROBADO y emitir certificado si supera el umbral del 70%")
    void debeCalificarAprobadoYEmitirCertificado() {
        IntentoEvaluacion intento = new IntentoEvaluacion(matricula);
        intento.setId(50L);

        when(intentoRepository.findById(50L)).thenReturn(Optional.of(intento));

        List<Pregunta> preguntas = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            preguntas.add(new Pregunta(evaluacion, "Pregunta " + i, i));
        }
        when(preguntaRepository.findByEvaluacionIdOrderByOrdenAsc(evaluacion.getId())).thenReturn(preguntas);

        // Simular 4 respuestas correctas y 1 incorrecta (4/5 = 80%)
        List<RespuestaIntento> respuestas = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            OpcionPregunta opcCorrecta = new OpcionPregunta(preguntas.get(i), "Correcta", "", true);
            respuestas.add(new RespuestaIntento(intento, preguntas.get(i), opcCorrecta));
        }
        OpcionPregunta opcIncorrecta = new OpcionPregunta(preguntas.get(4), "Incorrecta", "", false);
        respuestas.add(new RespuestaIntento(intento, preguntas.get(4), opcIncorrecta));

        when(respuestaRepository.findByIntentoId(50L)).thenReturn(respuestas);
        when(intentoRepository.save(any(IntentoEvaluacion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IntentoEvaluacion resultado = evaluacionService.calificarIntento(50L);

        assertNotNull(resultado);
        assertEquals(80.0, resultado.getPuntaje());
        assertTrue(resultado.getAprobado());
        assertEquals(EstadoIntento.CALIFICADO, resultado.getEstado());

        // Verificar que se emitió el certificado
        verify(certificacionService, times(1)).emitirCertificado(matricula);
    }

    @Test
    @DisplayName("Debe calificar como REPROBADO y NO emitir certificado si el puntaje es menor a 70%")
    void debeCalificarReprobadoSinCertificado() {
        IntentoEvaluacion intento = new IntentoEvaluacion(matricula);
        intento.setId(51L);

        when(intentoRepository.findById(51L)).thenReturn(Optional.of(intento));

        List<Pregunta> preguntas = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            preguntas.add(new Pregunta(evaluacion, "Pregunta " + i, i));
        }
        when(preguntaRepository.findByEvaluacionIdOrderByOrdenAsc(evaluacion.getId())).thenReturn(preguntas);

        // Simular 2 respuestas correctas de 5 (2/5 = 40%)
        List<RespuestaIntento> respuestas = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            OpcionPregunta opcCorrecta = new OpcionPregunta(preguntas.get(i), "Correcta", "", true);
            respuestas.add(new RespuestaIntento(intento, preguntas.get(i), opcCorrecta));
        }
        for (int i = 2; i < 5; i++) {
            OpcionPregunta opcIncorrecta = new OpcionPregunta(preguntas.get(i), "Incorrecta", "", false);
            respuestas.add(new RespuestaIntento(intento, preguntas.get(i), opcIncorrecta));
        }

        when(respuestaRepository.findByIntentoId(51L)).thenReturn(respuestas);
        when(intentoRepository.save(any(IntentoEvaluacion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IntentoEvaluacion resultado = evaluacionService.calificarIntento(51L);

        assertNotNull(resultado);
        assertEquals(40.0, resultado.getPuntaje());
        assertFalse(resultado.getAprobado());

        // No debe emitir certificado
        verify(certificacionService, never()).emitirCertificado(any());
    }
}
