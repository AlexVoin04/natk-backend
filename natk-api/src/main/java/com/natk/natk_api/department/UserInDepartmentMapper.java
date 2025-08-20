package com.natk.natk_api.department;

import com.natk.natk_api.department.dto.UserInDepartmentDto;
import com.natk.natk_api.users.model.UserEntity;

public class UserInDepartmentMapper {
    public static UserInDepartmentDto toUserInDepartmentDto(UserEntity entity) {
        if (entity == null) return null;
        return new UserInDepartmentDto(
                entity.getId(),
                entity.getName(),
                entity.getSurname(),
                entity.getPatronymic()
        );
    }
}
