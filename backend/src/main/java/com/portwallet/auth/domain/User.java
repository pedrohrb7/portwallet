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

    private User(String name, String email, String passwordHash, Cpf cpf, String documentHash, Instant now) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.document = cpf.digits();
        this.documentHash = documentHash;
        this.role = Role.USER;
        this.status = UserStatus.PENDING;
        this.createdAt = now;
    }

    public static User register(String name, String email, String passwordHash, Cpf cpf, String documentHash,
            Instant now) {
        return new User(name, email, passwordHash, cpf, documentHash, now);
    }

    /**
     * Replaces the data of a registration not yet confirmed with the latest submission. Without this, whoever registered
     * an e-mail first (possibly an attacker) would choose the password of the account its owner later activates.
     */
    public void replacePendingRegistration(String name, String passwordHash, Cpf cpf, String documentHash) {
        if (status != UserStatus.PENDING) {
            throw new IllegalStateException("Only pending registrations can be replaced");
        }
        this.name = name;
        this.passwordHash = passwordHash;
        this.document = cpf.digits();
        this.documentHash = documentHash;
    }

    public void activate() {
        if (status != UserStatus.PENDING) {
            throw new IllegalStateException("Only pending users can be activated");
        }
        status = UserStatus.ACTIVE;
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

    public String passwordHash() {
        return passwordHash;
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
