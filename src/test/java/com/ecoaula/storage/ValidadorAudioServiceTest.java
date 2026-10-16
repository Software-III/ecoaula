package com.ecoaula.storage;

import com.ecoaula.exception.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidadorAudioServiceTest {

    @Mock
    private AlmacenamientoContenido almacenamientoContenido;

    private ValidadorAudioService validadorAudioService;

    private static final long MAX_BYTES = 10 * 1024 * 1024; // 10 MB

    @BeforeEach
    void setUp() {
        validadorAudioService = new ValidadorAudioService(almacenamientoContenido, MAX_BYTES);
    }

    @Test
    @DisplayName("Debe rechazar archivo nulo o vacío")
    void debeRechazarArchivoVacio() {
        MockMultipartFile vacio = new MockMultipartFile("audio", "audio.wav", "audio/wav", new byte[0]);

        assertThrows(ReglaNegocioException.class, () -> validadorAudioService.validarYGuardar(null));
        assertThrows(ReglaNegocioException.class, () -> validadorAudioService.validarYGuardar(vacio));
    }

    @Test
    @DisplayName("Debe rechazar archivos que superen el tamaño máximo configurable")
    void debeRechazarArchivoQueSuperaTamanoMaximo() {
        byte[] datosGrandes = new byte[(int) (MAX_BYTES + 1024)];
        MockMultipartFile archivoGrande = new MockMultipartFile("audio", "audio_pesado.wav", "audio/wav", datosGrandes);

        ReglaNegocioException ex = assertThrows(ReglaNegocioException.class,
                () -> validadorAudioService.validarYGuardar(archivoGrande));

        assertTrue(ex.getMessage().contains("supera el tamaño máximo"));
    }

    @Test
    @DisplayName("Debe rechazar formatos no permitidos (.exe, .pdf, .txt)")
    void debeRechazarFormatosNoPermitidos() {
        MockMultipartFile ejecutable = new MockMultipartFile("audio", "malware.exe", "application/octet-stream", "dummy".getBytes());
        MockMultipartFile pdf = new MockMultipartFile("audio", "documento.pdf", "application/pdf", "dummy".getBytes());

        assertThrows(ReglaNegocioException.class, () -> validadorAudioService.validarYGuardar(ejecutable));
        assertThrows(ReglaNegocioException.class, () -> validadorAudioService.validarYGuardar(pdf));
    }

    @Test
    @DisplayName("Debe sanear el nombre del archivo y guardarlo correctamente")
    void debeSanearNombreYGuardarAudioValido() {
        byte[] contenidoWav = "RIFF....WAVEfmt ".getBytes();
        MockMultipartFile archivoOriginal = new MockMultipartFile("audio", "../../mi audio peligroso.wav", "audio/wav", contenidoWav);

        when(almacenamientoContenido.guardarAudio(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        String nombreGuardado = validadorAudioService.validarYGuardar(archivoOriginal);

        assertNotNull(nombreGuardado);
        assertTrue(nombreGuardado.startsWith("audio_"));
        assertTrue(nombreGuardado.endsWith(".wav"));
        assertFalse(nombreGuardado.contains(".."));
        assertFalse(nombreGuardado.contains("peligroso"));

        verify(almacenamientoContenido).guardarAudio(argThat(nombre -> nombre.startsWith("audio_") && nombre.endsWith(".wav")), eq(contenidoWav));
    }

    @Test
    @DisplayName("Debe aceptar formatos MP3 y OGG válidos")
    void debeAceptarFormatosMp3YOgg() {
        MockMultipartFile mp3 = new MockMultipartFile("audio", "leccion.mp3", "audio/mpeg", "mp3data".getBytes());
        MockMultipartFile ogg = new MockMultipartFile("audio", "leccion.ogg", "audio/ogg", "oggdata".getBytes());

        when(almacenamientoContenido.guardarAudio(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        String resMp3 = validadorAudioService.validarYGuardar(mp3);
        String resOgg = validadorAudioService.validarYGuardar(ogg);

        assertTrue(resMp3.endsWith(".mp3"));
        assertTrue(resOgg.endsWith(".ogg"));
    }
}
