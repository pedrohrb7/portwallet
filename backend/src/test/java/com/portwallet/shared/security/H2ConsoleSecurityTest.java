package com.portwallet.shared.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exercises the security filters only. MockMvc does not reach the console servlet itself, so a request that
 * passes security ends in 404 here; that keeps the test independent of the servlet, which resolves the
 * machine's hostname on start. The full console is checked through {@code docker-compose.dev.yml}.
 */
@SpringBootTest(properties = {"spring.h2.console.enabled=true", "spring.h2.console.path=/h2-console"})
@AutoConfigureMockMvc
class H2ConsoleSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void enabledConsolePassesSecurityWithoutATokenAndMayRenderInFrames() throws Exception {
        mockMvc.perform(get("/h2-console/"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }

    @Test
    void consoleFormsPostWithoutACsrfToken() throws Exception {
        mockMvc.perform(post("/h2-console/login.do")).andExpect(status().isNotFound());
    }

    @Test
    void consoleChainDoesNotLoosenTheApi() throws Exception {
        mockMvc.perform(get("/api/v1/anything"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }
}
