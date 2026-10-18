package com.ecoaula.config;

import com.ecoaula.EcoaulaApplication;
import com.ecoaula.model.Aprendiz;
import com.ecoaula.repository.AprendizRepository;
import com.ecoaula.repository.CursoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La siembra de datos de demostración en el perfil prod no crea cuentas con contraseñas conocidas
 * ni restablece contraseñas al arrancar.
 */
class SiembraProduccionTest {

    private static final List<String> CUENTAS = List.of(
            "aprendiz@ecoaula.local", "gestor@ecoaula.local", "revisor@ecoaula.local", "coordinador@ecoaula.local");

    @TempDir
    Path almacenamiento;

    private ConfigurableApplicationContext arrancarEnProduccion(String baseDeDatos, String... propiedades) {
        List<String> props = new ArrayList<>(List.of(
                "server.port=0",
                "spring.datasource.url=jdbc:h2:mem:" + baseDeDatos + ";DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "ecoaula.seguridad.clave-cifrado=clave-de-prueba-para-produccion-123",
                "ecoaula.almacenamiento.ruta-local=" + almacenamiento));
        props.addAll(List.of(propiedades));
        // Como argumentos de línea de comandos: tienen prioridad sobre application-prod.yml
        return new SpringApplicationBuilder(EcoaulaApplication.class)
                .profiles("prod")
                .run(props.stream().map(p -> "--" + p).toArray(String[]::new));
    }

    private static Optional<Aprendiz> cuenta(ConfigurableApplicationContext contexto, String correo) {
        return contexto.getBean(AprendizRepository.class).findByCorreo(correo);
    }

    @Test
    @DisplayName("Sin variables de contraseña, en producción no se crea ninguna cuenta demo")
    void sinVariablesNoSeCreanCuentas() {
        try (ConfigurableApplicationContext contexto = arrancarEnProduccion("prod_sin_variables")) {
            CUENTAS.forEach(correo -> assertFalse(cuenta(contexto, correo).isPresent(), correo));
            // Los cursos de demostración sí se crean: no exponen credenciales
            assertTrue(contexto.getBean(CursoRepository.class).count() > 0);
        }
    }

    @Test
    @DisplayName("En producción se rechazan las contraseñas de desarrollo y las de menos de 12 caracteres")
    void rechazaContrasenasConocidasODebiles() {
        try (ConfigurableApplicationContext contexto = arrancarEnProduccion("prod_contrasenas_debiles",
                "ecoaula.seed.gestor-password=GestorEcoAula2026!",
                "ecoaula.seed.revisor-password=corta123",
                "ecoaula.seed.aprendiz-password=Aprendiz#Propia2026")) {
            assertFalse(cuenta(contexto, "gestor@ecoaula.local").isPresent());
            assertFalse(cuenta(contexto, "revisor@ecoaula.local").isPresent());
            assertTrue(cuenta(contexto, "aprendiz@ecoaula.local").isPresent());
        }
    }

    @Test
    @DisplayName("En producción las contraseñas propias se usan al crear la cuenta y no se restablecen al reiniciar")
    void contrasenasPropiasSinRestablecimiento() {
        String base = "prod_contrasenas_propias";
        try (ConfigurableApplicationContext contexto = arrancarEnProduccion(base,
                "ecoaula.seed.gestor-password=Gestor#Propia2026",
                "ecoaula.seed.coordinador-password=Coordinacion#Propia2026")) {
            PasswordEncoder codificador = contexto.getBean(PasswordEncoder.class);
            assertTrue(codificador.matches("Gestor#Propia2026", cuenta(contexto, "gestor@ecoaula.local").orElseThrow().getPasswordHash()));
            assertTrue(cuenta(contexto, "coordinador@ecoaula.local").isPresent());
        }
        // Reinicio con otra contraseña en la variable: la cuenta existente conserva la suya
        try (ConfigurableApplicationContext contexto = arrancarEnProduccion(base,
                "ecoaula.seed.gestor-password=Otra#Contrasena2026")) {
            PasswordEncoder codificador = contexto.getBean(PasswordEncoder.class);
            String hash = cuenta(contexto, "gestor@ecoaula.local").orElseThrow().getPasswordHash();
            assertTrue(codificador.matches("Gestor#Propia2026", hash));
            assertFalse(codificador.matches("Otra#Contrasena2026", hash));
        }
    }

    @Test
    @DisplayName("Con ecoaula.seed.habilitado=false no se siembra nada en producción")
    void siembraDeshabilitada() {
        try (ConfigurableApplicationContext contexto = arrancarEnProduccion("prod_sin_siembra",
                "ecoaula.seed.habilitado=false",
                "ecoaula.seed.gestor-password=Gestor#Propia2026")) {
            assertEquals(0, contexto.getBean(CursoRepository.class).count());
            assertFalse(cuenta(contexto, "gestor@ecoaula.local").isPresent());
        }
    }
}
