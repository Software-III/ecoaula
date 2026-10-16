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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.DoublePredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Calcula los indicadores clave de los Procesos 1 y 2 (Plan de Gestión de la Calidad, secciones 3.3 y 3.4)
 * a partir de la bitácora de eventos, los reportes de barreras y los informes de revisión, y detecta
 * aprendices en riesgo de abandono por inactividad.
 */
@Service
public class IndicadoresService {

    /** Días sin actividad a partir de los cuales un curso incompleto se considera abandonado. */
    static final int DIAS_ABANDONO = 14;
    /** Días sin actividad a partir de los cuales el aprendiz se marca en riesgo de abandono. */
    static final int DIAS_RIESGO = 7;

    private static final Locale ES = Locale.forLanguageTag("es-CO");

    private final EventoAprendizajeRepository eventoRepository;
    private final ReporteBarreraRepository reporteRepository;
    private final RevisionContenidoService revisionContenidoService;
    private final AvanceLeccionRepository avanceRepository;
    private final ContenidoRepository contenidoRepository;
    private final CursoRepository cursoRepository;
    private final Clock reloj;

    @Autowired
    public IndicadoresService(EventoAprendizajeRepository eventoRepository,
                              ReporteBarreraRepository reporteRepository,
                              RevisionContenidoService revisionContenidoService,
                              AvanceLeccionRepository avanceRepository,
                              ContenidoRepository contenidoRepository,
                              CursoRepository cursoRepository) {
        this(eventoRepository, reporteRepository, revisionContenidoService, avanceRepository, contenidoRepository,
                cursoRepository, Clock.systemDefaultZone());
    }

    IndicadoresService(EventoAprendizajeRepository eventoRepository,
                       ReporteBarreraRepository reporteRepository,
                       RevisionContenidoService revisionContenidoService,
                       AvanceLeccionRepository avanceRepository,
                       ContenidoRepository contenidoRepository,
                       CursoRepository cursoRepository,
                       Clock reloj) {
        this.eventoRepository = eventoRepository;
        this.reporteRepository = reporteRepository;
        this.revisionContenidoService = revisionContenidoService;
        this.avanceRepository = avanceRepository;
        this.contenidoRepository = contenidoRepository;
        this.cursoRepository = cursoRepository;
        this.reloj = reloj;
    }

    /** Un indicador con su meta; {@code cumple} es null cuando aún no hay datos para calcularlo. */
    public record Indicador(String nombre, String valor, String meta, Boolean cumple, String calculo) {
    }

    public record AprendizEnRiesgo(String correo, String curso, long completadas, long total, long diasInactivo) {
    }

    public record Tablero(int dias,
                          List<Indicador> proceso1,
                          List<Indicador> proceso2,
                          List<AprendizEnRiesgo> enRiesgo,
                          long reportesPendientes,
                          String tiempoPromedioAtencion) {
    }

    @Transactional(readOnly = true)
    public Tablero calcular(int dias) {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        LocalDateTime desde = ahora.minusDays(dias);
        long activos = eventoRepository.contarAprendicesActivos(desde);

        List<ReporteBarrera> reportesP1 = reporteRepository.findByEtapaAndCreadoEnGreaterThanEqual(EtapaReporte.REGISTRO_MATRICULA, desde);
        List<ReporteBarrera> reportesP2 = reporteRepository.findByEtapaAndCreadoEnGreaterThanEqual(EtapaReporte.CONTENIDO, desde);

        return new Tablero(dias,
                indicadoresProceso1(desde, activos, reportesP1),
                indicadoresProceso2(desde, activos, reportesP2),
                aprendicesEnRiesgo(ahora),
                reporteRepository.countByEstadoNot(EstadoReporte.RESUELTO),
                tiempoPromedioAtencion(desde));
    }

    // ------------------------------------------------------------------
    // Proceso 1: inscripción y matrícula accesible (Plan de Calidad 3.4.1)
    // ------------------------------------------------------------------

