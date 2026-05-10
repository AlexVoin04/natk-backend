package com.example.natk_auth.messaging;

import java.util.UUID;

public record PasswordResetRequestedEvent(
        UUID userId,
        String email,
        String resetLink
) {}
