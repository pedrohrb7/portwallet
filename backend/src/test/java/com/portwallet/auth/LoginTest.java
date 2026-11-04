package com.portwallet.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portwallet.mock.inbox.InboxStore;
import com.portwallet.support.TestUsers;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class LoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InboxStore inbox;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private JwtDecoder jwtDecoder;

    private TestUsers users;

    @BeforeEach
    void setUp() {
        users = new TestUsers(mockMvc, objectMapper, inbox);
    }

    @Test
    void validLoginReturnsAVerifiableAccessTokenAndTheUser() throws Exception {
        String email = users.active("Ana Souza");

        MvcResult result = login(email, TestUsers.PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.name").value("Ana Souza"))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        Jwt jwt = jwtDecoder.decode(body.get("accessToken").asText());
        assertThat(jwt.getSubject()).isEqualTo(body.get("user").get("id").asText());
        assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void accessTokenFromTheLoginAuthenticatesRequests() throws Exception {
        String accessToken = accessToken(login(users.active("Ana Souza"), TestUsers.PASSWORD));

        // An authenticated request to an unknown route reaches the router (404) instead of stopping at 401.
        mockMvc.perform(get("/api/v1/not-a-route").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void loginSetsAHardenedRefreshCookieValidForSevenDays() throws Exception {
        String cookie = login(users.active("Ana Souza"), TestUsers.PASSWORD)
                .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);

        assertThat(cookie).matches("refresh_token=[A-Za-z0-9_-]{43}; .*");
        assertThat(cookie).contains("Path=/api/v1/auth", "Max-Age=604800", "Secure", "HttpOnly", "SameSite=Strict");
    }

    @Test
    void emailIsMatchedWhateverItsCase() throws Exception {
        String email = users.active("Ana Souza");

        login("  " + email.toUpperCase() + " ", TestUsers.PASSWORD).andExpect(status().isOk());
    }

    @Test
    void storesOnlyTheHashOfTheRefreshTokenInANewFamilyPerLogin() throws Exception {
        String email = users.active("Ana Souza");
        String first = refreshTokenOf(login(email, TestUsers.PASSWORD));
        String second = refreshTokenOf(login(email, TestUsers.PASSWORD));

        List<StoredToken> rows = jdbc.sql("""
                        SELECT t.token_hash, t.family_id, t.created_at, t.expires_at FROM refresh_tokens t
                        JOIN users u ON u.id = t.user_id WHERE u.email = :email
                        """)
                .param("email", email)
                .query((rs, i) -> new StoredToken(
                        rs.getString("token_hash"),
                        rs.getObject("family_id", UUID.class),
                        rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                        rs.getObject("expires_at", OffsetDateTime.class).toInstant()))
                .list();
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(StoredToken::hash)
                .doesNotContain(first, second)
                .allSatisfy(hash -> assertThat(hash).hasSize(64));
        assertThat(rows.get(0).familyId()).isNotEqualTo(rows.get(1).familyId());
        rows.forEach(row -> assertThat(Duration.between(row.createdAt(), row.expiresAt()))
                .isEqualTo(Duration.ofDays(7)));
    }

    private record StoredToken(String hash, UUID familyId, Instant createdAt, Instant expiresAt) {
    }

    @Test
    void blockedUserCanStillSignIn() throws Exception {
        String email = users.active("Ana Souza");
        jdbc.sql("UPDATE users SET status = 'BLOCKED' WHERE email = :email").param("email", email).update();

        login(email, TestUsers.PASSWORD).andExpect(status().isOk());
    }

    @Test
    void unknownEmailWrongPasswordAndPendingUserGetTheSameAnswer() throws Exception {
        String active = users.active("Ana Souza");
        String pending = users.pending("Bruno Lima");

        String unknown = body(login("nobody-" + Instant.now().toEpochMilli() + "@test.dev", TestUsers.PASSWORD));
        String wrongPassword = body(login(active, "senha-errada"));
        String notActivated = body(login(pending, TestUsers.PASSWORD));

        assertThat(objectMapper.readTree(unknown)).isEqualTo(objectMapper.readTree("""
                { "type": "invalid-credentials", "title": "E-mail ou senha inválidos", "status": 401,
                  "instance": "/api/v1/auth/login" }
                """));
        assertThat(wrongPassword).isEqualTo(unknown);
        assertThat(notActivated).isEqualTo(unknown);
    }

    @Test
    void failedLoginSetsNoCookie() throws Exception {
        login(users.active("Ana Souza"), "senha-errada")
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void invalidCredentialsTitleFollowsTheLanguageOfTheRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "nobody@test.dev", "password", "x"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("invalid-credentials"))
                .andExpect(jsonPath("$.title").value("Invalid e-mail or password"));
    }

    @Test
    void missingOrMalformedFieldsAreAValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("email", "password")));

        login("not-an-email", TestUsers.PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void passwordLongerThan72BytesIsAValidationError() throws Exception {
        login(users.active("Ana Souza"), "ç".repeat(40))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", password))));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private static String body(ResultActions result) throws Exception {
        return result.andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
    }

    private String accessToken(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("accessToken").asText();
    }

    private static String refreshTokenOf(ResultActions result) {
        String cookie = result.andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        return cookie.substring("refresh_token=".length(), cookie.indexOf(';'));
    }
}