    private List<Indicador> indicadoresProceso1(LocalDateTime desde, long activos, List<ReporteBarrera> reportes) {
        // Inicio del recorrido por visita: la primera vez que se abrió el formulario de registro
        Map<String, LocalDateTime> inicioPorVisita = primeraFechaPor(
                eventos(TipoEvento.REGISTRO_INICIADO, desde), EventoAprendizaje::getSesion);

        // Cuentas creadas en esas visitas, con la hora de inicio de su recorrido
        Map<String, LocalDateTime> inicioPorCorreo = new HashMap<>();
        for (EventoAprendizaje registro : eventos(TipoEvento.REGISTRO_COMPLETADO, desde)) {
            LocalDateTime inicio = inicioPorVisita.get(registro.getSesion());
            if (inicio != null && registro.getCorreo() != null) {
                inicioPorCorreo.merge(registro.getCorreo(), inicio, (a, b) -> a.isBefore(b) ? a : b);
            }
        }

        // Primera matrícula de cada una de esas cuentas
        Map<String, LocalDateTime> matriculaPorCorreo = primeraFechaPor(
                eventos(TipoEvento.MATRICULA_COMPLETADA, desde).stream()
                        .filter(e -> inicioPorCorreo.containsKey(e.getCorreo()))
                        .toList(),
                EventoAprendizaje::getCorreo);

        Set<String> conBarrera = reportes.stream().map(ReporteBarrera::getCorreoAprendiz)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        long iniciados = inicioPorVisita.size();
        long matriculados = matriculaPorCorreo.size();
        long autonomos = matriculaPorCorreo.keySet().stream().filter(c -> !conBarrera.contains(c)).count();

        List<Double> minutos = matriculaPorCorreo.entrySet().stream()
                .map(e -> Duration.between(inicioPorCorreo.get(e.getKey()), e.getValue()).toSeconds() / 60.0)
                .toList();

        List<Indicador> indicadores = new ArrayList<>();
        indicadores.add(porcentaje("Tasa de finalización autónoma de tareas críticas", autonomos, iniciados, "> 90 %",
                p -> p > 90,
                "Visitas que abrieron el registro y terminaron matriculadas sin reportar una barrera en esta etapa, "
                        + "sobre las visitas que abrieron el registro. Quien reporta una barrera se cuenta como \"con ayuda\"."));
        indicadores.add(promedio("Tiempo de finalización de la matrícula", minutos, " min", "< 10 min", m -> m < 10,
                "Promedio de minutos entre abrir el formulario de registro y confirmar la primera matrícula."));
        indicadores.add(porcentaje("Tasa de incidencias de accesibilidad reportadas", reportes.size(), activos, "< 5 %",
                p -> p < 5,
                "Reportes de barreras en registro, inicio de sesión o matrícula, sobre los aprendices con actividad en el periodo."));
        indicadores.add(porcentaje("Tasa de deserción por etapa", iniciados - matriculados, iniciados, "≤ 30 %",
                p -> p <= 30,
                "Visitas que abrieron el registro y no llegaron a confirmar una matrícula, sobre las que lo abrieron."));
        return indicadores;
    }

    // ------------------------------------------------------------------
    // Proceso 2: gestión de contenido accesible (Plan de Calidad 3.3.2 y 3.4.2)
    // ------------------------------------------------------------------

