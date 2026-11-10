package com.ecoaula.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * NC-03 de la auditoría (Ley 1581 de 2012): la autorización del registro debe poder leerse antes de aceptarla.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PoliticaDatosMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("La política de tratamiento de datos es pública y describe datos, finalidad, derechos y dato sensible")
    void politicaEsPublicaYCompleta() throws Exception {
        mockMvc.perform(get("/politica-datos"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Política de Tratamiento de Datos Personales")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Qué datos recogemos")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Para qué los usamos")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("condición visual")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sus derechos")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Superintendencia de Industria y Comercio")));
    }

    @Test
    @DisplayName("El formulario de registro enlaza la política junto a la casilla de autorización")
    void registroEnlazaLaPolitica() throws Exception {
        mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/politica-datos")));
    }

    @Test
    @DisplayName("La consola de H2 ya no es pública: sin sesión exige iniciar sesión")
    void consolaH2NoEsPublica() throws Exception {
        mockMvc.perform(get("/h2-console/"))
                .andExpect(status().is3xxRedirection());
    }
}
