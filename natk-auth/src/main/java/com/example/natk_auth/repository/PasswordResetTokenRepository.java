package com.example.natk_auth.repository;

import com.example.natk_auth.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {
    Optional<PasswordResetTokenEntity> findByTokenHashAndUsedAtIsNull(String tokenHash);

    long deleteByExpiresAtBefore(Instant now);
}
