package com.example.natk_auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RegisterRequestDto(
        @Email String login,
        @Size(min=8) String password,
        String name,
        String surname,
        String patronymic,
        List<String> roles
) {}