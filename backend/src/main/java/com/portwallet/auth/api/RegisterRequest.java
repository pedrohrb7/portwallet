package com.portwallet.auth.api;

import com.portwallet.auth.application.RegisterCommand;
import com.portwallet.auth.domain.Cpf;
import com.portwallet.shared.validation.MaxUtf8Bytes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record RegisterRequest(
        @Schema(example = "Ana Souza")
        @NotBlank @Size(min = 2, max = 100)
        String name,

        @Schema(example = "ana@demo.com")
        @NotBlank @Email @Size(max = 254)
        String email,

        @Schema(description = "CPF fictício, com ou sem máscara", example = "529.982.247-25")
        @NotNull @ValidCpf
        String document,

        @Schema(example = "s3nh@forte", minLength = 8, maxLength = 72)
        @NotNull @Size(min = 8, max = 72) @MaxUtf8Bytes(72)
        String password) {

    /** Normalizes before validation runs: trimmed name, trimmed lower-case e-mail. */
    public RegisterRequest {
        name = name == null ? null : name.strip().replaceAll("\\s+", " ");
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
        document = document == null ? null : document.strip();
    }

    RegisterCommand toCommand() {
        return new RegisterCommand(name, email, Cpf.parse(document).orElseThrow(), password);
    }

    @Override
    public String toString() {
        return "RegisterRequest[name=" + name + ", email=***, document=***, password=***]";
    }
}
