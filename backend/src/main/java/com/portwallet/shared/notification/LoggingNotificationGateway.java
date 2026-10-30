package com.portwallet.shared.notification;

import com.portwallet.shared.logging.Masking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Fallback when the demo inbox is off: records that an e-mail would be sent, without its content. */
@Component
@ConditionalOnProperty(name = "portwallet.mock.inbox.enabled", havingValue = "false", matchIfMissing = true)
class LoggingNotificationGateway implements NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationGateway.class);

    @Override
    public void send(Email email) {
        log.info("E-mail not delivered (no provider configured): subject='{}' to={}",
                email.subject(), Masking.email(email.to()));
    }
}
