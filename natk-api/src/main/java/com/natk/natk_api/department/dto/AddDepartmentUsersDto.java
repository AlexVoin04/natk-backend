package com.natk.natk_api.department.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AddDepartmentUsersDto(
        @NotNull(message = "departmentId is required")
        UUID departmentId,

        @NotEmpty(message = "At least one userId is required")
        List<UUID> userIds
) {}
