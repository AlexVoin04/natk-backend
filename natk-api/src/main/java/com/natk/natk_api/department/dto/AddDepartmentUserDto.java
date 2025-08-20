package com.natk.natk_api.department.dto;


import java.util.UUID;

public record AddDepartmentUserDto(
        UUID userId,
        UUID departmentId
) {}
