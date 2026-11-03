package com.portwallet.shared.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityTest.WhoAmIController.class)
class SecurityTest {

    private static final String WHO_AM_I = "/api/v1/test-security/me";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void missingTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get(WHO_AM_I))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("unauthorized"))
                .andExpect(jsonPath("$.instance").value(WHO_AM_I));
    }

    @Test
    void validTokenResolvesTheAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get(WHO_AM_I).header(HttpHeaders.AUTHORIZATION, bearer(userId, Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void expiredTokenIsUnauthorized() throws Exception {
        Instant past = Instant.now().minusSeconds(3600);
        String token = encode(jwtEncoder, claims(UUID.randomUUID().toString(), "USER", past, past.plusSeconds(900)));
        mockMvc.perform(get(WHO_AM_I).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("unauthorized"));
    }

    @Test
    void tokenSignedWithAnotherSecretIsUnauthorized() throws Exception {
        byte[] otherSecret = "another-secret-with-at-least-32-bytes!!".getBytes(StandardCharsets.UTF_8);
        JwtEncoder foreignEncoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(new SecretKeySpec(otherSecret, "HmacSHA256")));
        Instant now = Instant.now();
        String token = encode(foreignEncoder, claims(UUID.randomUUID().toString(), "ADMIN", now, now.plusSeconds(900)));
        mockMvc.perform(get(WHO_AM_I).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithUnknownRoleIsUnauthorized() throws Exception {
        Instant now = Instant.now();
        String token = encode(jwtEncoder, claims(UUID.randomUUID().toString(), "ROOT", now, now.plusSeconds(900)));
        mockMvc.perform(get(WHO_AM_I).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithNonUuidSubjectIsUnauthorized() throws Exception {
        Instant now = Instant.now();
        String token = encode(jwtEncoder, claims("ana@demo.com", "USER", now, now.plusSeconds(900)));
        mockMvc.perform(get(WHO_AM_I).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userWithoutAdminRoleIsForbiddenOnActuator() throws Exception {
        mockMvc.perform(get("/actuator/info").header(HttpHeaders.AUTHORIZATION, bearer(UUID.randomUUID(), Role.USER)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("forbidden"));
    }

    @Test
    void adminCanReadActuator() throws Exception {
        mockMvc.perform(get("/actuator/info").header(HttpHeaders.AUTHORIZATION, bearer(UUID.randomUUID(), Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void h2ConsoleIsClosedWhereItIsDisabled() throws Exception {
        mockMvc.perform(get("/h2-console/")).andExpect(status().isUnauthorized());
    }

    private String bearer(UUID userId, Role role) {
        return "Bearer " + jwtService.issue(userId, role).value();
    }

    private static JwtClaimsSet claims(String subject, String role, Instant issuedAt, Instant expiresAt) {
        return JwtClaimsSet.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
    }

    private static String encode(JwtEncoder encoder, JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @RestController
    static class WhoAmIController {

        @GetMapping(WHO_AM_I)
        AuthenticatedUser me(AuthenticatedUser user) {
            return user;
        }
    }
}
