package com.ecoaula.service;

import com.ecoaula.exception.AccesoDenegadoException;
import com.ecoaula.exception.EvaluacionNoHabilitadaException;
import com.ecoaula.exception.RecursoNoEncontradoException;
import com.ecoaula.model.*;
import com.ecoaula.model.enums.EstadoIntento;
import com.ecoaula.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio encargado de la gestión de evaluaciones, validación del 100% de avance,
 * registro de respuestas y calificación automática con emisión de certificado.
 */
@Service
public class EvaluacionService {

    private final EvaluacionRepository evaluacionRepository;
    private final PreguntaRepository preguntaRepository;
    private final OpcionPreguntaRepository opcionRepository;
    private final IntentoEvaluacionRepository intentoRepository;
    private final RespuestaIntentoRepository respuestaRepository;
    private final MatriculaRepository matriculaRepository;
    private final ContenidoService contenidoService;
    private final CertificacionService certificacionService;
    private final double umbralAprobacion;

    public EvaluacionService(EvaluacionRepository evaluacionRepository,
                             PreguntaRepository preguntaRepository,
                             OpcionPreguntaRepository opcionRepository,
                             IntentoEvaluacionRepository intentoRepository,
                             RespuestaIntentoRepository respuestaRepository,
                             MatriculaRepository matriculaRepository,
                             ContenidoService contenidoService,
                             CertificacionService certificacionService,
                             @Value("${ecoaula.evaluacion.umbral-aprobacion:70}") double umbralAprobacion) {
        this.evaluacionRepository = evaluacionRepository;
        this.preguntaRepository = preguntaRepository;
        this.opcionRepository = opcionRepository;
        this.intentoRepository = intentoRepository;
        this.respuestaRepository = respuestaRepository;
        this.matriculaRepository = matriculaRepository;
        this.contenidoService = contenidoService;
        this.certificacionService = certificacionService;
        this.umbralAprobacion = umbralAprobacion;
    }

    @Transactional
    public IntentoEvaluacion iniciarOContinuarIntento(Long matriculaId, String correoAprendiz) {
        Matricula matricula = matriculaRepository.findById(matriculaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Matrícula no encontrada: " + matriculaId));

        if (!matricula.getAprendiz().getCorreo().equalsIgnoreCase(correoAprendiz)) {
            throw new AccesoDenegadoException("No tiene autorización para realizar la evaluación de esta matrícula.");
        }

        // Regla estricta: Solo se habilita si el 100% de las lecciones están completadas
        if (!contenidoService.todasLeccionesCompletadas(correoAprendiz, matricula.getCurso().getId())) {
            throw new EvaluacionNoHabilitadaException(
                    "Debe completar el 100% de las lecciones del curso antes de poder presentar la evaluación final."
            );
        }

        // Si ya aprobó previamente y tiene certificado, retornar el intento previo
        Optional<Certificado> certificado = certificacionService.obtenerPorMatriculaId(matricula.getId());
        if (certificado.isPresent()) {
            List<IntentoEvaluacion> anteriores = intentoRepository.findByMatriculaIdOrderByFechaInicioDesc(matricula.getId());
            if (!anteriores.isEmpty()) {
                return anteriores.get(0);
            }
        }

        // Buscar intento en curso o crear uno nuevo
        return intentoRepository.findByMatriculaIdAndEstado(matricula.getId(), EstadoIntento.EN_CURSO)
                .orElseGet(() -> {
                    IntentoEvaluacion nuevo = new IntentoEvaluacion(matricula);
                    return intentoRepository.save(nuevo);
                });
    }

    @Transactional(readOnly = true)
    public Evaluacion obtenerPorCurso(Long cursoId) {
        return evaluacionRepository.findByCursoId(cursoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró evaluación para el curso: " + cursoId));
    }

    @Transactional(readOnly = true)
    public List<Pregunta> listarPreguntas(Long evaluacionId) {
        return preguntaRepository.findByEvaluacionIdOrderByOrdenAsc(evaluacionId);
    }

    @Transactional(readOnly = true)
    public Pregunta obtenerPreguntaPorIndice(Long evaluacionId, int indicePregunta) {
        List<Pregunta> preguntas = listarPreguntas(evaluacionId);
        if (indicePregunta < 1 || indicePregunta > preguntas.size()) {
            throw new RecursoNoEncontradoException("Índice de pregunta fuera de rango: " + indicePregunta);
        }
        return preguntas.get(indicePregunta - 1);
    }

    @Transactional(readOnly = true)
    public Optional<OpcionPregunta> obtenerRespuestaPrevia(Long intentoId, Long preguntaId) {
        return respuestaRepository.findByIntentoIdAndPreguntaId(intentoId, preguntaId)
                .map(RespuestaIntento::getOpcionSeleccionada);
    }

    @Transactional
    public void guardarRespuesta(Long intentoId, Long preguntaId, Long opcionId) {
        IntentoEvaluacion intento = intentoRepository.findById(intentoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Intento no encontrado: " + intentoId));

        Pregunta pregunta = preguntaRepository.findById(preguntaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pregunta no encontrada: " + preguntaId));

        OpcionPregunta opcion = opcionRepository.findById(opcionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Opción no encontrada: " + opcionId));

        RespuestaIntento respuesta = respuestaRepository.findByIntentoIdAndPreguntaId(intentoId, preguntaId)
                .orElseGet(() -> new RespuestaIntento(intento, pregunta, opcion));

        respuesta.setOpcionSeleccionada(opcion);
        respuestaRepository.save(respuesta);
    }

    @Transactional
    public IntentoEvaluacion calificarIntento(Long intentoId) {
        IntentoEvaluacion intento = intentoRepository.findById(intentoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Intento no encontrado: " + intentoId));

        List<Pregunta> preguntas = listarPreguntas(intento.getMatricula().getCurso().getEvaluacion().getId());
        List<RespuestaIntento> respuestas = respuestaRepository.findByIntentoId(intentoId);

        long correctas = 0;
        for (RespuestaIntento r : respuestas) {
            if (r.getOpcionSeleccionada() != null && r.getOpcionSeleccionada().isCorrecta()) {
                correctas++;
            }
        }

        double puntaje = preguntas.isEmpty() ? 0.0 : ((double) correctas / preguntas.size()) * 100.0;
        boolean aprobado = puntaje >= umbralAprobacion;

        intento.setPuntaje(Math.round(puntaje * 10.0) / 10.0);
        intento.setAprobado(aprobado);
        intento.setEstado(EstadoIntento.CALIFICADO);
        intento.setFechaFin(LocalDateTime.now());
        IntentoEvaluacion calificado = intentoRepository.save(intento);

        if (aprobado) {
            // Emisión automática del certificado
            certificacionService.emitirCertificado(calificado.getMatricula());
        }

        return calificado;
    }

    public double getUmbralAprobacion() {
        return umbralAprobacion;
    }
}
