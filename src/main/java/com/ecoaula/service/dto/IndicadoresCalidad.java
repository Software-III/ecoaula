package com.ecoaula.service.dto;

/**
 * DTO que encapsula los indicadores del Plan de Calidad y Accesibilidad.
 */
public class IndicadoresCalidad {

    private final double tasaAccesibilidadPrimeraRevision;
    private final long totalVersionesRevisadas;
    private final long aprobadasPrimeraRevision;
    private final double tiempoPromedioPublicacionHoras;
    private final long totalVersionesPublicadas;

    public IndicadoresCalidad(double tasaAccesibilidadPrimeraRevision,
                              long totalVersionesRevisadas,
                              long aprobadasPrimeraRevision,
                              double tiempoPromedioPublicacionHoras,
                              long totalVersionesPublicadas) {
        this.tasaAccesibilidadPrimeraRevision = tasaAccesibilidadPrimeraRevision;
        this.totalVersionesRevisadas = totalVersionesRevisadas;
        this.aprobadasPrimeraRevision = aprobadasPrimeraRevision;
        this.tiempoPromedioPublicacionHoras = tiempoPromedioPublicacionHoras;
        this.totalVersionesPublicadas = totalVersionesPublicadas;
    }

    public double getTasaAccesibilidadPrimeraRevision() {
        return tasaAccesibilidadPrimeraRevision;
    }

    public long getTotalVersionesRevisadas() {
        return totalVersionesRevisadas;
    }

    public long getAprobadasPrimeraRevision() {
        return aprobadasPrimeraRevision;
    }

    public double getTiempoPromedioPublicacionHoras() {
        return tiempoPromedioPublicacionHoras;
    }

    public long getTotalVersionesPublicadas() {
        return totalVersionesPublicadas;
    }

    public String getTasaFormateada() {
        return String.format(java.util.Locale.US, "%.1f%%", tasaAccesibilidadPrimeraRevision);
    }

    public String getTiempoPromedioFormateado() {
        if (tiempoPromedioPublicacionHoras < 1.0) {
            long minutos = Math.round(tiempoPromedioPublicacionHoras * 60);
            return minutos + " minutos";
        }
        return String.format(java.util.Locale.US, "%.1f horas", tiempoPromedioPublicacionHoras);
    }
}
