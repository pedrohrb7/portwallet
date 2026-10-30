package com.portwallet.mock.inbox;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.portwallet.shared.notification.Email;
import com.portwallet.support.TestData;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

class InboxControllerTest {

    @Nested
    @SpringBootTest
    @AutoConfigureMockMvc
    class WhenEnabled {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private InboxStore store;

        @Test
        void listsTheMessagesOfOneRecipientNewestFirstWithoutAuthentication() throws Exception {
            String recipient = TestData.uniqueEmail();
            store.save(new Email(recipient, "Primeiro", "corpo 1", null));
            store.save(new Email(recipient, "Segundo", "corpo 2", "http://localhost:5173/confirm#token=x"));
            store.save(new Email(TestData.uniqueEmail(), "De outra pessoa", "corpo", null));

            mockMvc.perform(get("/mock/inbox").param("to", recipient.toUpperCase()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].subject").value("Segundo"))
                    .andExpect(jsonPath("$[0].to").value(recipient))
                    .andExpect(jsonPath("$[0].link").value("http://localhost:5173/confirm#token=x"))
                    .andExpect(jsonPath("$[0].createdAt").exists())
                    .andExpect(jsonPath("$[1].subject").value("Primeiro"));
        }

        @Test
        void requiresAValidEmail() throws Exception {
            mockMvc.perform(get("/mock/inbox").param("to", "nope"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("validation-error"));
        }
    }

    @Nested
    @SpringBootTest(properties = "portwallet.mock.inbox.enabled=false")
    @AutoConfigureMockMvc
    class WhenDisabled {

        @Autowired
        private MockMvc mockMvc;

        @Test
        void inboxDoesNotExist() throws Exception {
            mockMvc.perform(get("/mock/inbox").param("to", "ana@demo.com"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("not-found"));
        }
    }
}