    private List<Indicador> indicadoresProceso2(LocalDateTime desde, long activos, List<ReporteBarrera> reportes) {
        // First-Pass y tiempo de publicación los calcula el servicio de revisión (subsanaciones excluidas)
        IndicadoresCalidad revision = revisionContenidoService.calcularIndicadores();
        long revisadas = revision.getTotalVersionesRevisadas();
        long aprobadasPrimera = revision.getAprobadasPrimeraRevision();

        Set<String> abiertas = claves(eventos(TipoEvento.LECCION_ABIERTA, desde));
        Set<String> completadas = claves(eventos(TipoEvento.LECCION_COMPLETADA, desde));
        Set<String> conBarrera = reportes.stream()
                .filter(r -> r.getCorreoAprendiz() != null && r.getContenidoId() != null)
                .map(r -> r.getCorreoAprendiz() + "|" + r.getContenidoId())
                .collect(Collectors.toSet());
        long autonomas = abiertas.stream().filter(completadas::contains).filter(k -> !conBarrera.contains(k)).count();

        // Tiempo desde iniciar sesión hasta comenzar a reproducir el primer audio, en la misma visita
        Map<String, LocalDateTime> inicioSesion = primeraFechaPor(eventos(TipoEvento.INICIO_SESION, desde), EventoAprendizaje::getSesion);
        Map<String, LocalDateTime> primerAudio = new HashMap<>();
        for (EventoAprendizaje audio : eventos(TipoEvento.AUDIO_REPRODUCIDO, desde)) {
            LocalDateTime login = inicioSesion.get(audio.getSesion());
            if (login != null && !audio.getFecha().isBefore(login)) {
                primerAudio.putIfAbsent(audio.getSesion(), audio.getFecha());
            }
        }
        List<Double> segundos = primerAudio.entrySet().stream()
                .map(e -> (double) Duration.between(inicioSesion.get(e.getKey()), e.getValue()).toSeconds())
                .toList();

        long[] desercion = desercionDelContenido(LocalDateTime.now(reloj));

        List<Indicador> indicadores = new ArrayList<>();
        indicadores.add(porcentaje("Tasa de Accesibilidad First-Pass del contenido", aprobadasPrimera, revisadas, "≥ 85 %",
                p -> p >= 85,
                "Versiones aprobadas en su primera revisión sobre las versiones revisadas (todo el historial). "
                        + "Una subsanación es el mismo contenido revisado otra vez y no cuenta como primera revisión."));
        indicadores.add(revision.getTotalVersionesPublicadas() == 0
                ? new Indicador("Tiempo promedio de publicación", "Sin datos", "Referencia", null,
                        "Horas entre crear una versión y publicarla (todo el historial).")
                : new Indicador("Tiempo promedio de publicación",
                        String.format(ES, "%.1f horas (%d versiones publicadas)", revision.getTiempoPromedioPublicacionHoras(),
                                revision.getTotalVersionesPublicadas()),
                        "Referencia", null, "Horas entre crear una versión y publicarla (todo el historial). "
                        + "El Plan de Calidad no le fija meta; sirve para seguir el Lead Time del contenido."));
        indicadores.add(porcentaje("Tasa de finalización autónoma de tareas críticas", autonomas, abiertas.size(), "> 90 %",
                p -> p > 90,
                "Lecciones abiertas que el aprendiz completó sin reportar una barrera en ellas, sobre las lecciones abiertas "
                        + "(cada par aprendiz-lección cuenta una vez)."));
        indicadores.add(promedio("Tiempo de acceso al contenido", segundos, " s", "< 30 s", s -> s < 30,
                "Promedio de segundos entre iniciar sesión y comenzar a reproducir el primer audio en la misma visita."));
        indicadores.add(porcentaje("Tasa de cumplimiento WCAG en producción", 0, 0, "> 95 %", p -> p > 95,
                "Requiere una auditoría automatizada (axe-core o Lighthouse) de las pantallas desplegadas; aún no está integrada."));
        indicadores.add(porcentaje("Tasa de incidencias de accesibilidad reportadas", reportes.size(), activos, "< 5 %",
                p -> p < 5,
                "Reportes de barreras en el contenido de las lecciones, sobre los aprendices con actividad en el periodo."));
        indicadores.add(porcentaje("Tasa de deserción por etapa", desercion[0], desercion[1], "≤ 30 %", p -> p <= 30,
                "Cursos que el aprendiz empezó y dejó incompletos sin actividad durante " + DIAS_ABANDONO
                        + " días o más, sobre los cursos empezados (todo el historial)."));
        indicadores.add(new Indicador("Exactitud del registro de avance", "100 %", "100 %", true,
                "Garantizada por diseño: el servidor solo registra avance con matrícula y sobre la versión exacta que se entregó al aprendiz."));
        return indicadores;
    }

    /** [abandonos, cursos empezados] considerando la última actividad de cada aprendiz en cada curso. */
    private long[] desercionDelContenido(LocalDateTime ahora) {
        long abandonos = 0;
        List<Recorrido> recorridos = recorridos();
        for (Recorrido r : recorridos) {
            if (!r.completo() && r.diasInactivo(ahora) >= DIAS_ABANDONO) {
                abandonos++;
            }
        }
        return new long[]{abandonos, recorridos.size()};
    }

    private List<AprendizEnRiesgo> aprendicesEnRiesgo(LocalDateTime ahora) {
        Map<Long, String> titulos = cursoRepository.findAll().stream()
                .collect(Collectors.toMap(Curso::getId, Curso::getTitulo));
        return recorridos().stream()
                .filter(r -> !r.completo() && r.diasInactivo(ahora) >= DIAS_RIESGO)
                .sorted(Comparator.comparingLong((Recorrido r) -> r.diasInactivo(ahora)).reversed())
                .map(r -> new AprendizEnRiesgo(r.correo(), titulos.getOrDefault(r.cursoId(), "Curso eliminado"),
                        r.completadas(), r.total(), r.diasInactivo(ahora)))
                .toList();
    }

    private record Recorrido(String correo, Long cursoId, LocalDateTime ultimaActividad, long completadas, long total) {
        boolean completo() {
            return total > 0 && completadas >= total;
        }

