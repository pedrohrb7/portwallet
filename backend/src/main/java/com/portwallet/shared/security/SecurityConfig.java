package com.portwallet.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.portwallet.shared.error.ProblemTitles;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig implements WebMvcConfigurer {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper, ProblemTitles titles,
            LocaleResolver localeResolver) throws Exception {
        ProblemSecurityHandlers problemHandlers = new ProblemSecurityHandlers(objectMapper, titles, localeResolver);
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").hasRole(Role.ADMIN.name())
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/confirm",
                                "/api/v1/auth/confirm/summary", "/api/v1/auth/login", "/api/v1/auth/refresh")
                        .permitAll()
                        // Demo inbox: only exists where portwallet.mock.inbox.enabled is true; 404 elsewhere.
                        .requestMatchers(HttpMethod.GET, "/mock/inbox").permitAll()
                        // API docs only exist in the dev and demo profiles; elsewhere these routes are 404.
                        .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui", "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(SecurityConfig::toAuthentication))
                        .authenticationEntryPoint(problemHandlers)
                        .accessDeniedHandler(problemHandlers))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemHandlers)
                        .accessDeniedHandler(problemHandlers))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(properties.secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(claimsValidator());
        return decoder;
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(properties.secretKey()));
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AuthenticatedUserArgumentResolver());
    }

    /** Maps the {@code role} claim, already checked by {@link #claimsValidator()}, to a {@code ROLE_*} authority. */
    private static AbstractAuthenticationToken toAuthentication(Jwt jwt) {
        String role = Role.valueOf(jwt.getClaimAsString(JwtService.ROLE_CLAIM)).authority();
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(role)), jwt.getSubject());
    }

    /** Default checks (expiry, not-before) plus a UUID subject and a known role. */
    private static OAuth2TokenValidator<Jwt> claimsValidator() {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                new JwtClaimValidator<String>(JwtClaimNames.SUB, SecurityConfig::isUuid),
                new JwtClaimValidator<String>(JwtService.ROLE_CLAIM, SecurityConfig::isRole));
    }

    private static boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }

    private static boolean isRole(String value) {
        return Arrays.stream(Role.values()).anyMatch(role -> role.name().equals(value));
    }
}
