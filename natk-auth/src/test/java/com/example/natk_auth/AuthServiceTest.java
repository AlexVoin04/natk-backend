package com.example.natk_auth;

import com.example.natk_auth.dto.LoginRequestDto;
import com.example.natk_auth.dto.TokenDto;
import com.example.natk_auth.dto.RegisterRequestDto;
import com.example.natk_auth.entity.RoleEntity;
import com.example.natk_auth.entity.UserEntity;
import com.example.natk_auth.repository.RoleRepository;
import com.example.natk_auth.repository.UserRepository;
import com.example.natk_auth.service.AuthService;
import com.example.natk_auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    RoleRepository roleRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    JwtService jwtService;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, roleRepository, passwordEncoder, jwtService);
    }

    @Test
    void register_whenUserExists_throws() {
        RegisterRequestDto dto = new RegisterRequestDto("login", "pass", "n", "s", null, List.of("USER"));
        when(userRepository.findByLogin("login")).thenReturn(Optional.of(new UserEntity()));

        assertThrows(IllegalArgumentException.class, () -> service.register(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_ok_encodesPassword_assignsRoles_savesUser() {
        RegisterRequestDto dto = new RegisterRequestDto("login", "pass", "n", "s", null, List.of("USER"));

        when(userRepository.findByLogin("login")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass")).thenReturn("ENC");

        RoleEntity role = new RoleEntity();
        role.setName("USER");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));

        service.register(dto);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());

        UserEntity saved = captor.getValue();
        assertEquals("login", saved.getLogin());
        assertEquals("ENC", saved.getPassword());
        assertNotNull(saved.getId());
        assertEquals(1, saved.getRoles().size());
        assertEquals("USER", saved.getRoles().getFirst().getName());
    }

    @Test
    void login_whenPasswordMismatch_throwsInvalidCredentials() {
        LoginRequestDto dto = new LoginRequestDto("login@mail.ru", "bad");

        UserEntity user = new UserEntity();
        user.setLogin("login@mail.ru");
        user.setPassword("HASH");

        when(userRepository.findByLogin("login@mail.ru")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "HASH")).thenReturn(false);

        assertThrows(AuthService.InvalidCredentialsException.class, () -> service.login(dto));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_ok_returnsTokenFromJwtService() {
        LoginRequestDto dto = new LoginRequestDto("login@mail.ru", "pass");

        UserEntity user = new UserEntity();
        user.setId(java.util.UUID.randomUUID());
        user.setLogin("login@mail.ru");
        user.setPassword("HASH");

        when(userRepository.findByLogin("login@mail.ru")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "HASH")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("JWT");

        TokenDto token = service.login(dto);
        assertEquals("JWT", token.accessToken());
    }
}
