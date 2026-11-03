package com.portwallet.shared.error;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Fills the title of every Problem Details body written by Spring MVC, in the language of the request. One place for
 * every path: business errors, framework errors and the handlers in {@link GlobalExceptionHandler}.
 */
@RestControllerAdvice
class ProblemTitleAdvice implements ResponseBodyAdvice<Object> {

    private final ProblemTitles titles;

    ProblemTitleAdvice(ProblemTitles titles) {
        this.titles = titles;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        if (body instanceof ProblemDetail problem) {
            // DispatcherServlet has already set the locale of the request from the LocaleResolver.
            titles.apply(problem, LocaleContextHolder.getLocale());
        }
        return body;
    }
}
