package com.ecoaula.service.dto;

import java.util.List;

/**
 * Avance de un aprendiz en un curso: qué lecciones ha completado y cuál es la siguiente pendiente.
 * Permite continuar el curso donde lo dejó en lugar de empezar siempre por la primera lección.
 */
public record AvanceCurso(List<Leccion> lecciones) {

    public record Leccion(Integer orden, String titulo, boolean completada) {
    }

    public long completadas() {
        return lecciones.stream().filter(Leccion::completada).count();
    }

    public int total() {
        return lecciones.size();
    }

    public boolean sinIniciar() {
        return completadas() == 0;
    }

    public boolean todasCompletadas() {
        return !lecciones.isEmpty() && completadas() == lecciones.size();
    }

    /** Primera lección no completada, o null si ya completó todas. */
    public Leccion siguientePendiente() {
        return lecciones.stream().filter(l -> !l.completada()).findFirst().orElse(null);
    }
}
