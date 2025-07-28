package com.natk.natk_api.users.service;

import com.natk.natk_api.auth.CustomUserDetails;
import com.natk.natk_api.users.model.UserEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserServiceImpl implements CurrentUserService {
    @Override
    public UserEntity getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            throw new IllegalStateException("Unauthorized");
        }
        return ((CustomUserDetails) auth.getPrincipal()).getUser();
    }
}
