package com.ecoaula.controller;

import com.ecoaula.service.ContenidoService;
import com.ecoaula.storage.AlmacenamientoContenido;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Entrega de los archivos de las lecciones (audio con controles nativos, ADR-05, e imágenes).
 * Requiere sesión: el aprendiz solo accede a archivos publicados de cursos en los que está matriculado,
 * y el personal de contenido a cualquier archivo registrado. Al devolver un Resource, Spring MVC
 * responde las peticiones Range (206), necesarias para adelantar y retroceder el audio.
 */
@Controller
public class ArchivoContenidoController {

    private static final Set<String> ROLES_PERSONAL_CONTENIDO =
            Set.of("ROLE_GESTOR_CONTENIDO", "ROLE_REVISOR_ACCESIBILIDAD");

    private static final Map<String, MediaType> TIPOS = Map.of(
            "wav", MediaType.parseMediaType("audio/wav"),
            "mp3", MediaType.parseMediaType("audio/mpeg"),
            "ogg", MediaType.parseMediaType("audio/ogg"),
            "png", MediaType.IMAGE_PNG,
            "jpg", MediaType.IMAGE_JPEG,
            "jpeg", MediaType.IMAGE_JPEG,
            "webp", MediaType.parseMediaType("image/webp"));

    private final ContenidoService contenidoService;
    private final AlmacenamientoContenido almacenamientoContenido;

    public ArchivoContenidoController(ContenidoService contenidoService,
                                      AlmacenamientoContenido almacenamientoContenido) {
        this.contenidoService = contenidoService;
        this.almacenamientoContenido = almacenamientoContenido;
    }

    @GetMapping("/audio/{nombreArchivo:.+}")
    public ResponseEntity<Resource> servirAudio(@PathVariable("nombreArchivo") String nombreArchivo,
                                                Authentication authentication) {
        contenidoService.verificarAccesoAudio(nombreArchivo, authentication.getName(), esPersonalDeContenido(authentication));
        return servir(nombreArchivo);
    }

    @GetMapping("/imagenes/{nombreArchivo:.+}")
    public ResponseEntity<Resource> servirImagen(@PathVariable("nombreArchivo") String nombreArchivo,
                                                 Authentication authentication) {
        contenidoService.verificarAccesoImagen(nombreArchivo, authentication.getName(), esPersonalDeContenido(authentication));
        return servir(nombreArchivo);
    }

    private ResponseEntity<Resource> servir(String nombreArchivo) {
        String extension = nombreArchivo.substring(nombreArchivo.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return ResponseEntity.ok()
                .contentType(TIPOS.getOrDefault(extension, MediaType.APPLICATION_OCTET_STREAM))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(almacenamientoContenido.obtenerRecurso(nombreArchivo));
    }

    private static boolean esPersonalDeContenido(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLES_PERSONAL_CONTENIDO::contains);
    }
}
