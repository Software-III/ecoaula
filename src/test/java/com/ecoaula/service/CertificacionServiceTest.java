package com.ecoaula.service;

import com.ecoaula.model.Aprendiz;
import com.ecoaula.model.Certificado;
import com.ecoaula.model.Curso;
import com.ecoaula.model.Matricula;
import com.ecoaula.notification.NotificacionService;
import com.ecoaula.repository.CertificadoRepository;
import com.ecoaula.repository.MatriculaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificacionServiceTest {

    @Mock
    private CertificadoRepository certificadoRepository;

    @Mock
    private MatriculaRepository matriculaRepository;

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private CertificacionService certificacionService;

    private Matricula matricula;

    @BeforeEach
    void setUp() {
        Aprendiz aprendiz = new Aprendiz();
        aprendiz.setId(1L);
        aprendiz.setCorreo("aprendiz@ecoaula.local");
        aprendiz.setNombreCompleto("Juan Pérez");

        Curso curso = new Curso("Ofimática", "Curso", 10, "Tecnología", "Virtual");
        curso.setId(2L);

        matricula = new Matricula(aprendiz, curso);
        matricula.setId(10L);
    }

    @Test
    @DisplayName("Debe generar códigos únicos e impredecibles que cumplan el patrón EA-XXXX-XXXX-XXXX-XXXX")
    void debeGenerarCodigosUnicosEImpredecibles() {
        Set<String> codigosGenerados = new HashSet<>();
        int muestra = 1000;

        for (int i = 0; i < muestra; i++) {
            String codigo = certificacionService.construirCodigoAleatorio();

            // Validar formato estricto: EA seguido de 4 bloques de 4 caracteres
            assertTrue(codigo.matches("^EA-[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}$"),
                    "El código " + codigo + " no cumple el patrón o contiene caracteres ambiguos");

            // Validar que no haya colisiones en la muestra aleatoria (impredecibilidad)
            assertTrue(codigosGenerados.add(codigo), "Colisión detectada en código aleatorio: " + codigo);
        }

        assertEquals(muestra, codigosGenerados.size());
    }

    @Test
    @DisplayName("Debe verificar validez retornando true si existe y false si no existe")
    void debeVerificarValidezCodigo() {
        when(certificadoRepository.existsByCodigoVerificacion("EA-7K3M-Q9XW-2D4F-8HNP")).thenReturn(true);
        when(certificadoRepository.existsByCodigoVerificacion("EA-0000-0000-0000-0000")).thenReturn(false);

        assertTrue(certificacionService.verificarValidez("EA-7K3M-Q9XW-2D4F-8HNP"));
        assertFalse(certificacionService.verificarValidez("EA-0000-0000-0000-0000"));
        assertFalse(certificacionService.verificarValidez(null));
        assertFalse(certificacionService.verificarValidez(""));
    }

    @Test
    @DisplayName("Debe emitir certificado y notificar al aprendiz")
    void debeEmitirCertificadoExitosamente() {
        when(certificadoRepository.findByMatriculaId(matricula.getId())).thenReturn(Optional.empty());
        when(certificadoRepository.existsByCodigoVerificacion(anyString())).thenReturn(false);

        Certificado certMock = new Certificado(matricula, "EA-ABCD-EFGH-JKLM-NPQR");
        when(certificadoRepository.save(any(Certificado.class))).thenReturn(certMock);

        Certificado resultado = certificacionService.emitirCertificado(matricula);

        assertNotNull(resultado);
        assertNotNull(resultado.getCodigoVerificacion());
        verify(notificacionService, times(1)).notificarEmisionCertificado(anyString(), anyString(), anyString());
    }
}
