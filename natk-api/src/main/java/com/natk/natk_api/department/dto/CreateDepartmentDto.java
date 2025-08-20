package com.natk.natk_api.department.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateDepartmentDto(
        @NotBlank(message = "The department name cannot be empty")
        String name,
        @NotNull(message = "The chiefId is required")
        UUID chiefId
) {}
