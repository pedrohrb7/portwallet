package com.portwallet.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.portwallet.PortWalletApplication;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

class JwtSecretRequiredTest {

    @Test
    void applicationDoesNotStartWithoutSecretOutsideDevelopmentProfiles() {
        SpringApplicationBuilder app = new SpringApplicationBuilder(PortWalletApplication.class);
        assertThatThrownBy(() -> app.run("--spring.profiles.active=production-like", "--server.port=0"))
                .rootCause()
                .hasMessageContaining("JWT_SECRET must be set");
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtProperties("too-short", null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void defaultsAccessTokenTtlToFifteenMinutes() {
        JwtProperties properties = new JwtProperties("a-secret-with-at-least-32-bytes-ok", null);
        assertThat(properties.accessTokenTtl()).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void neverPrintsTheSecret() {
        JwtProperties properties = new JwtProperties("a-secret-with-at-least-32-bytes-ok", null);
        assertThat(properties.toString()).doesNotContain("a-secret");
    }
}
