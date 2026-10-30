package com.portwallet.shared.openapi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portwallet.shared.money.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Assertions shared by every profile that exposes the API docs. */
abstract class OpenApiDocsContract {

    static final String SAMPLE_PATH = "/api/v1/test-docs/sample";
    static final String OUTSIDE_API_PATH = "/mock/test-docs/sample";

    @Autowired
    MockMvc mockMvc;

    @Test
    void publishesAnOpenApi31DocumentWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value(startsWith("3.1")))
                .andExpect(jsonPath("$.info.title").value("PortWallet API"))
                .andExpect(jsonPath("$.servers[0].url").value("/"));
    }

    @Test
    void declaresBearerAuthenticationForEveryOperation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.security[0].bearerAuth").isArray());
    }

    @Test
    void documentsCommonErrorsAsProblemDetails() throws Exception {
        String responses = "$.paths['" + SAMPLE_PATH + "'].get.responses";
        String problemRef = "#/components/schemas/Problem";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas.Problem.properties.type").exists())
                .andExpect(jsonPath("$.components.schemas.Problem.properties.errors").exists())
                .andExpect(jsonPath(responses + "['400'].content['application/problem+json'].schema.$ref")
                        .value(problemRef))
                .andExpect(jsonPath(responses + "['401'].content['application/problem+json'].schema.$ref")
                        .value(problemRef))
                .andExpect(jsonPath(responses + "['500'].content['application/problem+json'].schema.$ref")
                        .value(problemRef));
    }

    @Test
    void documentsMoneyAsAString() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas.SampleResponse.properties.balance.type").value("string"))
                .andExpect(jsonPath("$.components.schemas.SampleResponse.properties.balance.example").value("150.00"));
    }

    @Test
    void onlyDocumentsTheBusinessApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['" + SAMPLE_PATH + "']").exists())
                .andExpect(jsonPath("$.paths['" + OUTSIDE_API_PATH + "']").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("/actuator"))));
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

    record SampleResponse(Money balance) {
    }

    @RestController
    static class SampleController {

        @GetMapping(SAMPLE_PATH)
        SampleResponse sample() {
            return new SampleResponse(Money.of("150"));
        }

        @GetMapping(OUTSIDE_API_PATH)
        String outside() {
            return "outside";
        }
    }
}
