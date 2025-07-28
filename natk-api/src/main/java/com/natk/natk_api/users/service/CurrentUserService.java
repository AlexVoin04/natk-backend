package com.natk.natk_api.users.service;

import com.natk.natk_api.users.model.UserEntity;

public interface CurrentUserService {
    UserEntity getCurrentUser();
}
