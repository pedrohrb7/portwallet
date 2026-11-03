package com.portwallet.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void newUserIsCreatedPendingWithoutPasswordAndReceivesALinkWithTheRegistrationData() throws Exception {
        String email = TestData.uniqueEmail();
        String cpf = TestData.validCpf();
        String body = register("Ana  Souza ", email, cpf)
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).isEqualTo(EXPECTED_BODY);
        assertThat(userColumn(email, "status")).isEqualTo("PENDING");
        assertThat(userColumn(email, "name")).isEqualTo("Ana Souza");
        assertThat(userColumn(email, "password_hash")).isNull();
        List<InboxMessage> messages = inbox.findByRecipient(email);
        assertThat(messages).singleElement().satisfies(message -> {
            assertThat(message.subject()).isEqualTo("Ative sua conta PortWallet");
            assertThat(message.link()).startsWith("http://localhost:5173/confirm#token=");
            assertThat(message.body())
                    .contains(message.link())
                    .contains("Olá, Ana!")
                    .contains("Nome: Ana Souza")
                    .contains("CPF: ***.***.***-" + cpf.substring(9))
                    .doesNotContain(cpf);
        });
    }

    @Test
    void passwordSentAtRegistrationIsIgnored() throws Exception {
        String email = TestData.uniqueEmail();
        Map<String, String> request = Map.of(
                "name", "Ana Souza", "email", email, "document", TestData.validCpf(), "password", "s3nh@forte");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        assertThat(userColumn(email, "password_hash")).isNull();
    }

    @Test
    void cpfIsStoredEncryptedWithABlindIndex() throws Exception {
        String email = TestData.uniqueEmail();
        String cpf = TestData.validCpf();
        register("Ana Souza", email, TestData.masked(cpf)).andExpect(status().isAccepted());

        assertThat(userColumn(email, "document_enc")).doesNotContain(cpf).isNotBlank();
        assertThat(userColumn(email, "document_hash")).hasSize(64).doesNotContain(cpf);
    }

    @Test
    void emailIsNormalized() throws Exception {
        String email = TestData.uniqueEmail();
        register("Ana Souza", "  " + email.toUpperCase() + " ", TestData.validCpf()).andExpect(status().isAccepted());

        assertThat(userColumn(email, "email")).isEqualTo(email);
    }

    @Test
    void pendingEmailGetsANewLinkAndTheOldOneStopsWorking() throws Exception {
        String email = TestData.uniqueEmail();
        register("Ana Souza", email, TestData.validCpf());
        String firstToken = latestToken(email);

        String body = register("Ana Souza", email, TestData.validCpf())
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).isEqualTo(EXPECTED_BODY);
        assertThat(countUsers(email)).isEqualTo(1);
        assertThat(inbox.findByRecipient(email)).hasSize(2);
        summary(firstToken).andExpect(status().isBadRequest()).andExpect(jsonPath("$.type").value("invalid-token"));
        confirm(firstToken).andExpect(status().isBadRequest()).andExpect(jsonPath("$.type").value("invalid-token"));
        confirm(latestToken(email)).andExpect(status().isOk());
    }

    @Test
    void preRegisteredEmailGivesTheAttackerNoAccess() throws Exception {
        // An attacker registers the victim's e-mail first, with a CPF of their own.
        String email = TestData.uniqueEmail();
        String attackerCpf = TestData.validCpf();
        register("Atacante", email, attackerCpf).andExpect(status().isAccepted());
        String attackerLink = latestToken(email);

        // The victim registers later; the latest submission replaces the pending data and the old link.
        String victimCpf = TestData.validCpf();
        register("Ana Souza", email, victimCpf).andExpect(status().isAccepted());

        confirm(attackerLink, "senha-do-atacante").andExpect(status().isBadRequest());
        confirm(latestToken(email), "senha-da-vitima").andExpect(status().isOk());

        assertThat(userColumn(email, "name")).isEqualTo("Ana Souza");
        assertThat(userColumn(email, "document_hash")).isEqualTo(blindIndex.of(victimCpf));
        assertThat(passwordEncoder.matches("senha-da-vitima", userColumn(email, "password_hash"))).isTrue();
        // The attacker's CPF was released and can be used by its real owner.
        String cpfOwnerEmail = TestData.uniqueEmail();
        register("Dono do CPF", cpfOwnerEmail, attackerCpf).andExpect(status().isAccepted());
        assertThat(countUsers(cpfOwnerEmail)).isEqualTo(1);
    }

    @Test
    void dataReplacedByAnAttackerShowUpInTheSummaryBeforeActivation() throws Exception {
        String email = TestData.uniqueEmail();
        String victimCpf = TestData.validCpf();
        register("Ana Souza", email, victimCpf);
        String victimLink = latestToken(email);
        String attackerCpf = TestData.validCpf();
        register("Carlos Atacante", email, attackerCpf);

        // The link the victim already had stops working; the newest one, also in the victim's inbox, shows
        // someone else's data, so the victim does not activate it.
        summary(victimLink).andExpect(status().isBadRequest());
        summary(latestToken(email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedName").value("Carlos A****"))
                .andExpect(jsonPath("$.maskedDocument").value("***.***.***-" + attackerCpf.substring(9)));
        assertThat(inbox.findByRecipient(email).getFirst().body()).contains("Nome: Carlos Atacante");

        // Registering again restores the victim's data, which the newest link then shows.
        register("Ana Souza", email, victimCpf);
        summary(latestToken(email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedName").value("Ana S****"))
                .andExpect(jsonPath("$.maskedDocument").value("***.***.***-" + victimCpf.substring(9)));
        confirm(latestToken(email)).andExpect(status().isOk());
        assertThat(userColumn(email, "name")).isEqualTo("Ana Souza");
    }

    @Test
    void pendingRegistrationKeepsItsDataWhenTheNewCpfBelongsToSomeoneElse() throws Exception {
        String ownerEmail = TestData.uniqueEmail();
        String ownerCpf = TestData.validCpf();
        register("Dono do CPF", ownerEmail, ownerCpf);

        String pendingEmail = TestData.uniqueEmail();
        register("Ana Souza", pendingEmail, TestData.validCpf());
        String body = register("Outro Nome", pendingEmail, ownerCpf)
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).isEqualTo(EXPECTED_BODY);
        assertThat(userColumn(pendingEmail, "name")).isEqualTo("Ana Souza");
        assertThat(inbox.findByRecipient(pendingEmail)).hasSize(1);
        assertThat(inbox.findByRecipient(ownerEmail).getFirst().subject())
                .isEqualTo("Tentativa de cadastro com os seus dados");
        confirm(latestToken(pendingEmail)).andExpect(status().isOk());
    }

    @Test
    void activeEmailOwnerIsWarnedAndNothingIsCreated() throws Exception {
        String email = TestData.uniqueEmail();
        register("Ana Souza", email, TestData.validCpf());
        confirm(latestToken(email)).andExpect(status().isOk());

        String body = register("Intruso", email, TestData.validCpf())
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
        register("Ana Souza", ownerEmail, cpf);

        String intruderEmail = TestData.uniqueEmail();
        String body = register("Intruso", intruderEmail, TestData.masked(cpf))
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
        register("A", "not-an-email", "529.982.247-24")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder("name", "email", "document")));
    }

    @Test
    void registrationWithoutAcceptLanguageIsSavedInBrazilianPortuguese() throws Exception {
        String email = TestData.uniqueEmail();
        register("Ana Souza", email, TestData.validCpf()).andExpect(status().isAccepted());

        assertThat(userColumn(email, "language")).isEqualTo("PT_BR");
    }

    @Test
    void messageAndConfirmationEmailFollowTheLanguageOfTheRequest() throws Exception {
        String email = TestData.uniqueEmail();
        String cpf = TestData.validCpf();
        String body = register("Ana Souza", email, cpf, "en-US")
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).isEqualTo(
                "{\"message\":\"If the details are correct, you will receive an e-mail to activate your account.\"}");
        assertThat(userColumn(email, "language")).isEqualTo("EN");
        assertThat(inbox.findByRecipient(email)).singleElement().satisfies(message -> {
            assertThat(message.subject()).isEqualTo("Activate your PortWallet account");
            assertThat(message.body())
                    .startsWith("Hello, Ana!")
                    .contains("Name: Ana Souza")
                    .contains("CPF: ***.***.***-" + cpf.substring(9))
                    .contains(message.link());
        });
    }

    @Test
    void registrationAttemptWarningIsWrittenInTheLanguageOfTheOwner() throws Exception {
        String email = TestData.uniqueEmail();
        register("Ana Souza", email, TestData.validCpf(), "fr").andExpect(status().isAccepted());
        confirm(latestToken(email)).andExpect(status().isOk());

        // Someone registers the same e-mail in another language: the answer is in theirs, the warning in the owner's.
        String body = register("Intruso", email, TestData.validCpf(), "es")
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("Si los datos son correctos");
        InboxMessage warning = inbox.findByRecipient(email).getFirst();
        assertThat(warning.subject()).isEqualTo("Tentative d’inscription avec vos informations");
        assertThat(warning.body()).startsWith("Bonjour Ana,").contains(warning.link());
    }

    @Test
    void newPendingRegistrationReplacesTheLanguage() throws Exception {
        String email = TestData.uniqueEmail();
        register("Ana Souza", email, TestData.validCpf(), "es").andExpect(status().isAccepted());
        assertThat(inbox.findByRecipient(email).getFirst().subject()).isEqualTo("Activa tu cuenta PortWallet");

        register("Ana Souza", email, TestData.validCpf(), "pt-BR").andExpect(status().isAccepted());

        assertThat(userColumn(email, "language")).isEqualTo("PT_BR");
        assertThat(inbox.findByRecipient(email).getFirst().subject()).isEqualTo("Ative sua conta PortWallet");
    }

    @Test
    void usersWithoutALanguageGetBrazilianPortuguese() {
        // Rows that existed before the language column, inserted without it.
        String email = TestData.uniqueEmail();
        jdbc.sql("""
                        INSERT INTO users (id, name, email, document_enc, document_hash, role, status, created_at)
                        VALUES (:id, 'Ana Souza', :email, 'x', :hash, 'USER', 'PENDING', CURRENT_TIMESTAMP)
                        """)
                .param("id", UUID.randomUUID())
                .param("email", email)
                .param("hash", UUID.randomUUID().toString())
                .update();

        assertThat(userColumn(email, "language")).isEqualTo("PT_BR");
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

    private ResultActions register(String name, String email, String document) throws Exception {
        return register(name, email, document, null);
    }

    private ResultActions register(String name, String email, String document, String acceptLanguage)
            throws Exception {
        Map<String, String> request = Map.of("name", name, "email", email, "document", document);
        var builder = post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
        if (acceptLanguage != null) {
            builder.header(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage);
        }
        return mockMvc.perform(builder);
    }

    private String registerBody(String name, String email, String document) throws Exception {
        return register(name, email, document).andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
    }

    private ResultActions summary(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/confirm/summary")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", token))));
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

    /** Value of one column of the user, which may be null. */
    private String userColumn(String email, String column) {
        List<String> values = jdbc.sql("SELECT " + column + " FROM users WHERE email = :email")
                .param("email", email)
                .query(String.class)
                .list();
        assertThat(values).hasSize(1);
        return values.getFirst();
    }

    private long countUsers(String email) {
        return jdbc.sql("SELECT COUNT(*) FROM users WHERE email = :email")
                .param("email", email)
                .query(Long.class)
                .single();
    }
}
