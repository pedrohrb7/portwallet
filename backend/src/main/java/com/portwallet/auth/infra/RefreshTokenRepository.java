package com.portwallet.auth.infra;

import com.portwallet.auth.domain.RefreshToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revokes the token only if nobody did it before. Returns 0 when another request already rotated it, so a token
     * is rotated at most once even under simultaneous requests.
     */
    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = TRUE WHERE t.id = :id AND t.revoked = FALSE")
    int revoke(@Param("id") UUID id);
}
