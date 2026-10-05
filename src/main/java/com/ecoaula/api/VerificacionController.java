package com.ecoaula.api;

import com.ecoaula.service.CertificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * Controlador de la API pública REST para verificación de certificados (ADR-06).
 * No expone datos personales del aprendiz ni del curso; únicamente confirma autenticidad y vigencia.
 */
@RestController
@RequestMapping("/api/v1/certificados")
public class VerificacionController {

    private final CertificacionService certificacionService;

    public VerificacionController(CertificacionService certificacionService) {
        this.certificacionService = certificacionService;
    }

    @GetMapping("/{codigo}/verificacion")
    public ResponseEntity<Map<String, Boolean>> verificarCertificado(@PathVariable("codigo") String codigo) {
        boolean esValido = certificacionService.verificarValidez(codigo);
        return ResponseEntity.ok(Collections.singletonMap("valido", esValido));
    }
}
