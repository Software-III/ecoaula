package com.ecoaula.service;

import com.ecoaula.exception.AccesoDenegadoException;
import com.ecoaula.exception.RecursoNoEncontradoException;
import com.ecoaula.model.Certificado;
import com.ecoaula.model.Matricula;
import com.ecoaula.model.enums.EstadoMatricula;
import com.ecoaula.notification.NotificacionService;
import com.ecoaula.repository.CertificadoRepository;
import com.ecoaula.repository.MatriculaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Servicio encargado de la emisión, verificación pública y descarga de certificados (ADR-06).
 * Emplea SecureRandom con alfabeto no ambiguo para garantizar códigos únicos e impredecibles.
 */
@Service
public class CertificacionService {

    // Alfabeto sin caracteres ambiguos (excluye 0, O, 1, I, L)
    private static final String ALFABETO_SEGURO = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int LONGITUD_BLOQUE = 4;
    private static final int CANTIDAD_BLOQUES = 4;

    private final CertificadoRepository certificadoRepository;
    private final MatriculaRepository matriculaRepository;
    private final NotificacionService notificacionService;
    private final SecureRandom secureRandom = new SecureRandom();

    public CertificacionService(CertificadoRepository certificadoRepository,
                                MatriculaRepository matriculaRepository,
                                NotificacionService notificacionService) {
        this.certificadoRepository = certificadoRepository;
        this.matriculaRepository = matriculaRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public Certificado emitirCertificado(Matricula matricula) {
        Optional<Certificado> existente = certificadoRepository.findByMatriculaId(matricula.getId());
        if (existente.isPresent()) {
            return existente.get();
        }

        String codigo = generarCodigoUnico();
        Certificado certificado = new Certificado(matricula, codigo);
        Certificado guardado = certificadoRepository.save(certificado);

        matricula.setEstado(EstadoMatricula.COMPLETADA);
        matriculaRepository.save(matricula);

        notificacionService.notificarEmisionCertificado(
                matricula.getAprendiz().getCorreo(),
                matricula.getCurso().getTitulo(),
                guardado.getCodigoVerificacion()
        );

        return guardado;
    }

    @Transactional(readOnly = true)
    public boolean verificarValidez(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return false;
        }
        String codigoLimpio = codigo.trim().toUpperCase();
        return certificadoRepository.existsByCodigoVerificacion(codigoLimpio);
    }

    @Transactional(readOnly = true)
    public Certificado obtenerPorIdYValidarPropiedad(Long id, String correoAprendiz) {
        Certificado certificado = certificadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Certificado no encontrado con ID: " + id));

        if (!certificado.getMatricula().getAprendiz().getCorreo().equalsIgnoreCase(correoAprendiz)) {
            throw new AccesoDenegadoException("No tiene autorización para ver este certificado.");
        }

        return certificado;
    }

    @Transactional(readOnly = true)
    public Optional<Certificado> obtenerPorMatriculaId(Long matriculaId) {
        return certificadoRepository.findByMatriculaId(matriculaId);
    }

    public String generarCodigoUnico() {
        String codigo;
        int intentos = 0;
        do {
            if (intentos++ > 100) {
                throw new IllegalStateException("No se pudo generar un código único de certificado tras múltiples intentos");
            }
            codigo = construirCodigoAleatorio();
        } while (certificadoRepository.existsByCodigoVerificacion(codigo));

        return codigo;
    }

    public String construirCodigoAleatorio() {
        StringBuilder sb = new StringBuilder("EA");
        for (int b = 0; b < CANTIDAD_BLOQUES; b++) {
            sb.append("-");
            for (int i = 0; i < LONGITUD_BLOQUE; i++) {
                int indice = secureRandom.nextInt(ALFABETO_SEGURO.length());
                sb.append(ALFABETO_SEGURO.charAt(indice));
            }
        }
        return sb.toString();
    }

    public byte[] generarArchivoCertificadoTxt(Certificado certificado) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy, HH:mm");
        String fechaFormateada = certificado.getEmitidoEn().format(formatter);

        StringBuilder sb = new StringBuilder();
        sb.append("=================================================================\n");
        sb.append("          ECOAULA — PLATAFORMA DE APRENDIZAJE ACCESIBLE          \n");
        sb.append("                 CERTIFICADO DE APROBACIÓN                       \n");
        sb.append("=================================================================\n\n");
        sb.append("Certifica que el/la aprendiz:\n\n");
        sb.append("       ").append(certificado.getMatricula().getAprendiz().getNombreCompleto().toUpperCase()).append("\n\n");
        sb.append("Ha completado y aprobado satisfactoriamente el curso:\n\n");
        sb.append("       \"").append(certificado.getMatricula().getCurso().getTitulo()).append("\"\n\n");
        sb.append("Con una intensidad horaria de: ").append(certificado.getMatricula().getCurso().getHoras()).append(" horas académicas.\n");
        sb.append("Modalidad: ").append(certificado.getMatricula().getCurso().getModalidad()).append("\n");
        sb.append("Fecha de expedición: ").append(fechaFormateada).append("\n\n");
        sb.append("-----------------------------------------------------------------\n");
        sb.append("CÓDIGO DE VERIFICACIÓN AUTÉNTICA:\n");
        sb.append("       ").append(certificado.getCodigoVerificacion()).append("\n");
        sb.append("-----------------------------------------------------------------\n");
        sb.append("Este certificado puede ser validado públicamente en cualquier momento\n");
        sb.append("a través del sistema de verificación en línea de EcoAula.\n");
        sb.append("Cumple con los estándares de accesibilidad digital NTC 5854 / WCAG 2.1 AA.\n");
        sb.append("=================================================================\n");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
