package com.example.natk_auth;

import com.example.natk_auth.dto.TokenDto;
import com.example.natk_auth.dto.UserCredentialsDto;
import com.example.natk_auth.dto.UserDto;
import com.example.natk_auth.entity.User;
import com.example.natk_auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public User register(UserCredentialsDto dto) {
        if (userRepository.findByLogin(dto.login()).isPresent())
            throw new IllegalArgumentException("User already exists");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setLogin(dto.login());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setName("John");
        user.setSurname("Doe");
        userRepository.save(user);
        return user;
    }

    public TokenDto login(UserCredentialsDto dto) {
        User user = userRepository.findByLogin(dto.login())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(dto.password(), user.getPassword()))
            throw new IllegalArgumentException("Invalid credentials");

        return new TokenDto(jwtService.generateToken(user));
    }

    public UserDto getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new UserDto(user.getId(), user.getName(), user.getSurname());
    }
}
