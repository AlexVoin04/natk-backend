package com.natk.natk_api.department.dto;


import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddDepartmentUserDto(
        @NotNull(message = "The userId is required")
        UUID userId,
        @NotNull(message = "The departmentId is required")
        UUID departmentId
) {}
