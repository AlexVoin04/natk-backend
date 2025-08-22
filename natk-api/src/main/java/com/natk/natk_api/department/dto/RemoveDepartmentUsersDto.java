package com.natk.natk_api.department.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record RemoveDepartmentUsersDto(
        @NotEmpty(message = "At least one userId is required")
        List<UUID> userIds
) {}
