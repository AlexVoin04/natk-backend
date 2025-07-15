package com.natk.natk_api.users;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {
    public UserDto toDto(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        List<String> roles = entity.getRoles() == null
                ? List.of()
                : entity.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toList());

        return new UserDto(
                entity.getId(),
                entity.getLogin(),
                entity.getName(),
                entity.getSurname(),
                entity.getPatronymic(),
                entity.getPhoneNumber(),
                roles
        );
    }
}
