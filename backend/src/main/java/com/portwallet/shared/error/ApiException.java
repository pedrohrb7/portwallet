package com.portwallet.shared.error;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Base class for business errors that map to a Problem Details response with a stable {@code type}.
 * The message is never sent to the client; only {@code type}, {@code title} and the extra properties are.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String title;

    protected ApiException(HttpStatus status, String type, String title) {
        super(type);
        this.status = status;
        this.type = type;
        this.title = title;
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
        problem.setTitle(title);
        customize(problem);
        return problem;
    }

    /** Hook for subclasses that add properties to the problem, such as {@code availableToday}. */
    protected void customize(ProblemDetail problem) {
    }
}
