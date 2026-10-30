package com.portwallet.mock.inbox;

import com.portwallet.mock.inbox.InboxStore.InboxMessage;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Demo inbox. Exists only where {@code portwallet.mock.inbox.enabled} is true (dev, e2e and demo profiles). */
@RestController
@ConditionalOnProperty(name = "portwallet.mock.inbox.enabled", havingValue = "true")
class InboxController {

    private final InboxStore store;

    InboxController(InboxStore store) {
        this.store = store;
    }

    @GetMapping("/mock/inbox")
    List<InboxMessage> inbox(@RequestParam @NotBlank @Email String to) {
        return store.findByRecipient(to.trim().toLowerCase(Locale.ROOT));
    }
}
