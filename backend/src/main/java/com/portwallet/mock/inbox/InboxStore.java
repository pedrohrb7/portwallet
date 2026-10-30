package com.portwallet.mock.inbox;

import com.portwallet.shared.notification.Email;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Storage of the demo inbox. */
@Component
@ConditionalOnProperty(name = "portwallet.mock.inbox.enabled", havingValue = "true")
public class InboxStore {

    private final JdbcClient jdbc;
    private final Clock clock;

    InboxStore(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    void save(Email email) {
        jdbc.sql("""
                        INSERT INTO mock_inbox_messages (id, recipient, subject, body, link, created_at)
                        VALUES (:id, :recipient, :subject, :body, :link, :createdAt)
                        """)
                .param("id", UUID.randomUUID())
                .param("recipient", email.to())
                .param("subject", email.subject())
                .param("body", email.body())
                .param("link", email.link())
                .param("createdAt", Timestamp.from(clock.instant()))
                .update();
    }

    /** Messages for a recipient, newest first. */
    public List<InboxMessage> findByRecipient(String recipient) {
        return jdbc.sql("""
                        SELECT id, recipient, subject, body, link, created_at
                        FROM mock_inbox_messages
                        WHERE recipient = :recipient
                        ORDER BY seq DESC
                        """)
                .param("recipient", recipient)
                .query((rs, row) -> new InboxMessage(
                        rs.getObject("id", UUID.class),
                        rs.getString("recipient"),
                        rs.getString("subject"),
                        rs.getString("body"),
                        rs.getString("link"),
                        rs.getTimestamp("created_at").toInstant()))
                .list();
    }

    public record InboxMessage(UUID id, String to, String subject, String body, String link, Instant createdAt) {
    }
}
