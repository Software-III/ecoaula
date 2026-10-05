package com.ecoaula.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CifradorAESGCMTest {

    private CifradorAESGCM cifrador;

    @BeforeEach
    void setUp() {
        cifrador = new CifradorAESGCM("clave-secreta-de-prueba-para-aes-gcm-256");
    }

    @Test
    @DisplayName("Debe cifrar y descifrar texto sensible manteniendo la integridad")
    void debeCifrarYDescifrarCorrectamente() {
        String datoSensible = "Baja visión severa con ceguera nocturna (Retinosis Pigmentaria)";

        String cifrado = cifrador.cifrar(datoSensible);

        assertNotNull(cifrado);
        assertNotEquals(datoSensible, cifrado);

        String descifrado = cifrador.descifrar(cifrado);
        assertEquals(datoSensible, descifrado);
    }

    @Test
    @DisplayName("Debe retornar null cuando el texto de entrada es nulo o vacío")
    void debeManejarEntradasNulasOVacias() {
        assertNull(cifrador.cifrar(null));
        assertNull(cifrador.cifrar(""));
        assertNull(cifrador.descifrar(null));
        assertNull(cifrador.descifrar(""));
    }

    @Test
    @DisplayName("Debe fallar al intentar descifrar datos corruptos o manipulados")
    void debeDetectarManipulacionEnTagGCM() {
        String cifrado = cifrador.cifrar("Dato seguro");
        String manipulado = cifrado.substring(0, cifrado.length() - 4) + "AAAA";
        assertThrows(IllegalStateException.class, () -> cifrador.descifrar(manipulado));
    }

    @Test
    @DisplayName("Debe fallar al arrancar en perfil prod si falta la variable ECOAULA_CLAVE_CIFRADO")
    void debeFallarEnPerfilProdSiFaltaClaveCifrado() {
        org.springframework.mock.env.MockEnvironment env = new org.springframework.mock.env.MockEnvironment();
        env.setActiveProfiles("prod");

        assertThrows(IllegalStateException.class, () -> new CifradorAESGCM(null, env));
        assertThrows(IllegalStateException.class, () -> new CifradorAESGCM("", env));
        assertThrows(IllegalStateException.class, () -> new CifradorAESGCM("ecoaula-clave-secreta-aes-256-ok!", env));
    }

    @Test
    @DisplayName("Debe arrancar satisfactoriamente en perfil prod si se suministra una clave de cifrado válida")
    void debeArrancarEnPerfilProdConClaveValida() {
        org.springframework.mock.env.MockEnvironment env = new org.springframework.mock.env.MockEnvironment();
        env.setActiveProfiles("prod");

        assertDoesNotThrow(() -> new CifradorAESGCM("mi-clave-super-secreta-de-produccion-12345", env));
    }
}
