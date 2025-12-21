package com.natk.natk_api.minIO;

import com.natk.natk_api.minio.MinioConfig;
import com.natk.natk_api.minio.MinioFileService;
import com.natk.natk_api.minio.MinioMetrics;
import com.natk.natk_api.minio.MinioRetryableService;
import com.natk.natk_api.minio.SecureKeyGenerator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(
        classes = {
                MinioConfig.class,
                MinioRetryableService.class,
                MinioFileService.class,
                MinioMetrics.class,
                SecureKeyGenerator.class,
                MinioFileServiceIT.TestMetersConfig.class
        }
)
@EnableRetry // чтобы @Retryable реально работал через прокси
public class MinioFileServiceIT {

    // MinIO сервер
    @Container
    @SuppressWarnings("resource") // lifecycle управляет Testcontainers JUnit5 extension
    static final GenericContainer<?> minio = new GenericContainer<>(
            DockerImageName.parse("minio/minio:RELEASE.2023-09-04T19-57-37Z")
    )
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withCommand("server", "/data")
            .withExposedPorts(9000)
            .waitingFor(Wait.forHttp("/minio/health/live").forStatusCode(200));

//    static {
//        minio.start(); // <-- гарантирует, что getMappedPort уже доступен
//    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        String endpoint = "http://" + minio.getHost() + ":" + minio.getMappedPort(9000);
        r.add("minio.endpoint", () -> endpoint);
        r.add("minio.access-key", () -> "minioadmin");
        r.add("minio.secret-key", () -> "minioadmin");
    }

    @Autowired
    private MinioFileService minioFileService;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MeterRegistry meterRegistry;

    @TestConfiguration
    static class TestMetersConfig {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry(); // in-memory registry для тестов
        }
    }


    @Test
    void upload_download_delete_roundTrip() throws Exception {
        String bucket = "it-bucket";
        String key = "hello.txt";
        String mime = "text/plain";
        byte[] bytes = "hello-minio".getBytes(StandardCharsets.UTF_8);

        minioFileService.uploadFile(new ByteArrayInputStream(bytes), bytes.length, bucket, key, mime);

        // stat для проверки существования объекта и заданного типа содержимого
        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder().bucket(bucket).object(key).build()
        );
        assertEquals(mime, stat.contentType());

        try (InputStream in = minioFileService.downloadFile(bucket, key)) {
            assertArrayEquals(bytes, in.readAllBytes());
        }

        minioFileService.deleteFile(bucket, key);

        // Проверка удаления (stat завершиться неудачей)
        assertThrows(ErrorResponseException.class, () ->
                minioClient.statObject(StatObjectArgs.builder().bucket(bucket).object(key).build())
        );

        assertNotNull(meterRegistry.find("minio.upload.time").timer());
        assertNotNull(meterRegistry.find("minio.upload.bytes").counter());
        assertNotNull(meterRegistry.find("minio.download.time").timer());
    }

    @Test
    void copyObjectServerSide_copiesObject() throws Exception {
        String bucket = "it-bucket-copy";
        String sourceKey = "a.txt";
        String targetKey = "b.txt";
        byte[] bytes = "copy-me".getBytes(StandardCharsets.UTF_8);

        minioFileService.uploadFile(new ByteArrayInputStream(bytes), bytes.length, bucket, sourceKey, "text/plain");

        minioFileService.copyObjectServerSide(bucket, sourceKey, targetKey);

        try (InputStream in = minioFileService.downloadFile(bucket, targetKey)) {
            assertArrayEquals(bytes, in.readAllBytes());
        }
    }

    @Test
    void deleteFiles_deletesAll_evenIfAlreadyMissing() throws Exception {
        String bucket = "it-bucket-del";
        String k1 = "1.txt";
        String k2 = "2.txt";

        minioFileService.uploadFile(new ByteArrayInputStream(new byte[]{1}), 1, bucket, k1, "application/octet-stream");

        // k2 специально не существует; метод должен быть best-effort и не падать
        assertDoesNotThrow(() -> minioFileService.deleteFiles(bucket, java.util.List.of(k1, k2)));
    }
}
