package com.example.natk_auth;

import com.example.natk_auth.entity.UserEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtServiceTest {

    @Test
    void generateToken_and_extractUserId_roundTrip() throws Exception {
        JwtService jwtService = new JwtService();

        // проставляем @Value поля вручную
        setField(jwtService, "secret", "01234567890123456789012345678901"); // 32+ bytes для HMAC
        setField(jwtService, "expiration", 60_000L);
        jwtService.init();

        UserEntity user = new UserEntity();
        UUID id = UUID.randomUUID();
        user.setId(id);
        user.setName("Ivan");
        user.setSurname("Ivanov");

        String token = jwtService.generateToken(user);
        UUID extracted = jwtService.extractUserId(token);

        assertEquals(id, extracted);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}
