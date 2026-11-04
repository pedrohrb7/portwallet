package com.portwallet.auth.application;

import com.portwallet.auth.application.AuthSession.SessionUser;
import com.portwallet.auth.domain.RefreshToken;
import com.portwallet.auth.domain.User;
import com.portwallet.auth.domain.UserStatus;
import com.portwallet.auth.infra.RefreshTokenRepository;
import com.portwallet.auth.infra.UserRepository;
import com.portwallet.shared.crypto.SecureTokens;
import com.portwallet.shared.security.JwtService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issuing and rotation of refresh tokens. A login starts a family valid for {@link #SESSION_TTL}; each refresh revokes
 * the token used and issues the next one of the family, in the same transaction, without extending the family.
 */
@Service
public class RefreshTokenService {

    /** How long a session lasts after the login, however often it is refreshed. */
    public static final Duration SESSION_TTL = Duration.ofDays(7);

    private static final Set<UserStatus> CAN_SIGN_IN = Set.of(UserStatus.ACTIVE, UserStatus.BLOCKED);

    private final RefreshTokenRepository tokens;
    private final UserRepository users;
    private final JwtService jwt;
    private final Clock clock;

    RefreshTokenService(RefreshTokenRepository tokens, UserRepository users, JwtService jwt, Clock clock) {
        this.tokens = tokens;
        this.users = users;
        this.jwt = jwt;
        this.clock = clock;
    }

    static boolean canSignIn(User user) {
        return CAN_SIGN_IN.contains(user.status());
    }

    /** Opens a session for a user who just proved their credentials: a new family and an access token. */
    @Transactional
    AuthSession open(User user) {
        Instant now = clock.instant();
        String raw = SecureTokens.generate();
        RefreshToken token = tokens.save(
                RefreshToken.startFamily(user.id(), SecureTokens.hash(raw), now, now.plus(SESSION_TTL)));
        return session(user, raw, token);
    }

    /** Swaps a valid refresh token for a new one of the same family and a new access token. */
    @Transactional
    public AuthSession rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        Instant now = clock.instant();
        RefreshToken current = tokens.findByTokenHash(SecureTokens.hash(rawToken))
                .filter(candidate -> candidate.isUsable(now))
                .orElseThrow(InvalidRefreshTokenException::new);
        User user = users.findById(current.userId())
                .filter(RefreshTokenService::canSignIn)
                .orElseThrow(InvalidRefreshTokenException::new);
        if (tokens.revoke(current.id()) == 0) {
            throw new InvalidRefreshTokenException();
        }
        String raw = SecureTokens.generate();
        RefreshToken next = tokens.save(current.successor(SecureTokens.hash(raw), now));
        return session(user, raw, next);
    }

    private AuthSession session(User user, String rawRefreshToken, RefreshToken refreshToken) {
        return new AuthSession(
                jwt.issue(user.id(), user.role()),
                rawRefreshToken,
                refreshToken.expiresAt(),
                new SessionUser(user.id(), user.name(), user.email(), user.role()));
    }
}
