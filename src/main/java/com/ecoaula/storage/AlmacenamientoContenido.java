package com.ecoaula.storage;

import java.io.InputStream;

/**
 * Interfaz que abstrae el almacenamiento de contenidos multimedia (archivos de audio, transcripciones).
 * Desacopla la lógica de negocio del proveedor físico (disco local, AWS S3, etc.).
 */
public interface AlmacenamientoContenido {

    String guardarAudio(String nombreArchivo, byte[] contenido);

    byte[] obtenerAudio(String rutaRelativa);

    InputStream obtenerAudioInputStream(String rutaRelativa);

    boolean existe(String rutaRelativa);
}
