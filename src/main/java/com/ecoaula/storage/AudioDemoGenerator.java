package com.ecoaula.storage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Generador programático de archivos de audio WAV sintéticos para lecciones de demostración.
 * Permite que el elemento nativo <audio controls> funcione de inmediato sin dependencias externas.
 */
public class AudioDemoGenerator {

    /**
     * Genera un archivo WAV válido en memoria con un tono suave y atenuado (fade in / fade out).
     *
     * @param duracionSegundos Duración total del audio.
     * @param frecuenciaHz     Frecuencia base en Hz (ej. 440Hz tono La).
     * @return Array de bytes con la estructura completa de un archivo WAV PCM 16-bit mono.
     */
    public static byte[] generarTonoSuaveWav(int duracionSegundos, double frecuenciaHz) {
        int sampleRate = 16000;
        int numSamples = duracionSegundos * sampleRate;
        short[] samples = new short[numSamples];

        for (int i = 0; i < numSamples; i++) {
            double tiempo = (double) i / sampleRate;
            // Modulación de amplitud suave
            double volumen = 0.3;
            // Suavizado inicial y final (fade)
            if (i < sampleRate) {
                volumen *= ((double) i / sampleRate);
            } else if (i > numSamples - sampleRate) {
                volumen *= ((double) (numSamples - i) / sampleRate);
            }
            double valorSeno = Math.sin(2.0 * Math.PI * frecuenciaHz * tiempo);
            samples[i] = (short) (valorSeno * 32767 * volumen);
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            int subChunk2Size = numSamples * 2; // 16 bits = 2 bytes por muestra
            int chunkSize = 36 + subChunk2Size;

            // 1. Cabecera RIFF
            dos.writeBytes("RIFF");
            dos.writeInt(Integer.reverseBytes(chunkSize));
            dos.writeBytes("WAVE");

            // 2. Sub-bloque 'fmt '
            dos.writeBytes("fmt ");
            dos.writeInt(Integer.reverseBytes(16)); // SubChunk1Size para PCM
            dos.writeShort(Short.reverseBytes((short) 1)); // Formato PCM = 1
            dos.writeShort(Short.reverseBytes((short) 1)); // Canales = 1 (Mono)
            dos.writeInt(Integer.reverseBytes(sampleRate));
            dos.writeInt(Integer.reverseBytes(sampleRate * 2)); // ByteRate
            dos.writeShort(Short.reverseBytes((short) 2)); // BlockAlign
            dos.writeShort(Short.reverseBytes((short) 16)); // BitsPerSample

            // 3. Sub-bloque 'data'
            dos.writeBytes("data");
            dos.writeInt(Integer.reverseBytes(subChunk2Size));
            for (short sample : samples) {
                dos.writeShort(Short.reverseBytes(sample));
            }

            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Error al generar audio sintético de demostración", e);
        }
    }
}
