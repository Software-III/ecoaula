package com.ecoaula.service;

import com.ecoaula.model.Aprendiz;
import com.ecoaula.model.AvanceLeccion;
import com.ecoaula.model.Contenido;
import com.ecoaula.model.ContenidoVersion;
import com.ecoaula.model.Curso;
import com.ecoaula.model.enums.EstadoContenido;
import com.ecoaula.repository.AprendizRepository;
import com.ecoaula.repository.AvanceLeccionRepository;
import com.ecoaula.repository.ContenidoRepository;
import com.ecoaula.repository.ContenidoVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VersionamientoInmutableAprendizTest {

    @Mock
    private ContenidoRepository contenidoRepository;

    @Mock
    private ContenidoVersionRepository versionRepository;

    @Mock
    private AvanceLeccionRepository avanceRepository;

    @Mock
    private AprendizRepository aprendizRepository;

    private ContenidoService contenidoService;

    private Contenido leccion;
    private ContenidoVersion v1Retirada;
    private ContenidoVersion v2Publicada;
    private Aprendiz aprendizAntiguo;
    private Aprendiz aprendizNuevo;

    @BeforeEach
    void setUp() {
        contenidoService = new ContenidoService(
                contenidoRepository,
                versionRepository,
                avanceRepository,
                aprendizRepository
        );

        Curso curso = new Curso("Curso Demo", "Desc", 10, "Area", "Virtual");
        leccion = new Contenido("Lección 1", 1, curso);
        leccion.setId(10L);

        // v1 fue completada anteriormente y luego pasó a RETIRADA tras publicarse v2
        v1Retirada = new ContenidoVersion(leccion, 1, null);
        v1Retirada.setId(101L);
        v1Retirada.setEstado(EstadoContenido.RETIRADA);
        v1Retirada.setRutaAudio("audio_v1.wav");
        v1Retirada.setTranscripcion("Transcripción Versión 1");

        // v2 es la versión actualmente vigente PUBLICADA
        v2Publicada = new ContenidoVersion(leccion, 2, null);
        v2Publicada.setId(102L);
        v2Publicada.setEstado(EstadoContenido.PUBLICADA);
        v2Publicada.setRutaAudio("audio_v2.wav");
        v2Publicada.setTranscripcion("Transcripción Versión 2 Mejorada");

        aprendizAntiguo = new Aprendiz();
        aprendizAntiguo.setId(1L);
        aprendizAntiguo.setCorreo("antiguo@ecoaula.local");

        aprendizNuevo = new Aprendiz();
        aprendizNuevo.setId(2L);
        aprendizNuevo.setCorreo("nuevo@ecoaula.local");
    }

    @Test
    @DisplayName("El aprendiz que ya completó una lección conserva la versión v1 que cursó aunque exista v2 publicada (ADR-08)")
    void aprendizQueYaCompletoConservaVersionOriginal() {
        AvanceLeccion avanceV1 = new AvanceLeccion(aprendizAntiguo, v1Retirada);

        when(aprendizRepository.findByCorreo("antiguo@ecoaula.local")).thenReturn(Optional.of(aprendizAntiguo));
        when(avanceRepository.findByAprendizIdAndContenidoId(1L, 10L)).thenReturn(Optional.of(avanceV1));

        ContenidoVersion versionObtenida = contenidoService.obtenerVersionParaAprendiz(10L, "antiguo@ecoaula.local");

        assertNotNull(versionObtenida);
        assertEquals(101L, versionObtenida.getId());
        assertEquals(1, versionObtenida.getNumeroVersion());
        assertEquals(EstadoContenido.RETIRADA, versionObtenida.getEstado());
        assertEquals("Transcripción Versión 1", versionObtenida.getTranscripcion());

        // No debe consultar la versión publicada si ya tenía avance
        verify(versionRepository, never()).findByContenidoIdAndEstado(any(), any());
    }

    @Test
    @DisplayName("Un aprendiz nuevo que aún no ha iniciado la lección recibe la versión v2 vigente PUBLICADA")
    void aprendizNuevoRecibeVersionVigentePublicada() {
        when(aprendizRepository.findByCorreo("nuevo@ecoaula.local")).thenReturn(Optional.of(aprendizNuevo));
        when(avanceRepository.findByAprendizIdAndContenidoId(2L, 10L)).thenReturn(Optional.empty());
        when(versionRepository.findByContenidoIdAndEstado(10L, EstadoContenido.PUBLICADA))
                .thenReturn(Optional.of(v2Publicada));

        ContenidoVersion versionObtenida = contenidoService.obtenerVersionParaAprendiz(10L, "nuevo@ecoaula.local");

        assertNotNull(versionObtenida);
        assertEquals(102L, versionObtenida.getId());
        assertEquals(2, versionObtenida.getNumeroVersion());
        assertEquals(EstadoContenido.PUBLICADA, versionObtenida.getEstado());
        assertEquals("Transcripción Versión 2 Mejorada", versionObtenida.getTranscripcion());
    }
}
