package com.portwallet.auth.application;

import com.portwallet.account.application.AccountProvisioning;
import com.portwallet.auth.domain.TokenPurpose;
import com.portwallet.auth.domain.User;
import com.portwallet.auth.domain.UserStatus;
import com.portwallet.auth.domain.UserToken;
import com.portwallet.auth.infra.UserRepository;
import com.portwallet.auth.infra.UserTokenRepository;
import com.portwallet.shared.crypto.SecureTokens;
import com.portwallet.shared.privacy.NameMasker;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Activation of a pending user through the e-mail link. The password is chosen here, so only who controls the e-mail
 * sets it: registering someone else's e-mail first gives no access to the account. Before activating, the link shows
 * a masked summary of the registration it belongs to, so the owner notices if someone else replaced the data.
 */
@Service
public class ConfirmationService {

    private final UserTokenRepository tokens;
    private final UserRepository users;
    private final AccountProvisioning accounts;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    ConfirmationService(UserTokenRepository tokens, UserRepository users, AccountProvisioning accounts,
            PasswordEncoder passwordEncoder, Clock clock) {
        this.tokens = tokens;
        this.users = users;
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /** Masked data of the registration the link belongs to. Does not consume the token. */
    @Transactional(readOnly = true)
    public RegistrationSummary summary(String rawToken) {
        User user = pendingUserOf(usableToken(rawToken, clock.instant()));
        return new RegistrationSummary(NameMasker.mask(user.name()), user.cpf().masked());
    }

    /** Sets the password, activates the user and opens the wallet account, in one transaction. */
    @Transactional
    public void confirm(String rawToken, String password) {
        Instant now = clock.instant();
        UserToken token = usableToken(rawToken, now);
        User user = pendingUserOf(token);
        if (tokens.consume(token.id(), now) == 0) {
            throw new InvalidTokenException();
        }
        user.activate(passwordEncoder.encode(password));
        accounts.openUserAccount(user.id());
    }

    private UserToken usableToken(String rawToken, Instant now) {
        return tokens.findByTokenHashAndPurpose(SecureTokens.hash(rawToken), TokenPurpose.EMAIL_CONFIRMATION)
                .filter(candidate -> candidate.isUsable(now))
                .orElseThrow(InvalidTokenException::new);
    }

    private User pendingUserOf(UserToken token) {
        return users.findById(token.userId())
                .filter(candidate -> candidate.status() == UserStatus.PENDING)
                .orElseThrow(InvalidTokenException::new);
    }

    /** Name and CPF of a pending registration, masked. */
    public record RegistrationSummary(String maskedName, String maskedDocument) {
    }
}
