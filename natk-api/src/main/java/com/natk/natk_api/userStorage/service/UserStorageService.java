package com.natk.natk_api.userStorage.service;

import com.natk.natk_api.userStorage.dto.UserDeletedItemDto;
import com.natk.natk_api.userStorage.dto.FolderContentResponseDto;
import com.natk.natk_api.userStorage.dto.UserStorageItemDto;
import com.natk.natk_api.userStorage.mapper.UserStorageItemMapper;
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

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UserStorageService {

    private final UserFolderRepository folderRepo;
    private final UserFileRepository fileRepo;
    private final CurrentUserService currentUserService;
    private final UserStorageItemMapper mapper;

    @Transactional(readOnly = true)
    public FolderContentResponseDto getStorageItems(UUID folderId) {
        UserEntity user = currentUserService.getCurrentUser();
        UserFolderEntity folder = resolveFolderOrRoot(folderId, user);

        List<UserStorageItemDto> folders = fetchActiveFolders(folder, user);
        List<UserStorageItemDto> files = fetchActiveFiles(folder, user);

        List<UserStorageItemDto> combined = sortByFolderThenName(folders, files);
        String path = folder != null ? folder.buildPath() : "Все файлы";

        return new FolderContentResponseDto(
                folder != null ? folder.getId() : null,
                path,
                combined
        );
    }

    @Transactional(readOnly = true)
    public List<UserDeletedItemDto> getDeletedItems() {
        UserEntity user = currentUserService.getCurrentUser();

        List<UserDeletedItemDto> folders = folderRepo
                .findByUserAndIsDeletedTrueOrderByDeletedAtDesc(user).stream()
                .map(mapper::toDeletedItem)
                .toList();

        List<UserDeletedItemDto> files = fileRepo
                .findByCreatedByAndIsDeletedTrueOrderByDeletedAtDesc(user).stream()
                .map(mapper::toDeletedItem)
                .toList();

        return Stream.concat(folders.stream(), files.stream())
                .sorted(Comparator.comparing(UserDeletedItemDto::deletedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }


    private UserFolderEntity resolveFolderOrRoot(UUID folderId, UserEntity user) {
        if (folderId == null) return null;

        return folderRepo.findByIdAndUser(folderId, user)
                .orElseThrow(() -> new AccessDeniedException("Folder not found or not owned by user"));
    }

    private List<UserStorageItemDto> fetchActiveFolders(UserFolderEntity parent, UserEntity user) {
        return folderRepo.findByUserAndParentFolderAndIsDeletedFalse(user, parent).stream()
                .map(mapper::toStorageItem)
                .toList();
    }

    private List<UserStorageItemDto> fetchActiveFiles(UserFolderEntity folder, UserEntity user) {
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

    private List<UserStorageItemDto> sortByFolderThenName(List<UserStorageItemDto> folders, List<UserStorageItemDto> files) {
        Comparator<UserStorageItemDto> byTypeThenName = Comparator
                .comparing((UserStorageItemDto i) -> !i.type().equals("folder")) // folders first
                .thenComparing(UserStorageItemDto::name, String.CASE_INSENSITIVE_ORDER);

        return Stream.concat(folders.stream(), files.stream())
                .sorted(byTypeThenName)
                .toList();
    }
}