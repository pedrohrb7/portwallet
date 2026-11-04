package com.portwallet.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portwallet.mock.inbox.InboxStore;
import com.portwallet.shared.crypto.SecureTokens;
import com.portwallet.support.TestUsers;
import jakarta.servlet.http.Cookie;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class RefreshTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InboxStore inbox;

    @Autowired
    private JdbcClient jdbc;

    private TestUsers users;

    @BeforeEach
    void setUp() {
        users = new TestUsers(mockMvc, objectMapper, inbox);
    }

    @Test
    void refreshRotatesTheTokenAndReturnsANewSession() throws Exception {
        String email = users.active("Ana Souza");
        String first = login(email);

        ResultActions result = refresh(first)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.name").value("Ana Souza"));
        String second = cookieValue(result);

        assertThat(second).isNotEqualTo(first);
        assertThat(revoked(first)).isTrue();
        assertThat(revoked(second)).isFalse();
        assertThat(familyOf(second)).isEqualTo(familyOf(first));
        refresh(second).andExpect(status().isOk());
    }

    @Test
    void anAlreadyRotatedTokenIsRefusedAndTheCookieCleared() throws Exception {
        String first = login(users.active("Ana Souza"));
        refresh(first).andExpect(status().isOk());

        expectRefused(refresh(first));
    }

    @Test
    void rotationKeepsTheFamilyExpirationAndTheCookieLifetimeFollowsIt() throws Exception {
        String first = login(users.active("Ana Souza"));
        Instant familyExpiration = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        jdbc.sql("UPDATE refresh_tokens SET expires_at = :at WHERE token_hash = :hash")
                .param("at", Timestamp.from(familyExpiration))
                .param("hash", SecureTokens.hash(first))
                .update();

        ResultActions result = refresh(first).andExpect(status().isOk());
        String second = cookieValue(result);

        assertThat(expiresAt(second)).isEqualTo(familyExpiration);
        long maxAge = maxAge(result.andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE));
        assertThat(maxAge).isBetween(3500L, 3600L);
    }

    @Test
    void expiredTokenIsRefusedAndTheCookieCleared() throws Exception {
        String token = login(users.active("Ana Souza"));
        jdbc.sql("UPDATE refresh_tokens SET expires_at = :past WHERE token_hash = :hash")
                .param("past", Timestamp.from(Instant.now().minusSeconds(1)))
                .param("hash", SecureTokens.hash(token))
                .update();

        expectRefused(refresh(token));
    }

    @Test
    void revokedTokenIsRefusedAndTheCookieCleared() throws Exception {
        String token = login(users.active("Ana Souza"));
        jdbc.sql("UPDATE refresh_tokens SET revoked = TRUE WHERE token_hash = :hash")
                .param("hash", SecureTokens.hash(token))
                .update();

        expectRefused(refresh(token));
    }

    @Test
    void unknownTokenIsRefusedAndTheCookieCleared() throws Exception {
        expectRefused(refresh(SecureTokens.generate()));
    }

    @Test
    void missingCookieIsRefusedAndTheCookieCleared() throws Exception {
        expectRefused(mockMvc.perform(post("/api/v1/auth/refresh")));
    }

    @Test
    void userWhoCanNoLongerSignInIsRefused() throws Exception {
        String email = users.active("Ana Souza");
        String token = login(email);
        // No status other than ACTIVE and BLOCKED may sign in; PENDING is the one that exists today.
        jdbc.sql("UPDATE users SET status = 'PENDING' WHERE email = :email").param("email", email).update();

        expectRefused(refresh(token));
        assertThat(revoked(token)).isFalse();
    }

    @Test
    void blockedUserCanStillRefresh() throws Exception {
        String email = users.active("Ana Souza");
        String token = login(email);
        jdbc.sql("UPDATE users SET status = 'BLOCKED' WHERE email = :email").param("email", email).update();

        refresh(token).andExpect(status().isOk());
    }

    @Test
    void refusedRefreshTitleFollowsTheLanguageOfTheRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh").header(HttpHeaders.ACCEPT_LANGUAGE, "es"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("invalid-refresh-token"))
                .andExpect(jsonPath("$.title").value("Sesión caducada"));
    }

    private void expectRefused(ResultActions result) throws Exception {
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("invalid-refresh-token"))
                .andExpect(jsonPath("$.title").value("Sessão expirada"));
        String cookie = result.andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(cookie).startsWith("refresh_token=;")
                .contains("Path=/api/v1/auth", "Max-Age=0", "Secure", "HttpOnly", "SameSite=Strict");
    }

    private String login(String email) throws Exception {
        return cookieValue(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", TestUsers.PASSWORD))))
                .andExpect(status().isOk()));
    }

    private ResultActions refresh(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("refresh_token", token)));
    }

    private static String cookieValue(ResultActions result) {
        String cookie = result.andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        return cookie.substring("refresh_token=".length(), cookie.indexOf(';'));
    }

    private static long maxAge(String cookie) {
        String rest = cookie.substring(cookie.indexOf("Max-Age=") + "Max-Age=".length());
        return Long.parseLong(rest.substring(0, rest.indexOf(';')));
    }

    private boolean revoked(String token) {
        return column(token, "revoked", Boolean.class);
    }

    private UUID familyOf(String token) {
        return column(token, "family_id", UUID.class);
    }

    private Instant expiresAt(String token) {
        return column(token, "expires_at", Timestamp.class).toInstant();
    }

    private <T> T column(String token, String column, Class<T> type) {
        List<T> values = jdbc.sql("SELECT " + column + " FROM refresh_tokens WHERE token_hash = :hash")
                .param("hash", SecureTokens.hash(token))
                .query(type)
                .list();
        assertThat(values).hasSize(1);
        return values.getFirst();
    }
}
