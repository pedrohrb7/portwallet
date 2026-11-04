package com.portwallet.auth.api;

import com.portwallet.auth.application.AuthSession;
import com.portwallet.auth.application.ConfirmationService;
import com.portwallet.auth.application.InvalidRefreshTokenException;
import com.portwallet.auth.application.LoginService;
import com.portwallet.auth.application.RefreshTokenService;
import com.portwallet.auth.application.RegistrationService;
import com.portwallet.shared.i18n.Language;
import com.portwallet.shared.openapi.ApiProblem;
import com.portwallet.shared.validation.MaxUtf8Bytes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação")
class AuthController {

    /** Example of the registration message in the docs; the real one follows the language of the request. */
    private static final String REGISTER_MESSAGE_EXAMPLE =
            "Se os dados estiverem corretos, você receberá um e-mail para ativar a conta.";
    private static final String TOKEN_DESCRIPTION = "Token do link de confirmação (o que vem depois de `#token=`)";
    private static final String TOKEN_EXAMPLE = "q2VxN3V0c2Ftb3BsZS10b2tlbi1kZS0zMi1ieXRlcw";

    private final RegistrationService registration;
    private final ConfirmationService confirmation;
    private final LoginService login;
    private final RefreshTokenService refreshTokens;
    private final MessageSource messages;
    private final Clock clock;

    AuthController(RegistrationService registration, ConfirmationService confirmation, LoginService login,
            RefreshTokenService refreshTokens, MessageSource messages, Clock clock) {
        this.registration = registration;
        this.confirmation = confirmation;
        this.login = login;
        this.refreshTokens = refreshTokens;
        this.messages = messages;
        this.clock = clock;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @SecurityRequirements
    @Operation(
            summary = "Cadastra um usuário e envia o link de confirmação",
            description = "A resposta é sempre a mesma, exista ou não o e-mail ou o CPF, para não revelar contas "
                    + "cadastradas. Só o dono dos dados descobre o que aconteceu, por e-mail.")
    @ApiResponse(responseCode = "202", description = "Pedido recebido; a mesma resposta em todos os casos")
    RegisterResponse register(@Valid @RequestBody RegisterRequest request, Locale locale) {
        registration.register(request.toCommand(Language.of(locale)));
        // Same message in every case for a given language, so it reveals nothing about existing accounts.
        return new RegisterResponse(messages.getMessage("auth.register.accepted", null, locale));
    }

    @PostMapping("/confirm/summary")
    @SecurityRequirements
    @Operation(
            summary = "Mostra os dados mascarados do cadastro a que o link de confirmação pertence",
            description = "Não consome o link. Serve para o dono do e-mail conferir que os dados são os dele antes de "
                    + "ativar: se alguém refez o cadastro com outros dados, o link mais recente mostra esses dados. "
                    + "O token vai no corpo, e não na URL, para não aparecer em logs de acesso.")
    @ApiResponse(responseCode = "200", description = "Dados do cadastro, mascarados")
    @ApiProblem(status = 400, type = "invalid-token", description = "Token desconhecido, expirado ou já usado")
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
    @ApiProblem(status = 400, type = "invalid-token", description = "Token desconhecido, expirado ou já usado")
    ConfirmResponse confirm(@Valid @RequestBody ConfirmRequest request) {
        confirmation.confirm(request.token(), request.password());
        return new ConfirmResponse("ACTIVE");
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(
            summary = "Entra com e-mail e senha",
            description = "Devolve o access token (JWT, 15 minutos) no corpo e o refresh token no cookie httpOnly "
                    + "`refresh_token`, que vale 7 dias. E-mail desconhecido, senha errada e conta ainda não ativada "
                    + "recebem a mesma resposta, para não revelar contas cadastradas.")
    @ApiResponse(responseCode = "200", description = "Sessão aberta; o refresh token vai no header `Set-Cookie`")
    @ApiProblem(status = 401, type = "invalid-credentials",
            description = "E-mail desconhecido, senha errada ou conta ainda não ativada")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return withSession(login.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(
            summary = "Renova o access token com o refresh token do cookie",
            description = "Revoga o refresh token usado e emite outro da mesma família, que mantém a expiração do "
                    + "login: renovar não estende a sessão além de 7 dias. Um token já usado não funciona de novo.")
    @ApiResponse(responseCode = "200", description = "Sessão renovada; o novo refresh token vai no header `Set-Cookie`")
    @ApiProblem(status = 401, type = "invalid-refresh-token",
            description = "Cookie ausente, ou refresh token desconhecido, expirado ou já usado; o cookie é apagado")
    ResponseEntity<AuthResponse> refresh(
            @Parameter(hidden = true)
            @CookieValue(name = RefreshCookie.NAME, required = false) String refreshToken) {
        return withSession(refreshTokens.rotate(refreshToken));
    }

    /** A refused refresh also drops the cookie, so the browser stops sending a token that no longer works. */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    ResponseEntity<ProblemDetail> invalidRefreshToken(InvalidRefreshTokenException ex) {
        return ResponseEntity.status(ex.status())
                .header(HttpHeaders.SET_COOKIE, RefreshCookie.cleared().toString())
                .body(ex.toProblemDetail());
    }

    private ResponseEntity<AuthResponse> withSession(AuthSession session) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        RefreshCookie.of(session.refreshToken(), session.refreshTokenExpiresAt(), clock.instant())
                                .toString())
                .body(AuthResponse.from(session));
    }

    record RegisterResponse(
            @Schema(example = REGISTER_MESSAGE_EXAMPLE)
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
