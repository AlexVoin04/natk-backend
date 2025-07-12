package com.natk.natk_api.users;

import com.natk.natk_api.service.JwtService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserService {


    public UserService(JwtService jwtService, UserRepository userRepository) {

    }

    public UserEntity getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            throw new IllegalStateException("Unauthorized");
        }
        return ((CustomUserDetails) auth.getPrincipal()).getUser();
    }


}
