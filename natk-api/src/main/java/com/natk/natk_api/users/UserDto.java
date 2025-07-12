package com.natk.natk_api.users;

import java.util.UUID;

public record UserDto(UUID id, String name, String surname) {}
