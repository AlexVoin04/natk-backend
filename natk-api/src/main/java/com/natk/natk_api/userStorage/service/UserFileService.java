package com.natk.natk_api.userStorage.service;

import com.natk.natk_api.userStorage.dto.FileDownloadDto;
import com.natk.natk_api.userStorage.dto.FileInfoDto;
import com.natk.natk_api.userStorage.dto.UpdateFileDto;
import com.natk.natk_api.userStorage.dto.UploadFileDto;
import com.natk.natk_api.userStorage.mapper.UserFileMapper;
import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.userStorage.repository.UserFileRepository;
import com.natk.natk_api.userStorage.repository.UserFolderRepository;
import com.natk.natk_api.users.model.UserEntity;
import com.natk.natk_api.users.service.CurrentUserService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserFileService {
    private final UserFileRepository fileRepo;
    private final UserFolderRepository folderRepo;
    private final CurrentUserService currentUserService;
    private final UserFileMapper userFileMapper;
    private final TransliterationService transliterationService;
    private final FileNameResolverService fileNameResolverService;
    private final MimeTypeValidatorService mimeTypeValidatorService;


    @Transactional
    public FileInfoDto uploadFile(UploadFileDto dto) {
        mimeTypeValidatorService.validate(dto.fileData());
        String mimeType = mimeTypeValidatorService.detectMimeType(dto.fileData());

        UserEntity user = currentUserService.getCurrentUser();
        UserFolderEntity folder = null;
        if (dto.folderId() != null) {
            folder = folderRepo.findByIdAndUserAndIsDeletedFalse(dto.folderId(), user)
                    .orElseThrow(() -> new AccessDeniedException("Folder not found or not owned by user or deleted"));
        }

        fileNameResolverService.ensureUniqueNameOrThrow(dto.name(), folder, user);

        UserFileEntity file = new UserFileEntity();
        file.setName(dto.name());
        file.setFolder(folder);
        file.setFileData(dto.fileData());
        file.setFileType(mimeType);
        file.setCreatedBy(user);
        file.setCreatedAt(Instant.now());
        file.setDeleted(false);
        file.setDeletedAt(null);

        return userFileMapper.toDto(fileRepo.save(file));
    }

    @Transactional(readOnly = true)
    public UserFileEntity getFileEntity(UUID fileId) {
        UserEntity user = currentUserService.getCurrentUser();
        return fileRepo.findByIdAndCreatedBy(fileId, user)
                .orElseThrow(() -> new AccessDeniedException("File not found or not owned by user"));
    }

    public FileDownloadDto getFileDownloadData(UUID fileId) {
        UserFileEntity file = getFileEntity(fileId);
        String originalName = file.getName();
        String encodedName = UriUtils.encode(originalName, StandardCharsets.UTF_8);
        String translitName = transliterationService.transliterate(originalName);

        return new FileDownloadDto(file.getFileData(), originalName, encodedName, translitName);
    }

    @Transactional(readOnly = true)
    public FileInfoDto getFile(UUID fileId) {
        UserEntity user = currentUserService.getCurrentUser();
        UserFileEntity file = fileRepo.findByIdAndCreatedBy(fileId, user)
                .orElseThrow(() -> new AccessDeniedException("File not found or not owned by user"));

        return userFileMapper.toDto(file);
    }

    @Transactional
    public void deleteFile(UUID fileId) {
        UserEntity user = currentUserService.getCurrentUser();
        UserFileEntity file = fileRepo.findByIdAndCreatedByAndIsDeletedFalse(fileId, user)
                .orElseThrow(() -> new AccessDeniedException("File not found or not owned by user or deleted"));

        file.setDeleted(true);
        file.setDeletedAt(Instant.now());
        fileRepo.save(file);
    }

    @Transactional
    public FileInfoDto restoreFile(UUID fileId, UUID targetFolderId) {
        UserEntity user = currentUserService.getCurrentUser();

        UserFileEntity file = fileRepo.findByIdAndCreatedByAndIsDeletedTrue(fileId, user)
                .orElseThrow(() -> new AccessDeniedException("File not found, already restored or not owned by user"));

        UserFolderEntity targetFolder = null;
        if (targetFolderId != null) {
            targetFolder = folderRepo.findByIdAndUserAndIsDeletedFalse(targetFolderId, user)
                    .orElseThrow(() -> new AccessDeniedException("Target folder not found, deleted or not owned by user"));
        }

        String uniqueName = fileNameResolverService.generateUniqueFileName(file.getName(), targetFolder, user);

        file.setName(uniqueName);
        file.setDeleted(false);
        file.setDeletedAt(null);
        file.setFolder(targetFolder);

        fileRepo.save(file);
        return userFileMapper.toDto(fileRepo.save(file));
    }

    @Transactional(readOnly = true)
    public List<FileInfoDto> listFiles(UUID folderId) {
        UserEntity user = currentUserService.getCurrentUser();
        UserFolderEntity folder = folderRepo.findByIdAndUser(folderId, user)
                .orElseThrow(() -> new AccessDeniedException("Folder not found or not owned by user"));

        return fileRepo.findByFolderAndIsDeletedFalse(folder).stream()
                .map(userFileMapper::toDto)
                .toList();
    }

    @Transactional
    public FileInfoDto updateFile(UUID fileId, UpdateFileDto dto) {
        UserEntity user = currentUserService.getCurrentUser();
        UserFileEntity file = fileRepo.findByIdAndCreatedBy(fileId, user)
                .orElseThrow(() -> new AccessDeniedException("File not found or not owned by user"));

        boolean modified = false;

        if (dto.newName() != null && !dto.newName().isBlank()) {
            fileNameResolverService.ensureUniqueNameOrThrow(dto.newName(), file.getFolder(), user);
            file.setName(dto.newName());
            modified = true;
        }

        if (Boolean.TRUE.equals(dto.moveToRoot())) {
            file.setFolder(null);
            modified = true;
        }

        else if (dto.newFolderId() != null) {
            UserFolderEntity newFolder = folderRepo.findByIdAndUserAndIsDeletedFalse(dto.newFolderId(), user)
                    .orElseThrow(() -> new AccessDeniedException("Folder not found or not owned by user or deleted"));

            file.setFolder(newFolder);
            modified = true;
        }

        if (!modified) {
            throw new IllegalArgumentException("No changes provided for file update");
        }

        return userFileMapper.toDto(file);
    }

    @Transactional
    public FileInfoDto copyFile(UUID fileId, UUID targetFolderId) {
        UserEntity user = currentUserService.getCurrentUser();

        UserFileEntity original = fileRepo.findByIdAndCreatedBy(fileId, user)
                .orElseThrow(() -> new AccessDeniedException("File not found or not owned by user"));

        UserFolderEntity targetFolder = null;
        if (targetFolderId != null) {
            targetFolder = folderRepo.findByIdAndUserAndIsDeletedFalse(targetFolderId, user)
                    .orElseThrow(() -> new AccessDeniedException("Target folder not found or not owned by user or deleted"));
        }

        String uniqueName = fileNameResolverService.generateUniqueFileName(original.getName(), targetFolder, user);

        UserFileEntity copy = new UserFileEntity();
        copy.setName(uniqueName);
        copy.setFolder(targetFolder);
        copy.setCreatedBy(user);
        copy.setCreatedAt(Instant.now());
        copy.setDeleted(false);
        copy.setFileType(original.getFileType());
        copy.setFileData(original.getFileData());

        return userFileMapper.toDto(fileRepo.save(copy));
    }
}
