package com.natk.natk_api.minio;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioFileService {

    private final MinioClient minioClient;

    public void uploadFile(byte[] data, String bucket, String objectKey, String initialMimeType) {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }


            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(new ByteArrayInputStream(data), data.length, -1)
                            .contentType(initialMimeType)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Не удалось загрузить файл в MinIO", e);
        }
    }

    public byte[] downloadFile(String bucket, String objectKey) {
        try (GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectKey)
                        .build()
        )) {
            return response.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Не удалось скачать файл из MinIO", e);
        }
    }

    public String generateUserFileKey(UUID userId, UUID fileId) {
        return "user/%s/file/%s".formatted(userId, fileId);
    }

    public String generateDepartmentFileKey(UUID departmentId, UUID fileId) {
        return "department/%s/file/%s".formatted(departmentId, fileId);
    }
}