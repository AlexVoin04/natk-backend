package com.example.natk_auth;

import com.example.natk_auth.dto.ForgotPasswordRequestDto;
import com.example.natk_auth.dto.ResetPasswordRequestDto;
import com.example.natk_auth.service.JwtService;
import com.example.natk_auth.dto.TokenDto;
import com.example.natk_auth.dto.UserCredentialsDto;
import com.example.natk_auth.entity.UserEntity;
import com.example.natk_auth.service.AuthService;
import com.example.natk_auth.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;

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

    @PostMapping("/password/forgot")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto dto) {
        passwordResetService.requestReset(dto);
        return ResponseEntity.ok(Map.of("message", "If the account exists, a reset email has been sent"));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDto dto) {
        passwordResetService.resetPassword(dto);
        return ResponseEntity.ok().build();
    }
}
