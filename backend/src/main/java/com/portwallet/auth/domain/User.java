package com.portwallet.auth.domain;

import com.portwallet.shared.crypto.EncryptedStringConverter;
import com.portwallet.shared.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    private String name;

    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    /** CPF digits, encrypted at rest. */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "document_enc")
    private String document;

    /** Blind index of the CPF, used for uniqueness and lookup. */
    @Column(name = "document_hash")
    private String documentHash;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "totp_secret_enc")
    private String totpSecret;

    @Column(name = "totp_enabled")
    private boolean totpEnabled;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column(name = "created_at")
    private Instant createdAt;

    protected User() {
    }

    private User(String name, String email, Cpf cpf, String documentHash, Instant now) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.email = email;
        this.document = cpf.digits();
        this.documentHash = documentHash;
        this.role = Role.USER;
        this.status = UserStatus.PENDING;
        this.createdAt = now;
    }

    /** New pending user, still without a password: it is chosen on activation, by whoever holds the e-mail link. */
    public static User register(String name, String email, Cpf cpf, String documentHash, Instant now) {
        return new User(name, email, cpf, documentHash, now);
    }

    /**
     * Replaces the data of a registration not yet confirmed with the latest submission, so a typo can be fixed by
     * registering again. Each submission issues a new link that shows the data it carries, so the owner of the e-mail
     * notices if someone else replaced them.
     */
    public void replacePendingRegistration(String name, Cpf cpf, String documentHash) {
        if (status != UserStatus.PENDING) {
            throw new IllegalStateException("Only pending registrations can be replaced");
        }
        this.name = name;
        this.document = cpf.digits();
        this.documentHash = documentHash;
    }

    /** Sets the password chosen on activation and activates the user. */
    public void activate(String passwordHash) {
        if (status != UserStatus.PENDING) {
            throw new IllegalStateException("Only pending users can be activated");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("An active user needs a password");
        }
        this.passwordHash = passwordHash;
        this.status = UserStatus.ACTIVE;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    /** BCrypt hash of the password; empty while the user is pending. */
    public Optional<String> passwordHash() {
        return Optional.ofNullable(passwordHash);
    }

    public Cpf cpf() {
        return new Cpf(document);
    }

    public Role role() {
        return role;
    }

    public UserStatus status() {
        return status;
    }

    public String firstName() {
        return name.strip().split("\\s+")[0];
    }
}
