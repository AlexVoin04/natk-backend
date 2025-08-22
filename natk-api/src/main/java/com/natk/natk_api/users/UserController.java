package com.natk.natk_api.users;

import com.natk.natk_api.users.dto.UserDto;
import com.natk.natk_api.users.dto.UserFilterCriteria;
import com.natk.natk_api.users.dto.UserUpdateDto;
import com.natk.natk_api.users.model.UserEntity;
import com.natk.natk_api.users.service.CurrentUserService;
import com.natk.natk_api.users.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;

    public UserController(UserService userService, UserMapper userMapper, CurrentUserService currentUserService) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/me")
    public UserDto getCurrentUser() {
        UserEntity userEntity = currentUserService.getCurrentUser();
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
        UserFilterCriteria criteria = new UserFilterCriteria(role, name, surname, page, size, sortBy, direction);
        Page<UserEntity> userPage = userService.getUsersFiltered(criteria);
        return userPage.map(userMapper::toDto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public UserDto getUserById(@PathVariable UUID id) {
        UserEntity user = userService.getUserById(id);
        return userMapper.toDto(user);
    }

    @PutMapping("/{id}")
    public UserDto updateUser(@PathVariable UUID id, @RequestBody UserUpdateDto dto) {
        UserEntity updatedUser = userService.updateUser(id, dto);
        return userMapper.toDto(updatedUser);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }
}
