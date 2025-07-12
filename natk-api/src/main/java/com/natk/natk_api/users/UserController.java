package com.natk.natk_api.users;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

//    @GetMapping("/me")
//    public UserEntity getCurrentUser() {
//        return userService.getCurrentUser();
//    }

    @GetMapping("/me")
    public UserDto getCurrentUser() {
        UserEntity userEntity = userService.getCurrentUser();
        return userMapper.toDto(userEntity);
    }
}
