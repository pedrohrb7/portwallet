package com.portwallet.auth.application;

import com.portwallet.auth.domain.Cpf;

/** Validated registration data. {@code email} is already normalized (trimmed, lower case). */
public record RegisterCommand(String name, String email, Cpf cpf, String password) {

    @Override
    public String toString() {
        return "RegisterCommand[name=" + name + ", email=***, cpf=" + cpf + ", password=***]";
    }
}
