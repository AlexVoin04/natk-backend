package com.example.natk_auth.service;

import com.example.natk_auth.config.RabbitConfig;
import com.example.natk_auth.dto.ForgotPasswordRequestDto;
import com.example.natk_auth.dto.ResetPasswordRequestDto;
import com.example.natk_auth.entity.PasswordResetTokenEntity;
import com.example.natk_auth.entity.UserEntity;
import com.example.natk_auth.messaging.PasswordResetRequestedEvent;
import com.example.natk_auth.repository.PasswordResetTokenRepository;
import com.example.natk_auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.frontend.reset-password-url}")
    private String resetPasswordUrl;

    private static final int TOKEN_TTL_MINUTES = 30;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void requestReset(ForgotPasswordRequestDto dto) {
        Optional<UserEntity> userOpt = userRepository.findByLogin(dto.email());

        if (userOpt.isEmpty()) {
            return;
        }

        UserEntity user = userOpt.get();

        String rawToken = generateToken();
        String tokenHash = sha256(rawToken);
        Instant now = Instant.now();

        PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash(tokenHash)
                .createdAt(now)
                .expiresAt(now.plusSeconds(TOKEN_TTL_MINUTES * 60L))
                .build();

        tokenRepository.save(token);

        String link = resetPasswordUrl + "?token=" + UriUtils.encodePathSegment(rawToken, StandardCharsets.UTF_8);

        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.RESET_REQUESTED_ROUTING_KEY,
                new PasswordResetRequestedEvent(user.getId(), user.getLogin(), link)
        );

        log.info("Password reset requested for userId={}, email={}", user.getId(), user.getLogin());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequestDto dto) {
        String hash = sha256(dto.token());

        PasswordResetTokenEntity token = tokenRepository.findByTokenHashAndUsedAtIsNull(hash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        UserEntity user = token.getUser();
        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        tokenRepository.save(token);

        log.info("Password successfully reset for userId={}", user.getId());
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new RuntimeException("Cannot hash token", e);
        }
    }
}
