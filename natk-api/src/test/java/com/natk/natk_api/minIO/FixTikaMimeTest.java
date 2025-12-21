package com.natk.natk_api.minIO;

import io.minio.GetObjectArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
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

import java.io.ByteArrayInputStream;

/**
 * Исправление неверного Content-Type application/x-tika-ooxml с Content-Type octet-stream → правильные MIME для Office
 */
@Disabled("Использовался при переходе на минио.")
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = FixTikaMimeTest.MinioConfig.class)
public class FixTikaMimeTest {

    @Configuration
    @PropertySource("classpath:application.properties")
    static class MinioConfig {
        @Value("${minio.endpoint.local}")
        private String endpoint;

        @Value("${minio.access-key}")
        private String accessKey;

        @Value("${minio.secret-key}")
        private String secretKey;

        @Bean
        public MinioClient minioClient() {
            return MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
        }
    }

    @Autowired
    private MinioClient minioClient;

    private static final String[] BUCKETS = {"user-files", "department-files"};
    private static final String NEW_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    @Test
    void fixOctetStreamFiles() throws Exception {
        for (String bucket : BUCKETS) {
            System.out.println("🔍 Проверяем бакет: " + bucket);

            for (Result<Item> result : minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(bucket).recursive(true).build())) {

                Item item = result.get();
                if (item.isDir()) continue;

                StatObjectResponse stat = minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucket)
                                .object(item.objectName())
                                .build()
                );

                if (!"application/octet-stream".equals(stat.contentType())) continue;

                System.out.println("🔧 Исправляем: " + item.objectName());

                // Скачиваем объект
                try (var stream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucket)
                                .object(item.objectName())
                                .build()
                )) {
                    byte[] data = stream.readAllBytes();

                    // Перезаписываем с новым Content-Type
                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(bucket)
                                    .object(item.objectName())
                                    .stream(new ByteArrayInputStream(data), data.length, -1)
                                    .contentType(NEW_CONTENT_TYPE)
                                    .build()
                    );

                    System.out.println("✅ Исправлено: " + NEW_CONTENT_TYPE);
                }
            }
        }
    }
}
