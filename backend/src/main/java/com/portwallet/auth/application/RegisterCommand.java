package com.portwallet.auth.application;

import com.portwallet.auth.domain.Cpf;
import com.portwallet.shared.i18n.Language;

/**
 * Validated registration data. {@code email} is already normalized (trimmed, lower case); {@code language} is the
 * language of the request, used for every e-mail sent to this user.
 */
public record RegisterCommand(String name, String email, Cpf cpf, Language language) {

    @Override
    public String toString() {
        return "RegisterCommand[name=" + name + ", email=***, cpf=" + cpf + ", language=" + language + "]";
    }
}
