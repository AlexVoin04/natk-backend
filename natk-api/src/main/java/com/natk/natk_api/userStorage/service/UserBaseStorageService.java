package com.natk.natk_api.userStorage.service;

import com.natk.natk_api.baseStorage.context.StorageContext;
import com.natk.natk_api.baseStorage.context.UserContext;
import com.natk.natk_api.baseStorage.service.BaseStorageService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class UserBaseStorageService extends BaseStorageService<
        UserFolderEntity,
        UserFileEntity,
        UserStorageItemDto,
        UserDeletedItemDto
        > {

    private final UserFolderRepository folderRepo;
    private final UserFileRepository fileRepo;
    private final UserStorageItemMapper mapper;
    private final CurrentUserService currentUserService;

    public UserBaseStorageService(
            UserFolderRepository folderRepo,
            UserFileRepository fileRepo,
            UserStorageItemMapper mapper,
            CurrentUserService currentUserService
    ) {
        this.folderRepo = folderRepo;
        this.fileRepo = fileRepo;
        this.mapper = mapper;
        this.currentUserService = currentUserService;
    }

    protected UserContext getContext() {
        return new UserContext(currentUserService.getCurrentUser());
    }

    @Transactional(readOnly = true)
    public FolderContentResponseDto<UserStorageItemDto> getStorageItems(UUID folderId) {
        return super.getStorageItems(folderId, getContext());
    }

    @Transactional(readOnly = true)
    public List<UserDeletedItemDto> getDeletedItems() {
        return super.getDeletedItems(getContext());
    }

    @Override
    protected UserFolderEntity resolveFolderOrRoot(UUID folderId, StorageContext ctx) {
        if (folderId == null) return null;
        UserEntity user = ((UserContext) ctx).user();
        return folderRepo.findByIdAndUser(folderId, user)
                .orElseThrow(() -> new AccessDeniedException("Folder not found or not owned by user"));
    }

    @Override
    protected List<UserFolderEntity> fetchActiveFolders(UserFolderEntity parent, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        return folderRepo.findByUserAndParentFolderAndIsDeletedFalse(user, parent);
    }

    @Override
    protected List<UserFileEntity> fetchActiveFiles(UserFolderEntity parent, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        if (parent == null) {
            return fileRepo.findByCreatedByAndFolderIsNullAndIsDeletedFalse(user);
        } else {
            // repo has findByCreatedByAndFolderAndIsDeletedFalse
            return fileRepo.findByCreatedByAndFolderAndIsDeletedFalse(user, parent);
        }
    }

    @Override
    protected List<UserFolderEntity> findDeletedFolders(StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        return folderRepo.findByUserAndIsDeletedTrueOrderByDeletedAtDesc(user);
    }

    @Override
    protected List<UserFileEntity> findDeletedFiles(StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        return fileRepo.findByCreatedByAndIsDeletedTrueOrderByDeletedAtDesc(user);
    }

    @Override
    protected UserStorageItemDto mapFolderToItem(UserFolderEntity folder, StorageContext ctx) {
        return mapper.toStorageItem(folder);
    }

    @Override
    protected UserStorageItemDto mapFileToItem(UserFileEntity file, StorageContext ctx) {
        return mapper.toStorageItem(file);
    }

    @Override
    protected UserDeletedItemDto mapFolderToDeleted(UserFolderEntity folder, StorageContext ctx) {
        return mapper.toDeletedItem(folder);
    }

    @Override
    protected UserDeletedItemDto mapFileToDeleted(UserFileEntity file, StorageContext ctx) {
        return mapper.toDeletedItem(file);
    }

    @Override
    protected UUID extractFolderId(UserFolderEntity folder) {
        return folder.getId();
    }

    @Override
    protected Instant extractDeletedAt(UserDeletedItemDto dto) {
        return dto.deletedAt();
    }

    @Override
    protected String resolvePathForResponse(UserFolderEntity folder, StorageContext ctx) {
        // same logic as resolvePath in mapper
        if (folder == null) return "Все файлы";

        UserFolderEntity current = folder;
        while (current != null) {
            if (current.isDeleted()) {
                return "Все файлы";
            }
            current = current.getParentFolder();
        }

        return folder.buildPath();
    }

    @Override
    protected boolean isFile(UserStorageItemDto dto) {
        return !"folder".equals(dto.type());
    }

    @Override
    protected String extractName(UserStorageItemDto dto) {
        return dto.name();
    }
}
