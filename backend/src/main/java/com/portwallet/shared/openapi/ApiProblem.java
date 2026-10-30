package com.portwallet.shared.openapi;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documents a business error an endpoint can return, as Problem Details with a stable {@code type}. Generic errors
 * (validation, authentication, unexpected) are added by {@link OpenApiConfig} and do not need to be declared.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ApiProblem.List.class)
public @interface ApiProblem {

    int status();

    /** The {@code type} of the catalog in docs/convencoes.md. */
    String type();

    /** The {@code title} the API sends for this type. */
    String title();

    /** When it happens, for the reader of the docs. */
    String description();

    @Documented
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        ApiProblem[] value();
    }
}
