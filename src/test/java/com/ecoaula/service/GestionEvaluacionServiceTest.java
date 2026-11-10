package com.ecoaula.service;

import com.ecoaula.exception.ReglaNegocioException;
import com.ecoaula.model.Curso;
import com.ecoaula.model.Evaluacion;
import com.ecoaula.model.OpcionPregunta;
import com.ecoaula.model.Pregunta;
import com.ecoaula.repository.CursoRepository;
import com.ecoaula.repository.EvaluacionRepository;
import com.ecoaula.repository.MatriculaRepository;
import com.ecoaula.repository.OpcionPreguntaRepository;
import com.ecoaula.repository.PreguntaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proceso 3: el gestor crea la evaluación final de un curso (NC-01 de la auditoría).
 */
@ExtendWith(MockitoExtension.class)
class GestionEvaluacionServiceTest {

    @Mock
    private CursoRepository cursoRepository;
    @Mock
    private EvaluacionRepository evaluacionRepository;
    @Mock
    private PreguntaRepository preguntaRepository;
    @Mock
    private OpcionPreguntaRepository opcionRepository;
    @Mock
    private MatriculaRepository matriculaRepository;

    private GestionEvaluacionService servicio;
    private Curso curso;
    private Evaluacion evaluacion;

    @BeforeEach
    void setUp() {
        servicio = new GestionEvaluacionService(cursoRepository, evaluacionRepository, preguntaRepository,
                opcionRepository, matriculaRepository);
        curso = new Curso("Curso accesible", "Desc", 10, "Tecnología", "Virtual");
        curso.setId(1L);
        curso.setActivo(false);
        evaluacion = new Evaluacion(curso);
        evaluacion.setId(7L);
    }

    private void cursoExiste() {
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
    }

    @Test
    @DisplayName("Agrega una pregunta con 4 opciones y exactamente una correcta; crea la evaluación si no existía")
    void agregaPreguntaYCreaLaEvaluacion() {
        cursoExiste();
        when(evaluacionRepository.findByCursoId(1L)).thenReturn(Optional.empty());
        when(evaluacionRepository.save(any(Evaluacion.class))).thenReturn(evaluacion);
        when(preguntaRepository.countByEvaluacionId(7L)).thenReturn(0L);
        when(preguntaRepository.save(any(Pregunta.class))).thenAnswer(i -> i.getArgument(0));

        Pregunta creada = servicio.agregarPregunta(1L, "  ¿Qué tecla avanza entre elementos?  ",
                Arrays.asList("Escape", "Tabulador", "Bloq Mayús", "Impr Pant"), 2);

        assertEquals("¿Qué tecla avanza entre elementos?", creada.getEnunciado());
        assertEquals(1, creada.getOrden());
        ArgumentCaptor<OpcionPregunta> opciones = ArgumentCaptor.forClass(OpcionPregunta.class);
        verify(opcionRepository, times(4)).save(opciones.capture());
        List<OpcionPregunta> guardadas = opciones.getAllValues();
        assertEquals(1, guardadas.stream().filter(OpcionPregunta::isCorrecta).count());
        assertEquals("Tabulador", guardadas.stream().filter(OpcionPregunta::isCorrecta).findFirst().orElseThrow().getTexto());
    }

    @Test
    @DisplayName("La nueva pregunta queda al final del orden existente")
    void laNuevaPreguntaVaAlFinal() {
        cursoExiste();
        when(evaluacionRepository.findByCursoId(1L)).thenReturn(Optional.of(evaluacion));
        when(preguntaRepository.countByEvaluacionId(7L)).thenReturn(2L);
        when(preguntaRepository.save(any(Pregunta.class))).thenAnswer(i -> i.getArgument(0));

        Pregunta creada = servicio.agregarPregunta(1L, "Pregunta tres", Arrays.asList("Sí", "No", null, null), 1);

        assertEquals(3, creada.getOrden());
        verify(opcionRepository, times(2)).save(any(OpcionPregunta.class));
    }

    @Test
    @DisplayName("Las opciones vacías se descartan y la correcta se identifica por su posición original")
    void descartaOpcionesVaciasSinPerderLaCorrecta() {
        cursoExiste();
        when(evaluacionRepository.findByCursoId(1L)).thenReturn(Optional.of(evaluacion));
        when(preguntaRepository.countByEvaluacionId(7L)).thenReturn(0L);
        when(preguntaRepository.save(any(Pregunta.class))).thenAnswer(i -> i.getArgument(0));

        servicio.agregarPregunta(1L, "Pregunta", Arrays.asList("Falsa", "", "Verdadera", "  "), 3);

        ArgumentCaptor<OpcionPregunta> opciones = ArgumentCaptor.forClass(OpcionPregunta.class);
        verify(opcionRepository, times(2)).save(opciones.capture());
        OpcionPregunta correcta = opciones.getAllValues().stream().filter(OpcionPregunta::isCorrecta).findFirst().orElseThrow();
        assertEquals("Verdadera", correcta.getTexto());
    }

