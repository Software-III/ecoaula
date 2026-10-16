package com.ecoaula.analitica;

import com.ecoaula.model.Curso;
import com.ecoaula.model.EventoAprendizaje;
import com.ecoaula.model.ReporteBarrera;
import com.ecoaula.model.enums.EstadoReporte;
import com.ecoaula.model.enums.EtapaReporte;
import com.ecoaula.model.enums.TipoEvento;
import com.ecoaula.repository.AvanceLeccionRepository;
import com.ecoaula.repository.ContenidoRepository;
import com.ecoaula.repository.CursoRepository;
import com.ecoaula.repository.EventoAprendizajeRepository;
import com.ecoaula.repository.ReporteBarreraRepository;
import com.ecoaula.service.RevisionContenidoService;
import com.ecoaula.service.dto.IndicadoresCalidad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IndicadoresServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 15, 12, 0);

    @Mock
    private EventoAprendizajeRepository eventoRepository;
    @Mock
    private ReporteBarreraRepository reporteRepository;
    @Mock
    private RevisionContenidoService revisionContenidoService;
    @Mock
    private AvanceLeccionRepository avanceRepository;
    @Mock
    private ContenidoRepository contenidoRepository;
    @Mock
    private CursoRepository cursoRepository;

    private IndicadoresService servicio;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(AHORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        servicio = new IndicadoresService(eventoRepository, reporteRepository, revisionContenidoService,
                avanceRepository, contenidoRepository, cursoRepository, reloj);
        when(eventoRepository.findByTipoAndFechaGreaterThanEqualOrderByFechaAsc(any(), any())).thenReturn(List.of());
        when(eventoRepository.findByTipoInOrderByFechaAsc(anyCollection())).thenReturn(List.of());
        when(reporteRepository.findByEtapaAndCreadoEnGreaterThanEqual(any(), any())).thenReturn(List.of());
        when(reporteRepository.findByEstadoAndResueltoEnGreaterThanEqual(any(), any())).thenReturn(List.of());
        when(cursoRepository.findAll()).thenReturn(List.of());
        when(revisionContenidoService.calcularIndicadores()).thenReturn(new IndicadoresCalidad(0, 0, 0, 0, 0));
    }

    private static EventoAprendizaje evento(TipoEvento tipo, String correo, String sesion, Long curso, Long leccion,
                                            LocalDateTime fecha) {
        EventoAprendizaje e = new EventoAprendizaje(tipo, correo, sesion, curso, leccion);
        e.setFecha(fecha);
        return e;
    }

    private void eventos(TipoEvento tipo, EventoAprendizaje... lista) {
        when(eventoRepository.findByTipoAndFechaGreaterThanEqualOrderByFechaAsc(eq(tipo), any())).thenReturn(List.of(lista));
    }

    private static IndicadoresService.Indicador buscar(List<IndicadoresService.Indicador> indicadores, String nombre) {
        return indicadores.stream().filter(i -> i.nombre().startsWith(nombre)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Proceso 1: finalización autónoma, tiempo de matrícula y deserción a partir del recorrido de registro")
    void indicadoresDelProceso1() {
        LocalDateTime inicio = AHORA.minusDays(1);
        // Visita v1: abre el registro, crea la cuenta y se matricula 6 minutos después
        // Visita v2: abre el registro y abandona
        // Visita v3: abre el registro, se matricula a los 12 minutos, pero reportó una barrera (con ayuda)
        eventos(TipoEvento.REGISTRO_INICIADO,
                evento(TipoEvento.REGISTRO_INICIADO, null, "v1", null, null, inicio),
                evento(TipoEvento.REGISTRO_INICIADO, null, "v2", null, null, inicio),
                evento(TipoEvento.REGISTRO_INICIADO, null, "v3", null, null, inicio));
        eventos(TipoEvento.REGISTRO_COMPLETADO,
                evento(TipoEvento.REGISTRO_COMPLETADO, "a@x.co", "v1", null, null, inicio.plusMinutes(3)),
                evento(TipoEvento.REGISTRO_COMPLETADO, "c@x.co", "v3", null, null, inicio.plusMinutes(5)));
        eventos(TipoEvento.MATRICULA_COMPLETADA,
                evento(TipoEvento.MATRICULA_COMPLETADA, "a@x.co", "v1", 1L, null, inicio.plusMinutes(6)),
                evento(TipoEvento.MATRICULA_COMPLETADA, "c@x.co", "v3", 1L, null, inicio.plusMinutes(12)));
        ReporteBarrera reporte = new ReporteBarrera("c@x.co", null, EtapaReporte.REGISTRO_MATRICULA, null, null, null, null, "No anuncia el botón");
        when(reporteRepository.findByEtapaAndCreadoEnGreaterThanEqual(eq(EtapaReporte.REGISTRO_MATRICULA), any()))
                .thenReturn(List.of(reporte));
        when(eventoRepository.contarAprendicesActivos(any())).thenReturn(10L);

        List<IndicadoresService.Indicador> p1 = servicio.calcular(30).proceso1();

        IndicadoresService.Indicador autonomia = buscar(p1, "Tasa de finalización autónoma");
        assertTrue(autonomia.valor().startsWith("33,3 %"), autonomia.valor());
        assertFalse(autonomia.cumple());

        IndicadoresService.Indicador tiempo = buscar(p1, "Tiempo de finalización");
        assertTrue(tiempo.valor().startsWith("9,0 min"), tiempo.valor());
        assertTrue(tiempo.cumple());

        IndicadoresService.Indicador desercion = buscar(p1, "Tasa de deserción");
        assertTrue(desercion.valor().startsWith("33,3 %"), desercion.valor());
        assertFalse(desercion.cumple());

        IndicadoresService.Indicador incidencias = buscar(p1, "Tasa de incidencias");
        assertTrue(incidencias.valor().startsWith("10,0 %"), incidencias.valor());
        assertFalse(incidencias.cumple());
    }

    @Test
    @DisplayName("Proceso 2: First-Pass, finalización autónoma, tiempo de acceso, deserción y aprendices en riesgo")
    void indicadoresDelProceso2() {
        // 9 de 10 versiones aprobadas en su primera revisión; 4 publicadas con 5 horas promedio
        when(revisionContenidoService.calcularIndicadores()).thenReturn(new IndicadoresCalidad(90.0, 10, 9, 5.0, 4));

        LocalDateTime login = AHORA.minusHours(2);
        eventos(TipoEvento.INICIO_SESION, evento(TipoEvento.INICIO_SESION, "a@x.co", "s1", null, null, login));
        eventos(TipoEvento.AUDIO_REPRODUCIDO, evento(TipoEvento.AUDIO_REPRODUCIDO, "a@x.co", "s1", 1L, 11L, login.plusSeconds(20)));
        // a abre y completa la lección 11; b abre la 11 y no la completa
        eventos(TipoEvento.LECCION_ABIERTA,
                evento(TipoEvento.LECCION_ABIERTA, "a@x.co", "s1", 1L, 11L, login.plusSeconds(10)),
                evento(TipoEvento.LECCION_ABIERTA, "b@x.co", "s2", 1L, 11L, login));
        eventos(TipoEvento.LECCION_COMPLETADA, evento(TipoEvento.LECCION_COMPLETADA, "a@x.co", "s1", 1L, 11L, login.plusMinutes(5)));

        // Recorridos históricos: a terminó el curso; b lo dejó hace 20 días; d lo dejó hace 9 días (en riesgo)
        when(eventoRepository.findByTipoInOrderByFechaAsc(anyCollection())).thenReturn(List.of(
                evento(TipoEvento.LECCION_ABIERTA, "b@x.co", "s0", 1L, 11L, AHORA.minusDays(20)),
                evento(TipoEvento.LECCION_ABIERTA, "d@x.co", "s9", 1L, 11L, AHORA.minusDays(9)),
                evento(TipoEvento.LECCION_COMPLETADA, "a@x.co", "s1", 1L, 11L, AHORA.minusHours(1))));
        when(contenidoRepository.countByCursoId(1L)).thenReturn(2L);
        when(avanceRepository.findLeccionesCompletadasEnCurso("a@x.co", 1L)).thenReturn(List.of(11L, 12L));
        when(avanceRepository.findLeccionesCompletadasEnCurso("b@x.co", 1L)).thenReturn(List.of());
        when(avanceRepository.findLeccionesCompletadasEnCurso("d@x.co", 1L)).thenReturn(List.of(11L));
        Curso curso = new Curso("Ofimática", "d", 10, "a", "m");
        curso.setId(1L);
        when(cursoRepository.findAll()).thenReturn(List.of(curso));

        IndicadoresService.Tablero tablero = servicio.calcular(30);
        List<IndicadoresService.Indicador> p2 = tablero.proceso2();

        assertTrue(buscar(p2, "Tasa de Accesibilidad First-Pass").valor().startsWith("90,0 %"));
        assertTrue(buscar(p2, "Tasa de Accesibilidad First-Pass").cumple());
        assertTrue(buscar(p2, "Tiempo promedio de publicación").valor().startsWith("5,0 horas (4 versiones publicadas)"));
        assertTrue(buscar(p2, "Tasa de finalización autónoma").valor().startsWith("50,0 %"));
        IndicadoresService.Indicador acceso = buscar(p2, "Tiempo de acceso");
        assertTrue(acceso.valor().startsWith("20,0 s"), acceso.valor());
        assertTrue(acceso.cumple());
        // Solo b superó los 14 días de inactividad con el curso incompleto: 1 de 3 recorridos
        assertTrue(buscar(p2, "Tasa de deserción").valor().startsWith("33,3 %"));
        assertNull(buscar(p2, "Tasa de cumplimiento WCAG").cumple());
        assertTrue(buscar(p2, "Exactitud del registro de avance").cumple());

        // En riesgo: b (20 días) y d (9 días), del más inactivo al menos inactivo; a terminó
        assertEquals(List.of("b@x.co", "d@x.co"), tablero.enRiesgo().stream().map(IndicadoresService.AprendizEnRiesgo::correo).toList());
        assertEquals("Ofimática", tablero.enRiesgo().get(0).curso());
        assertEquals(1, tablero.enRiesgo().get(1).completadas());
    }

    @Test
    @DisplayName("Sin actividad, los indicadores dicen que no hay datos en lugar de mostrar 0 %")
    void sinDatos() {
        IndicadoresService.Tablero tablero = servicio.calcular(30);

        assertTrue(tablero.proceso1().stream().allMatch(i -> i.cumple() == null));
        assertEquals("Sin datos en el periodo", buscar(tablero.proceso2(), "Tasa de Accesibilidad First-Pass").valor());
        assertEquals("Sin reportes resueltos en el periodo", tablero.tiempoPromedioAtencion());
        assertTrue(tablero.enRiesgo().isEmpty());
    }

    @Test
    @DisplayName("Calcula el tiempo promedio de atención de los reportes resueltos")
    void tiempoDeAtencion() {
        ReporteBarrera resuelto = new ReporteBarrera("a@x.co", null, EtapaReporte.CONTENIDO, null, null, null, null, "x");
        resuelto.setCreadoEn(AHORA.minusHours(5));
        resuelto.setResueltoEn(AHORA.minusHours(2));
        when(reporteRepository.findByEstadoAndResueltoEnGreaterThanEqual(eq(EstadoReporte.RESUELTO), any()))
                .thenReturn(List.of(resuelto));

        assertEquals("3,0 horas (1 reportes resueltos)", servicio.calcular(30).tiempoPromedioAtencion());
    }
}
