package com.portwallet.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portwallet.mock.inbox.InboxStore;
import com.portwallet.mock.inbox.InboxStore.InboxMessage;
import com.portwallet.shared.crypto.BlindIndex;
import com.portwallet.support.TestData;
import java.util.List;
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
class RegistrationTest {

    static final String EXPECTED_BODY =
            "{\"message\":\"Se os dados estiverem corretos, você receberá um e-mail para ativar a conta.\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InboxStore inbox;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private BlindIndex blindIndex;

    @Test
    void newUserIsCreatedPendingAndReceivesAConfirmationLink() throws Exception {
        String email = TestData.uniqueEmail();
        String body = register("Ana  Souza ", email, TestData.validCpf(), "s3nh@forte")
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).isEqualTo(EXPECTED_BODY);
        assertThat(userColumn(email, "status")).isEqualTo("PENDING");
        assertThat(userColumn(email, "name")).isEqualTo("Ana Souza");
        List<InboxMessage> messages = inbox.findByRecipient(email);
        assertThat(messages).singleElement().satisfies(message -> {
            assertThat(message.subject()).isEqualTo("Ative sua conta PortWallet");
            assertThat(message.link()).startsWith("http://localhost:5173/confirm#token=");
            assertThat(message.body()).contains(message.link()).contains("Olá, Ana!");
        });
    }

    @Test
    void cpfIsStoredEncryptedWithABlindIndex() throws Exception {
        String email = TestData.uniqueEmail();
        String cpf = TestData.validCpf();
        register("Ana Souza", email, TestData.masked(cpf), "s3nh@forte").andExpect(status().isAccepted());

        assertThat(userColumn(email, "document_enc")).doesNotContain(cpf).isNotBlank();
        assertThat(userColumn(email, "document_hash")).hasSize(64).doesNotContain(cpf);
    }

    @Test
    void passwordIsStoredWithBcryptCost12() throws Exception {
        String email = TestData.uniqueEmail();
        register("Ana Souza", email, TestData.validCpf(), "s3nh@forte").andExpect(status().isAccepted());

        assertThat(userColumn(email, "password_hash")).startsWith("$2a$12$");
    }

    @Test
    void emailIsNormalized() throws Exception {
        String email = TestData.uniqueEmail();
        register("Ana Souza", "  " + email.toUpperCase() + " ", TestData.validCpf(), "s3nh@forte")
                .andExpect(status().isAccepted());

        assertThat(userColumn(email, "email")).isEqualTo(email);
    }

    @Test
    void pendingEmailGetsANewLinkAndTheOldOneStopsWorking() throws Exception {
        String email = TestData.uniqueEmail();
        register("Ana Souza", email, TestData.validCpf(), "s3nh@forte");
        String firstToken = latestToken(email);

        String body = register("Ana Souza", email, TestData.validCpf(), "outra-senha")
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).isEqualTo(EXPECTED_BODY);
        assertThat(countUsers(email)).isEqualTo(1);
        assertThat(inbox.findByRecipient(email)).hasSize(2);
        confirm(firstToken, "outra-senha")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("invalid-token"));
        confirm(latestToken(email), "outra-senha").andExpect(status().isOk());
    }

    @Test
    void preRegisteredEmailCannotBeHijacked() throws Exception {
        // An attacker registers the victim's e-mail first, with a password and CPF of their own.
        String email = TestData.uniqueEmail();
        String attackerCpf = TestData.validCpf();
        register("Atacante", email, attackerCpf, "senha-do-atacante").andExpect(status().isAccepted());

        // The victim registers later; the latest submission replaces the pending data.
        String victimCpf = TestData.validCpf();
        register("Ana Souza", email, victimCpf, "senha-da-vitima").andExpect(status().isAccepted());

        confirm(latestToken(email), "senha-do-atacante")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("wrong-password"));
        confirm(latestToken(email), "senha-da-vitima").andExpect(status().isOk());

        assertThat(userColumn(email, "name")).isEqualTo("Ana Souza");
        assertThat(userColumn(email, "document_hash")).isEqualTo(blindIndex.of(victimCpf));
        // The attacker's CPF was released and can be used by its real owner.
        String cpfOwnerEmail = TestData.uniqueEmail();
        register("Dono do CPF", cpfOwnerEmail, attackerCpf, "s3nh@forte").andExpect(status().isAccepted());
        assertThat(countUsers(cpfOwnerEmail)).isEqualTo(1);
    }

    @Test
    void attackerRegisteringAfterTheVictimCannotGetTheAccountActivated() throws Exception {
        String email = TestData.uniqueEmail();
        register("Ana Souza", email, TestData.validCpf(), "senha-da-vitima");
        register("Atacante", email, TestData.validCpf(), "senha-do-atacante");

        // The victim opens the latest link, but the password is no longer theirs: nothing is activated.
        confirm(latestToken(email), "senha-da-vitima")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("wrong-password"));
        assertThat(userColumn(email, "status")).isEqualTo("PENDING");

        // Registering again restores the victim's data, and only then the account can be activated.
        register("Ana Souza", email, TestData.validCpf(), "senha-da-vitima");
        confirm(latestToken(email), "senha-da-vitima").andExpect(status().isOk());
        assertThat(userColumn(email, "name")).isEqualTo("Ana Souza");
    }

    @Test
    void pendingRegistrationKeepsItsDataWhenTheNewCpfBelongsToSomeoneElse() throws Exception {
        String ownerEmail = TestData.uniqueEmail();
        String ownerCpf = TestData.validCpf();
        register("Dono do CPF", ownerEmail, ownerCpf, "s3nh@forte");

        String pendingEmail = TestData.uniqueEmail();
        register("Ana Souza", pendingEmail, TestData.validCpf(), "s3nh@forte");
        String body = register("Outro Nome", pendingEmail, ownerCpf, "outra-senha")
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).isEqualTo(EXPECTED_BODY);
        assertThat(userColumn(pendingEmail, "name")).isEqualTo("Ana Souza");
        assertThat(inbox.findByRecipient(pendingEmail)).hasSize(1);
        assertThat(inbox.findByRecipient(ownerEmail).getFirst().subject())
                .isEqualTo("Tentativa de cadastro com os seus dados");
        confirm(latestToken(pendingEmail), "s3nh@forte").andExpect(status().isOk());
    }

    @Test
    void activeEmailOwnerIsWarnedAndNothingIsCreated() throws Exception {
        String email = TestData.uniqueEmail();
        register("Ana Souza", email, TestData.validCpf(), "s3nh@forte");
        confirm(latestToken(email)).andExpect(status().isOk());

        String body = register("Intruso", email, TestData.validCpf(), "s3nh@forte")
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).isEqualTo(EXPECTED_BODY);
        assertThat(countUsers(email)).isEqualTo(1);
        assertThat(userColumn(email, "name")).isEqualTo("Ana Souza");
        assertThat(inbox.findByRecipient(email).getFirst().subject())
                .isEqualTo("Tentativa de cadastro com os seus dados");
    }

    @Test
    void cpfOwnerIsWarnedAndTheNewEmailReceivesNothing() throws Exception {
        String ownerEmail = TestData.uniqueEmail();
        String cpf = TestData.validCpf();
        register("Ana Souza", ownerEmail, cpf, "s3nh@forte");

        String intruderEmail = TestData.uniqueEmail();
        String body = register("Intruso", intruderEmail, TestData.masked(cpf), "s3nh@forte")
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).isEqualTo(EXPECTED_BODY);
        assertThat(countUsers(intruderEmail)).isZero();
        assertThat(inbox.findByRecipient(intruderEmail)).isEmpty();
        assertThat(inbox.findByRecipient(ownerEmail).getFirst().subject())
                .isEqualTo("Tentativa de cadastro com os seus dados");
    }

    @Test
    void invalidDataIsRejectedWithTheFields() throws Exception {
        register("A", "not-an-email", "529.982.247-24", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"))
                .andExpect(jsonPath("$.errors[*].field").value(
                        org.hamcrest.Matchers.containsInAnyOrder("name", "email", "document", "password")));
    }

    @Test
    void passwordLongerThan72BytesIsRejectedEvenWithFewerCharacters() throws Exception {
        // 40 characters, 80 bytes in UTF-8: BCrypt would silently ignore the end of it.
        register("Ana Souza", TestData.uniqueEmail(), TestData.validCpf(), "ç".repeat(40))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void responseDoesNotLeakTheStatusOfTheEmail() throws Exception {
        String email = TestData.uniqueEmail();
        String cpf = TestData.validCpf();
        String created = registerBody("Ana Souza", email, cpf);
        String pending = registerBody("Ana Souza", email, cpf);
        confirm(latestToken(email)).andExpect(status().isOk());
        String active = registerBody("Ana Souza", email, TestData.validCpf());
        String cpfTaken = registerBody("Ana Souza", TestData.uniqueEmail(), cpf);

        assertThat(List.of(created, pending, active, cpfTaken)).containsOnly(EXPECTED_BODY);
    }

    private ResultActions register(String name, String email, String document, String password) throws Exception {
        Map<String, String> request = Map.of("name", name, "email", email, "document", document, "password", password);
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private String registerBody(String name, String email, String document) throws Exception {
        return register(name, email, document, "s3nh@forte").andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
    }

    private ResultActions confirm(String token) throws Exception {
        return confirm(token, "s3nh@forte");
    }

    private ResultActions confirm(String token, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", token, "password", password))));
    }

    private String latestToken(String email) {
        String link = inbox.findByRecipient(email).getFirst().link();
        return link.substring(link.indexOf("#token=") + "#token=".length());
    }

    private String userColumn(String email, String column) {
        return jdbc.sql("SELECT " + column + " FROM users WHERE email = :email")
                .param("email", email)
                .query(String.class)
                .single();
    }

    private long countUsers(String email) {
        return jdbc.sql("SELECT COUNT(*) FROM users WHERE email = :email")
                .param("email", email)
                .query(Long.class)
                .single();
    }
}
