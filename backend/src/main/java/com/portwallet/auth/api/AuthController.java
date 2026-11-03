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
    private static final String TOKEN_DESCRIPTION = "Token do link de confirmação (o que vem depois de `#token=`)";
    private static final String TOKEN_EXAMPLE = "q2VxN3V0c2Ftb3BsZS10b2tlbi1kZS0zMi1ieXRlcw";

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

    @PostMapping("/confirm/summary")
    @SecurityRequirements
    @Operation(
            summary = "Mostra os dados mascarados do cadastro a que o link de confirmação pertence",
            description = "Não consome o link. Serve para o dono do e-mail conferir que os dados são os dele antes de "
                    + "ativar: se alguém refez o cadastro com outros dados, o link mais recente mostra esses dados. "
                    + "O token vai no corpo, e não na URL, para não aparecer em logs de acesso.")
    @ApiResponse(responseCode = "200", description = "Dados do cadastro, mascarados")
    @ApiProblem(status = 400, type = "invalid-token", title = "Link inválido ou expirado",
            description = "Token desconhecido, expirado ou já usado")
    SummaryResponse summary(@Valid @RequestBody SummaryRequest request) {
        ConfirmationService.RegistrationSummary summary = confirmation.summary(request.token());
        return new SummaryResponse(summary.maskedName(), summary.maskedDocument());
    }

    @PostMapping("/confirm")
    @SecurityRequirements
    @Operation(
            summary = "Ativa a conta com o link de confirmação, definindo a senha, e abre a carteira",
            description = "Só quem tem o link, e portanto acesso ao e-mail, escolhe a senha. Senha gravada, usuário "
                    + "ativado, link consumido e conta aberta acontecem na mesma transação.")
    @ApiResponse(responseCode = "200", description = "Conta ativada")
    @ApiProblem(status = 400, type = "invalid-token", title = "Link inválido ou expirado",
            description = "Token desconhecido, expirado ou já usado")
    ConfirmResponse confirm(@Valid @RequestBody ConfirmRequest request) {
        confirmation.confirm(request.token(), request.password());
        return new ConfirmResponse("ACTIVE");
    }

    record RegisterResponse(
            @Schema(example = REGISTER_MESSAGE)
            String message) {
    }

    record SummaryRequest(
            @Schema(description = TOKEN_DESCRIPTION, example = TOKEN_EXAMPLE)
            @NotBlank @Size(max = 100)
            String token) {

        @Override
        public String toString() {
            return "SummaryRequest[token=***]";
        }
    }

    record SummaryResponse(
            @Schema(description = "Primeiro nome e inicial do último sobrenome", example = "Ana S****")
            String maskedName,

            @Schema(description = "Só os dígitos verificadores do CPF", example = "***.***.***-25")
            String maskedDocument) {
    }

    record ConfirmRequest(
            @Schema(description = TOKEN_DESCRIPTION, example = TOKEN_EXAMPLE)
            @NotBlank @Size(max = 100)
            String token,

            @Schema(description = "Senha da conta, escolhida agora", example = "s3nh@forte", minLength = 8,
                    maxLength = 72)
            @NotNull @Size(min = 8, max = 72) @MaxUtf8Bytes(72)
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
