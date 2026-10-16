package com.ecoaula.controller.dto;

import org.springframework.web.multipart.MultipartFile;

/**
 * Formulario para edición de transcripción y subida de audio en un borrador de lección.
 */
public class BorradorVersionForm {

    private String transcripcion;
    private MultipartFile archivoAudio;

    public BorradorVersionForm() {
    }

    public BorradorVersionForm(String transcripcion) {
        this.transcripcion = transcripcion;
    }

    public String getTranscripcion() {
        return transcripcion;
    }

    public void setTranscripcion(String transcripcion) {
        this.transcripcion = transcripcion;
    }

    public MultipartFile getArchivoAudio() {
        return archivoAudio;
    }

    public void setArchivoAudio(MultipartFile archivoAudio) {
        this.archivoAudio = archivoAudio;
    }
}
