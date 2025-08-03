package com.natk.natk_api.userStorage.service;

import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.userStorage.repository.UserFileRepository;
import com.natk.natk_api.userStorage.repository.UserFolderRepository;
import com.natk.natk_api.users.model.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StorageLifecycleService {
    private final UserFolderRepository folderRepo;
    private final UserFileRepository fileRepo;

    @Transactional
    public void deleteFolderRecursive(UserFolderEntity folder, UserEntity user) {
        Instant now = Instant.now();

        List<UserFileEntity> files = fileRepo.findByCreatedByAndFolderAndIsDeletedFalse(user, folder);
        files.forEach(file -> {
            file.setDeleted(true);
            file.setDeletedAt(now);
        });
        fileRepo.saveAll(files);

        List<UserFolderEntity> children = folderRepo.findByUserAndParentFolderAndIsDeletedFalse(user, folder);
        for (UserFolderEntity child : children) {
            deleteFolderRecursive(child, user);
        }

        folder.setDeleted(true);
        folder.setDeletedAt(now);
        folderRepo.save(folder);
    }

    @Transactional
    public void restoreFolderRecursive(UserFolderEntity folder, UserEntity user) {
        folder.setDeleted(false);
        folder.setDeletedAt(null);
        folderRepo.save(folder);

        List<UserFileEntity> files = fileRepo.findByCreatedByAndFolderAndIsDeletedTrue(user, folder);
        files.forEach(file -> {
            file.setDeleted(false);
            file.setDeletedAt(null);
        });
        fileRepo.saveAll(files);

        List<UserFolderEntity> children = folderRepo.findByUserAndParentFolderAndIsDeletedTrue(user, folder);
        for (UserFolderEntity child : children) {
            restoreFolderRecursive(child, user);
        }
    }

    /**
     * Сбор всех подпапок и файлов, начиная с переданной папки.
     * @param root корневая папка
     * @param user владелец
     * @param deleted флаг для фильтрации: true = собрать удалённые, false = не удалённые
     * @return собранное дерево
     */
    private FolderTree collectTree(UserFolderEntity root, UserEntity user, boolean deleted) {
        List<UserFolderEntity> folders = new ArrayList<>();
        List<UserFileEntity> files = new ArrayList<>();

        collectRecursive(root, user, deleted, folders, files);

        return new FolderTree(folders, files);
    }

    private void collectRecursive(
            UserFolderEntity folder,
            UserEntity user,
            boolean deleted,
            List<UserFolderEntity> folders,
            List<UserFileEntity> files
    ) {
        folders.add(folder);

        List<UserFileEntity> fileList = deleted
                ? fileRepo.findByCreatedByAndFolderAndIsDeletedTrue(user, folder)
                : fileRepo.findByCreatedByAndFolderAndIsDeletedFalse(user, folder);
        files.addAll(fileList);

        List<UserFolderEntity> children = deleted
                ? folderRepo.findByUserAndParentFolderAndIsDeletedTrue(user, folder)
                : folderRepo.findByUserAndParentFolderAndIsDeletedFalse(user, folder);

        for (UserFolderEntity child : children) {
            collectRecursive(child, user, deleted, folders, files);
        }
    }
        private record FolderTree(List<UserFolderEntity> folders, List<UserFileEntity> files) {
    }

}
