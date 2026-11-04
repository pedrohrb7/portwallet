package com.portwallet.auth.application;

import com.portwallet.shared.error.ApiException;
import org.springframework.http.HttpStatus;

/** Unknown e-mail, wrong password or account not yet activated. One answer for all, so no account is revealed. */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "invalid-credentials");
    }
}
