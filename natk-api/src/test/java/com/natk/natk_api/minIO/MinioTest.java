package com.natk.natk_api.minIO;

import io.minio.BucketExistsArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Тест только конфигурации MinIO, без запуска Spring Boot.
 */
@Disabled("Ручной smoke-тест MinIO. Запускать локально при необходимости.")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MinioTest.MinioTestConfig.class)
public class MinioTest {

    @Configuration
    @PropertySource("classpath:application.properties")
    static class MinioTestConfig {
        @Value("${minio.endpoint.local}")
        private String endpoint;

        @Value("${minio.access-key}")
        private String accessKey;

        @Value("${minio.secret-key}")
        private String secretKey;

        @Bean
        public MinioClient minioClient() {
            System.out.println("🔧 MinIO config loaded:");
            System.out.println("endpoint = " + endpoint);
            System.out.println("accessKey = " + accessKey);
            return MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
        }
    }

    @Autowired
    private MinioClient minioClient;

    private static final String BUCKET_NAME = "test-bucket";

    /**
     * Загрузка файла в MinIO
     */
    @Test
    void uploadFile() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET_NAME).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET_NAME).build());
            System.out.println("✅ Bucket создан: " + BUCKET_NAME);
        }

        Path path = Path.of(System.getProperty("user.home"), "Downloads", "стажировка.pdf");
        if (!Files.exists(path)) {
            System.err.println("❌ Файл не найден: " + path);
            return;
        }

        try (FileInputStream in = new FileInputStream(path.toFile())) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(BUCKET_NAME)
                    .object(path.getFileName().toString())
                    .stream(in, Files.size(path), -1)
                    .contentType("application/pdf")
                    .build());
            System.out.println("✅ Загружен: " + path.getFileName());
        }
    }

    /**
     * Получение списка файлов в bucket
     */
    @Test
    void listFiles() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET_NAME).build());
        if (!exists) {
            System.err.println("❌ Bucket не найден: " + BUCKET_NAME);
            return;
        }

        System.out.println("📂 Содержимое bucket `" + BUCKET_NAME + "`:");
        for (Result<Item> result : minioClient.listObjects(ListObjectsArgs.builder().bucket(BUCKET_NAME).build())) {
            Item item = result.get();

            // Основная информация из listObjects
            System.out.println("Файл: " + item.objectName());
            System.out.println("Размер: " + item.size() + " bytes");
            System.out.println("Дата изменения: " + item.lastModified());
            System.out.println("ETag: " + item.etag());
            System.out.println("Класс хранения: " + item.storageClass());
            System.out.println("Это директория? " + item.isDir());

            // Дополнительные метаданные через statObject
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(item.objectName())
                            .build());

            System.out.println("Content-Type: " + stat.contentType());
            System.out.println("User Metadata: " + stat.userMetadata()); // пользовательские метаданные
            System.out.println("Version ID: " + stat.versionId());
            System.out.println("Маркер удаления: " + stat.deleteMarker());
            System.out.println("----------");
        }
    }
}
