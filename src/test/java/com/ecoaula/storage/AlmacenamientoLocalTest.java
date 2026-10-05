package com.ecoaula.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AlmacenamientoLocalTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Debe generar audio WAV válido y almacenarlo en disco")
    void debeGenerarYAlmacenarAudio() {
        AlmacenamientoLocal almacenamiento = new AlmacenamientoLocal(tempDir.toString());

        byte[] audioBytes = AudioDemoGenerator.generarTonoSuaveWav(2, 440.0);
        assertNotNull(audioBytes);
        assertTrue(audioBytes.length > 44, "El archivo WAV debe contener la cabecera y datos PCM");

        // Validar cabecera RIFF
        assertEquals('R', (char) audioBytes[0]);
        assertEquals('I', (char) audioBytes[1]);
        assertEquals('F', (char) audioBytes[2]);
        assertEquals('F', (char) audioBytes[3]);

        String guardado = almacenamiento.guardarAudio("prueba.wav", audioBytes);
        assertEquals("prueba.wav", guardado);
        assertTrue(almacenamiento.existe("prueba.wav"));

        byte[] recuperado = almacenamiento.obtenerAudio("prueba.wav");
        assertArrayEquals(audioBytes, recuperado);
    }
}
