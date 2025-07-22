package com.natk.natk_api.users;

import java.util.List;

public record UserUpdateDto (
        String name,
        String surname,
        String patronymic,
        String phoneNumber,
        List<String> roles
){
}
