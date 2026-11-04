package com.portwallet.auth.application;

import com.portwallet.shared.security.JwtService.AccessToken;
import com.portwallet.shared.security.Role;
import java.time.Instant;
import java.util.UUID;

/** Result of a login or a refresh: the access token, the raw refresh token for the cookie and who signed in. */
public record AuthSession(AccessToken accessToken, String refreshToken, Instant refreshTokenExpiresAt,
        SessionUser user) {

    public record SessionUser(UUID id, String name, String email, Role role) {
    }

    @Override
    public String toString() {
        return "AuthSession[accessToken=***, refreshToken=***, refreshTokenExpiresAt=" + refreshTokenExpiresAt
                + ", user=" + user.id() + "]";
    }
}
