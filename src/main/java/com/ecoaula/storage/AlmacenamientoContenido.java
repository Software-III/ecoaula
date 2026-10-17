package com.ecoaula.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;

/**
 * Interfaz que abstrae el almacenamiento de contenidos multimedia (audio e imágenes de las lecciones).
 * Desacopla la lógica de negocio del proveedor físico (disco local, AWS S3, etc.).
 */
public interface AlmacenamientoContenido {

    String guardarArchivo(String nombreArchivo, byte[] contenido);

    default String guardarAudio(String nombreArchivo, byte[] contenido) {
        return guardarArchivo(nombreArchivo, contenido);
    }

    byte[] obtenerAudio(String rutaRelativa);

    InputStream obtenerAudioInputStream(String rutaRelativa);

    /**
     * Recurso para servir el archivo por HTTP; Spring MVC atiende las peticiones Range sobre él,
     * lo que permite adelantar y retroceder el audio sin descargarlo completo.
     */
    Resource obtenerRecurso(String rutaRelativa);

    boolean existe(String rutaRelativa);
}
