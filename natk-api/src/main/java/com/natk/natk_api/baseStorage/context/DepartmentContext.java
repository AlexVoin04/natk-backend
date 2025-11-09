package com.natk.natk_api.baseStorage.context;

import com.natk.natk_api.users.model.UserEntity;

import java.util.UUID;

public record DepartmentContext(UserEntity user, UUID departmentId) implements StorageContext {}
