package com.portwallet.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portwallet.shared.error.ProblemTitles;
import com.portwallet.shared.error.Problems;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.LocaleResolver;

/**
 * Writes 401 and 403 raised by Spring Security in the same Problem Details format as the controllers. These run in
 * the filter chain, before DispatcherServlet, so the language is resolved here with the same LocaleResolver.
 */
class ProblemSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final ProblemTitles titles;
    private final LocaleResolver localeResolver;

    ProblemSecurityHandlers(ObjectMapper objectMapper, ProblemTitles titles, LocaleResolver localeResolver) {
        this.objectMapper = objectMapper;
        this.titles = titles;
        this.localeResolver = localeResolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        write(request, response, Problems.unauthorized());
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        write(request, response, Problems.forbidden());
    }

    private void write(HttpServletRequest request, HttpServletResponse response, ProblemDetail problem)
            throws IOException {
        problem.setInstance(URI.create(request.getRequestURI()));
        titles.apply(problem, localeResolver.resolveLocale(request));
        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
