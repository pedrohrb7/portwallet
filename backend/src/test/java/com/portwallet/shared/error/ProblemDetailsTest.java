package com.portwallet.shared.error;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portwallet.shared.money.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ProblemDetailsTest.ThrowingController.class)
class ProblemDetailsTest {

    private static final String BASE = "/api/v1/test-errors";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void beanValidationFailureListsTheFields() throws Exception {
        mockMvc.perform(authenticated(post(BASE + "/validated"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("validation-error"))
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value(BASE + "/validated"))
                .andExpect(jsonPath("$.errors[0].field").value("amount"))
                .andExpect(jsonPath("$.errors[1].field").value("name"));
    }

    @Test
    void malformedJsonIsAValidationError() throws Exception {
        mockMvc.perform(authenticated(post(BASE + "/validated"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("body"));
    }

    @Test
    void moneyWithMoreThanTwoDecimalPlacesPointsToTheField() throws Exception {
        mockMvc.perform(authenticated(post(BASE + "/validated"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ok\",\"amount\":\"1.001\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("amount"));
    }

    @Test
    void parameterOfTheWrongTypeIsAValidationError() throws Exception {
        mockMvc.perform(authenticated(get(BASE + "/items/not-a-uuid")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("id"));
    }

    @Test
    void missingRequiredParameterIsAValidationError() throws Exception {
        mockMvc.perform(authenticated(get(BASE + "/search")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("query"));
    }

    @Test
    void invalidParameterConstraintIsAValidationError() throws Exception {
        mockMvc.perform(authenticated(get(BASE + "/search").param("query", "ab")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("query"));
    }

    @Test
    void apiExceptionKeepsItsTypeAndExtraProperties() throws Exception {
        mockMvc.perform(authenticated(get(BASE + "/business")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("insufficient-balance"))
                .andExpect(jsonPath("$.title").value("Saldo insuficiente"))
                .andExpect(jsonPath("$.availableToday").value("10.00"));
    }

    @Test
    void unexpectedErrorHidesInternals() throws Exception {
        mockMvc.perform(authenticated(get(BASE + "/boom")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("internal-error"))
                .andExpect(jsonPath("$.detail").doesNotExist())
                .andExpect(content().string(not(containsString("secret-internal-detail"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void unknownRouteIsNotFound() throws Exception {
        mockMvc.perform(authenticated(get("/api/v1/does-not-exist")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("not-found"));
    }

    @Test
    void unsupportedMethodIsMethodNotAllowed() throws Exception {
        mockMvc.perform(authenticated(delete(BASE + "/business")))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.type").value("method-not-allowed"));
    }

    @Test
    void unsupportedContentTypeIsReported() throws Exception {
        mockMvc.perform(authenticated(post(BASE + "/validated"))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.type").value("unsupported-media-type"));
    }

    private static MockHttpServletRequestBuilder authenticated(MockHttpServletRequestBuilder request) {
        return request.with(jwt());
    }

    record ValidatedRequest(@NotNull Money amount, @NotNull @Size(min = 1) String name) {
    }

    static class SampleBusinessException extends ApiException {
        SampleBusinessException() {
            super(HttpStatus.UNPROCESSABLE_ENTITY, "insufficient-balance", "Saldo insuficiente");
        }

        @Override
        protected void customize(ProblemDetail problem) {
            problem.setProperty("availableToday", Money.of("10"));
        }
    }

    @RestController
    static class ThrowingController {

        @PostMapping(BASE + "/validated")
        void validated(@Valid @RequestBody ValidatedRequest request) {
        }

        @GetMapping(BASE + "/items/{id}")
        UUID item(@PathVariable UUID id) {
            return id;
        }

        @GetMapping(BASE + "/search")
        String search(@RequestParam @Size(min = 3) String query) {
            return query;
        }

        @GetMapping(BASE + "/business")
        void business() {
            throw new SampleBusinessException();
        }

        @GetMapping(BASE + "/boom")
        void boom() {
            throw new IllegalStateException("secret-internal-detail");
        }
    }
}
