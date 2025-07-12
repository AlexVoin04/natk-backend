package com.natk.natk_api.users;

import java.util.List;
import java.util.UUID;

public record UserDto(UUID id, String name, String surname, String patronymic, String phoneNumber, List<String> roles) {}
