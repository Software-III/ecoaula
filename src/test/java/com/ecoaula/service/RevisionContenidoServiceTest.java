package com.ecoaula.service;

import com.ecoaula.exception.AccesoDenegadoException;
import com.ecoaula.exception.ReglaNegocioException;
import com.ecoaula.model.Aprendiz;
import com.ecoaula.model.Contenido;
import com.ecoaula.model.ContenidoVersion;
import com.ecoaula.model.Curso;
import com.ecoaula.model.RevisionAccesibilidad;
import com.ecoaula.model.enums.CriterioAccesibilidad;
import com.ecoaula.model.enums.EstadoContenido;
import com.ecoaula.model.enums.ResultadoRevision;
import com.ecoaula.model.enums.Rol;
import com.ecoaula.repository.AprendizRepository;
import com.ecoaula.repository.ContenidoVersionRepository;
import com.ecoaula.repository.RevisionAccesibilidadRepository;
import com.ecoaula.service.dto.IndicadoresCalidad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevisionContenidoServiceTest {

    @Mock
    private ContenidoVersionRepository versionRepository;

    @Mock
    private RevisionAccesibilidadRepository revisionRepository;

    @Mock
    private AprendizRepository aprendizRepository;

    @Mock
    private com.ecoaula.repository.ContenidoRepository contenidoRepository;

    @Mock
    private com.ecoaula.repository.MatriculaRepository matriculaRepository;

    @Mock
    private com.ecoaula.notification.NotificacionService notificacionService;

    private RevisionContenidoService revisionService;

    private Aprendiz gestorAutor;
    private Aprendiz revisor;
    private Contenido leccion;
    private ContenidoVersion versionEnRevision;
    private Set<String> checklistCompleto;

    @BeforeEach
    void setUp() {
        revisionService = new RevisionContenidoService(
                versionRepository,
                revisionRepository,
                aprendizRepository,
                contenidoRepository,
                matriculaRepository,
                notificacionService
        );

        gestorAutor = new Aprendiz();
        gestorAutor.setId(10L);
        gestorAutor.setCorreo("gestor@ecoaula.local");
        gestorAutor.setRol(Rol.GESTOR_CONTENIDO);

        revisor = new Aprendiz();
        revisor.setId(20L);
        revisor.setCorreo("revisor@ecoaula.local");
        revisor.setRol(Rol.REVISOR_ACCESIBILIDAD);

        Curso curso = new Curso("Curso Demo", "Desc", 10, "Area", "Virtual");
        leccion = new Contenido("Lección Accesible", 1, curso);
        leccion.setId(100L);

        versionEnRevision = new ContenidoVersion(leccion, 2, gestorAutor);
        versionEnRevision.setId(200L);
        versionEnRevision.setEstado(EstadoContenido.EN_REVISION);
        versionEnRevision.setRutaAudio("audio_demo.wav");
        versionEnRevision.setTranscripcion("Transcripción completa");
        versionEnRevision.setCreadoEn(LocalDateTime.now().minusHours(3));
        versionEnRevision.setEnviadaARevisionEn(LocalDateTime.now().minusHours(2));

        checklistCompleto = Arrays.stream(CriterioAccesibilidad.values())
                .map(CriterioAccesibilidad::getCodigo)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("Debe aprobar y publicar la versión, retirando la versión previamente publicada (ADR-08)")
    void debeAprobarYPublicarRetirandoAnterior() {
        ContenidoVersion versionAnteriorPublicada = new ContenidoVersion(leccion, 1, gestorAutor);
        versionAnteriorPublicada.setId(199L);
        versionAnteriorPublicada.setEstado(EstadoContenido.PUBLICADA);

        when(aprendizRepository.findByCorreo("revisor@ecoaula.local")).thenReturn(Optional.of(revisor));
        when(versionRepository.findById(200L)).thenReturn(Optional.of(versionEnRevision));
        when(versionRepository.findByContenidoIdAndEstado(100L, EstadoContenido.PUBLICADA))
                .thenReturn(Optional.of(versionAnteriorPublicada));
        when(versionRepository.save(any(ContenidoVersion.class))).thenAnswer(i -> i.getArgument(0));
        when(revisionRepository.save(any(RevisionAccesibilidad.class))).thenAnswer(i -> i.getArgument(0));

        RevisionAccesibilidad resultado = revisionService.aprobarYPublicar(200L, "revisor@ecoaula.local", checklistCompleto);

        assertNotNull(resultado);
        assertEquals(ResultadoRevision.APROBADA, resultado.getResultado());
        assertEquals(EstadoContenido.PUBLICADA, versionEnRevision.getEstado());
        assertNotNull(versionEnRevision.getPublicadaEn());
        assertEquals(EstadoContenido.RETIRADA, versionAnteriorPublicada.getEstado());

        verify(versionRepository).save(versionAnteriorPublicada);
        verify(versionRepository).save(versionEnRevision);
        verify(revisionRepository).save(resultado);
    }

    @Test
    @DisplayName("No se puede aprobar si falta algún criterio de la lista de chequeo de accesibilidad")
    void noSePuedeAprobarConChecklistIncompleto() {
        Set<String> checklistIncompleto = new HashSet<>(checklistCompleto);
        checklistIncompleto.remove(CriterioAccesibilidad.TRANSCRIPCION_FIEL.getCodigo());

        when(aprendizRepository.findByCorreo("revisor@ecoaula.local")).thenReturn(Optional.of(revisor));
        when(versionRepository.findById(200L)).thenReturn(Optional.of(versionEnRevision));

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () ->
                revisionService.aprobarYPublicar(200L, "revisor@ecoaula.local", checklistIncompleto));

        assertTrue(ex.getMessage().contains("todos los criterios"));
        verify(versionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un usuario no puede revisar una versión que él mismo creó (separación de funciones)")
    void autorNoPuedeRevisarSuPropiaVersion() {
        // Revisor que figura como autor de la versión
        versionEnRevision.setCreadoPor(revisor);

        when(aprendizRepository.findByCorreo("revisor@ecoaula.local")).thenReturn(Optional.of(revisor));
        when(versionRepository.findById(200L)).thenReturn(Optional.of(versionEnRevision));

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, () ->
                revisionService.aprobarYPublicar(200L, "revisor@ecoaula.local", checklistCompleto));

        assertTrue(ex.getMessage().contains("Un usuario no puede revisar una versión que él mismo creó"));
    }

    @Test
    @DisplayName("Un gestor de contenido no puede aprobar ni rechazar revisiones")
    void gestorNoPuedeAprobar() {
        when(aprendizRepository.findByCorreo("gestor@ecoaula.local")).thenReturn(Optional.of(gestorAutor));

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, () ->
                revisionService.aprobarYPublicar(205L, "gestor@ecoaula.local", checklistCompleto));

        assertTrue(ex.getMessage().contains("Separación de funciones"));
    }

    @Test
    @DisplayName("El rechazo sin motivo obligatorio debe fallar")
    void rechazoSinMotivoFalla() {
        when(aprendizRepository.findByCorreo("revisor@ecoaula.local")).thenReturn(Optional.of(revisor));
        when(versionRepository.findById(200L)).thenReturn(Optional.of(versionEnRevision));

        assertThrows(ReglaNegocioException.class, () ->
                revisionService.rechazar(200L, "revisor@ecoaula.local", null, Collections.emptySet()));
        assertThrows(ReglaNegocioException.class, () ->
                revisionService.rechazar(200L, "revisor@ecoaula.local", "    ", Collections.emptySet()));
    }

    @Test
    @DisplayName("Debe rechazar la versión guardando el motivo de no conformidad")
    void debeRechazarVersionConMotivo() {
        when(aprendizRepository.findByCorreo("revisor@ecoaula.local")).thenReturn(Optional.of(revisor));
        when(versionRepository.findById(200L)).thenReturn(Optional.of(versionEnRevision));
        when(versionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(revisionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RevisionAccesibilidad resultado = revisionService.rechazar(
                200L,
                "revisor@ecoaula.local",
                "El audio contiene ruido y faltan párrafos en la transcripción",
                Collections.emptySet()
        );

        assertNotNull(resultado);
        assertEquals(ResultadoRevision.RECHAZADA, resultado.getResultado());
        assertEquals("El audio contiene ruido y faltan párrafos en la transcripción", resultado.getMotivo());
        assertEquals(EstadoContenido.RECHAZADA, versionEnRevision.getEstado());
        assertEquals("El audio contiene ruido y faltan párrafos en la transcripción", versionEnRevision.getMotivoRechazo());

        verify(versionRepository).save(versionEnRevision);
        verify(revisionRepository).save(resultado);
    }

    @Test
    @DisplayName("Debe calcular los indicadores del plan de calidad de accesibilidad correctamente")
    void debeCalcularIndicadoresDeCalidad() {
        // Configurar revisiones históricas:
        // Version 1: 1a revision APROBADA
        // Version 2: 1a revision RECHAZADA, 2a revision APROBADA
        ContenidoVersion v1 = new ContenidoVersion(leccion, 1, gestorAutor);
        v1.setId(1L);
        v1.setCreadoEn(LocalDateTime.now().minusHours(10));
        v1.setPublicadaEn(LocalDateTime.now().minusHours(8)); // demoró 2 horas
        v1.setEstado(EstadoContenido.PUBLICADA);

        ContenidoVersion v2 = new ContenidoVersion(leccion, 2, gestorAutor);
        v2.setId(2L);
        v2.setCreadoEn(LocalDateTime.now().minusHours(6));
        v2.setPublicadaEn(LocalDateTime.now().minusHours(2)); // demoró 4 horas
        v2.setEstado(EstadoContenido.PUBLICADA);

        RevisionAccesibilidad rev1_v1 = new RevisionAccesibilidad(v1, revisor, ResultadoRevision.APROBADA, null, checklistCompleto);
        rev1_v1.setFecha(LocalDateTime.now().minusHours(8));

        RevisionAccesibilidad rev1_v2 = new RevisionAccesibilidad(v2, revisor, ResultadoRevision.RECHAZADA, "Falla", Collections.emptySet());
        rev1_v2.setFecha(LocalDateTime.now().minusHours(5));

        RevisionAccesibilidad rev2_v2 = new RevisionAccesibilidad(v2, revisor, ResultadoRevision.APROBADA, null, checklistCompleto);
        rev2_v2.setFecha(LocalDateTime.now().minusHours(2));

        when(revisionRepository.findAllByOrderByFechaDesc()).thenReturn(Arrays.asList(rev2_v2, rev1_v2, rev1_v1));
        when(versionRepository.findByEstado(EstadoContenido.PUBLICADA)).thenReturn(Arrays.asList(v1, v2));
        when(versionRepository.findByEstado(EstadoContenido.RETIRADA)).thenReturn(Collections.emptyList());

        IndicadoresCalidad ind = revisionService.calcularIndicadores();

        assertEquals(2, ind.getTotalVersionesRevisadas());
        assertEquals(1, ind.getAprobadasPrimeraRevision());
        assertEquals(50.0, ind.getTasaAccesibilidadPrimeraRevision());
        assertEquals(3.0, ind.getTiempoPromedioPublicacionHoras(), 0.1); // (2h + 4h)/2 = 3h
    }

    @Test
    @DisplayName("Una subsanación es el mismo contenido revisado otra vez: no cuenta como aprobada en primera revisión")
    void subsanacionNoCuentaComoPrimeraRevision() {
        ContenidoVersion v1 = new ContenidoVersion(leccion, 1, gestorAutor);
        v1.setId(1L);
        v1.setEstado(EstadoContenido.RECHAZADA);

        ContenidoVersion v2 = new ContenidoVersion(leccion, 2, gestorAutor);
        v2.setId(2L);
        v2.setVersionOrigen(v1);
        v2.setEstado(EstadoContenido.PUBLICADA);

        RevisionAccesibilidad rechazoV1 = new RevisionAccesibilidad(v1, revisor, ResultadoRevision.RECHAZADA, "Falla", Collections.emptySet());
        RevisionAccesibilidad aprobacionV2 = new RevisionAccesibilidad(v2, revisor, ResultadoRevision.APROBADA, null, checklistCompleto);
        when(revisionRepository.findAllByOrderByFechaDesc()).thenReturn(Arrays.asList(aprobacionV2, rechazoV1));
        when(versionRepository.findByEstado(EstadoContenido.PUBLICADA)).thenReturn(Collections.emptyList());
        when(versionRepository.findByEstado(EstadoContenido.RETIRADA)).thenReturn(Collections.emptyList());

        IndicadoresCalidad ind = revisionService.calcularIndicadores();

        assertEquals(1, ind.getTotalVersionesRevisadas());
        assertEquals(0, ind.getAprobadasPrimeraRevision());
        assertEquals(0.0, ind.getTasaAccesibilidadPrimeraRevision());
    }
}
