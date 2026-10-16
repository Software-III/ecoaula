package com.ecoaula.controller.dto;

import org.springframework.web.multipart.MultipartFile;

/**
 * Formulario para edición de transcripción y subida de audio en un borrador de lección.
 */
public class BorradorVersionForm {

    private String transcripcion;
    private MultipartFile archivoAudio;

    /** Fuente del material y su licencia o autorización de uso (Ley 23 de 1982). */
    private String fuenteMaterial;

    private boolean licenciaDeclarada;

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

    public String getFuenteMaterial() {
        return fuenteMaterial;
    }

    public void setFuenteMaterial(String fuenteMaterial) {
        this.fuenteMaterial = fuenteMaterial;
    }

    public boolean isLicenciaDeclarada() {
        return licenciaDeclarada;
    }

    public void setLicenciaDeclarada(boolean licenciaDeclarada) {
        this.licenciaDeclarada = licenciaDeclarada;
    }
}
