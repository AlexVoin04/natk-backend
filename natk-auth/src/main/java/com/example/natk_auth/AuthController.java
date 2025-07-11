package com.example.natk_auth;

import com.example.natk_auth.dto.TokenDto;
import com.example.natk_auth.dto.UserCredentialsDto;
import com.example.natk_auth.dto.UserDto;
import com.example.natk_auth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserCredentialsDto dto) {
        User user = authService.register(dto);
        log.info("Registered new user: username='{}', userId={}", user.getName(), user.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenDto> login(@RequestBody UserCredentialsDto dto) {
        log.info("Login attempt: username='{}'", dto.login());
        return ResponseEntity.ok(authService.login(dto));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@RequestHeader("Authorization") String authHeader) {
        log.info("Received /me request with header: {}", authHeader);
        String token = authHeader.replace("Bearer ", "");
        UUID userId = jwtService.extractUserId(token);
        log.info("Accessing /me with userId={}", userId);
        return ResponseEntity.ok(authService.getCurrentUser(userId));
    }
}
