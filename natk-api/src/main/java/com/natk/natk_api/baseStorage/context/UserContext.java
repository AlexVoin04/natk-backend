package com.natk.natk_api.baseStorage.context;

import com.natk.natk_api.users.model.UserEntity;

public record UserContext(UserEntity user) implements StorageContext {}
