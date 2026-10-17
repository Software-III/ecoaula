package com.ecoaula.storage;

import com.ecoaula.exception.ReglaNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Servicio encargado de la validación estricta, saneamiento y persistencia
 * de archivos de audio subidos por los gestores de contenido.
 */
@Service
public class ValidadorAudioService {

    private static final List<String> EXTENSIONES_PERMITIDAS = Arrays.asList("mp3", "wav", "ogg");
    private static final List<String> TIPOS_MIME_PERMITIDOS = Arrays.asList(
            "audio/mpeg",
            "audio/mp3",
            "audio/wav",
            "audio/x-wav",
            "audio/wave",
            "audio/ogg",
            "audio/vorbis",
            "application/ogg"
    );

    private final AlmacenamientoContenido almacenamientoContenido;
    private final long maxSizeBytes;

    public ValidadorAudioService(AlmacenamientoContenido almacenamientoContenido,
                                 @Value("${ecoaula.audio.max-size-bytes:15728640}") long maxSizeBytes) {
        this.almacenamientoContenido = almacenamientoContenido;
        this.maxSizeBytes = maxSizeBytes;
    }

    /**
     * Valida el archivo de audio y lo almacena con un nombre saneado y único.
     *
     * @param archivo Archivo multipart subido
     * @return Nombre saneado del archivo almacenado
     */
    public String validarYGuardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ReglaNegocioException("Debe seleccionar un archivo de audio para la lección.");
        }

        if (archivo.getSize() > maxSizeBytes) {
            long maxMb = maxSizeBytes / (1024 * 1024);
            throw new ReglaNegocioException("El archivo de audio supera el tamaño máximo permitido de " + maxMb + " MB.");
        }

        String nombreOriginal = archivo.getOriginalFilename();
        String extension = extraerExtension(nombreOriginal);

        if (!EXTENSIONES_PERMITIDAS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new ReglaNegocioException("Formato de audio no permitido (." + extension + "). Solo se admiten archivos MP3, WAV y OGG.");
        }

        String contentType = archivo.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            boolean tipoValido = TIPOS_MIME_PERMITIDOS.stream()
                    .anyMatch(tipo -> tipo.equalsIgnoreCase(contentType.trim()));
            if (!tipoValido) {
                throw new ReglaNegocioException("El tipo de medio '" + contentType + "' no es un formato de audio válido (MP3, WAV, OGG).");
            }
        }

        // Nombre saneado con UUID para evitar colisiones y path traversal
        String nombreSaneado = "audio_" + UUID.randomUUID() + "." + extension.toLowerCase(Locale.ROOT);

        try {
            return almacenamientoContenido.guardarAudio(nombreSaneado, archivo.getBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Error al leer los bytes del archivo de audio subido", e);
        }
    }

    private String extraerExtension(String nombreArchivo) {
        if (nombreArchivo == null || !nombreArchivo.contains(".")) {
            return "";
        }
        return nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1);
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }
}
