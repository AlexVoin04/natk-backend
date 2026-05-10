package com.example.natk_auth.dto;

import jakarta.validation.constraints.Email;

public record LoginRequestDto(
        @Email String login,
        String password
) {}
