package com.natk.natk_api.minio;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@EnableRetry
@Service
@RequiredArgsConstructor
public class MinioRetryableService {

    private final MinioClient minioClient;

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2),
            include = { Exception.class }
    )
    public void putObject(PutObjectArgs args) throws Exception {
        minioClient.putObject(args);
    }

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 200, multiplier = 2),
            include = { Exception.class }
    )
    public InputStream getObject(GetObjectArgs args) throws Exception {
        return minioClient.getObject(args);
    }
}
