package com.portwallet.auth.application;

import com.portwallet.shared.error.ApiException;
import org.springframework.http.HttpStatus;

/** Missing, unknown, expired or revoked refresh token, or a user who can no longer sign in. */
public class InvalidRefreshTokenException extends ApiException {

    public InvalidRefreshTokenException() {
        super(HttpStatus.UNAUTHORIZED, "invalid-refresh-token");
    }
}
