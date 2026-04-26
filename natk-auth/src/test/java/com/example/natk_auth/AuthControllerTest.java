package com.example.natk_auth;

import com.example.natk_auth.dto.TokenDto;
import com.example.natk_auth.dto.UserCredentialsDto;
import com.example.natk_auth.entity.UserEntity;
import com.example.natk_auth.service.AuthService;
import com.example.natk_auth.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@WebMvcTest(controllers = AuthController.class) // поднимает только MVC слой
@AutoConfigureMockMvc(addFilters = false) // отключает Spring Security фильтры
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    JwtService jwtService;

    @Test
    void register_ok_callsService() throws Exception {
        UserCredentialsDto dto = new UserCredentialsDto(
                "login",
                "pass",
                "Ivan",
                "Ivanov",
                null,
                List.of("USER")
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        ArgumentCaptor<UserCredentialsDto> captor = ArgumentCaptor.forClass(UserCredentialsDto.class);
        verify(authService).register(captor.capture());
        assertEquals("login", captor.getValue().login());
        verifyNoMoreInteractions(authService);
    }

    @Test
    void login_ok_returnsToken() throws Exception {
        UserCredentialsDto dto = new UserCredentialsDto(
                "login",
                "pass",
                "Ivan",
                "Ivanov",
                null,
                List.of("USER")
        );

        when(authService.login(any())).thenReturn(new TokenDto("jwt-token"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("jwt-token"));

        verify(authService).login(any(UserCredentialsDto.class));
        verifyNoMoreInteractions(authService);
    }

    @Test
    void refresh_ok_extractsBearerAndReturnsNewToken() throws Exception {
        String incomingJwt = "IN.TOKEN.VALUE";
        UUID userId = UUID.randomUUID();

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setName("Ivan");
        user.setSurname("Ivanov");

        when(jwtService.extractUserId(eq(incomingJwt))).thenReturn(userId);
        when(authService.findUserById(eq(userId))).thenReturn(user);
        when(jwtService.generateToken(eq(user))).thenReturn("NEW.TOKEN");

        mockMvc.perform(get("/auth/tokens/refresh")
                        .header("Authorization", "Bearer " + incomingJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("NEW.TOKEN"));

        verify(jwtService).extractUserId(incomingJwt);
        verify(authService).findUserById(userId);
        verify(jwtService).generateToken(user);
    }
}
