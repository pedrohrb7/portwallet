package com.portwallet.shared.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues HS256 access tokens. Validation happens in the resource server configured by {@link SecurityConfig}. */
@Service
public class JwtService {

    static final String ROLE_CLAIM = "role";

    private final JwtEncoder encoder;
    private final Clock clock;
    private final Duration ttl;

    JwtService(JwtEncoder encoder, Clock clock, JwtProperties properties) {
        this.encoder = encoder;
        this.clock = clock;
        this.ttl = properties.accessTokenTtl();
    }

    public AccessToken issue(UUID userId, Role role) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(value, ttl.toSeconds());
    }

    public record AccessToken(String value, long expiresInSeconds) {
    }
}
