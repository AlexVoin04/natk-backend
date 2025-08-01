package com.natk.natk_api.userStorage.service;

import com.natk.natk_api.userStorage.dto.DeletedItemDto;
import com.natk.natk_api.userStorage.dto.FolderContentResponseDto;
import com.natk.natk_api.userStorage.dto.StorageItemDto;
import com.natk.natk_api.userStorage.mapper.StorageItemMapper;
import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.userStorage.repository.UserFileRepository;
import com.natk.natk_api.userStorage.repository.UserFolderRepository;
import com.natk.natk_api.users.model.UserEntity;
import com.natk.natk_api.users.service.CurrentUserService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UserStorageService {

    private final UserFolderRepository folderRepo;
    private final UserFileRepository fileRepo;
    private final CurrentUserService currentUserService;
    private final StorageItemMapper mapper;

    @Transactional(readOnly = true)
    public FolderContentResponseDto getStorageItems(UUID folderId) {
        UserEntity user = currentUserService.getCurrentUser();
        UserFolderEntity folder = resolveFolderOrRoot(folderId, user);

        List<StorageItemDto> folders = fetchActiveFolders(folder, user);
        List<StorageItemDto> files = fetchActiveFiles(folder, user);

        List<StorageItemDto> combined = sortByFolderThenName(folders, files);
        String path = folder != null ? folder.buildPath() : "Все файлы";

        return new FolderContentResponseDto(
                folder != null ? folder.getId() : null,
                path,
                combined
        );
    }

    @Transactional(readOnly = true)
    public List<DeletedItemDto> getDeletedItems() {
        UserEntity user = currentUserService.getCurrentUser();

        List<UserFolderEntity> deletedFolders = folderRepo
                .findByUserAndIsDeletedTrueOrderByDeletedAtDesc(user);

        Set<UUID> deletedFolderIds = deletedFolders.stream()
                .map(UserFolderEntity::getId)
                .collect(Collectors.toSet());

        List<DeletedItemDto> folders = deletedFolders.stream()
                .map(mapper::toDeletedItem)
                .toList();

        List<DeletedItemDto> files = fileRepo
                .findByCreatedByAndIsDeletedTrueOrderByDeletedAtDesc(user).stream()
                .filter(f -> f.getFolder() == null || !deletedFolderIds.contains(f.getFolder().getId()))
                .map(mapper::toDeletedItem)
                .toList();

        return Stream.concat(folders.stream(), files.stream())
                .sorted(Comparator.comparing(DeletedItemDto::deletedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }


    private UserFolderEntity resolveFolderOrRoot(UUID folderId, UserEntity user) {
        if (folderId == null) return null;

        return folderRepo.findById(folderId)
                .filter(f -> f.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("Folder not found or not owned by user"));
    }

    private List<StorageItemDto> fetchActiveFolders(UserFolderEntity parent, UserEntity user) {
        return folderRepo.findByUserAndParentFolderAndIsDeletedFalse(user, parent).stream()
                .map(mapper::toStorageItem)
                .toList();
    }

    private List<StorageItemDto> fetchActiveFiles(UserFolderEntity folder, UserEntity user) {
        List<UserFileEntity> files;

        if (folder == null) {
            files = fileRepo.findByCreatedByAndFolderIsNullAndIsDeletedFalse(user);
        } else {
            files = fileRepo.findByFolderAndIsDeletedFalse(folder).stream()
                    .filter(f -> f.getCreatedBy().getId().equals(user.getId()))
                    .toList();
        }

        return files.stream()
                .map(mapper::toStorageItem)
                .toList();
    }

    private List<StorageItemDto> sortByFolderThenName(List<StorageItemDto> folders, List<StorageItemDto> files) {
        Comparator<StorageItemDto> byTypeThenName = Comparator
                .comparing((StorageItemDto i) -> !i.type().equals("folder")) // folders first
                .thenComparing(StorageItemDto::name, String.CASE_INSENSITIVE_ORDER);

        return Stream.concat(folders.stream(), files.stream())
                .sorted(byTypeThenName)
                .toList();
    }
}