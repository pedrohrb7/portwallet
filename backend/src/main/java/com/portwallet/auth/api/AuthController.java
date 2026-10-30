package com.portwallet.auth.api;

import com.portwallet.auth.application.ConfirmationService;
import com.portwallet.auth.application.RegistrationService;
import com.portwallet.shared.openapi.ApiProblem;
import com.portwallet.shared.validation.MaxUtf8Bytes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação")
class AuthController {

    static final String REGISTER_MESSAGE =
            "Se os dados estiverem corretos, você receberá um e-mail para ativar a conta.";

    private final RegistrationService registration;
    private final ConfirmationService confirmation;

    AuthController(RegistrationService registration, ConfirmationService confirmation) {
        this.registration = registration;
        this.confirmation = confirmation;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @SecurityRequirements
    @Operation(
            summary = "Cadastra um usuário e envia o link de confirmação",
            description = "A resposta é sempre a mesma, exista ou não o e-mail ou o CPF, para não revelar contas "
                    + "cadastradas. Só o dono dos dados descobre o que aconteceu, por e-mail.")
    @ApiResponse(responseCode = "202", description = "Pedido recebido; a mesma resposta em todos os casos")
    RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        registration.register(request.toCommand());
        return new RegisterResponse(REGISTER_MESSAGE);
    }

    @PostMapping("/confirm")
    @SecurityRequirements
    @Operation(
            summary = "Ativa a conta com o link de confirmação e a senha do cadastro, e abre a carteira",
            description = "A senha precisa ser a do cadastro mais recente desse e-mail. Assim, só quem controla o "
                    + "e-mail e conhece a senha ativa a conta.")
    @ApiResponse(responseCode = "200", description = "Conta ativada")
    @ApiProblem(status = 400, type = "invalid-token", title = "Link inválido ou expirado",
            description = "Token desconhecido, expirado ou já usado")
    @ApiProblem(status = 422, type = "wrong-password", title = "Senha incorreta",
            description = "Senha diferente da do cadastro mais recente; o link continua válido")
    ConfirmResponse confirm(@Valid @RequestBody ConfirmRequest request) {
        confirmation.confirm(request.token(), request.password());
        return new ConfirmResponse("ACTIVE");
    }

    record RegisterResponse(
            @Schema(example = REGISTER_MESSAGE)
            String message) {
    }

    record ConfirmRequest(
            @Schema(description = "Token do link de confirmação (o que vem depois de `#token=`)",
                    example = "q2VxN3V0c2Ftb3BsZS10b2tlbi1kZS0zMi1ieXRlcw")
            @NotBlank @Size(max = 100)
            String token,

            @Schema(description = "Senha escolhida no cadastro mais recente", example = "s3nh@forte")
            @NotNull @Size(max = 72) @MaxUtf8Bytes(72)
            String password) {

        @Override
        public String toString() {
            return "ConfirmRequest[token=***, password=***]";
        }
    }

    record ConfirmResponse(
            @Schema(example = "ACTIVE")
            String status) {
    }
}
