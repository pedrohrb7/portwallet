package com.portwallet.shared.security;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * The user behind the current request, read from the access token. Controllers receive it as a method parameter.
 */
public record AuthenticatedUser(UUID id, Role role) {

    static AuthenticatedUser from(Jwt jwt) {
        return new AuthenticatedUser(
                UUID.fromString(jwt.getSubject()), Role.valueOf(jwt.getClaimAsString(JwtService.ROLE_CLAIM)));
    }
}
