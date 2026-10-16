package com.ecoaula.controller;

import com.ecoaula.analitica.RegistroEventosService;
import com.ecoaula.model.enums.TipoEvento;
import com.ecoaula.service.ContenidoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Recibe desde el navegador el inicio de la reproducción del audio de una lección, que alimenta el
 * indicador "Tiempo de acceso al contenido" (Proceso 2).
 */
@RestController
public class EventoController {

    private final RegistroEventosService registroEventos;
    private final ContenidoService contenidoService;

    public EventoController(RegistroEventosService registroEventos, ContenidoService contenidoService) {
        this.registroEventos = registroEventos;
        this.contenidoService = contenidoService;
    }

    @PostMapping("/eventos/reproduccion")
    public ResponseEntity<Void> reproduccion(@RequestParam("curso") Long cursoId,
                                             @RequestParam("leccion") Long contenidoId,
                                             Principal principal,
                                             HttpSession sesion) {
        // Solo se registra la reproducción de cursos en los que el aprendiz está matriculado
        contenidoService.verificarMatricula(principal.getName(), cursoId);
        registroEventos.registrar(TipoEvento.AUDIO_REPRODUCIDO, sesion, principal.getName(), cursoId, contenidoId);
        return ResponseEntity.noContent().build();
    }
}
