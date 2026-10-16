package com.ecoaula.model.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Criterios de la lista de chequeo de accesibilidad (WCAG 2.1 AA / NTC 5854).
 * Centralizado y configurable en un solo lugar.
 */
public enum CriterioAccesibilidad {
    TRANSCRIPCION_FIEL(
            "TRANSCRIPCION_FIEL",
            "La transcripción es completa y fiel al audio (sin omisiones ni discrepancias esenciales).",
            "WCAG 1.2.1 / NTC 5854 - Solo audio grabado"
    ),
    AUDIO_INTELIGIBLE(
            "AUDIO_INTELIGIBLE",
            "El audio es nítido, inteligible, con volumen adecuado y sin ruidos distractores de fondo.",
            "WCAG 1.4.7 / NTC 5854 - Audio de fondo bajo o nulo"
    ),
    TEXTO_ESTRUCTURADO(
            "TEXTO_ESTRUCTURADO",
            "El texto está estructurado semánticamente con párrafos, encabezados y listas claras.",
            "WCAG 1.3.1 / NTC 5854 - Información y relaciones"
    ),
    IDIOMA_ESPANOL(
            "IDIOMA_ESPANOL",
            "El contenido está redactado y narrado en español comprensible y accesible.",
            "WCAG 3.1.1 / NTC 5854 - Idioma del contenido"
    ),
    SIN_SOLO_VISUAL(
            "SIN_SOLO_VISUAL",
            "No se transmiten instrucciones ni conceptos basados exclusivamente en referencias visuales o espaciales.",
            "WCAG 1.3.3 / NTC 5854 - Características sensoriales"
    ),
    ENLACES_DESCRIPTIVOS(
            "ENLACES_DESCRIPTIVOS",
            "Los enlaces y llamados a la acción tienen textos descriptivos claros que explican su propósito.",
            "WCAG 2.4.4 / NTC 5854 - Propósito de enlaces y referencias"
    );

    private final String codigo;
    private final String descripcion;
    private final String normaReferencia;

    CriterioAccesibilidad(String codigo, String descripcion, String normaReferencia) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.normaReferencia = normaReferencia;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getNormaReferencia() {
        return normaReferencia;
    }

    public static List<CriterioAccesibilidad> todos() {
        return Arrays.asList(values());
    }

    public static boolean cumpleTodos(Set<String> codigosChequeados) {
        if (codigosChequeados == null || codigosChequeados.isEmpty()) {
            return false;
        }
        for (CriterioAccesibilidad c : values()) {
            if (!codigosChequeados.contains(c.getCodigo())) {
                return false;
            }
        }
        return true;
    }
}
