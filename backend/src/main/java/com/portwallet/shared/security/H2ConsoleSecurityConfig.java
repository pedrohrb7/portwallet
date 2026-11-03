package com.portwallet.shared.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Opens the H2 console where it is enabled ({@code spring.h2.console.enabled}, the dev profile only).
 *
 * <p>A separate chain, matched before the API chain, keeps the console's needs away from the API:
 * the console has its own login (JDBC URL, user and password), posts forms without a CSRF token and
 * renders in frames, so it needs {@code X-Frame-Options: SAMEORIGIN} instead of {@code DENY}. Where the
 * console is disabled this chain does not exist and {@code /h2-console} falls to the API chain (401).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.h2.console", name = "enabled", havingValue = "true")
class H2ConsoleSecurityConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(PathRequest.toH2Console())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .build();
    }
}
