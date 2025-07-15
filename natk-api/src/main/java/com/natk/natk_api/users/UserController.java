package com.natk.natk_api.users;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping("/me")
    public UserDto getCurrentUser() {
        UserEntity userEntity = userService.getCurrentUser();
        return userMapper.toDto(userEntity);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public List<UserDto> getAllUsers() {
        return userService.getAllUsers().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/filter")
    public Page<UserDto> getFilteredUsers(
            @RequestParam(required = false, defaultValue = "") String role,
            @RequestParam(required = false, defaultValue = "") String name,
            @RequestParam(required = false, defaultValue = "") String surname,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "none") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Page<UserEntity> userPage = userService.getUsersFiltered(role, name, surname, page, size, sortBy, direction);
        return userPage.map(userMapper::toDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public UserDto getUserById(@PathVariable UUID id) {
        UserEntity user = userService.getUserById(id);
        return userMapper.toDto(user);
    }
}
