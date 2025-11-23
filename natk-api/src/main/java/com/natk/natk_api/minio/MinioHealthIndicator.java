package com.natk.natk_api.minio;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MinioHealthIndicator implements HealthIndicator {

    private final MinioClient minioClient;

    @Override
    public Health health() {
        try {
            minioClient.listBuckets();
            return Health.up().withDetail("minio", "OK").build();
        } catch (Exception e) {
            return Health.down(e).withDetail("minio", "UNAVAILABLE").build();
        }
    }
}
