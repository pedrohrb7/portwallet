package com.portwallet.auth.application;

import com.portwallet.shared.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * The password typed to activate the account is not the one of the latest registration. Only who holds the link sent
 * to the e-mail can get this answer, so it reveals nothing to someone without access to the inbox.
 */
public class WrongPasswordException extends ApiException {

    public WrongPasswordException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "wrong-password", "Senha incorreta");
    }
}
