package com.portwallet.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portwallet.mock.inbox.InboxStore;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Creates users through the public API, the same way a person does: registration, then the e-mail link. */
public final class TestUsers {

    public static final String PASSWORD = "s3nh@forte";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final InboxStore inbox;

    public TestUsers(MockMvc mockMvc, ObjectMapper objectMapper, InboxStore inbox) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.inbox = inbox;
    }

    /** Registers a user who has not opened the confirmation link yet. Returns the e-mail. */
    public String pending(String name) throws Exception {
        String email = TestData.uniqueEmail();
        Map<String, String> request = Map.of("name", name, "email", email, "document", TestData.validCpf());
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
        return email;
    }

    /** Registers and activates a user with {@link #PASSWORD}. Returns the e-mail. */
    public String active(String name) throws Exception {
        String email = pending(name);
        String link = inbox.findByRecipient(email).getFirst().link();
        String token = link.substring(link.indexOf("#token=") + "#token=".length());
        mockMvc.perform(post("/api/v1/auth/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", token, "password", PASSWORD))))
                .andExpect(status().isOk());
        return email;
    }
}
