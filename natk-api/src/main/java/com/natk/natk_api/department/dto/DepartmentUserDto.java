package com.natk.natk_api.department.dto;

import java.util.UUID;

public record DepartmentUserDto(
        UUID id,
        UserInDepartmentDto user,
        UUID departmentId //TODO: МБ Стоит убрать
) {}