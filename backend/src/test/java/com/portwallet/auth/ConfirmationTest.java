package com.portwallet.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portwallet.mock.inbox.InboxStore;
import com.portwallet.mock.inbox.InboxStore.InboxMessage;
import com.portwallet.support.TestData;
import com.portwallet.support.TestUsers;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ConfirmationTest {

    private static final String PASSWORD = "s3nh@forte";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InboxStore inbox;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void summaryShowsTheMaskedDataWithoutConsumingTheLink() throws Exception {
        String cpf = TestData.validCpf();
        String email = registerPending("Ana Souza Lima", cpf);
        String token = tokenFor(email);

        summary(token)
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        { "maskedName": "Ana L****", "maskedDocument": "***.***.***-%s" }
                        """.formatted(cpf.substring(9)), true));
        summary(token).andExpect(status().isOk());

        confirm(token).andExpect(status().isOk());
    }

    @Test
    void summaryOfAnInvalidLinkIsRejected() throws Exception {
        summary("this-token-was-never-issued")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("invalid-token"))
                .andExpect(jsonPath("$.title").value("Link inválido ou expirado"));

        String token = tokenFor(registerPending());
        confirm(token).andExpect(status().isOk());
        summary(token).andExpect(status().isBadRequest()).andExpect(jsonPath("$.type").value("invalid-token"));
    }

    @Test
    void summaryWithoutTokenIsAValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/confirm/summary").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("token"));
    }

    @Test
    void confirmationSetsThePasswordActivatesTheUserAndOpensAnEmptyAccount() throws Exception {
        String email = registerPending();

        confirm(tokenFor(email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(userColumn(email, "status")).isEqualTo("ACTIVE");
        String passwordHash = userColumn(email, "password_hash");
        assertThat(passwordHash).startsWith("$2a$12$");
        assertThat(passwordEncoder.matches(PASSWORD, passwordHash)).isTrue();
        Map<String, Object> account = jdbc.sql("""
                        SELECT a.kind, a.balance, a.daily_limit, a.version
                        FROM accounts a JOIN users u ON u.id = a.user_id
                        WHERE u.email = :email
                        """)
                .param("email", email).query().singleRow();
        assertThat(account.get("kind")).isEqualTo("USER");
        assertThat((BigDecimal) account.get("balance")).isEqualByComparingTo("0.00");
        assertThat((BigDecimal) account.get("daily_limit")).isEqualByComparingTo("5000.00");
    }

    @Test
    void activationSendsTheAccountActivatedEmailWithTheLoginLink() throws Exception {
        String email = registerPending("Ana Souza Lima", TestData.validCpf());

        confirm(tokenFor(email)).andExpect(status().isOk());

        List<InboxMessage> messages = inbox.findByRecipient(email);
        assertThat(messages).extracting(InboxMessage::subject)
                .containsExactly("Sua conta PortWallet está ativa", "Ative sua conta PortWallet");
        InboxMessage activated = messages.getFirst();
        assertThat(activated.link()).isEqualTo("http://localhost:7173/login");
        assertThat(activated.body())
                .startsWith("Olá, Ana!")
                .contains("Sua conta PortWallet foi ativada", "http://localhost:7173/login")
                // Nothing of the account beyond the first name.
                .doesNotContain("Souza Lima", "CPF");
    }

    @Test
    void accountActivatedEmailIsInTheLanguageOfTheRegistration() throws Exception {
        String email = TestData.uniqueEmail();
        mockMvc.perform(post("/api/v1/auth/register")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Ana Souza", "email", email, "document", TestData.validCpf()))))
                .andExpect(status().isAccepted());

        // Activated from a browser in another language: the e-mail still follows the registration.
        mockMvc.perform(post("/api/v1/auth/confirm")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "es")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", tokenFor(email), "password", PASSWORD))))
                .andExpect(status().isOk());

        InboxMessage activated = inbox.findByRecipient(email).getFirst();
        assertThat(activated.subject()).isEqualTo("Your PortWallet account is active");
        assertThat(activated.body()).startsWith("Hello, Ana!");
    }

    @Test
    void refusedActivationSendsNoEmail() throws Exception {
        String email = registerPending();
        String token = tokenFor(email);

        confirm(token, "short").andExpect(status().isBadRequest());
        assertThat(inbox.findByRecipient(email)).hasSize(1);

        confirm(token).andExpect(status().isOk());
        confirm(token).andExpect(status().isBadRequest());
        assertThat(inbox.findByRecipient(email)).extracting(InboxMessage::subject)
                .containsExactly("Sua conta PortWallet está ativa", "Ative sua conta PortWallet");
    }

    @Test
    void tokenCannotBeUsedTwiceNorChangeThePasswordAfterActivation() throws Exception {
        String email = registerPending();
        String token = tokenFor(email);
        confirm(token).andExpect(status().isOk());
        String passwordHash = userColumn(email, "password_hash");

        confirm(token, "outra-senha-qualquer")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("invalid-token"))
                .andExpect(jsonPath("$.title").value("Link inválido ou expirado"));
        assertThat(userColumn(email, "password_hash")).isEqualTo(passwordHash);
    }

    @Test
    void expiredTokenIsRejectedAndTheUserStaysPendingWithoutPassword() throws Exception {
        String email = registerPending();
        jdbc.sql("""
                        UPDATE user_tokens SET expires_at = :past
                        WHERE user_id = (SELECT id FROM users WHERE email = :email)
                        """)
                .param("past", Timestamp.from(Instant.now().minusSeconds(1)))
                .param("email", email)
                .update();

        summary(tokenFor(email)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.type").value("invalid-token"));
        confirm(tokenFor(email)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.type").value("invalid-token"));

        assertThat(userColumn(email, "status")).isEqualTo("PENDING");
        assertThat(userColumn(email, "password_hash")).isNull();
    }

    @Test
    void unknownTokenIsRejectedWithTheSameAnswer() throws Exception {
        confirm("this-token-was-never-issued")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("invalid-token"))
                .andExpect(jsonPath("$.title").value("Link inválido ou expirado"));
    }

    @Test
    void missingFieldsAreAValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/confirm").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("password", "token")));
    }

    @Test
    void shortPasswordIsRejectedAndTheLinkStaysValid() throws Exception {
        String email = registerPending();
        String token = tokenFor(email);

        confirm(token, "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));
        assertThat(userColumn(email, "status")).isEqualTo("PENDING");

        confirm(token).andExpect(status().isOk());
    }

    @Test
    void passwordLongerThan72BytesIsRejectedEvenWithFewerCharacters() throws Exception {
        // 40 characters, 80 bytes in UTF-8: BCrypt would silently ignore the end of it.
        confirm(tokenFor(registerPending()), "ç".repeat(40))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void databaseRefusesAnActiveUserWithoutPassword() throws Exception {
        String email = registerPending();

        assertThatThrownBy(() -> jdbc.sql("UPDATE users SET status = 'ACTIVE' WHERE email = :email")
                .param("email", email).update())
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(error -> assertThat(error.getMessage()).containsIgnoringCase("ck_users_password_when_active"));
        assertThat(userColumn(email, "status")).isEqualTo("PENDING");
    }

    private String registerPending() throws Exception {
        return registerPending("Ana Souza", TestData.validCpf());
    }

    private String registerPending(String name, String cpf) throws Exception {
        String email = TestData.uniqueEmail();
        Map<String, String> request = Map.of("name", name, "email", email, "document", cpf);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
        return email;
    }

    private String tokenFor(String email) {
        return TestUsers.latestConfirmationToken(inbox, email);
    }

    /** Value of one column of the user, which may be null. */
    private String userColumn(String email, String column) {
        List<String> values = jdbc.sql("SELECT " + column + " FROM users WHERE email = :email")
                .param("email", email)
                .query(String.class)
                .list();
        assertThat(values).hasSize(1);
        return values.getFirst();
    }

    private ResultActions summary(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/confirm/summary")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", token))));
    }

    private ResultActions confirm(String token) throws Exception {
        return confirm(token, PASSWORD);
    }

    private ResultActions confirm(String token, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", token, "password", password))));
    }
}
