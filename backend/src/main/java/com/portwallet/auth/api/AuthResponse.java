package com.portwallet.auth.api;

import com.portwallet.auth.application.AuthSession;
import com.portwallet.shared.security.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Body of a login or refresh. The refresh token never appears here: it only travels in the httpOnly cookie. */
record AuthResponse(
        @Schema(description = "Access token (JWT), enviado em `Authorization: Bearer`",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxYjdlIn0.c2lnbmF0dXJl")
        String accessToken,

        @Schema(description = "Segundos até o access token expirar", example = "900")
        long expiresIn,

        UserView user) {

    static AuthResponse from(AuthSession session) {
        AuthSession.SessionUser user = session.user();
        return new AuthResponse(
                session.accessToken().value(),
                session.accessToken().expiresInSeconds(),
                new UserView(user.id(), user.name(), user.email(), user.role()));
    }

    record UserView(
            @Schema(example = "1b7e4f3a-9c2d-4e8b-a1f0-5d6c7b8a9e0f")
            UUID id,

            @Schema(example = "Ana Souza")
            String name,

            @Schema(example = "ana@demo.com")
            String email,

            @Schema(example = "USER")
            Role role) {
    }

    @Override
    public String toString() {
        return "AuthResponse[accessToken=***, expiresIn=" + expiresIn + ", user=" + user.id() + "]";
    }
}
