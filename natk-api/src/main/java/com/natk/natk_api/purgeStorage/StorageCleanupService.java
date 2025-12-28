package com.natk.natk_api.purgeStorage;


import com.natk.natk_api.baseStorage.enums.BucketName;
import com.natk.natk_api.departmentStorage.model.DepartmentFileEntity;
import com.natk.natk_api.departmentStorage.model.DepartmentFolderEntity;
import com.natk.natk_api.departmentStorage.repository.DepartmentFileRepository;
import com.natk.natk_api.departmentStorage.repository.DepartmentFolderRepository;
import com.natk.natk_api.minio.MinioFileService;
import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.userStorage.repository.UserFileRepository;
import com.natk.natk_api.userStorage.repository.UserFolderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageCleanupService {

    private final UserFileRepository userFileRepo;
    private final UserFolderRepository userFolderRepo;
    private final DepartmentFileRepository deptFileRepo;
    private final DepartmentFolderRepository deptFolderRepo;
    private final MinioFileService minioFileService;

    private static final int BATCH_SIZE = 100; // пакетная обработка MinIO


    @Transactional
    public void purgeDeletedItemsOlderThan(Duration duration) {
        Instant cutoff = Instant.now().minus(duration);
        log.info("=== Запуск очистки устаревших файлов и папок старше {} ===", duration);

        purgeUserFiles(userFileRepo.findByIsDeletedTrueAndDeletedAtBefore(cutoff));
        purgeDepartmentFiles(deptFileRepo.findByIsDeletedTrueAndDeletedAtBefore(cutoff));

        purgeUserFolders(userFolderRepo.findByIsDeletedTrueAndDeletedAtBefore(cutoff));
        purgeDepartmentFolders(deptFolderRepo.findByIsDeletedTrueAndDeletedAtBefore(cutoff));

        log.info("=== Очистка завершена ===");
    }

    private void purgeUserFiles(List<UserFileEntity> files) {
        log.info("Найдено {} удалённых файлов User", files.size());
        for (int i = 0; i < files.size(); i += BATCH_SIZE) {
            List<UserFileEntity> batch = files.subList(i, Math.min(i + BATCH_SIZE, files.size()));
            List<String> keys = batch.stream()
                    .map(UserFileEntity::getStorageKey)
                    .filter(k -> k != null && !k.isEmpty())
                    .toList();
            try {
                minioFileService.deleteFiles(BucketName.USER_FILES.value(), keys);
            } catch (Exception e) {
                log.error("Ошибка при удалении файлов User: {}", e.getMessage(), e);
            }
        }
        userFileRepo.deleteAll(files);
    }

    private void purgeDepartmentFiles(List<DepartmentFileEntity> files) {
        log.info("Найдено {} удалённых файлов Department", files.size());
        for (int i = 0; i < files.size(); i += BATCH_SIZE) {
            List<DepartmentFileEntity> batch = files.subList(i, Math.min(i + BATCH_SIZE, files.size()));
            List<String> keys = batch.stream()
                    .map(DepartmentFileEntity::getStorageKey)
                    .filter(k -> k != null && !k.isEmpty())
                    .toList();
            try {
                minioFileService.deleteFiles(BucketName.DEPARTMENTS_FILES.value(), keys);
            } catch (Exception e) {
                log.error("Ошибка при удалении файлов Department: {}", e.getMessage(), e);
            }
        }
        deptFileRepo.deleteAll(files);
    }

    private void purgeUserFolders(List<UserFolderEntity> folders) {
        log.info("Удаление {} устаревших папок User", folders.size());
        userFolderRepo.deleteAll(folders);
    }

    private void purgeDepartmentFolders(List<DepartmentFolderEntity> folders) {
        log.info("Удаление {} устаревших папок Department", folders.size());
        deptFolderRepo.deleteAll(folders);
    }

}
