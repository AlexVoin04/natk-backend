package com.natk.natk_api.purgeStorage;


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
    private final UserFolderRepository folderRepo;
    private final UserFileRepository fileRepo;


    @Transactional
    public void purgeDeletedItemsOlderThan(Duration duration) {
        Instant cutoff = Instant.now().minus(duration);
        log.info("=== Запуск очистки устаревших файлов и папок старше {} ===", duration);

        List<UserFileEntity> oldDeletedFiles = fileRepo.findByIsDeletedTrueAndDeletedAtBefore(cutoff);
        int filesCount = oldDeletedFiles.size();
        fileRepo.deleteAll(oldDeletedFiles);

        List<UserFolderEntity> oldDeletedFolders = folderRepo.findByIsDeletedTrueAndDeletedAtBefore(cutoff);
        int foldersCount = oldDeletedFolders.size();
        folderRepo.deleteAll(oldDeletedFolders);

        log.info("=== Очистка завершена: {} файлов, {} папок удалено ===", filesCount, foldersCount);

    }

}
