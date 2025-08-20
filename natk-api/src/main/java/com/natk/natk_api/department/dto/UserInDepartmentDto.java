package com.natk.natk_api.department.dto;


import java.util.UUID;

public record UserInDepartmentDto(
        UUID id,
        String name,
        String surname,
        String patronymic
) {}