    @Test
    @DisplayName("Rechaza un enunciado vacío")
    void rechazaEnunciadoVacio() {
        cursoExiste();
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.agregarPregunta(1L, "   ", Arrays.asList("A", "B", null, null), 1));
        assertTrue(ex.getMessage().contains("enunciado"));
        verify(preguntaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza una pregunta sin respuesta correcta marcada")
    void rechazaSinCorrecta() {
        cursoExiste();
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.agregarPregunta(1L, "Pregunta", Arrays.asList("A", "B", null, null), 0));
        assertTrue(ex.getMessage().contains("correcta"));
    }

    @Test
    @DisplayName("Rechaza marcar como correcta una opción vacía")
    void rechazaCorrectaVacia() {
        cursoExiste();
        assertThrows(ReglaNegocioException.class,
                () -> servicio.agregarPregunta(1L, "Pregunta", Arrays.asList("A", "B", "", null), 3));
    }

    @Test
    @DisplayName("Rechaza preguntas con menos de 2 opciones con texto")
    void rechazaMenosDeDosOpciones() {
        cursoExiste();
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.agregarPregunta(1L, "Pregunta", Arrays.asList("Única", "", null, null), 1));
        assertTrue(ex.getMessage().contains("al menos 2 opciones"));
    }

    @Test
    @DisplayName("Rechaza opciones repetidas, sin distinguir mayúsculas")
    void rechazaOpcionesRepetidas() {
        cursoExiste();
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.agregarPregunta(1L, "Pregunta", Arrays.asList("Tab", "tab", null, null), 1));
        assertTrue(ex.getMessage().contains("repetirse"));
    }

    @Test
    @DisplayName("Rechaza enunciado y opciones que superan el largo permitido")
    void rechazaTextosDemasiadoLargos() {
        cursoExiste();
        String largo = "x".repeat(GestionEvaluacionService.MAX_ENUNCIADO + 1);
        assertThrows(ReglaNegocioException.class,
                () -> servicio.agregarPregunta(1L, largo, Arrays.asList("A", "B", null, null), 1));
        String opcionLarga = "y".repeat(GestionEvaluacionService.MAX_OPCION + 1);
        assertThrows(ReglaNegocioException.class,
                () -> servicio.agregarPregunta(1L, "Pregunta", Arrays.asList(opcionLarga, "B", null, null), 1));
    }

    @Test
    @DisplayName("No se modifica la evaluación de un curso publicado en el catálogo")
    void noModificaConElCursoPublicado() {
        curso.setActivo(true);
        cursoExiste();
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.agregarPregunta(1L, "Pregunta", Arrays.asList("A", "B", null, null), 1));
        assertTrue(ex.getMessage().contains("Retírelo del catálogo"));
        assertTrue(servicio.motivoDeBloqueo(1L).contains("publicado"));
    }

    @Test
    @DisplayName("No se modifica la evaluación si ya hay aprendices matriculados")
    void noModificaConMatriculados() {
        cursoExiste();
        when(matriculaRepository.countByCursoId(1L)).thenReturn(2L);
        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> servicio.agregarPregunta(1L, "Pregunta", Arrays.asList("A", "B", null, null), 1));
        assertTrue(ex.getMessage().contains("2 aprendiz"));
    }

    @Test
    @DisplayName("Eliminar una pregunta renumera las restantes de forma continua")
    void eliminarRenumera() {
        cursoExiste();
        when(evaluacionRepository.findByCursoId(1L)).thenReturn(Optional.of(evaluacion));
        Pregunta p1 = new Pregunta(evaluacion, "Uno", 1);
        p1.setId(11L);
        Pregunta p2 = new Pregunta(evaluacion, "Dos", 2);
        p2.setId(12L);
        Pregunta p3 = new Pregunta(evaluacion, "Tres", 3);
        p3.setId(13L);
        when(preguntaRepository.findById(12L)).thenReturn(Optional.of(p2));
        when(preguntaRepository.findByEvaluacionIdOrderByOrdenAsc(7L)).thenReturn(List.of(p1, p3));

        servicio.eliminarPregunta(1L, 12L);

        verify(preguntaRepository).delete(p2);
        assertEquals(1, p1.getOrden());
        assertEquals(2, p3.getOrden());
    }

    @Test
    @DisplayName("No elimina una pregunta que pertenece a la evaluación de otro curso")
    void noEliminaPreguntaAjena() {
        cursoExiste();
        when(evaluacionRepository.findByCursoId(1L)).thenReturn(Optional.of(evaluacion));
        Evaluacion otra = new Evaluacion(new Curso("Otro", "D", 10, "A", "V"));
        otra.setId(99L);
        Pregunta ajena = new Pregunta(otra, "Ajena", 1);
        ajena.setId(50L);
        when(preguntaRepository.findById(50L)).thenReturn(Optional.of(ajena));

        assertThrows(com.ecoaula.exception.RecursoNoEncontradoException.class, () -> servicio.eliminarPregunta(1L, 50L));
        verify(preguntaRepository, never()).delete(any(Pregunta.class));
    }

    @Test
    @DisplayName("Un curso sin evaluación devuelve una lista vacía de preguntas")
    void cursoSinEvaluacionListaVacia() {
        when(evaluacionRepository.findByCursoId(1L)).thenReturn(Optional.empty());
        assertTrue(servicio.listarPreguntas(1L).isEmpty());
    }
}
