package com.portwallet.auth.application;

import com.portwallet.account.application.AccountProvisioning;
import com.portwallet.auth.domain.TokenPurpose;
import com.portwallet.auth.domain.User;
import com.portwallet.auth.domain.UserStatus;
import com.portwallet.auth.domain.UserToken;
import com.portwallet.auth.infra.UserRepository;
import com.portwallet.auth.infra.UserTokenRepository;
import com.portwallet.shared.crypto.SecureTokens;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Activates a pending user and opens the wallet account, in one transaction. Activation needs both the e-mail link
 * and the password of the latest registration, so nobody can pre-register someone else's e-mail with a password of
 * their own and have the owner activate it.
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

    @Transactional
    public void confirm(String rawToken, String password) {
        Instant now = clock.instant();
        UserToken token = tokens.findByTokenHashAndPurpose(SecureTokens.hash(rawToken), TokenPurpose.EMAIL_CONFIRMATION)
                .filter(candidate -> candidate.isUsable(now))
                .orElseThrow(InvalidTokenException::new);
        User user = users.findById(token.userId())
                .filter(candidate -> candidate.status() == UserStatus.PENDING)
                .orElseThrow(InvalidTokenException::new);
        // Checked before consuming the token, so the owner can retry after a typo.
        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new WrongPasswordException();
        }
        if (tokens.consume(token.id(), now) == 0) {
            throw new InvalidTokenException();
        }
        user.activate();
        accounts.openUserAccount(user.id());
    }
}
