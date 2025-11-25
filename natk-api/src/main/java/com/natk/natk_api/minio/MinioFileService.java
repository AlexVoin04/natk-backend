package com.natk.natk_api.minio;

import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileService {

    private final MinioClient minioClient;
    private final MinioMetrics metrics;
    private final SecureKeyGenerator keyGenerator;

    /**
     * Кеш существующих бакетов (без повторных обращений в MinIO)
     */
    private final ConcurrentHashMap<String, Boolean> bucketCache = new ConcurrentHashMap<>();

    private void ensureBucketExists(String bucket) {
        bucketCache.computeIfAbsent(bucket, b -> {
            try {
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(b).build()
                );

                if (!exists) {
                    minioClient.makeBucket(
                            MakeBucketArgs.builder().bucket(b).build()
                    );
                }

                return true;
            } catch (Exception e) {
                throw new RuntimeException("Failed to create/check MinIO bucket: " + b, e);
            }
        });
    }

    public void uploadFile(InputStream stream, long size, String bucket, String objectKey, String initialMimeType) {
        ensureBucketExists(bucket);

        long start = System.currentTimeMillis();
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(stream, size, -1)
                            .contentType(initialMimeType)
                            .build()
            );

            long took = System.currentTimeMillis() - start;
            metrics.recordUpload(size, took);
        } catch (Exception e) {
            metrics.recordError();
            throw new RuntimeException("Не удалось загрузить файл в MinIO", e);
        }
    }

    public InputStream downloadFile(String bucket, String objectKey) {
        ensureBucketExists(bucket);

        long start = System.currentTimeMillis();
        try {
            InputStream is = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );

            long took = System.currentTimeMillis() - start;
            metrics.recordDownload(took);

            return is;
        } catch (Exception e) {
            metrics.recordError();
            throw new RuntimeException("Не удалось скачать файл из MinIO", e);
        }
    }

    public void deleteFile(String bucket, String objectKey) {
        ensureBucketExists(bucket);

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            metrics.recordError();
            throw new RuntimeException("Не удалось удалить файл из MinIO: " + bucket + "/" + objectKey, e);
        }
    }

    public void deleteFiles(String bucket, List<String> keys) {
        for (String key : keys) {
            try {
                deleteFile(bucket, key);
            } catch (Exception ex) {
                log.error("Не удалось удалить файл {} из MinIO", key, ex);
            }
        }
    }

    public byte[] downloadFileAsBytes(String bucket, String objectKey) {
        try (InputStream is = downloadFile(bucket, objectKey)) {
            return is.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to download file into memory", e);
        }
    }

    public void copyObjectServerSide(String bucket, String sourceKey, String targetKey) throws Exception {
        minioClient.copyObject(
                CopyObjectArgs.builder()
                        .bucket(bucket)
                        .object(targetKey)
                        .source(CopySource.builder().bucket(bucket).object(sourceKey).build())
                        .build()
        );
    }

    public String generateUserFileKey(UUID userId) {
        String randomKey = keyGenerator.generate256BitKey();
        return "user/%s/file/%s".formatted(userId, randomKey);
    }

    public String generateIncomingUserFileKey(UUID userId) {
        String randomKey = keyGenerator.generate256BitKey();
        return "incoming/%s/file/%s".formatted(userId, randomKey);
    }

    public String generateDepartmentFileKey(UUID departmentId) {
        String randomKey = keyGenerator.generate256BitKey();
        return "department/%s/file/%s".formatted(departmentId, randomKey);
    }
}