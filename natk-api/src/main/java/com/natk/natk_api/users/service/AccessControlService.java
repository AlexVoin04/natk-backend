package com.natk.natk_api.users.service;

import com.natk.natk_api.users.model.UserEntity;

public interface AccessControlService {
    boolean isAdmin(UserEntity user);
    boolean canUpdateUser(UserEntity currentUser, UserEntity targetUser);
}
