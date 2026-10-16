package com.ecoaula.service;

import com.ecoaula.exception.AccesoDenegadoException;
import com.ecoaula.exception.ReglaNegocioException;
import com.ecoaula.model.Aprendiz;
import com.ecoaula.model.Contenido;
import com.ecoaula.model.ContenidoVersion;
import com.ecoaula.model.Curso;
import com.ecoaula.model.enums.EstadoContenido;
import com.ecoaula.model.enums.Rol;
import com.ecoaula.repository.AprendizRepository;
import com.ecoaula.repository.ContenidoRepository;
import com.ecoaula.repository.ContenidoVersionRepository;
import com.ecoaula.repository.CursoRepository;
import com.ecoaula.storage.ValidadorAudioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestionContenidoServiceTest {

    @Mock
    private CursoRepository cursoRepository;

    @Mock
    private ContenidoRepository contenidoRepository;

    @Mock
    private ContenidoVersionRepository versionRepository;

    @Mock
    private AprendizRepository aprendizRepository;

    @Mock
    private ValidadorAudioService validadorAudioService;

    private GestionContenidoService gestionService;

    private Aprendiz gestor;
    private Aprendiz revisor;
    private Curso curso;
    private Contenido leccion;

    @BeforeEach
    void setUp() {
        gestionService = new GestionContenidoService(
                cursoRepository,
                contenidoRepository,
                versionRepository,
                aprendizRepository,
                validadorAudioService
        );

        gestor = new Aprendiz();
        gestor.setId(10L);
        gestor.setCorreo("gestor@ecoaula.local");
        gestor.setRol(Rol.GESTOR_CONTENIDO);

        revisor = new Aprendiz();
        revisor.setId(20L);
        revisor.setCorreo("revisor@ecoaula.local");
        revisor.setRol(Rol.REVISOR_ACCESIBILIDAD);

        curso = new Curso("Curso Accesible", "Desc", 10, "Tecnologia", "Virtual");
        curso.setId(1L);

        leccion = new Contenido("Lección 1", 1, curso);
        leccion.setId(100L);
    }

    @Test
    @DisplayName("Debe crear una nueva lección con su versión 1 en estado BORRADOR")
    void debeCrearLeccionConVersion1EnBorrador() {
        when(aprendizRepository.findByCorreo("gestor@ecoaula.local")).thenReturn(Optional.of(gestor));
        when(cursoRepository.findById(1L)).thenReturn(Optional.of(curso));
        when(contenidoRepository.save(any(Contenido.class))).thenAnswer(i -> {
            Contenido c = i.getArgument(0);
            c.setId(100L);
            return c;
        });
        when(versionRepository.save(any(ContenidoVersion.class))).thenAnswer(i -> i.getArgument(0));

        Contenido resultado = gestionService.agregarLeccion(1L, "Introducción al teclado", 1, "gestor@ecoaula.local");

        assertNotNull(resultado);
        assertEquals("Introducción al teclado", resultado.getTitulo());
        verify(versionRepository).save(argThat(v ->
                v.getNumeroVersion() == 1 &&
                v.getEstado() == EstadoContenido.BORRADOR &&
                v.getCreadoPor().equals(gestor)
        ));
    }

    @Test
    @DisplayName("Un revisor de accesibilidad no puede crear contenidos (separación de funciones)")
    void revisorNoPuedeCrearLecciones() {
        when(aprendizRepository.findByCorreo("revisor@ecoaula.local")).thenReturn(Optional.of(revisor));

        assertThrows(AccesoDenegadoException.class, () ->
                gestionService.agregarLeccion(1L, "Título", 1, "revisor@ecoaula.local"));
    }

    @Test
    @DisplayName("Debe actualizar la transcripción y el audio de una versión en BORRADOR")
    void debeActualizarBorradorExitosamente() {
        ContenidoVersion borrador = new ContenidoVersion(leccion, 1, gestor);
        borrador.setId(500L);
        borrador.setEstado(EstadoContenido.BORRADOR);

        when(aprendizRepository.findByCorreo("gestor@ecoaula.local")).thenReturn(Optional.of(gestor));
        when(versionRepository.findById(500L)).thenReturn(Optional.of(borrador));
        when(validadorAudioService.validarYGuardar(any())).thenReturn("audio_saneado.wav");
        when(versionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MockMultipartFile audio = new MockMultipartFile("audio", "audio.wav", "audio/wav", "dummy".getBytes());

        ContenidoVersion actualizada = gestionService.actualizarBorrador(500L, "Nueva transcripción", audio, "gestor@ecoaula.local");

        assertEquals("Nueva transcripción", actualizada.getTranscripcion());
        assertEquals("audio_saneado.wav", actualizada.getRutaAudio());
        verify(versionRepository).save(actualizada);
    }

    @Test
    @DisplayName("No se puede editar una versión PUBLICADA ni RETIRADA (ADR-08 inmutabilidad)")
    void noSePuedeEditarVersionPublicadaORetirada() {
        ContenidoVersion publicada = new ContenidoVersion(leccion, 1, gestor);
        publicada.setId(501L);
        publicada.setEstado(EstadoContenido.PUBLICADA);

        when(aprendizRepository.findByCorreo("gestor@ecoaula.local")).thenReturn(Optional.of(gestor));
        when(versionRepository.findById(501L)).thenReturn(Optional.of(publicada));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                gestionService.actualizarBorrador(501L, "Texto", null, "gestor@ecoaula.local"));

        assertTrue(ex.getMessage().contains("solo lectura"));
    }

    @Test
    @DisplayName("No se puede enviar a revisión si falta el archivo de audio")
    void noSePuedeEnviarARevisionSinAudio() {
        ContenidoVersion borrador = new ContenidoVersion(leccion, 1, gestor);
        borrador.setId(502L);
        borrador.setEstado(EstadoContenido.BORRADOR);
        borrador.setTranscripcion("Transcripción completa");
        borrador.setRutaAudio(null);

        when(aprendizRepository.findByCorreo("gestor@ecoaula.local")).thenReturn(Optional.of(gestor));
        when(versionRepository.findById(502L)).thenReturn(Optional.of(borrador));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                gestionService.enviarARevision(502L, "gestor@ecoaula.local"));

        assertTrue(ex.getMessage().contains("debe contar con un archivo de audio"));
    }

    @Test
    @DisplayName("No se puede enviar a revisión si falta la transcripción completa")
    void noSePuedeEnviarARevisionSinTranscripcion() {
        ContenidoVersion borrador = new ContenidoVersion(leccion, 1, gestor);
        borrador.setId(503L);
        borrador.setEstado(EstadoContenido.BORRADOR);
        borrador.setRutaAudio("audio.wav");
        borrador.setTranscripcion("   ");

        when(aprendizRepository.findByCorreo("gestor@ecoaula.local")).thenReturn(Optional.of(gestor));
        when(versionRepository.findById(503L)).thenReturn(Optional.of(borrador));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                gestionService.enviarARevision(503L, "gestor@ecoaula.local"));

        assertTrue(ex.getMessage().contains("debe contar con la transcripción completa"));
    }

    @Test
    @DisplayName("Debe enviar a revisión exitosamente cuando cumple audio y transcripción")
    void debeEnviarARevisionExitosamente() {
        ContenidoVersion borrador = new ContenidoVersion(leccion, 1, gestor);
        borrador.setId(504L);
        borrador.setEstado(EstadoContenido.BORRADOR);
        borrador.setRutaAudio("audio.wav");
        borrador.setTranscripcion("Texto transcrito fielmente");

        when(aprendizRepository.findByCorreo("gestor@ecoaula.local")).thenReturn(Optional.of(gestor));
        when(versionRepository.findById(504L)).thenReturn(Optional.of(borrador));
        when(versionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ContenidoVersion enRevision = gestionService.enviarARevision(504L, "gestor@ecoaula.local");

        assertEquals(EstadoContenido.EN_REVISION, enRevision.getEstado());
        assertNotNull(enRevision.getEnviadaARevisionEn());
        verify(versionRepository).save(enRevision);
    }

    @Test
    @DisplayName("Debe crear un nuevo borrador copiando una versión rechazada para subsanación")
    void debeCrearNuevoBorradorDesdeRechazada() {
        ContenidoVersion rechazada = new ContenidoVersion(leccion, 1, gestor);
        rechazada.setId(505L);
        rechazada.setEstado(EstadoContenido.RECHAZADA);
        rechazada.setRutaAudio("audio_antiguo.wav");
        rechazada.setTranscripcion("Texto con fallas");
        rechazada.setMotivoRechazo("Faltan descripciones no visuales");

        when(aprendizRepository.findByCorreo("gestor@ecoaula.local")).thenReturn(Optional.of(gestor));
        when(versionRepository.findById(505L)).thenReturn(Optional.of(rechazada));
        when(versionRepository.findTopByContenidoIdOrderByNumeroVersionDesc(100L)).thenReturn(Optional.of(rechazada));
        when(versionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ContenidoVersion subsanacion = gestionService.crearVersionDesdeRechazada(505L, "gestor@ecoaula.local");

        assertEquals(2, subsanacion.getNumeroVersion());
        assertEquals(EstadoContenido.BORRADOR, subsanacion.getEstado());
        assertEquals(rechazada.getTranscripcion(), subsanacion.getTranscripcion());
        assertEquals(rechazada.getRutaAudio(), subsanacion.getRutaAudio());
        assertEquals(rechazada, subsanacion.getVersionOrigen());
        verify(versionRepository).save(subsanacion);
    }
}
