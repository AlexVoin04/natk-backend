package com.natk.natk_api.minio;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Генератор ключей для minio
 */
@Component
public class SecureKeyGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder BASE64URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    public String generate256BitKey() {
        byte[] keyBytes = new byte[32]; // 256 бит
        SECURE_RANDOM.nextBytes(keyBytes);
        return BASE64URL_ENCODER.encodeToString(keyBytes);
    }
}
