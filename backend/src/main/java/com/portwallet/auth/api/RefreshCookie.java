package com.portwallet.auth.api;

import java.time.Duration;
import java.time.Instant;
import org.springframework.http.ResponseCookie;

/**
 * The refresh token cookie. {@code HttpOnly}, so JavaScript never reads it; {@code SameSite=Strict} and a path limited
 * to {@code /api/v1/auth}, so the browser only sends it to the endpoints that need it.
 */
final class RefreshCookie {

    static final String NAME = "refresh_token";
    static final String PATH = "/api/v1/auth";

    private RefreshCookie() {
    }

    /**
     * Cookie that lives until the session expires. The remaining time is rounded up to whole seconds, so the few
     * milliseconds between issuing the token and building the response do not cut a second off the session.
     */
    static ResponseCookie of(String token, Instant expiresAt, Instant now) {
        Duration remaining = Duration.between(now, expiresAt);
        if (remaining.isNegative()) {
            return base(token).maxAge(Duration.ZERO).build();
        }
        long seconds = remaining.getSeconds() + (remaining.getNano() > 0 ? 1 : 0);
        return base(token).maxAge(Duration.ofSeconds(seconds)).build();
    }

    /** Tells the browser to drop the cookie. */
    static ResponseCookie cleared() {
        return base("").maxAge(Duration.ZERO).build();
    }

    private static ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(PATH);
    }
}
