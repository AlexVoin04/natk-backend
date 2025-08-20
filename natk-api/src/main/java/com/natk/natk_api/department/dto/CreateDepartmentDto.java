package com.natk.natk_api.department.dto;


import java.util.UUID;

public record CreateDepartmentDto(
        String name,
        UUID chiefId
) {}
