package com.ecoaula.storage;

import com.ecoaula.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Implementación local en disco del almacenamiento de contenidos.
 */
@Service
public class AlmacenamientoLocal implements AlmacenamientoContenido {

    private final Path rutaBase;

    public AlmacenamientoLocal(@Value("${ecoaula.almacenamiento.ruta-local:./storage/contenidos}") String rutaConfigurada) {
        this.rutaBase = Paths.get(rutaConfigurada).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rutaBase);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el directorio de almacenamiento local: " + rutaBase, e);
        }
    }

    @Override
    public String guardarArchivo(String nombreArchivo, byte[] contenido) {
        try {
            Files.write(resolver(nombreArchivo), contenido, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return nombreArchivo;
        } catch (IOException e) {
            throw new IllegalStateException("Error al guardar archivo en almacenamiento local", e);
        }
    }

    @Override
    public byte[] obtenerAudio(String rutaRelativa) {
        try {
            Path archivo = resolver(rutaRelativa);
            if (!Files.exists(archivo)) {
                throw new RecursoNoEncontradoException("Archivo de audio no encontrado: " + rutaRelativa);
            }
            return Files.readAllBytes(archivo);
        } catch (IOException e) {
            throw new IllegalStateException("Error al leer archivo de audio: " + rutaRelativa, e);
        }
    }

    @Override
    public InputStream obtenerAudioInputStream(String rutaRelativa) {
        return new ByteArrayInputStream(obtenerAudio(rutaRelativa));
    }

    @Override
    public Resource obtenerRecurso(String rutaRelativa) {
        Path archivo = resolver(rutaRelativa);
        if (!Files.isRegularFile(archivo)) {
            throw new RecursoNoEncontradoException("Archivo no encontrado: " + rutaRelativa);
        }
        return new PathResource(archivo);
    }

    @Override
    public boolean existe(String rutaRelativa) {
        return Files.exists(resolver(rutaRelativa));
    }

    private Path resolver(String rutaRelativa) {
        Path archivo = rutaBase.resolve(rutaRelativa).normalize();
        if (!archivo.startsWith(rutaBase)) {
            throw new IllegalArgumentException("Ruta no válida: intento de path traversal");
        }
        return archivo;
    }
}
