package com.portwallet.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token of a session. Only its SHA-256 hash is stored. Tokens of the same login share a family, and every
 * token of a family keeps the family's expiration, so rotating never extends a session.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "family_id")
    private UUID familyId;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    private boolean revoked;

    protected RefreshToken() {
    }

    private RefreshToken(UUID userId, String tokenHash, UUID familyId, Instant now, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    /** First token of a new family, started by a login. */
    public static RefreshToken startFamily(UUID userId, String tokenHash, Instant now, Instant expiresAt) {
        return new RefreshToken(userId, tokenHash, UUID.randomUUID(), now, expiresAt);
    }

    /** Next token of this family, with the family's expiration. */
    public RefreshToken successor(String tokenHash, Instant now) {
        return new RefreshToken(userId, tokenHash, familyId, now, expiresAt);
    }

    public boolean isUsable(Instant now) {
        return !revoked && now.isBefore(expiresAt);
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public UUID familyId() {
        return familyId;
    }

    public Instant expiresAt() {
        return expiresAt;
    }
}
