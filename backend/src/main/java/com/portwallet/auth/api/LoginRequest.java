package com.portwallet.auth.api;

import com.portwallet.shared.validation.MaxUtf8Bytes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Locale;

record LoginRequest(
        @Schema(example = "ana@demo.com")
        @NotBlank @Email @Size(max = 254)
        String email,

        @Schema(example = "s3nh@forte", maxLength = 72)
        @NotNull @Size(min = 1, max = 72) @MaxUtf8Bytes(72)
        String password) {

    LoginRequest {
        // Same normalization as the registration, so the e-mail matches however it is typed.
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "LoginRequest[email=***, password=***]";
    }
}
