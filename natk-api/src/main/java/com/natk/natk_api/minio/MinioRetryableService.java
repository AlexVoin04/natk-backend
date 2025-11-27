package com.natk.natk_api.minio;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Slf4j
@EnableRetry
@Service
@RequiredArgsConstructor
public class MinioRetryableService {

    private final MinioClient minioClient;

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 300, multiplier = 2, random = true),
            include = { Exception.class }
    )
    public void putObject(PutObjectArgs args) throws Exception {
        log.debug("Uploading object to MinIO: bucket={}, key={}", args.bucket(), args.object());
        minioClient.putObject(args);
    }

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 300, multiplier = 2, random = true),
            include = { Exception.class }
    )
    public InputStream getObject(GetObjectArgs args) throws Exception {
        log.debug("Downloading object from MinIO: bucket={}, key={}", args.bucket(), args.object());
        return minioClient.getObject(args);
    }

    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 400, multiplier = 2, random = true),
            include = { Exception.class }
    )
    public void copyObject(CopyObjectArgs args) throws Exception {
        log.debug("Copying object in MinIO: {} -> {}", args.source().object(), args.object());
        minioClient.copyObject(args);
    }

    @Retryable(
            maxAttempts = 2,
            backoff = @Backoff(delay = 300, multiplier = 2, random = true),
            include = { Exception.class }
    )
    public void removeObject(RemoveObjectArgs args) throws Exception {
        log.debug("Deleting object from MinIO: bucket={}, key={}", args.bucket(), args.object());
        minioClient.removeObject(args);
    }

    // -----------------------------
    // Логируем каждый retry
    // -----------------------------
    @Recover
    public void recover(Exception e, PutObjectArgs args) {
        log.error("Failed to upload after retries: {}/{}", args.bucket(), args.object(), e);
        throw new RuntimeException("Upload error", e);
    }

    @Recover
    public InputStream recover(Exception e, GetObjectArgs args) {
        log.error("Failed to download after retries: {}/{}", args.bucket(), args.object(), e);
        throw new RuntimeException("Download error", e);
    }

    @Recover
    public void recover(Exception e, CopyObjectArgs args) {
        log.error("Failed to copy after retries: {} -> {}", args.source().object(), args.object(), e);
        throw new RuntimeException("Copy error", e);
    }

    @Recover
    public void recover(Exception e, RemoveObjectArgs args) {
        log.error("Failed to delete after retries: {} / {}", args.bucket(), args.object(), e);
        throw new RuntimeException("Delete error", e);
    }
}
