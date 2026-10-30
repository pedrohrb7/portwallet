package com.portwallet.mock.inbox;

import com.portwallet.shared.notification.Email;
import com.portwallet.shared.notification.NotificationGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Simulated e-mail delivery: messages land in the demo inbox, visible at {@code /inbox}. */
@Component
@ConditionalOnProperty(name = "portwallet.mock.inbox.enabled", havingValue = "true")
class InboxNotificationGateway implements NotificationGateway {

    private final InboxStore store;

    InboxNotificationGateway(InboxStore store) {
        this.store = store;
    }

    @Override
    public void send(Email email) {
        store.save(email);
    }
}
