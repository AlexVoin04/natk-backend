package com.example.natk_auth.dto;

import java.util.UUID;

public record UserDto(UUID id, String name, String surname) {}