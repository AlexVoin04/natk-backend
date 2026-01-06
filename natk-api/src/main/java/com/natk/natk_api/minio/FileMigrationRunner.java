package com.natk.natk_api.minio;

import com.natk.natk_api.baseStorage.enums.BucketName;
import com.natk.natk_api.departmentStorage.model.DepartmentFileEntity;
import com.natk.natk_api.departmentStorage.repository.DepartmentFileRepository;
import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.repository.UserFileRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.Map;

@Component
@Profile("migration")
@RequiredArgsConstructor
public class FileMigrationRunner implements CommandLineRunner {

    private final MinioClient minioClient;
    private final DepartmentFileRepository departmentRepo;
    private final UserFileRepository userRepo;

    @Override
    public void run(String... args) throws Exception {
        ensureBucketExists(BucketName.DEPARTMENTS_FILES.value());
        ensureBucketExists(BucketName.USER_FILES.value());

        migrateDepartmentFiles();
        migrateUserFiles();
    }

    private void migrateDepartmentFiles() throws Exception {
        for (DepartmentFileEntity file : departmentRepo.findAll()) {

            byte[] data = file.getFileData();
            if (data == null) continue;

            String key = "department/%s/file/%s".formatted(
                    file.getDepartment().getId(),
                    file.getId()
            );

            try {
                String contentType = getMimeTypeByName(file.getName());

                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(BucketName.DEPARTMENTS_FILES.value())
                                .object(key)
                                .stream(new ByteArrayInputStream(data), data.length, -1)
                                .contentType(contentType)
                                .build()
                );

                file.setStorageKey(key);
                file.setFileData(null);
                departmentRepo.save(file);

                System.out.println("✅ Успешно мигрировано: " + key);

            } catch (Exception e) {

                System.err.println("❌ Ошибка загрузки " + key + ": " + e.getMessage());
            }
        }
    }

    private void migrateUserFiles() throws Exception {
        for (UserFileEntity file : userRepo.findAll()) {

            byte[] data = file.getFileData();
            if (data == null) continue;

            String key = "user/%s/file/%s".formatted(
                    file.getCreatedBy().getId(),
                    file.getId()
            );

            try {
                String contentType = getMimeTypeByName(file.getName());

                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(BucketName.USER_FILES.value())
                                .object(key)
                                .stream(new ByteArrayInputStream(data), data.length, -1)
                                .contentType(contentType)
                                .build()
                );

                file.setStorageKey(key);
                file.setFileData(null);
                userRepo.save(file);

                System.out.println("✅ Успешно мигрировано: " + key);

            } catch (Exception e) {
                System.err.println("❌ Ошибка загрузки " + key + ": " + e.getMessage());
            }
        }
    }

    private String getMimeTypeByName(String fileName) {
        String ext = "";

        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            ext = fileName.substring(dotIndex + 1).toLowerCase();
        }

        return EXT_TO_MIME.getOrDefault(ext, "application/octet-stream");
    }

    private void ensureBucketExists(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build()
        );

        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucket).build()
            );
            System.out.println("🪣 Создан bucket: " + bucket);
        }
    }

    private static final Map<String, String> EXT_TO_MIME = Map.ofEntries(
            Map.entry("pdf",  "application/pdf"),
            Map.entry("jpg",  "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png",  "image/png"),
            Map.entry("gif",  "image/gif"),

            Map.entry("doc",  "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),

            Map.entry("xls",  "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),

            Map.entry("ppt",  "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),

            Map.entry("txt",  "text/plain"),
            Map.entry("csv",  "text/csv"),
            Map.entry("json", "application/json"),
            Map.entry("xml",  "application/xml"),
            Map.entry("zip",  "application/zip")
    );
}

