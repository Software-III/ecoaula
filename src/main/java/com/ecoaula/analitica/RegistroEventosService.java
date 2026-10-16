package com.ecoaula.analitica;

import com.ecoaula.model.EventoAprendizaje;
import com.ecoaula.model.enums.TipoEvento;
import com.ecoaula.repository.EventoAprendizajeRepository;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Registra los eventos del recorrido del aprendiz (componente de analítica del aprendizaje).
 * Se ejecuta en su propia transacción y nunca propaga errores: una falla de la analítica no debe
 * impedir que el aprendiz se matricule o avance.
 */
@Service
public class RegistroEventosService {

    private static final Logger log = LoggerFactory.getLogger(RegistroEventosService.class);

    /** Atributo de sesión con el identificador de la visita (sobrevive al inicio de sesión). */
    static final String ATRIBUTO_SESION = "ecoaula.analitica.sesion";

    private final EventoAprendizajeRepository eventoRepository;

    public RegistroEventosService(EventoAprendizajeRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(TipoEvento tipo, HttpSession sesion, String correo, Long cursoId, Long contenidoId) {
        try {
            eventoRepository.save(new EventoAprendizaje(tipo, correo, identificadorDeVisita(sesion), cursoId, contenidoId));
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar el evento {}: {}", tipo, e.getMessage());
        }
    }

    public void registrar(TipoEvento tipo, HttpSession sesion, String correo) {
        registrar(tipo, sesion, correo, null, null);
    }

    /** Registra el evento solo la primera vez en la visita (por ejemplo, abrir el formulario de registro). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarUnaVezPorVisita(TipoEvento tipo, HttpSession sesion) {
        try {
            String visita = identificadorDeVisita(sesion);
            if (!eventoRepository.existsBySesionAndTipo(visita, tipo)) {
                eventoRepository.save(new EventoAprendizaje(tipo, null, visita, null, null));
            }
        } catch (RuntimeException e) {
            log.warn("No se pudo registrar el evento {}: {}", tipo, e.getMessage());
        }
    }

    private static String identificadorDeVisita(HttpSession sesion) {
        Object actual = sesion.getAttribute(ATRIBUTO_SESION);
        if (actual instanceof String visita) {
            return visita;
        }
        String nueva = UUID.randomUUID().toString();
        sesion.setAttribute(ATRIBUTO_SESION, nueva);
        return nueva;
    }
}
