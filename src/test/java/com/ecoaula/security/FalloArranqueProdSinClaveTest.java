package com.ecoaula.security;

import com.ecoaula.EcoaulaApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba que valida que la aplicación EcoAula rehúsa iniciar en perfil "prod"
 * si no se suministra la variable de entorno obligatoria ECOAULA_CLAVE_CIFRADO.
 */
class FalloArranqueProdSinClaveTest {

    @Test
    @DisplayName("Debe fallar el arranque de Spring Boot en perfil prod sin clave de cifrado")
    void debeFallarArranqueEnProdSinClave() {
        SpringApplicationBuilder app = new SpringApplicationBuilder(EcoaulaApplication.class)
                .profiles("prod");

        // Como argumentos de línea de comandos: si no, application-prod.yml los pisa, la aplicación falla por
        // no encontrar MySQL y la prueba pasaría aunque la validación de la clave no existiera
        Exception error = assertThrows(Exception.class, () -> app.run(
                "--server.port=0",
                "--spring.datasource.url=jdbc:h2:mem:test_prod;DB_CLOSE_DELAY=-1",
                "--spring.datasource.driver-class-name=org.h2.Driver",
                "--spring.datasource.username=sa",
                "--spring.datasource.password=",
                "--spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
        ));
        Throwable causa = error;
        while (causa.getCause() != null) {
            causa = causa.getCause();
        }
        assertTrue(causa.getMessage().contains("ECOAULA_CLAVE_CIFRADO"),
                "El arranque debe fallar por la clave de cifrado, no por otra causa: " + causa.getMessage());
    }
}
