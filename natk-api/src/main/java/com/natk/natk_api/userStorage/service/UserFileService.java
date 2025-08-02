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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.apache.tika.Tika;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserFileService {
    private final UserFileRepository fileRepo;
    private final UserFolderRepository folderRepo;
    private final CurrentUserService currentUserService;
    private final UserFileMapper userFileMapper;
    private final TransliterationService transliterationService;

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
    public FileInfoDto uploadFile(UploadFileDto dto) {
        if (dto.fileData().length > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }

        String mimeType = detectMimeType(dto.fileData());
        if (!ALLOWED_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException("Unsupported file type: " + mimeType);
        }

        UserEntity user = currentUserService.getCurrentUser();
        UserFolderEntity folder = null;
        if (dto.folderId() != null) {
            folder = folderRepo.findById(dto.folderId())
                    .filter(f -> f.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new AccessDeniedException("Folder not found or not owned by user"));
        }

        String uniqueName = generateUniqueFileName(dto.name(), folder, user); //можно уникально имя использовать сразу

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
        return fileRepo.findById(fileId)
                .filter(f -> f.getCreatedBy().getId().equals(user.getId()))
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
        UserFileEntity file = fileRepo.findById(fileId)
                .filter(f -> f.getCreatedBy().getId().equals(user.getId()) && !f.isDeleted())
                .orElseThrow(() -> new AccessDeniedException("File not found or not owned by user"));

        file.setDeleted(true);
        file.setDeletedAt(Instant.now());

        fileRepo.save(file);
    }

    @Transactional(readOnly = true)
    public List<FileInfoDto> listFiles(UUID folderId) {
        UserEntity user = currentUserService.getCurrentUser();
        UserFolderEntity folder = folderRepo.findById(folderId)
                .filter(f -> f.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("Folder not found or not owned by user"));

        return fileRepo.findByFolderAndIsDeletedFalse(folder).stream()
                .map(userFileMapper::toDto)
                .toList();
    }

    @Transactional
    public FileInfoDto updateFile(UUID fileId, UpdateFileDto dto) {
        UserEntity user = currentUserService.getCurrentUser();
        UserFileEntity file = fileRepo.findById(fileId)
                .filter(f -> f.getCreatedBy().getId().equals(user.getId()))
                .orElseThrow(() -> new AccessDeniedException("File not found or not owned by user"));

        boolean modified = false;

        if (dto.newName() != null && !dto.newName().isBlank()) {
            generateUniqueFileName(dto.newName(), file.getFolder(), user);
            file.setName(dto.newName());
            modified = true;
        }

        if (dto.newFolderId() != null) {
            UserFolderEntity newFolder = folderRepo.findById(dto.newFolderId())
                    .filter(f -> f.getUser().getId().equals(user.getId()))
                    .orElseThrow(() -> new AccessDeniedException("Target folder not found or not owned by user"));

            file.setFolder(newFolder);
            modified = true;
        }

        if (!modified) {
            throw new IllegalArgumentException("No changes provided for file update");
        }

        return userFileMapper.toDto(file);
    }

    private String generateUniqueFileName(String originalName, UserFolderEntity folder, UserEntity user) {
        String baseName;
        String extension;

        int lastDot = originalName.lastIndexOf('.');
        if (lastDot != -1) {
            baseName = originalName.substring(0, lastDot);
            extension = originalName.substring(lastDot);
        } else {
            baseName = originalName;
            extension = "";
        }

        Pattern suffixPattern = Pattern.compile("^(.*)\\((\\d+)\\)$");
        Matcher suffixMatcher = suffixPattern.matcher(baseName);

        String cleanBaseName = baseName;
        if (suffixMatcher.matches()) {
            cleanBaseName = suffixMatcher.group(1);
        }

        Set<String> existingNames = getExistingFileNames(folder, user);

        // Проверяем, есть ли файл с точным именем originalName
        if (!existingNames.contains(originalName)) {
            // Если такого нет, можно вернуть его сразу
            return originalName;
        }

        // Ищем максимальный индекс для cleanBaseName
        Pattern pattern = Pattern.compile(Pattern.quote(cleanBaseName) + "\\((\\d+)\\)" + Pattern.quote(extension));

        int maxIndex = 0;
        for (String name : existingNames) {
            Matcher matcher = pattern.matcher(name);
            if (matcher.matches()) {
                int index = Integer.parseInt(matcher.group(1));
                maxIndex = Math.max(maxIndex, index);
            }
        }

        String suggestedName = cleanBaseName + "(" + (maxIndex + 1) + ")" + extension;
        throw new IllegalArgumentException("File with the same name already exists. Suggested name: " + suggestedName);
    }

    private Set<String> getExistingFileNames(UserFolderEntity folder, UserEntity user) {
        List<UserFileEntity> files;
        if (folder == null) {
            files = fileRepo.findByCreatedByAndFolderIsNullAndIsDeletedFalse(user);
        } else {
            files = fileRepo.findByFolderAndIsDeletedFalse(folder);
        }

        return files.stream()
                .map(UserFileEntity::getName)
                .collect(Collectors.toSet());
    }
}
