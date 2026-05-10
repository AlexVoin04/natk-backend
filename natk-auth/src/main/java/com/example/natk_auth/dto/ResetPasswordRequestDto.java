package com.example.natk_auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDto(
        @NotBlank String token,
        @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword
) {}
