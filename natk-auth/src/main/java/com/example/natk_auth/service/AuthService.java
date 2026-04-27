package com.example.natk_auth.service;

import com.example.natk_auth.dto.LoginRequestDto;
import com.example.natk_auth.dto.TokenDto;
import com.example.natk_auth.dto.RegisterRequestDto;
import com.example.natk_auth.dto.UserDto;
import com.example.natk_auth.entity.RoleEntity;
import com.example.natk_auth.entity.UserEntity;
import com.example.natk_auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.natk_auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public void register(RegisterRequestDto dto) {
        if (userRepository.findByLogin(dto.login()).isPresent())
            throw new IllegalArgumentException("User already exists");

        UserEntity user = createUser(dto);
        assignDefaultRole(user);
//        assignRolesToUser(user, dto.roles());//пока подумать надо ли оно
        userRepository.save(user);
        log.info("Registered new user: username='{}', userId={}, roles={}", user.getName(), user.getId(), user.getRoles());
    }

    private UserEntity createUser(RegisterRequestDto dto) {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setLogin(dto.login());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setName(dto.name());
        user.setSurname(dto.surname());
        user.setPatronymic(dto.patronymic() != null ? dto.patronymic() : null);
        return user;
    }

    private void assignRolesToUser(UserEntity user, List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            throw new IllegalArgumentException("User must have at least one role");
        }

        List<RoleEntity> roles = roleNames.stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName)))
                .toList();

        user.setRoles(roles);
    }

    public TokenDto login(LoginRequestDto dto) {
        UserEntity user = userRepository.findByLogin(dto.login())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid login or password"));

        if (!passwordEncoder.matches(dto.password(), user.getPassword()))
            throw new InvalidCredentialsException("Invalid login or password");

        return new TokenDto(jwtService.generateToken(user));
    }

    public UserDto getCurrentUser(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new UserDto(user.getId(), user.getName(), user.getSurname());
    }

    public UserEntity findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }

    private void assignDefaultRole(UserEntity user) {
        RoleEntity role = roleRepository.findByName("TEACHER")
                .orElseThrow();
        user.setRoles(List.of(role));
    }
}
