package com.portwallet.shared.openapi;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portwallet.shared.money.Money;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Assertions shared by every profile that exposes the API docs. */
abstract class OpenApiDocsContract {

    static final String PROTECTED_PATH = "/api/v1/test-docs/protected";
    static final String PUBLIC_PATH = "/api/v1/test-docs/public";
    static final String OUTSIDE_API_PATH = "/mock/test-docs/sample";

    private static final String PROBLEM_JSON = "['application/problem+json']";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void publishesAnOpenApi31DocumentWithoutAuthentication() throws Exception {
        docs().andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value(startsWith("3.1")))
                .andExpect(jsonPath("$.info.title").value("PortWallet API"))
                .andExpect(jsonPath("$.servers[0].url").value("/"));
    }

    @Test
    void declaresBearerAuthenticationByDefaultAndPublicEndpointsOptOut() throws Exception {
        docs().andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['" + PUBLIC_PATH + "'].post.security").isEmpty());
    }

    @Test
    void protectedEndpointWithoutInputDocumentsOnly401And500() throws Exception {
        assertThat(responseCodes(PROTECTED_PATH, "get")).containsExactly("200", "401", "500");
        String responses = responsesOf(PROTECTED_PATH, "get");
        docs().andExpect(jsonPath(responses + "['401'].content" + PROBLEM_JSON + ".examples.unauthorized.value.type")
                        .value("unauthorized"))
                .andExpect(jsonPath(responses + "['500'].content" + PROBLEM_JSON + ".examples['internal-error'].value.type")
                        .value("internal-error"));
    }

    @Test
    void publicEndpointNeverDocuments401() throws Exception {
        assertThat(responseCodes(PUBLIC_PATH, "post")).containsExactly("200", "400", "422", "500");
    }

    @Test
    void everyDeclaredProblemHasItsOwnExample() throws Exception {
        String responses = responsesOf(PUBLIC_PATH, "post");
        String examples400 = responses + "['400'].content" + PROBLEM_JSON + ".examples";
        String examples422 = responses + "['422'].content" + PROBLEM_JSON + ".examples";
        docs().andExpect(jsonPath(examples400 + "['validation-error'].value.type").value("validation-error"))
                .andExpect(jsonPath(examples400 + "['validation-error'].value.errors[0].field").exists())
                .andExpect(jsonPath(examples400 + "['invalid-token'].value.type").value("invalid-token"))
                .andExpect(jsonPath(examples400 + "['invalid-token'].value.title").value("Link inválido ou expirado"))
                .andExpect(jsonPath(examples422 + "['insufficient-balance'].value.type").value("insufficient-balance"))
                .andExpect(jsonPath(examples422 + "['insufficient-balance'].value.status").value(422))
                // Read from the pt-BR bundle, the same text the API sends by default.
                .andExpect(jsonPath(examples422 + "['insufficient-balance'].value.title").value("Saldo insuficiente"))
                .andExpect(jsonPath(examples422 + "['validation-error']").doesNotExist())
                .andExpect(jsonPath(responses + "['400'].description").value(containsString("`invalid-token`")))
                .andExpect(jsonPath(responses + "['400'].content" + PROBLEM_JSON + ".schema.$ref")
                        .value("#/components/schemas/Problem"));
    }

    @Test
    void successResponsesAreJson() throws Exception {
        docs().andExpect(jsonPath(responsesOf(PROTECTED_PATH, "get") + "['200'].content['application/json']").exists())
                .andExpect(jsonPath(responsesOf(PROTECTED_PATH, "get") + "['200'].content['*/*']").doesNotExist());
    }

    @Test
    void problemSchemaDescribesTheFields() throws Exception {
        docs().andExpect(jsonPath("$.components.schemas.Problem.properties.type").exists())
                .andExpect(jsonPath("$.components.schemas.Problem.properties.errors").exists())
                .andExpect(jsonPath("$.components.schemas.Problem.required", containsInAnyOrder("type", "title", "status")));
    }

    @Test
    void documentsMoneyAsAString() throws Exception {
        docs().andExpect(jsonPath("$.components.schemas.SampleResponse.properties.balance.type").value("string"))
                .andExpect(jsonPath("$.components.schemas.SampleResponse.properties.balance.example").value("150.00"));
    }

    @Test
    void onlyDocumentsTheBusinessApi() throws Exception {
        docs().andExpect(jsonPath("$.paths['" + PROTECTED_PATH + "']").exists())
                .andExpect(jsonPath("$.paths['" + OUTSIDE_API_PATH + "']").doesNotExist())
                .andExpect(content().string(not(containsString("/actuator"))));
    }

    @Test
    void servesTheSwaggerUiWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/swagger-ui/index.html")));
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("swagger-ui")));
    }

    private ResultActions docs() throws Exception {
        return mockMvc.perform(get("/v3/api-docs"));
    }

    private static String responsesOf(String path, String method) {
        return "$.paths['" + path + "']." + method + ".responses";
    }

    /** Response codes in document order, to check both the set and the sorting. */
    private List<String> responseCodes(String path, String method) throws Exception {
        String body = docs().andReturn().getResponse().getContentAsString();
        JsonNode responses = objectMapper.readTree(body).path("paths").path(path).path(method).path("responses");
        List<String> codes = new ArrayList<>();
        responses.fieldNames().forEachRemaining(codes::add);
        return codes;
    }

    private static org.assertj.core.api.ListAssert<String> assertThat(List<String> codes) {
        return org.assertj.core.api.Assertions.assertThat(codes);
    }

    record SampleResponse(Money balance) {
    }

    record SampleRequest(String token) {
    }

    @RestController
    static class SampleController {

        @GetMapping(PROTECTED_PATH)
        SampleResponse protectedSample() {
            return new SampleResponse(Money.of("150"));
        }

        @PostMapping(PUBLIC_PATH)
        @SecurityRequirements
        @ApiProblem(status = 400, type = "invalid-token", description = "Token desconhecido")
        @ApiProblem(status = 422, type = "insufficient-balance", description = "Saldo menor que o valor")
        SampleResponse publicSample(@RequestBody SampleRequest request) {
            return new SampleResponse(Money.of("1"));
        }

        @GetMapping(OUTSIDE_API_PATH)
        String outside() {
            return "outside";
        }
    }
}
