package com.natk.natk_api.users.service;

import com.natk.natk_api.users.model.UserEntity;
import org.springframework.stereotype.Service;

@Service
public class AccessControlServiceImpl implements AccessControlService {

    @Override
    public boolean isAdmin(UserEntity user) {
        return user.getRoles().stream()
                .anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getName()));
    }

    @Override
    public boolean canUpdateUser(UserEntity currentUser, UserEntity targetUser) {
        return isAdmin(currentUser) || currentUser.getId().equals(targetUser.getId());
    }
}