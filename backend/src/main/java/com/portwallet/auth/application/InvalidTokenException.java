package com.portwallet.auth.application;

import com.portwallet.shared.error.ApiException;
import org.springframework.http.HttpStatus;

/** Unknown, expired or already used token. One message for all cases, so nothing is revealed about the token. */
public class InvalidTokenException extends ApiException {

    public InvalidTokenException() {
        super(HttpStatus.BAD_REQUEST, "invalid-token");
    }
}
