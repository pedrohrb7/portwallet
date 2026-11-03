package com.portwallet.shared.i18n;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** User-facing texts of the API follow Accept-Language; nothing else in the response does. */
@SpringBootTest
@AutoConfigureMockMvc
class LocalizedResponsesTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "Accept-Language: {0}")
    @CsvSource(delimiter = '|', value = {
        "pt-BR                | Link inválido ou expirado",
        "en                   | Invalid or expired link",
        "en-US,en;q=0.9       | Invalid or expired link",
        "es-AR                | Enlace no válido o caducado",
        "fr-CA, en;q=0.5      | Lien invalide ou expiré",
        "de-DE, fr;q=0.8      | Lien invalide ou expiré",
        "de                   | Link inválido ou expirado",
        "pt-PT                | Link inválido ou expirado",
    })
    void problemTitleFollowsAcceptLanguage(String acceptLanguage, String title) throws Exception {
        summaryOfAnUnknownToken(acceptLanguage)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("invalid-token"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value(title));
    }

    @Test
    void withoutAcceptLanguageTheApiAnswersInBrazilianPortuguese() throws Exception {
        summaryOfAnUnknownToken(null).andExpect(jsonPath("$.title").value("Link inválido ou expirado"));
    }

    @Test
    void validationMessagesFollowAcceptLanguage() throws Exception {
        mockMvc.perform(withLanguage(post("/api/v1/auth/register"), "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"email\":\"not-an-email\",\"document\":\"529.982.247-24\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid data"))
                .andExpect(jsonPath("$.errors[*].message", containsInAnyOrder(
                        "must be between 2 and 100 characters", "must be a valid e-mail address", "invalid CPF")));
    }

    @Test
    void customConstraintMessagesFollowAcceptLanguage() throws Exception {
        String longPassword = "ç".repeat(40);
        mockMvc.perform(withLanguage(post("/api/v1/auth/confirm"), "es")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"t\",\"password\":\"" + longPassword + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"))
                .andExpect(jsonPath("$.errors[0].message").value("es demasiado largo"));
    }

    @Test
    void frameworkFieldMessagesFollowAcceptLanguage() throws Exception {
        mockMvc.perform(withLanguage(post("/api/v1/auth/register"), "fr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Données invalides"))
                .andExpect(jsonPath("$.errors[0].field").value("body"))
                .andExpect(jsonPath("$.errors[0].message").value("JSON mal formé"));
    }

    @Test
    void securityErrorsFollowAcceptLanguage() throws Exception {
        mockMvc.perform(withLanguage(get("/api/v1/accounts/me"), "en"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("unauthorized"))
                .andExpect(jsonPath("$.title").value("Authentication required"));
        mockMvc.perform(get("/api/v1/accounts/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Autenticação necessária"));
    }

    @Test
    void frameworkErrorsFollowAcceptLanguage() throws Exception {
        mockMvc.perform(withLanguage(get("/api/v1/does-not-exist"), "es").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("not-found"))
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    private ResultActions summaryOfAnUnknownToken(String acceptLanguage) throws Exception {
        return mockMvc.perform(withLanguage(post("/api/v1/auth/confirm/summary"), acceptLanguage)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"unknown-token\"}"));
    }

    private static MockHttpServletRequestBuilder withLanguage(MockHttpServletRequestBuilder request, String language) {
        return language == null ? request : request.header(HttpHeaders.ACCEPT_LANGUAGE, language);
    }
}
