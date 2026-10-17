package com.ecoaula.service;

import com.ecoaula.exception.AccesoDenegadoException;
import com.ecoaula.exception.RecursoNoEncontradoException;
import com.ecoaula.exception.ReglaNegocioException;
import com.ecoaula.model.Aprendiz;
import com.ecoaula.model.AvanceLeccion;
import com.ecoaula.model.Contenido;
import com.ecoaula.model.ContenidoVersion;
import com.ecoaula.model.Curso;
import com.ecoaula.model.RecursoImagen;
import com.ecoaula.model.enums.EstadoContenido;
import com.ecoaula.repository.AprendizRepository;
import com.ecoaula.repository.AvanceLeccionRepository;
import com.ecoaula.repository.ContenidoRepository;
import com.ecoaula.repository.ContenidoVersionRepository;
import com.ecoaula.repository.MatriculaRepository;
import com.ecoaula.repository.RecursoImagenRepository;
import com.ecoaula.repository.RevisionAccesibilidadRepository;
import com.ecoaula.storage.AlmacenamientoContenido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContenidoServiceTest {

    @Mock
    private ContenidoRepository contenidoRepository;

    @Mock
    private ContenidoVersionRepository versionRepository;

    @Mock
    private AvanceLeccionRepository avanceRepository;

    @Mock
    private AprendizRepository aprendizRepository;

    @Mock
    private AlmacenamientoContenido almacenamientoContenido;

    @Mock
    private RevisionAccesibilidadRepository revisionRepository;

    @Mock
    private RecursoImagenRepository imagenRepository;

    @Mock
    private MatriculaRepository matriculaRepository;

    @InjectMocks
    private ContenidoService contenidoService;

    private Aprendiz aprendiz;
    private Curso curso;
    private Contenido leccion;
    private ContenidoVersion v1;
    private ContenidoVersion v2;

    @BeforeEach
    void setUp() {
        aprendiz = new Aprendiz();
        aprendiz.setId(1L);
        aprendiz.setCorreo("estudiante@ecoaula.local");

        curso = new Curso("Ofimática", "Curso", 10, "Tecnología", "Virtual");
        curso.setId(5L);

        leccion = new Contenido("Lección 1", 1, curso);
        leccion.setId(20L);

        v1 = new ContenidoVersion(leccion, 1, EstadoContenido.RETIRADA, "audio1.wav", "Transcripción v1", LocalDateTime.now().minusDays(10));
        v1.setId(101L);

        v2 = new ContenidoVersion(leccion, 2, EstadoContenido.PUBLICADA, "audio2.wav", "Transcripción v2", LocalDateTime.now());
        v2.setId(102L);
    }

    @Test
    @DisplayName("Debe conservar la versión exacta que el aprendiz ya había iniciado (ADR-08)")
    void debeConservarVersionIniciadaPorAprendiz() {
        when(aprendizRepository.findByCorreo(aprendiz.getCorreo())).thenReturn(Optional.of(aprendiz));

        // El aprendiz tiene avance en la versión 1
        AvanceLeccion avanceV1 = new AvanceLeccion(aprendiz, v1);
        when(avanceRepository.findByAprendizIdAndContenidoId(aprendiz.getId(), leccion.getId()))
                .thenReturn(Optional.of(avanceV1));

        ContenidoVersion resultado = contenidoService.obtenerVersionParaAprendiz(leccion.getId(), aprendiz.getCorreo());

        assertNotNull(resultado);
        assertEquals(101L, resultado.getId());
        assertEquals(1, resultado.getNumeroVersion());
    }

    @Test
    @DisplayName("Debe entregar la versión vigente PUBLICADA si el aprendiz es nuevo en la lección")
    void debeEntregarVersionPublicadaANuevoAprendiz() {
        when(aprendizRepository.findByCorreo(aprendiz.getCorreo())).thenReturn(Optional.of(aprendiz));
        when(avanceRepository.findByAprendizIdAndContenidoId(aprendiz.getId(), leccion.getId()))
                .thenReturn(Optional.empty());
        when(versionRepository.findByContenidoIdAndEstado(leccion.getId(), EstadoContenido.PUBLICADA))
                .thenReturn(Optional.of(v2));

        ContenidoVersion resultado = contenidoService.obtenerVersionParaAprendiz(leccion.getId(), aprendiz.getCorreo());

        assertNotNull(resultado);
        assertEquals(102L, resultado.getId());
        assertEquals(2, resultado.getNumeroVersion());
    }

    @Test
    @DisplayName("Debe registrar avance vinculado a la versión exacta consumida (ADR-08)")
    void debeRegistrarAvanceLigadoAVersionExacta() {
        when(aprendizRepository.findByCorreo(aprendiz.getCorreo())).thenReturn(Optional.of(aprendiz));
        when(versionRepository.findById(v2.getId())).thenReturn(Optional.of(v2));
        when(avanceRepository.findByAprendizIdAndContenidoVersionId(aprendiz.getId(), v2.getId()))
                .thenReturn(Optional.empty());

        AvanceLeccion nuevoAvance = new AvanceLeccion(aprendiz, v2);
        nuevoAvance.setId(301L);
        when(avanceRepository.save(any(AvanceLeccion.class))).thenReturn(nuevoAvance);

        AvanceLeccion resultado = contenidoService.registrarAvance(aprendiz.getCorreo(), v2.getId());

        assertNotNull(resultado);
        assertEquals(301L, resultado.getId());
        assertEquals(v2.getId(), resultado.getContenidoVersion().getId());
        verify(avanceRepository, times(1)).save(any(AvanceLeccion.class));
    }

    @Test
    @DisplayName("Debe verificar correctamente si el aprendiz completó el 100% de las lecciones")
    void debeValidarTodasLeccionesCompletadas() {
        when(aprendizRepository.findByCorreo(aprendiz.getCorreo())).thenReturn(Optional.of(aprendiz));
        when(contenidoRepository.countByCursoId(curso.getId())).thenReturn(3L);
        when(avanceRepository.contarLeccionesCompletadasEnCurso(aprendiz.getId(), curso.getId())).thenReturn(3L);

        boolean completado = contenidoService.todasLeccionesCompletadas(aprendiz.getCorreo(), curso.getId());
        assertTrue(completado);

        when(avanceRepository.contarLeccionesCompletadasEnCurso(aprendiz.getId(), curso.getId())).thenReturn(2L);
        boolean incompleto = contenidoService.todasLeccionesCompletadas(aprendiz.getCorreo(), curso.getId());
        assertFalse(incompleto);
    }

    @Test
    @DisplayName("No debe registrar avance si el aprendiz no está matriculado en el curso")
    void noDebeRegistrarAvanceSinMatricula() {
        when(matriculaRepository.existsByAprendizCorreoAndCursoId(aprendiz.getCorreo(), curso.getId())).thenReturn(false);

        assertThrows(AccesoDenegadoException.class, () ->
                contenidoService.registrarAvanceEnLeccion(aprendiz.getCorreo(), curso.getId(), 1, v2.getId()));
        verify(avanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("No debe registrar avance con una versión distinta a la que se le entregó al aprendiz")
    void noDebeRegistrarAvanceConVersionAjena() {
        when(matriculaRepository.existsByAprendizCorreoAndCursoId(aprendiz.getCorreo(), curso.getId())).thenReturn(true);
        when(contenidoRepository.findByCursoIdAndOrden(curso.getId(), 1)).thenReturn(Optional.of(leccion));
        when(aprendizRepository.findByCorreo(aprendiz.getCorreo())).thenReturn(Optional.of(aprendiz));
        when(avanceRepository.findByAprendizIdAndContenidoId(aprendiz.getId(), leccion.getId())).thenReturn(Optional.empty());
        when(versionRepository.findByContenidoIdAndEstado(leccion.getId(), EstadoContenido.PUBLICADA)).thenReturn(Optional.of(v2));

        // v1 está retirada y el aprendiz nunca la inició: no puede marcarla como completada
        assertThrows(ReglaNegocioException.class, () ->
                contenidoService.registrarAvanceEnLeccion(aprendiz.getCorreo(), curso.getId(), 1, v1.getId()));
        verify(avanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe exigir texto alternativo al agregar una imagen (WCAG 1.1.1)")
    void debeExigirTextoAlternativo() {
        ContenidoVersion borrador = new ContenidoVersion(leccion, 3, EstadoContenido.BORRADOR, "audio3.wav", "Texto", null);
        borrador.setId(103L);
        when(versionRepository.findById(borrador.getId())).thenReturn(Optional.of(borrador));

        assertThrows(ReglaNegocioException.class, () ->
                contenidoService.agregarImagen(borrador.getId(), "gestor@ecoaula.local", "tabla.png", png(), "  ", null));
        assertThrows(ReglaNegocioException.class, () ->
                contenidoService.agregarImagen(borrador.getId(), "gestor@ecoaula.local", "tabla.png", png(),
                        "x".repeat(RecursoImagen.MAX_TEXTO_ALTERNATIVO + 1), null));
        verify(almacenamientoContenido, never()).guardarArchivo(any(), any());
    }

    @Test
    @DisplayName("Debe rechazar archivos que no son imágenes aunque tengan extensión de imagen")
    void debeRechazarImagenInvalida() {
        ContenidoVersion borrador = new ContenidoVersion(leccion, 3, EstadoContenido.BORRADOR, "audio3.wav", "Texto", null);
        borrador.setId(103L);
        when(versionRepository.findById(borrador.getId())).thenReturn(Optional.of(borrador));

        assertThrows(ReglaNegocioException.class, () ->
                contenidoService.agregarImagen(borrador.getId(), "gestor@ecoaula.local", "tabla.png",
                        "no es imagen".getBytes(), "Tabla de presupuesto", null));
    }

    @Test
    @DisplayName("Debe agregar la imagen al final de un borrador con su texto alternativo")
    void debeAgregarImagenValida() {
        ContenidoVersion rechazada = new ContenidoVersion(leccion, 3, EstadoContenido.BORRADOR, "audio3.wav", "Texto", null);
        rechazada.setId(103L);
        when(versionRepository.findById(rechazada.getId())).thenReturn(Optional.of(rechazada));
        when(almacenamientoContenido.guardarArchivo(startsWith("imagen-20-v3-"), any())).thenAnswer(inv -> inv.getArgument(0));
        when(imagenRepository.findTopByContenidoVersionIdOrderByOrdenDesc(rechazada.getId()))
                .thenReturn(Optional.of(new RecursoImagen(rechazada, "otra.png", "Otra", null, 2)));
        when(imagenRepository.save(any(RecursoImagen.class))).thenAnswer(inv -> inv.getArgument(0));

        RecursoImagen imagen = contenidoService.agregarImagen(rechazada.getId(), "gestor@ecoaula.local", "Tabla.PNG", png(),
                " Tabla de presupuesto mensual ", "  ");

        assertEquals("Tabla de presupuesto mensual", imagen.getTextoAlternativo());
        assertNull(imagen.getDescripcionDetallada());
        assertEquals(3, imagen.getOrden());
        assertTrue(imagen.getRutaArchivo().endsWith(".png"));
    }

    @Test
    @DisplayName("El aprendiz no accede a audios de cursos en los que no está matriculado")
    void aprendizNoAccedeAAudioAjeno() {
        when(versionRepository.aprendizPuedeEscuchar(eq("audio2.wav"), eq(aprendiz.getCorreo()), anyCollection())).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () ->
                contenidoService.verificarAccesoAudio("audio2.wav", aprendiz.getCorreo(), false));
    }

    @Test
    @DisplayName("Una versión que no está en BORRADOR no admite cambios en sus imágenes (ADR-08)")
    void imagenesSoloEnBorrador() {
        when(versionRepository.findById(v2.getId())).thenReturn(Optional.of(v2));

        assertThrows(ReglaNegocioException.class, () ->
                contenidoService.agregarImagen(v2.getId(), "gestor@ecoaula.local", "tabla.png", png(), "Tabla", null));
        verify(almacenamientoContenido, never()).guardarArchivo(any(), any());
    }

    private static byte[] png() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    }
}
