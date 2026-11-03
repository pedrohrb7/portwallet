package com.portwallet.shared.error;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Base class for business errors that map to a Problem Details response with a stable {@code type}.
 * The message is never sent to the client; only {@code type}, {@code title} and the extra properties are. The title
 * comes from the {@code type} in the language of the request ({@link ProblemTitles}).
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String type;

    protected ApiException(HttpStatus status, String type) {
        super(type);
        this.status = status;
        this.type = type;
    }

    public HttpStatus status() {
        return status;
    }

    public String type() {
        return type;
    }

    public ProblemDetail toProblemDetail() {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create(type));
        customize(problem);
        return problem;
    }

    /** Hook for subclasses that add properties to the problem, such as {@code availableToday}. */
    protected void customize(ProblemDetail problem) {
    }
}