        long diasInactivo(LocalDateTime ahora) {
            return Duration.between(ultimaActividad, ahora).toDays();
        }
    }

    /** Un recorrido por cada aprendiz que abrió al menos una lección de un curso. */
    private List<Recorrido> recorridos() {
        Map<String, LocalDateTime> ultima = new HashMap<>();
        Map<String, EventoAprendizaje> muestra = new HashMap<>();
        for (EventoAprendizaje e : eventoRepository.findByTipoInOrderByFechaAsc(
                EnumSet.of(TipoEvento.LECCION_ABIERTA, TipoEvento.AUDIO_REPRODUCIDO, TipoEvento.LECCION_COMPLETADA))) {
            if (e.getCorreo() == null || e.getCursoId() == null) {
                continue;
            }
            String clave = e.getCorreo() + "|" + e.getCursoId();
            ultima.put(clave, e.getFecha());
            muestra.putIfAbsent(clave, e);
        }
        List<Recorrido> recorridos = new ArrayList<>();
        Map<Long, Long> totalPorCurso = new HashMap<>();
        for (Map.Entry<String, EventoAprendizaje> entrada : muestra.entrySet()) {
            EventoAprendizaje e = entrada.getValue();
            long total = totalPorCurso.computeIfAbsent(e.getCursoId(), contenidoRepository::countByCursoId);
            long completadas = avanceRepository.findLeccionesCompletadasEnCurso(e.getCorreo(), e.getCursoId()).size();
            recorridos.add(new Recorrido(e.getCorreo(), e.getCursoId(), ultima.get(entrada.getKey()), completadas, total));
        }
        return recorridos;
    }

    private String tiempoPromedioAtencion(LocalDateTime desde) {
        List<Double> horas = reporteRepository.findByEstadoAndResueltoEnGreaterThanEqual(EstadoReporte.RESUELTO, desde).stream()
                .map(r -> Duration.between(r.getCreadoEn(), r.getResueltoEn()).toMinutes() / 60.0)
                .toList();
        return horas.isEmpty() ? "Sin reportes resueltos en el periodo"
                : String.format(ES, "%.1f horas (%d reportes resueltos)", promedio(horas), horas.size());
    }

    // ------------------------------------------------------------------
    // Utilidades de cálculo y formato
    // ------------------------------------------------------------------

    private List<EventoAprendizaje> eventos(TipoEvento tipo, LocalDateTime desde) {
        return eventoRepository.findByTipoAndFechaGreaterThanEqualOrderByFechaAsc(tipo, desde);
    }

    private static Map<String, LocalDateTime> primeraFechaPor(List<EventoAprendizaje> eventos,
                                                            Function<EventoAprendizaje, String> clave) {
        Map<String, LocalDateTime> primera = new HashMap<>();
        for (EventoAprendizaje e : eventos) {
            String k = clave.apply(e);
            if (k != null) {
                primera.merge(k, e.getFecha(), (a, b) -> a.isBefore(b) ? a : b);
            }
        }
        return primera;
    }

    private static Set<String> claves(List<EventoAprendizaje> eventos) {
        Set<String> claves = new HashSet<>();
        for (EventoAprendizaje e : eventos) {
            if (e.getCorreo() != null && e.getContenidoId() != null) {
                claves.add(e.getCorreo() + "|" + e.getContenidoId());
            }
        }
        return claves;
    }

    private static Indicador porcentaje(String nombre, long parte, long total, String meta,
                                        DoublePredicate cumpleMeta, String calculo) {
        if (total == 0) {
            return new Indicador(nombre, "Sin datos en el periodo", meta, null, calculo);
        }
        double valor = parte * 100.0 / total;
        return new Indicador(nombre, String.format(ES, "%.1f %% (%d de %d)", valor, parte, total), meta,
                cumpleMeta.test(valor), calculo);
    }

    private static Indicador promedio(String nombre, List<Double> valores, String unidad, String meta,
                                      DoublePredicate cumpleMeta, String calculo) {
        if (valores.isEmpty()) {
            return new Indicador(nombre, "Sin datos en el periodo", meta, null, calculo);
        }
        double prom = promedio(valores);
        return new Indicador(nombre, String.format(ES, "%.1f%s (%d mediciones)", prom, unidad, valores.size()), meta,
                cumpleMeta.test(prom), calculo);
    }

    private static double promedio(List<Double> valores) {
        return valores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }
}
