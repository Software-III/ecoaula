package com.ecoaula.security;

import com.ecoaula.EcoaulaApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Prueba que valida que la aplicación EcoAula rehúsa iniciar en perfil "prod"
 * si no se suministra la variable de entorno obligatoria ECOAULA_CLAVE_CIFRADO.
 */
class FalloArranqueProdSinClaveTest {

    @Test
    @DisplayName("Debe fallar el arranque de Spring Boot en perfil prod sin clave de cifrado")
    void debeFallarArranqueEnProdSinClave() {
        SpringApplicationBuilder app = new SpringApplicationBuilder(EcoaulaApplication.class)
                .profiles("prod")
                .properties(
                        "server.port=0",
                        "spring.datasource.url=jdbc:h2:mem:test_prod;DB_CLOSE_DELAY=-1",
                        "spring.datasource.driver-class-name=org.h2.Driver",
                        "spring.datasource.username=sa",
                        "spring.datasource.password=",
                        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
                );

        assertThrows(Exception.class, app::run);
    }
}
