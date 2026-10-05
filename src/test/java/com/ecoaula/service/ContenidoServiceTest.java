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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
}
