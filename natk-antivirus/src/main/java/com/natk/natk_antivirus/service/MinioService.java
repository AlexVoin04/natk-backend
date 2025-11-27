package com.natk.natk_antivirus.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
public class MinioService {

    private final MinioClient client;

    public MinioService(
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey
    ) {
        this.client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    public InputStream download(String bucket, String key) throws Exception {
        return client.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .build()
        );
    }
}
