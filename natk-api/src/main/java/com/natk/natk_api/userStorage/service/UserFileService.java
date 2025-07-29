package com.natk.natk_api.userStorage.service;

import com.natk.natk_api.userStorage.dto.FileInfoDto;
import com.natk.natk_api.userStorage.dto.UpdateFileDto;
import com.natk.natk_api.userStorage.dto.UploadFileDto;
import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.userStorage.repository.UserFileRepository;
import com.natk.natk_api.userStorage.repository.UserFolderRepository;
import com.natk.natk_api.users.model.UserEntity;
import com.natk.natk_api.users.service.CurrentUserService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.apache.tika.Tika;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserFileService {
    private final UserFileRepository fileRepo;
    private final UserFolderRepository folderRepo;
    private final CurrentUserService currentUserService;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            // Документы
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",       // .xlsx
            "application/vnd.ms-excel",                                               // .xls
            "application/x-tika-ooxml",                                               // Office Open XML (OOXML)
            "text/plain",

            // Изображения
            "image/png",
            "image/jpeg",

            // Draw.io / XML
            "application/xml",       // .xml, .drawio
            "text/xml",

            // Архивы
            "application/zip",       // .zip
            "application/x-rar-compressed", // .rar
            "application/x-7z-compressed",  // .7z
            "application/x-tar",     // .tar
            "application/gzip"       // .gz
    );

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    private static final Tika tika = new Tika();

    private String detectMimeType(byte[] data) {
        return tika.detect(data);
    }

    @Transactional
    public UserFileEntity uploadFile(UploadFileDto dto) {
        if (dto.fileData().length > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }

        String mimeType = detectMimeType(dto.fileData());

        log.info("detectMimeType: {}", mimeType);

        if (!ALLOWED_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException("Unsupported file type: " + mimeType);
        }

        log.info("Saving file: name={}, size={}, dataType={}", dto.name(), dto.fileData().length, dto.fileData().getClass().getName());

        UserEntity user = currentUserService.getCurrentUser();
        UserFolderEntity folder = folderRepo.findById(dto.folderId())
                .filter(f -> f.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("Folder not found or not owned by user"));

        UserFileEntity file = new UserFileEntity();
        file.setName(dto.name());
        file.setFolder(folder);
        file.setFileData(dto.fileData());
        file.setFileType(mimeType);
        file.setCreatedBy(user);
        file.setCreatedAt(Instant.now());

        return fileRepo.save(file);
    }

    @Transactional(readOnly = true)
    public UserFileEntity getFileEntity(UUID fileId) {
        UserEntity user = currentUserService.getCurrentUser();
        return fileRepo.findById(fileId)
                .filter(f -> f.getCreatedBy().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("File not found or not owned by user"));
    }

    @Transactional(readOnly = true)
    public FileInfoDto getFile(UUID fileId) {
        UserEntity user = currentUserService.getCurrentUser();
        UserFileEntity file = fileRepo.findById(fileId)
                .filter(f -> f.getCreatedBy().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("File not found or not owned by user"));

        return new FileInfoDto(file.getId(), file.getName(), file.getFileType(), file.getCreatedAt());
    }

    @Transactional
    public void deleteFile(UUID fileId) {
        UserEntity user = currentUserService.getCurrentUser();
        UserFileEntity file = fileRepo.findById(fileId)
                .filter(f -> f.getCreatedBy().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("File not found or not owned by user"));
        fileRepo.delete(file);
    }

    @Transactional(readOnly = true)
    public List<FileInfoDto> listFiles(UUID folderId) {
        UserEntity user = currentUserService.getCurrentUser();
        UserFolderEntity folder = folderRepo.findById(folderId)
                .filter(f -> f.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("Folder not found or not owned by user"));

        return fileRepo.findByFolder(folder).stream()
                .map(f -> new FileInfoDto(f.getId(), f.getName(), f.getFileType(), f.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void updateFile(UUID fileId, UpdateFileDto dto) {
        UserEntity user = currentUserService.getCurrentUser();
        UserFileEntity file = fileRepo.findById(fileId)
                .filter(f -> f.getCreatedBy().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("File not found or not owned by user"));

        if (dto.newName() != null && !dto.newName().isBlank()) {
            file.setName(dto.newName());
        }

        if (dto.newFolderId() != null) {
            UserFolderEntity newFolder = folderRepo.findById(dto.newFolderId())
                    .filter(f -> f.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new AccessDeniedException("Target folder not found or not owned by user"));

            file.setFolder(newFolder);
        }
    }
}
