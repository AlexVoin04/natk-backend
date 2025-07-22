package com.example.natk_auth;

import com.example.natk_auth.dto.TokenDto;
import com.example.natk_auth.dto.UserCredentialsDto;
import com.example.natk_auth.entity.UserEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserCredentialsDto dto) {
        authService.register(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenDto> login(@RequestBody UserCredentialsDto dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @GetMapping("/tokens/refresh")
    public ResponseEntity<TokenDto> refresh(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        UserEntity user = authService.findUserById(jwtService.extractUserId(token));
        return ResponseEntity.ok(new TokenDto(jwtService.generateToken(user)));
    }
}
