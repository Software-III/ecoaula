package com.ecoaula.controller;

import com.ecoaula.storage.AlmacenamientoContenido;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Controlador para la entrega y streaming accesible de archivos de audio nativos (ADR-05).
 */
@Controller
public class AudioController {

    private final AlmacenamientoContenido almacenamientoContenido;

    public AudioController(AlmacenamientoContenido almacenamientoContenido) {
        this.almacenamientoContenido = almacenamientoContenido;
    }

    @GetMapping("/audio/{nombreArchivo:.+}")
    public ResponseEntity<byte[]> servirAudio(@PathVariable("nombreArchivo") String nombreArchivo) {
        byte[] audioBytes = almacenamientoContenido.obtenerAudio(nombreArchivo);

        MediaType mediaType = MediaType.parseMediaType("audio/wav");
        if (nombreArchivo.toLowerCase().endsWith(".mp3")) {
            mediaType = MediaType.parseMediaType("audio/mpeg");
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(audioBytes);
    }
}
