package com.portwallet.shared.error;

import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

/** Factory for the generic problem types of the catalog in {@code docs/conventions.md}. */
public final class Problems {

    public static final String VALIDATION_ERROR = "validation-error";
    public static final String UNAUTHORIZED = "unauthorized";
    public static final String FORBIDDEN = "forbidden";
    public static final String NOT_FOUND = "not-found";
    public static final String METHOD_NOT_ALLOWED = "method-not-allowed";
    public static final String NOT_ACCEPTABLE = "not-acceptable";
    public static final String UNSUPPORTED_MEDIA_TYPE = "unsupported-media-type";
    public static final String INTERNAL_ERROR = "internal-error";

    private Problems() {
    }

    public static ProblemDetail of(HttpStatusCode status, String type, String title) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create(type));
        problem.setTitle(title);
        return problem;
    }

    public static ProblemDetail validation(List<FieldError> errors) {
        ProblemDetail problem = of(HttpStatusCode.valueOf(400), VALIDATION_ERROR, "Dados inválidos");
        problem.setProperty("errors", errors);
        return problem;
    }

    public static ProblemDetail unauthorized() {
        return of(HttpStatusCode.valueOf(401), UNAUTHORIZED, "Autenticação necessária");
    }

    public static ProblemDetail forbidden() {
        return of(HttpStatusCode.valueOf(403), FORBIDDEN, "Acesso negado");
    }

    public static ProblemDetail internalError() {
        return of(HttpStatusCode.valueOf(500), INTERNAL_ERROR, "Erro inesperado");
    }

    /** Maps a status raised by the framework itself to a generic problem of the catalog. */
    public static ProblemDetail forStatus(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> validation(List.of());
            case 401 -> unauthorized();
            case 403 -> forbidden();
            case 404 -> of(status, NOT_FOUND, "Recurso não encontrado");
            case 405 -> of(status, METHOD_NOT_ALLOWED, "Método não permitido");
            case 406 -> of(status, NOT_ACCEPTABLE, "Formato de resposta não suportado");
            case 415 -> of(status, UNSUPPORTED_MEDIA_TYPE, "Formato de requisição não suportado");
            default -> status.is4xxClientError()
                    ? of(status, VALIDATION_ERROR, "Requisição inválida")
                    : of(status, INTERNAL_ERROR, "Erro inesperado");
        };
    }

    public record FieldError(String field, String message) {
    }
}
