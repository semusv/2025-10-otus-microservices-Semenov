package com.microservices.auth.repository;

import com.microservices.auth.model.RefreshToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    List<RefreshToken> findByUserIdAndRevoked(UUID id, boolean revoked);

    Optional<RefreshToken> findByToken(String token);

    void deleteByUserId(UUID id);
}
