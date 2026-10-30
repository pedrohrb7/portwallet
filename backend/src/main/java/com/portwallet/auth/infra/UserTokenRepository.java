package com.portwallet.auth.infra;

import com.portwallet.auth.domain.TokenPurpose;
import com.portwallet.auth.domain.UserToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserTokenRepository extends JpaRepository<UserToken, UUID> {

    Optional<UserToken> findByTokenHashAndPurpose(String tokenHash, TokenPurpose purpose);

    /**
     * Marks the token as used only if nobody did it before. Returns 0 when another request already consumed it, which
     * makes the single use hold even for simultaneous requests.
     */
    @Modifying
    @Query("UPDATE UserToken t SET t.usedAt = :now WHERE t.id = :id AND t.usedAt IS NULL")
    int consume(@Param("id") UUID id, @Param("now") Instant now);

    /** Invalidates every pending token of a user for the given purpose, so only the newest link works. */
    @Modifying
    @Query("UPDATE UserToken t SET t.usedAt = :now WHERE t.userId = :userId AND t.purpose = :purpose "
            + "AND t.usedAt IS NULL")
    int invalidateAll(@Param("userId") UUID userId, @Param("purpose") TokenPurpose purpose, @Param("now") Instant now);
}
