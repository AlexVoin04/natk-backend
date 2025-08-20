package com.natk.natk_api.department.dto;

import com.natk.natk_api.users.dto.UserDto;

import java.util.UUID;

public record DepartmentUserDto(
        UUID id,
        UserInDepartmentDto user,
        UUID departmentId
) {}