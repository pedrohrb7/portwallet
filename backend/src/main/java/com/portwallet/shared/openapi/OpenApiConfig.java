package com.portwallet.shared.openapi;

import com.portwallet.shared.money.Money;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI document of the business API. Only active where springdoc is enabled (dev and demo profiles). */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
public class OpenApiConfig {

    static final String BEARER_AUTH = "bearerAuth";
    static final String PROBLEM_SCHEMA = "Problem";
    static final String PROBLEM_MEDIA_TYPE = "application/problem+json";

    /** Errors any operation can return; each feature documents its specific ones (403, 404, 409, 422). */
    private static final Map<String, String> COMMON_ERRORS = Map.of(
            "400", "Dados inválidos (`validation-error`)",
            "401", "Token ausente, inválido ou expirado (`unauthorized`)",
            "500", "Erro inesperado (`internal-error`)");

    static {
        // Money is serialized as a string with two decimal places (ADR 0004), not as an object.
        SpringDocUtils.getConfig().replaceWithSchema(Money.class, new StringSchema()
                .pattern("^-?\\d+\\.\\d{2}$")
                .example("150.00")
                .description("Valor em BRL com duas casas decimais"));
    }

    @Bean
    OpenAPI portWalletOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("PortWallet API")
                        .version("v1")
                        .description("""
                                Carteira digital de portfólio. Ambiente de demonstração, sem dinheiro real.

                                Erros seguem Problem Details (RFC 9457) com um `type` estável. Valores monetários \
                                são strings com duas casas decimais. Para chamar endpoints protegidos, faça login e \
                                use o `accessToken` em "Authorize"."""))
                .servers(List.of(new Server().url("/")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    /**
     * Adds the Problem schema together with the responses that reference it. Registering the schema in the
     * {@link OpenAPI} bean instead is not enough: springdoc drops schemas that nothing references yet.
     */
    @Bean
    OpenApiCustomizer commonErrorResponses() {
        return openApi -> {
            if (openApi.getPaths() == null || openApi.getPaths().isEmpty()) {
                return;
            }
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents().addSchemas(PROBLEM_SCHEMA, problemSchema());
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(this::addCommonErrors));
        };
    }

    private void addCommonErrors(Operation operation) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        COMMON_ERRORS.forEach((status, description) ->
                operation.getResponses().putIfAbsent(status, problemResponse(description)));
    }

    private static ApiResponse problemResponse(String description) {
        Schema<?> reference = new Schema<>().$ref("#/components/schemas/" + PROBLEM_SCHEMA);
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(PROBLEM_MEDIA_TYPE, new MediaType().schema(reference)));
    }

    private static Schema<?> problemSchema() {
        ObjectSchema fieldError = new ObjectSchema();
        fieldError.addProperty("field", new StringSchema().example("amount"));
        fieldError.addProperty("message", new StringSchema().example("deve ser maior que zero"));

        ObjectSchema problem = new ObjectSchema();
        problem.description("Problem Details (RFC 9457). O catálogo de `type` está em docs/convencoes.md.");
        problem.addProperty("type", new StringSchema().example("validation-error"));
        problem.addProperty("title", new StringSchema().example("Dados inválidos"));
        problem.addProperty("status", new IntegerSchema().example(400));
        problem.addProperty("detail", new StringSchema());
        problem.addProperty("instance", new StringSchema().example("/api/v1/deposits"));
        problem.addProperty("errors", new ArraySchema().items(fieldError));
        problem.setRequired(List.of("type", "title", "status"));
        return problem;
    }
}
