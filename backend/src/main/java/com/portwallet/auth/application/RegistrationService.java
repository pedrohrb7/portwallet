package com.portwallet.auth.application;

import com.portwallet.auth.domain.TokenPurpose;
import com.portwallet.auth.domain.User;
import com.portwallet.auth.domain.UserStatus;
import com.portwallet.auth.domain.UserToken;
import com.portwallet.auth.infra.UserRepository;
import com.portwallet.auth.infra.UserTokenRepository;
import com.portwallet.shared.crypto.BlindIndex;
import com.portwallet.shared.crypto.SecureTokens;
import com.portwallet.shared.logging.Masking;
import com.portwallet.shared.notification.NotificationGateway;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Registration without user enumeration: the caller always gets the same answer, and only the owner of the e-mail
 * or CPF learns what happened, by e-mail.
 */
@Service
public class RegistrationService {

    static final Duration CONFIRMATION_TTL = Duration.ofHours(24);

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final UserRepository users;
    private final UserTokenRepository tokens;
    private final BlindIndex blindIndex;
    private final NotificationGateway notifications;
    private final AuthEmails emails;
    private final TransactionTemplate transaction;
    private final Clock clock;

    RegistrationService(UserRepository users, UserTokenRepository tokens, BlindIndex blindIndex,
            NotificationGateway notifications, AuthEmails emails, TransactionTemplate transaction, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.blindIndex = blindIndex;
        this.notifications = notifications;
        this.emails = emails;
        this.transaction = transaction;
        this.clock = clock;
    }

    public void register(RegisterCommand command) {
        try {
            transaction.executeWithoutResult(status -> registerInTransaction(command));
        } catch (DataIntegrityViolationException e) {
            // A simultaneous registration won the race for the same e-mail or CPF. The answer stays the same.
            log.info("Concurrent registration ignored for {}", Masking.email(command.email()));
        }
    }

    private void registerInTransaction(RegisterCommand command) {
        Instant now = clock.instant();

        Optional<User> sameEmail = users.findByEmail(command.email());
        if (sameEmail.isPresent() && sameEmail.get().status() != UserStatus.PENDING) {
            notifications.send(emails.registrationAttempt(sameEmail.get()));
            return;
        }

        String documentHash = blindIndex.of(command.cpf().digits());
        Optional<User> sameCpf = users.findByDocumentHash(documentHash)
                .filter(owner -> sameEmail.map(pending -> !pending.id().equals(owner.id())).orElse(true));
        if (sameCpf.isPresent()) {
            notifications.send(emails.registrationAttempt(sameCpf.get()));
            return;
        }

        User user;
        if (sameEmail.isPresent()) {
            // Latest submission wins; its new link shows these data before activation (see ConfirmationService).
            user = sameEmail.get();
            user.replacePendingRegistration(command.name(), command.cpf(), documentHash);
        } else {
            user = User.register(command.name(), command.email(), command.cpf(), documentHash, now);
            users.save(user);
        }
        users.flush();
        sendConfirmation(user, now);
    }

    /** Issues a new confirmation link, invalidating the previous ones. */
    private void sendConfirmation(User user, Instant now) {
        tokens.invalidateAll(user.id(), TokenPurpose.EMAIL_CONFIRMATION, now);
        String rawToken = SecureTokens.generate();
        tokens.save(new UserToken(user.id(), TokenPurpose.EMAIL_CONFIRMATION, SecureTokens.hash(rawToken),
                now.plus(CONFIRMATION_TTL)));
        notifications.send(emails.confirmation(user, rawToken));
    }
}
