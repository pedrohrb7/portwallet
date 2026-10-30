package com.portwallet.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portwallet.mock.inbox.InboxStore;
import com.portwallet.support.TestData;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class ConfirmationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InboxStore inbox;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void validTokenActivatesTheUserAndOpensAnEmptyAccount() throws Exception {
        String email = registerPending();

        confirm(tokenFor(email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(jdbc.sql("SELECT status FROM users WHERE email = :email").param("email", email)
                .query(String.class).single()).isEqualTo("ACTIVE");
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
    void tokenCannotBeUsedTwice() throws Exception {
        String token = tokenFor(registerPending());
        confirm(token).andExpect(status().isOk());

        confirm(token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("invalid-token"))
                .andExpect(jsonPath("$.title").value("Link inválido ou expirado"));
    }

    @Test
    void expiredTokenIsRejectedAndTheUserStaysPending() throws Exception {
        String email = registerPending();
        jdbc.sql("""
                        UPDATE user_tokens SET expires_at = :past
                        WHERE user_id = (SELECT id FROM users WHERE email = :email)
                        """)
                .param("past", Timestamp.from(Instant.now().minusSeconds(1)))
                .param("email", email)
                .update();

        confirm(tokenFor(email)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.type").value("invalid-token"));

        assertThat(jdbc.sql("SELECT status FROM users WHERE email = :email").param("email", email)
                .query(String.class).single()).isEqualTo("PENDING");
    }

    @Test
    void unknownTokenIsRejectedWithTheSameAnswer() throws Exception {
        confirm("this-token-was-never-issued")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("invalid-token"))
                .andExpect(jsonPath("$.title").value("Link inválido ou expirado"));
    }

    @Test
    void wrongPasswordKeepsTheLinkValidForAnotherTry() throws Exception {
        String email = registerPending();
        String token = tokenFor(email);

        confirm(token, "nao-e-a-senha")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("wrong-password"))
                .andExpect(jsonPath("$.title").value("Senha incorreta"));
        assertThat(jdbc.sql("SELECT status FROM users WHERE email = :email").param("email", email)
                .query(String.class).single()).isEqualTo("PENDING");

        confirm(token).andExpect(status().isOk());
    }

    @Test
    void missingFieldsAreAValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/confirm").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"))
                .andExpect(jsonPath("$.errors[*].field").value(
                        org.hamcrest.Matchers.containsInAnyOrder("password", "token")));
    }

    private String registerPending() throws Exception {
        String email = TestData.uniqueEmail();
        Map<String, String> request = Map.of(
                "name", "Ana Souza", "email", email, "document", TestData.validCpf(), "password", "s3nh@forte");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
        return email;
    }

    private String tokenFor(String email) {
        String link = inbox.findByRecipient(email).getFirst().link();
        return link.substring(link.indexOf("#token=") + "#token=".length());
    }

    private ResultActions confirm(String token) throws Exception {
        return confirm(token, "s3nh@forte");
    }

    private ResultActions confirm(String token, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", token, "password", password))));
    }
}
