package com.natk.natk_api.controller;

import com.natk.natk_api.repository.UserRepository;
import com.natk.natk_api.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserController {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public UserController(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Missing token");
        }

        String token = authHeader.substring(7);
        UUID userId = jwtService.extractUserId(token);
        return userRepository.findById(userId)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok().body(
                        new UserDto(user.getName(), user.getSurname())
                ))
                .orElse(ResponseEntity.status(404).body("User not found"));
    }

    public record UserDto(String name, String surname) {}
}
