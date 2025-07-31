package com.natk.natk_api.userStorage.service;

import com.natk.natk_api.userStorage.dto.FolderContentResponseDto;
import com.natk.natk_api.userStorage.dto.StorageItemDto;
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
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UserStorageService {

    private final UserFolderRepository folderRepo;
    private final UserFileRepository fileRepo;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public FolderContentResponseDto getStorageItems(UUID folderId) {
        UserEntity user = currentUserService.getCurrentUser();

        UserFolderEntity folder = resolveFolderOrRoot(folderId, user);

        List<StorageItemDto> folders = getSubfolders(folder, user);
        List<StorageItemDto> files = getFilesInFolder(folder, user);

        List<StorageItemDto> combinedItems = sortItems(folders, files);
        String path = buildPath(folder);

        return new FolderContentResponseDto(
                folder != null ? folder.getId() : null,
                path,
                combinedItems
        );
    }

    private UserFolderEntity resolveFolderOrRoot(UUID folderId, UserEntity user) {
        if (folderId == null) return null;

        return folderRepo.findById(folderId)
                .filter(f -> f.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("Folder not found or not owned by user"));
    }

    private List<StorageItemDto> getSubfolders(UserFolderEntity folder, UserEntity user) {
        return folderRepo.findByUserAndParentFolder(user, folder).stream()
                .map(f -> new StorageItemDto(
                        f.getId(),
                        f.getName(),
                        "folder",
                        null
                ))
                .toList();
    }

    private List<StorageItemDto> getFilesInFolder(UserFolderEntity folder, UserEntity user) {
        Stream<UserFileEntity> fileStream;

        if (folder == null) {
            fileStream = fileRepo.findByCreatedByAndFolderIsNull(user).stream();
        } else {
            fileStream = fileRepo.findByFolder(folder).stream()
                    .filter(f -> f.getCreatedBy().getId().equals(user.getId()));
        }

        return fileStream
                .map(f -> new StorageItemDto(
                        f.getId(),
                        f.getName(),
                        f.getFileType(),
                        f.getCreatedAt()
                ))
                .toList();
    }

    private List<StorageItemDto> sortItems(List<StorageItemDto> folders, List<StorageItemDto> files) {
        Comparator<StorageItemDto> folderFirst = Comparator
                .comparing((StorageItemDto i) -> !i.type().equals("folder"))
                .thenComparing(StorageItemDto::name, String.CASE_INSENSITIVE_ORDER);

        return Stream.concat(folders.stream(), files.stream())
                .sorted(folderFirst)
                .toList();
    }

    private String buildPath(UserFolderEntity folder) {
        if (folder == null) return "Все файлы";

        List<String> pathParts = new ArrayList<>();
        for (UserFolderEntity current = folder; current != null; current = current.getParentFolder()) {
            pathParts.add(current.getName());
        }

        Collections.reverse(pathParts);
        return "Все файлы/" + String.join("/", pathParts);
    }
}