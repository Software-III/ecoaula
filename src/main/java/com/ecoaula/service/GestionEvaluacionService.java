package com.ecoaula.service;

import com.ecoaula.exception.RecursoNoEncontradoException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Autoría de la evaluación final de un curso por parte del gestor de contenido (Proceso 3: certificación).
 * <p>
 * Es el eslabón que faltaba para que un curso nuevo llegue al catálogo: publicar exige una evaluación
 * con al menos {@link #MIN_PREGUNTAS_PARA_PUBLICAR} preguntas. Las preguntas son de selección única con
 * entre {@link #MIN_OPCIONES} y {@link #MAX_OPCIONES} opciones y exactamente una correcta.
 * <p>
 * Igual que las lecciones, la evaluación solo se modifica con el curso fuera del catálogo y sin aprendices
 * matriculados: cambiarla cambiaría el examen de quien ya lo está presentando.
 */
@Service
public class GestionEvaluacionService {

    public static final int MIN_PREGUNTAS_PARA_PUBLICAR = 3;
    public static final int MIN_OPCIONES = 2;
    public static final int MAX_OPCIONES = 4;
    public static final int MAX_ENUNCIADO = 500;
    public static final int MAX_OPCION = 300;

    private final CursoRepository cursoRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final PreguntaRepository preguntaRepository;
    private final OpcionPreguntaRepository opcionRepository;
    private final MatriculaRepository matriculaRepository;

    public GestionEvaluacionService(CursoRepository cursoRepository,
                                    EvaluacionRepository evaluacionRepository,
                                    PreguntaRepository preguntaRepository,
                                    OpcionPreguntaRepository opcionRepository,
                                    MatriculaRepository matriculaRepository) {
        this.cursoRepository = cursoRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.preguntaRepository = preguntaRepository;
        this.opcionRepository = opcionRepository;
        this.matriculaRepository = matriculaRepository;
    }

    @Transactional(readOnly = true)
    public Curso obtenerCurso(Long cursoId) {
        return cursoRepository.findById(cursoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Curso no encontrado con ID: " + cursoId));
    }

    /** Preguntas del curso en su orden, con sus opciones; lista vacía si el curso aún no tiene evaluación. */
    @Transactional(readOnly = true)
    public List<Pregunta> listarPreguntas(Long cursoId) {
        return evaluacionRepository.findByCursoId(cursoId)
                .map(e -> preguntaRepository.findByEvaluacionIdOrderByOrdenAsc(e.getId()))
                .orElseGet(List::of);
    }

    /** Motivo por el que la evaluación no se puede modificar ahora, o {@code null} si se puede. */
    @Transactional(readOnly = true)
    public String motivoDeBloqueo(Long cursoId) {
        Curso curso = obtenerCurso(cursoId);
        return motivoDeBloqueo(curso);
    }

    /**
     * Agrega una pregunta al final de la evaluación (la crea si el curso aún no la tiene).
     *
     * @param opciones        textos de las opciones tal como llegan del formulario (pueden venir vacíos o nulos)
     * @param posicionCorrecta posición (1 a {@code opciones.size()}) de la opción correcta en esa lista
     */
    @Transactional
    public Pregunta agregarPregunta(Long cursoId, String enunciado, List<String> opciones, int posicionCorrecta) {
        Curso curso = obtenerCurso(cursoId);
        exigirModificable(curso);

        String texto = enunciado == null ? "" : enunciado.strip();
        if (texto.isEmpty()) {
            throw new ReglaNegocioException("El enunciado de la pregunta es obligatorio.");
        }
        if (texto.length() > MAX_ENUNCIADO) {
            throw new ReglaNegocioException("El enunciado no puede superar los " + MAX_ENUNCIADO + " caracteres.");
        }
        if (opciones == null || posicionCorrecta < 1 || posicionCorrecta > opciones.size()) {
            throw new ReglaNegocioException("Indique cuál de las opciones es la respuesta correcta.");
        }
        String correcta = opciones.get(posicionCorrecta - 1);
        if (correcta == null || correcta.isBlank()) {
            throw new ReglaNegocioException("La opción marcada como correcta está vacía. Escriba su texto o marque otra.");
        }

        List<String> limpias = new ArrayList<>();
        Set<String> vistas = new HashSet<>();
        String correctaLimpia = correcta.strip();
        for (String opcion : opciones) {
            if (opcion == null || opcion.isBlank()) {
                continue;
            }
            String limpia = opcion.strip();
            if (limpia.length() > MAX_OPCION) {
                throw new ReglaNegocioException("Cada opción puede tener como máximo " + MAX_OPCION + " caracteres.");
            }
            if (!vistas.add(limpia.toLowerCase(Locale.ROOT))) {
                throw new ReglaNegocioException("Las opciones no pueden repetirse: \"" + limpia + "\" aparece más de una vez.");
            }
            limpias.add(limpia);
        }
        if (limpias.size() < MIN_OPCIONES) {
            throw new ReglaNegocioException("La pregunta necesita al menos " + MIN_OPCIONES + " opciones con texto.");
        }
        if (limpias.size() > MAX_OPCIONES) {
            throw new ReglaNegocioException("La pregunta admite como máximo " + MAX_OPCIONES + " opciones.");
        }

        Evaluacion evaluacion = evaluacionRepository.findByCursoId(cursoId)
                .orElseGet(() -> evaluacionRepository.save(new Evaluacion(curso)));
        int orden = (int) preguntaRepository.countByEvaluacionId(evaluacion.getId()) + 1;

        Pregunta pregunta = preguntaRepository.save(new Pregunta(evaluacion, texto, orden));
        int numero = 1;
        for (String limpia : limpias) {
            boolean esCorrecta = limpia.equals(correctaLimpia);
            opcionRepository.save(new OpcionPregunta(pregunta, limpia, "Opción " + numero, esCorrecta));
            numero++;
        }
        return pregunta;
    }

    /** Elimina una pregunta y renumera las siguientes para que el orden sea continuo. */
    @Transactional
    public void eliminarPregunta(Long cursoId, Long preguntaId) {
        Curso curso = obtenerCurso(cursoId);
        exigirModificable(curso);

        Evaluacion evaluacion = evaluacionRepository.findByCursoId(cursoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El curso no tiene evaluación."));
        Pregunta pregunta = preguntaRepository.findById(preguntaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pregunta no encontrada con ID: " + preguntaId));
        if (!pregunta.getEvaluacion().getId().equals(evaluacion.getId())) {
            throw new RecursoNoEncontradoException("La pregunta no pertenece a la evaluación de este curso.");
        }

        preguntaRepository.delete(pregunta);
        preguntaRepository.flush();

        int orden = 1;
        for (Pregunta restante : preguntaRepository.findByEvaluacionIdOrderByOrdenAsc(evaluacion.getId())) {
            restante.setOrden(orden++);
            preguntaRepository.save(restante);
        }
    }

    private void exigirModificable(Curso curso) {
        String motivo = motivoDeBloqueo(curso);
        if (motivo != null) {
            throw new ReglaNegocioException(motivo);
        }
    }

    private String motivoDeBloqueo(Curso curso) {
        if (curso.isActivo()) {
            return "El curso está publicado en el catálogo. Retírelo del catálogo para modificar su evaluación final.";
        }
        long matriculados = matriculaRepository.countByCursoId(curso.getId());
        if (matriculados > 0) {
            return "No se puede modificar la evaluación: el curso tiene " + matriculados
                    + " aprendiz(es) matriculado(s) y cambiaría el examen que presentan.";
        }
        return null;
    }
}
