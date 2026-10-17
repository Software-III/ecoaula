package com.ecoaula.service.dto;

import com.ecoaula.model.ContenidoVersion;

import java.util.List;

/**
 * Estado de una lección para los paneles del gestor: la versión vigente que ven los aprendices,
 * las versiones en trámite (borrador o en revisión) y la rechazada que espera subsanación.
 */
public record EstadoLeccion(ContenidoVersion vigente,
                            List<ContenidoVersion> enTramite,
                            ContenidoVersion rechazadaPendiente) {
}
