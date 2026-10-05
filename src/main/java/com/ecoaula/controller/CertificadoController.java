package com.ecoaula.controller;

import com.ecoaula.model.Certificado;
import com.ecoaula.service.CertificacionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

/**
 * Controlador para la visualización, descarga accesible y verificación de certificados (Pantalla 6).
 */
@Controller
public class CertificadoController {

    private final CertificacionService certificacionService;

    public CertificadoController(CertificacionService certificacionService) {
        this.certificacionService = certificacionService;
    }

    @GetMapping("/certificados/{id}")
    public String verCertificado(@PathVariable("id") Long certificadoId,
                                 Principal principal,
                                 Model model) {
        Certificado certificado = certificacionService.obtenerPorIdYValidarPropiedad(certificadoId, principal.getName());
        model.addAttribute("titulo", "Certificado de Aprobación — " + certificado.getMatricula().getCurso().getTitulo());
        model.addAttribute("certificado", certificado);
        model.addAttribute("matricula", certificado.getMatricula());
        model.addAttribute("curso", certificado.getMatricula().getCurso());
        model.addAttribute("aprendiz", certificado.getMatricula().getAprendiz());

        return "certificados/detalle";
    }

    @GetMapping("/certificados/{id}/descargar")
    public ResponseEntity<byte[]> descargarCertificadoTxt(@PathVariable("id") Long certificadoId,
                                                          Principal principal) {
        Certificado certificado = certificacionService.obtenerPorIdYValidarPropiedad(certificadoId, principal.getName());
        byte[] archivoTxt = certificacionService.generarArchivoCertificadoTxt(certificado);

        String nombreArchivo = "certificado-" + certificado.getCodigoVerificacion() + ".txt";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(archivoTxt);
    }

    @GetMapping("/verificar")
    public String verificarPublicamente(@RequestParam(value = "codigo", required = false) String codigo,
                                        Model model) {
        model.addAttribute("titulo", "Verificación Pública de Autenticidad de Certificados");
        model.addAttribute("codigoConsultado", codigo);

        if (codigo != null && !codigo.isBlank()) {
            boolean valido = certificacionService.verificarValidez(codigo);
            model.addAttribute("resultadoValidacion", valido);
        }

        return "certificados/verificar";
    }
}
