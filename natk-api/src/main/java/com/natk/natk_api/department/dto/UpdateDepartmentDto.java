package com.natk.natk_api.department.dto;

import java.util.UUID;

public record UpdateDepartmentDto(
        String name,
        UUID chiefId
) {}
