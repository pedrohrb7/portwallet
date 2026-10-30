package com.portwallet.shared.openapi;

import com.portwallet.shared.money.Money;
import com.portwallet.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.HandlerMethod;

/** OpenAPI document of the business API. Only active where springdoc is enabled (dev and demo profiles). */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
public class OpenApiConfig {

    static final String BEARER_AUTH = "bearerAuth";
    static final String PROBLEM_SCHEMA = "Problem";
    static final String PROBLEM_MEDIA_TYPE = "application/problem+json";

    private static final ProblemDoc VALIDATION_ERROR = new ProblemDoc(400, "validation-error", "Dados inválidos",
            "Campos ou parâmetros inválidos; `errors` lista cada campo");
    private static final ProblemDoc UNAUTHORIZED = new ProblemDoc(401, "unauthorized", "Autenticação necessária",
            "Access token ausente, inválido ou expirado");
    private static final ProblemDoc INTERNAL_ERROR = new ProblemDoc(500, "internal-error", "Erro inesperado",
            "Erro inesperado; os detalhes ficam só no log");

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
     * Documents the errors of each operation: the ones it declares with {@link ApiProblem}, plus 400 when it takes
     * input, 401 when it requires a token, and 500 always. Each error gets its own example, so a 422 never shows the
     * example of a validation error.
     */
    @Bean
    OperationCustomizer problemResponses() {
        return (operation, handlerMethod) -> {
            List<ProblemDoc> problems = new ArrayList<>();
            if (takesInput(handlerMethod)) {
                problems.add(VALIDATION_ERROR);
            }
            if (!isPublic(handlerMethod)) {
                problems.add(UNAUTHORIZED);
            }
            Arrays.stream(handlerMethod.getMethod().getAnnotationsByType(ApiProblem.class))
                    .map(ProblemDoc::from)
                    .forEach(problems::add);
            problems.add(INTERNAL_ERROR);
            addProblemResponses(operation, problems);
            return operation;
        };
    }

    /** Registers the Problem schema once something references it, and lists responses by status code. */
    @Bean
    OpenApiCustomizer problemSchemaAndResponseOrder() {
        return openApi -> {
            if (openApi.getPaths() == null || openApi.getPaths().isEmpty()) {
                return;
            }
            // Added here and not in the OpenAPI bean: springdoc drops schemas that nothing references yet.
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents().addSchemas(PROBLEM_SCHEMA, problemSchema());
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(OpenApiConfig::sortResponses));
        };
    }

    private static void addProblemResponses(Operation operation, List<ProblemDoc> problems) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        Map<Integer, List<ProblemDoc>> byStatus = problems.stream()
                .collect(Collectors.groupingBy(ProblemDoc::status, TreeMap::new, Collectors.toList()));
        byStatus.forEach((status, docs) -> {
            MediaType mediaType = new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM_SCHEMA));
            docs.forEach(doc -> mediaType.addExamples(doc.type(), new Example()
                    .summary(doc.type())
                    .value(doc.example())));
            // Markdown list, rendered by the Swagger UI: one line per problem type.
            String description = docs.stream()
                    .map(doc -> "- `" + doc.type() + "`: " + doc.description())
                    .collect(Collectors.joining("\n"));
            operation.getResponses().put(String.valueOf(status), new ApiResponse()
                    .description(description)
                    .content(new Content().addMediaType(PROBLEM_MEDIA_TYPE, mediaType)));
        });
    }

    private static void sortResponses(Operation operation) {
        if (operation.getResponses() == null) {
            return;
        }
        Map<String, ApiResponse> sorted = new LinkedHashMap<>();
        operation.getResponses().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        ApiResponses responses = new ApiResponses();
        responses.putAll(sorted);
        operation.setResponses(responses);
    }

    /** Public endpoints opt out of the global bearer requirement with an empty {@code @SecurityRequirements}. */
    private static boolean isPublic(HandlerMethod handlerMethod) {
        SecurityRequirements requirements = handlerMethod.getMethodAnnotation(SecurityRequirements.class);
        return requirements != null && requirements.value().length == 0;
    }

    private static boolean takesInput(HandlerMethod handlerMethod) {
        return Arrays.stream(handlerMethod.getMethodParameters())
                .map(MethodParameter::getParameterType)
                .anyMatch(type -> !type.equals(AuthenticatedUser.class));
    }

    private static Schema<?> problemSchema() {
        ObjectSchema fieldError = new ObjectSchema();
        fieldError.addProperty("field", new StringSchema().description("Campo com problema"));
        fieldError.addProperty("message", new StringSchema().description("O que está errado"));

        ObjectSchema problem = new ObjectSchema();
        problem.description("Problem Details (RFC 9457). O catálogo de `type` está em docs/convencoes.md.");
        problem.addProperty("type", new StringSchema().description("Identificador estável do erro"));
        problem.addProperty("title", new StringSchema().description("Resumo legível do erro, em português"));
        problem.addProperty("status", new IntegerSchema().description("Status HTTP"));
        problem.addProperty("detail", new StringSchema().description("Detalhe opcional"));
        problem.addProperty("instance", new StringSchema().description("Caminho da requisição"));
        problem.addProperty("errors", new ArraySchema()
                .description("Só em `validation-error`: um item por campo inválido")
                .items(fieldError));
        problem.setRequired(List.of("type", "title", "status"));
        return problem;
    }

    record ProblemDoc(int status, String type, String title, String description) {

        static ProblemDoc from(ApiProblem annotation) {
            return new ProblemDoc(annotation.status(), annotation.type(), annotation.title(), annotation.description());
        }

        Map<String, Object> example() {
            Map<String, Object> example = new LinkedHashMap<>();
            example.put("type", type);
            example.put("title", title);
            example.put("status", status);
            if (status == 400 && type.equals(VALIDATION_ERROR.type())) {
                Map<String, String> fieldError = new LinkedHashMap<>();
                fieldError.put("field", "email");
                fieldError.put("message", "deve ser um endereço de e-mail");
                example.put("errors", List.of(fieldError));
            }
            return example;
        }
    }
}
