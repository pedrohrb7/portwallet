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
        // Crypto keys are provided so that the missing JWT secret is the only problem.
        assertThatThrownBy(() -> app.run("--spring.profiles.active=production-like", "--server.port=0",
                        "--portwallet.crypto.encryption-key=CofGSDsX497EScRUIT4I7b9wYzLr8TpTa1OV+rerNT0=",
                        "--portwallet.crypto.blind-index-key=21ONoO2JoA/oTp0Mws+VCh4rLMokzUTNQnUA07D+c8c="))
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
