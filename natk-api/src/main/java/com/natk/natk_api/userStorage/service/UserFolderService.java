package com.natk.natk_api.userStorage.service;

import com.natk.natk_api.userStorage.dto.CreateFolderDto;
import com.natk.natk_api.userStorage.dto.FolderDto;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserFolderService {

    private final UserFolderRepository folderRepo;
    private final CurrentUserService currentUserService;
    private final UserFolderMapper userFolderMapper;

    @Transactional
    public UserFolderEntity createFolder(CreateFolderDto dto) {
        UserEntity user = currentUserService.getCurrentUser();

        UserFolderEntity folder = new UserFolderEntity();
        folder.setName(dto.name());
        folder.setUser(user);
        folder.setDeleted(false);
        folder.setCreatedAt(Instant.now());
        folder.setUpdatedAt(null);

        if (dto.parentFolderId() != null) {
            UserFolderEntity parent = folderRepo.findById(dto.parentFolderId())
                    .filter(f -> f.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new AccessDeniedException("Parent folder not found or not owned by user"));
            folder.setParentFolder(parent);
        }
        return folderRepo.save(folder);
    }

    @Transactional
    public void deleteFolder(UUID folderId) {
        UserEntity user = currentUserService.getCurrentUser();
        UserFolderEntity folder = folderRepo.findById(folderId)
                .filter(f -> f.getUser().getId().equals(user.getId()) && !f.isDeleted())
                .orElseThrow(() -> new AccessDeniedException("Folder not found or not owned by user"));

        folder.setDeleted(true);
        folder.setDeletedAt(Instant.now());
        folder.setUpdatedAt(Instant.now());

        folderRepo.save(folder);
    }

    @Transactional(readOnly = true)
    public List<FolderDto> listFolders(UUID parentFolderId) {
        UserEntity user = currentUserService.getCurrentUser();
        UserFolderEntity parent = parentFolderId != null
                ? folderRepo.findById(parentFolderId)
                .filter(f -> f.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("Folder not found or not owned by user"))
                : null;

        return folderRepo.findByUserAndParentFolderAndIsDeletedFalse(user, parent).stream()
                .map(userFolderMapper::toDto)
                .toList();
    }

    @Transactional
    public FolderDto  updateFolder(UUID folderId, UpdateFolderDto dto) {
        UserEntity user = currentUserService.getCurrentUser();
        UserFolderEntity folder = folderRepo.findById(folderId)
                .filter(f -> f.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("Folder not found or not owned by user"));

        boolean modified = false;

        if (dto.newName() != null && !dto.newName().isBlank()) {
            folder.setName(dto.newName());
            modified = true;
        }

        if (dto.newParentFolderId() != null) {
            UserFolderEntity newParent = folderRepo.findById(dto.newParentFolderId())
                    .filter(f -> f.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new AccessDeniedException("New parent folder not found or not owned by user"));

            if (isDescendant(folder, newParent)) {
                throw new IllegalArgumentException("Cannot move folder inside its descendant");
            }

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
}
