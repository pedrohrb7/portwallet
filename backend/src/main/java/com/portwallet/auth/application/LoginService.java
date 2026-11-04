package com.portwallet.auth.application;

import com.portwallet.auth.domain.User;
import com.portwallet.auth.infra.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login with e-mail and password. Active and blocked users sign in (a blocked user cannot move money, FR-05); a
 * pending user has no password yet and always fails, with the same answer as an unknown e-mail or a wrong password.
 */
@Service
public class LoginService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService sessions;

    LoginService(UserRepository users, PasswordEncoder passwordEncoder, RefreshTokenService sessions) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.sessions = sessions;
    }

    /** {@code email} is already normalized (trimmed, lower case). */
    @Transactional
    public AuthSession login(String email, String password) {
        User user = users.findByEmail(email)
                .filter(RefreshTokenService::canSignIn)
                .filter(candidate -> candidate.passwordHash()
                        .map(hash -> passwordEncoder.matches(password, hash))
                        .orElse(false))
                .orElseThrow(InvalidCredentialsException::new);
        return sessions.open(user);
    }
}
