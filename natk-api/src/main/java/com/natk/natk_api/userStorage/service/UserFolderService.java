package com.natk.natk_api.userStorage.service;

import com.natk.natk_api.userStorage.dto.CreateFolderDto;
import com.natk.natk_api.userStorage.dto.FolderDto;
import com.natk.natk_api.userStorage.dto.FolderTreeDto;
import com.natk.natk_api.userStorage.dto.UpdateFolderDto;
import com.natk.natk_api.userStorage.mapper.UserFolderMapper;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.userStorage.repository.UserFolderRepository;
import com.natk.natk_api.users.model.UserEntity;
import com.natk.natk_api.users.service.CurrentUserService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserFolderService {

    private final UserFolderRepository folderRepo;
    private final CurrentUserService currentUserService;
    private final UserFolderMapper userFolderMapper;
    private final UserFolderNameResolverService folderNameResolverService;

    @Transactional
    public FolderDto createFolder(CreateFolderDto dto) {
        UserEntity user = currentUserService.getCurrentUser();

        UserFolderEntity folder = new UserFolderEntity();
        folder.setName(dto.name());
        folder.setUser(user);
        folder.setDeleted(false);
        folder.setCreatedAt(Instant.now());
        folder.setUpdatedAt(null);

        if (dto.parentFolderId() != null) {
            UserFolderEntity parent = folderRepo.findByIdAndUserAndIsDeletedFalse(dto.parentFolderId(), user)
                    .orElseThrow(() -> new AccessDeniedException("Parent folder not found or not owned by user or deleted"));
            folderNameResolverService.ensureUniqueNameOrThrow(dto.name(), parent, user);
            folder.setParentFolder(parent);
        }else {
            folderNameResolverService.ensureUniqueNameOrThrow(dto.name(), null, user);
        }

        return userFolderMapper.toDto(folderRepo.save(folder));
    }

    /*TODO:
       1.storageLifecycleService.deleteFolderRecursive(folder, user); - рекурсивное удаление сдержимого папки
     */
    @Transactional
    public void deleteFolder(UUID folderId) {
        UserEntity user = currentUserService.getCurrentUser();
        UserFolderEntity folder = folderRepo.findByIdAndUserAndIsDeletedFalse(folderId, user)
                .orElseThrow(() -> new AccessDeniedException("Folder not found or not owned by user or deleted"));

        folder.setDeleted(true);
        folder.setDeletedAt(Instant.now());
        folderRepo.save(folder);
    }

    /*TODO:
       1.возможно првоерять дубликат имени
       2.storageLifecycleService.restoreFolderRecursive(folder, user); - рекурсивное восстановление сдержимого папки
     */
    @Transactional
    public FolderDto restoreFolder(UUID folderId, UUID targetParentFolderId) {
        UserEntity user = currentUserService.getCurrentUser();
        UserFolderEntity folder = folderRepo.findByIdAndUserAndIsDeletedTrue(folderId, user)
                .orElseThrow(() -> new AccessDeniedException("Folder not found or not owned by user or not deleted"));

        UserFolderEntity targetParent = null;
        if (targetParentFolderId != null) {
            targetParent = folderRepo.findByIdAndUserAndIsDeletedFalse(targetParentFolderId, user)
                    .orElseThrow(() -> new AccessDeniedException("Target parent folder not found, deleted or not owned by user"));
        }

        if (isDescendant(folder, targetParent)) {
            throw new IllegalArgumentException("Cannot restore folder into its own descendant");
        }

        folder.setParentFolder(targetParent);
        folder.setDeleted(false);
        folder.setDeletedAt(null);
        folderRepo.save(folder);
        return userFolderMapper.toDto(folderRepo.save(folder));
    }

    @Transactional(readOnly = true)
    public List<FolderDto> listFolders(UUID parentFolderId) {
        UserEntity user = currentUserService.getCurrentUser();
        UserFolderEntity parent = parentFolderId != null
                ? folderRepo.findByIdAndUser(parentFolderId, user)
                .orElseThrow(() -> new AccessDeniedException("Folder not found or not owned by user"))
                : null;

        return folderRepo.findByUserAndParentFolderAndIsDeletedFalse(user, parent).stream()
                .map(userFolderMapper::toDto)
                .toList();
    }

    @Transactional
    public FolderDto updateFolder(UUID folderId, UpdateFolderDto dto) {
        UserEntity user = currentUserService.getCurrentUser();
        UserFolderEntity folder = folderRepo.findByIdAndUserAndIsDeletedFalse(folderId, user)
                .orElseThrow(() -> new AccessDeniedException("Folder not found or not owned by user or deleted"));

        boolean modified = false;

        if (dto.newName() != null && !dto.newName().isBlank()) {
            folder.setName(dto.newName());
            modified = true;
        }

        if (Boolean.TRUE.equals(dto.moveToRoot())) {
            folderNameResolverService.ensureUniqueNameOrThrow(dto.newName(), null, user);
            folder.setParentFolder(null);
            modified = true;
        }

        else if (dto.newParentFolderId().isPresent()) {
            UUID newParentId = dto.newParentFolderId().get();

            UserFolderEntity newParent = folderRepo.findByIdAndUserAndIsDeletedFalse(newParentId, user)
                    .orElseThrow(() -> new AccessDeniedException("New parent folder not found or not owned by user or deleted"));

            if (isDescendant(folder, newParent)) {
                throw new IllegalArgumentException("Cannot move folder inside its descendant");
            }

            folderNameResolverService.ensureUniqueNameOrThrow(dto.newName(), newParent, user);
            folder.setParentFolder(newParent);
            modified = true;
        }

        if (!modified) {
            throw new IllegalArgumentException("No changes provided for folder update");
        }

        return userFolderMapper.toDto(folder);
    }

    private boolean isDescendant(UserFolderEntity folder, UserFolderEntity target) {
        while (target != null) {
            if (target.getId().equals(folder.getId())) return true;
            target = target.getParentFolder();
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<FolderTreeDto> getFolderTree() {
        UserEntity user = currentUserService.getCurrentUser();

        List<UserFolderEntity> allFolders = folderRepo.findByUserAndIsDeletedFalse(user);

        Map<UUID, List<UserFolderEntity>> childrenMap = allFolders.stream()
                .filter(f -> f.getParentFolder() != null)
                .collect(Collectors.groupingBy(f -> f.getParentFolder().getId()));

        List<UserFolderEntity> rootFolders = allFolders.stream()
                .filter(f -> f.getParentFolder() == null)
                .toList();

        return rootFolders.stream()
                .map(folder -> buildTree(folder, childrenMap, 0))
                .toList();
    }

    private FolderTreeDto buildTree(UserFolderEntity folder, Map<UUID, List<UserFolderEntity>> childrenMap, int depth) {
        List<FolderTreeDto> children = childrenMap.getOrDefault(folder.getId(), List.of()).stream()
                .map(child -> buildTree(child, childrenMap, depth + 1))
                .toList();

        return new FolderTreeDto(
                folder.getId(),
                folder.getName(),
                depth,
                children
        );
    }
}
