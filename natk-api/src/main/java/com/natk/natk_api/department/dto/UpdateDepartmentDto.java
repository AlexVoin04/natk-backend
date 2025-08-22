package com.natk.natk_api.department.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateDepartmentDto(
        @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
        String name,
        UUID chiefId
) {}
