package com.natk.natk_api.departmentStorage.service;


import com.natk.natk_api.baseStorage.MagicValidationResult;
import com.natk.natk_api.baseStorage.context.DepartmentContext;
import com.natk.natk_api.baseStorage.context.StorageContext;
import com.natk.natk_api.baseStorage.service.BaseFileService;
import com.natk.natk_api.department.model.DepartmentEntity;
import com.natk.natk_api.department.permission.DepartmentAccessService;
import com.natk.natk_api.departmentStorage.dto.DepartmentFileInfoDto;
import com.natk.natk_api.departmentStorage.model.DepartmentFileEntity;
import com.natk.natk_api.departmentStorage.mapper.DepartmentFileMapper;
import com.natk.natk_api.departmentStorage.model.DepartmentFolderEntity;
import com.natk.natk_api.departmentStorage.repository.DepartmentFileRepository;
import com.natk.natk_api.departmentStorage.repository.DepartmentFolderRepository;
import com.natk.natk_api.baseStorage.dto.FileDownloadDto;
import com.natk.natk_api.baseStorage.dto.UploadFileDto;
import com.natk.natk_api.minio.MinioFileService;
import com.natk.natk_api.baseStorage.service.MimeTypeValidatorService;
import com.natk.natk_api.baseStorage.service.TransliterationService;
import com.natk.natk_api.users.model.UserEntity;
import com.natk.natk_api.users.service.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.util.UriUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DepartmentBaseFileService extends BaseFileService<
        DepartmentFileEntity,
        DepartmentFolderEntity,
        DepartmentFileRepository,
        DepartmentFolderRepository,
        DepartmentFileInfoDto> {

    private final DepartmentFileRepository fileRepo;
    private final DepartmentFolderRepository folderRepo;
    private final DepartmentFileNameResolverService fileNameResolverService;
    private final MimeTypeValidatorService mimeTypeValidatorService;
    private final TransliterationService transliterationService;
    private final CurrentUserService currentUserService;
    private final DepartmentAccessService departmentAccessService;
    private final DepartmentFileMapper departmentFileMapper;
    private final MinioFileService minioFileService;
    private static final String DEPARTMENT_BUCKET = "department-files";

    public DepartmentBaseFileService(
            DepartmentFileRepository fileRepo,
            DepartmentFolderRepository folderRepo,
            DepartmentFileNameResolverService fileNameResolverService,
            MimeTypeValidatorService mimeTypeValidatorService,
            TransliterationService transliterationService,
            CurrentUserService currentUserService,
            DepartmentAccessService departmentAccessService,
            DepartmentFileMapper departmentFileMapper, MinioFileService minioFileService
    ) {
        super(fileRepo, folderRepo);
        this.fileRepo = fileRepo;
        this.folderRepo = folderRepo;
        this.fileNameResolverService = fileNameResolverService;
        this.mimeTypeValidatorService = mimeTypeValidatorService;
        this.transliterationService = transliterationService;
        this.currentUserService = currentUserService;
        this.departmentAccessService = departmentAccessService;
        this.departmentFileMapper = departmentFileMapper;
        this.minioFileService = minioFileService;
    }

    @Transactional
    public DepartmentFileInfoDto uploadFile(UploadFileDto dto, UUID departmentId) {
        return super.uploadFile(dto, getContext(departmentId));
    }

    @Transactional(readOnly = true)
    public DepartmentFileInfoDto getFile(UUID id, UUID departmentId) {
        return super.getFile(id, getContext(departmentId));
    }

    @Transactional
    public FileDownloadDto getFileDownloadData(UUID id, UUID departmentId) {
        return super.getFileDownloadData(id, getContext(departmentId));
    }

    @Transactional
    public void deleteFile(UUID id, UUID departmentId) {
        super.deleteFile(id, getContext(departmentId));
    }

    @Transactional
    public DepartmentFileInfoDto restoreFile(UUID id, UUID targetFolderId, UUID departmentId) {
        return super.restoreFile(id, targetFolderId, getContext(departmentId));
    }

    @Transactional
    public DepartmentFileInfoDto renameFile(UUID id, String newName, UUID departmentId) {
        return super.renameFile(id, newName, getContext(departmentId));
    }

    @Transactional
    public DepartmentFileInfoDto moveFile(UUID id, UUID newFolderId, Boolean moveToRoot, UUID departmentId) {
        return super.moveFile(id, newFolderId, moveToRoot, getContext(departmentId));
    }

    @Transactional
    public DepartmentFileInfoDto copyFile(UUID id, UUID newFolderId, UUID departmentId) {
        return super.copyFile(id, newFolderId, getContext(departmentId));
    }

    @Transactional(readOnly = true)
    public List<DepartmentFileInfoDto> listFiles(UUID folderId, UUID departmentId) {
        return super.listFiles(folderId, getContext(departmentId));
    }

    protected StorageContext getContext(UUID departmentId) {
        UserEntity user = currentUserService.getCurrentUser();
        return new DepartmentContext(user, departmentId);
    }

    @Override
    protected DepartmentFileEntity findFile(UUID id, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());
        return fileRepo.findByIdAndDepartmentAndIsDeletedFalse(id, dept)
                .orElseThrow(() -> new AccessDeniedException("File not found in department"));
    }

    @Override
    protected DepartmentFileEntity findDeletedFile(UUID id, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());
        return fileRepo.findByIdAndDepartmentAndIsDeletedTrue(id, dept)
                .orElseThrow(() -> new AccessDeniedException("Deleted file not found in department"));
    }

    @Override
    protected DepartmentFolderEntity findFolder(UUID id, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());
        return folderRepo.findByIdAndDepartmentAndIsDeletedFalse(id, dept)
                .orElseThrow(() -> new AccessDeniedException("Folder not found in department"));
    }

    @Override
    protected void checkReadAccess(DepartmentFileEntity file, StorageContext ctx) {
        checkUploadAccess(file.getFolder(), ctx);
    }

    @Override
    protected void checkUploadAccess(DepartmentFolderEntity folder, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        if (folder == null) {
            if (!departmentAccessService.hasAnyAccess(dCtx.user(), dCtx.departmentId())) {
                throw new AccessDeniedException("Access denied to upload file in root");
            }
        } else {
            if (!folder.isPublic() && !departmentAccessService.hasFolderAccess(dCtx.user(), folder)) {
                throw new AccessDeniedException("Access denied to read file");
            }
        }
    }

    @Override
    protected void checkDeleteAccess(DepartmentFileEntity file, StorageContext ctx) {
        checkUploadAccess(file.getFolder(), ctx);
    }

    @Override
    protected void checkRestoreAccess(DepartmentFileEntity file, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        if (!departmentAccessService.canManage(dCtx.user(), dCtx.departmentId())) {
            throw new AccessDeniedException("No rights to delete file in department");
        }
    }

    @Override
    protected void checkUpdateAccess(DepartmentFileEntity file, StorageContext ctx) {
        checkUploadAccess(file.getFolder(), ctx);
    }

    @Override
    protected DepartmentFileInfoDto applyUploadFile(UploadFileDto dto, DepartmentFolderEntity folder, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());

        MagicValidationResult res = mimeTypeValidatorService.validate(dto.fileData(), dto.name());

        InputStream fullStream = new SequenceInputStream(
                new ByteArrayInputStream(res.header()),
                dto.fileData()
        );

        fileNameResolverService.ensureUniqueNameOrThrow(dto.name(), folder, dept);

        DepartmentFileEntity file = new DepartmentFileEntity();
        file.setName(dto.name());
        file.setFolder(folder);
        file.setFileType(res.mimeType());
        file.setDepartment(dept);
        file.setCreatedBy(dCtx.user().getShortFio());
        file.setCreatedAt(Instant.now());
        file.setDeleted(false);
        file.setFileSize(dto.size());

        String key = minioFileService.generateDepartmentFileKey(dept.getId());
        file.setStorageKey(key);

        minioFileService.uploadFile(fullStream, dto.size(), DEPARTMENT_BUCKET, key, res.mimeType());

        return mapToDto(fileRepo.save(file));
    }

    @Override
    protected FileDownloadDto applyDownload(DepartmentFileEntity file, StorageContext ctx) {

        String originalName = file.getName();
        String encoded = UriUtils.encode(originalName, StandardCharsets.UTF_8);
        String translit = transliterationService.transliterate(originalName);
        StreamingResponseBody body = outputStream -> {
            try (InputStream stream = minioFileService.downloadFile(DEPARTMENT_BUCKET, file.getStorageKey())) {
                stream.transferTo(outputStream);
            }
        };
        return new FileDownloadDto(body, originalName, encoded, translit);
    }

    @Override
    protected void applyDelete(DepartmentFileEntity file) {
        file.setDeleted(true);
        file.setDeletedAt(Instant.now());
        fileRepo.save(file);
    }

    @Override
    protected DepartmentFileInfoDto applyRestore(DepartmentFileEntity file, DepartmentFolderEntity folder, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());
        String unique = fileNameResolverService.ensureUniqueName(file.getName(), folder, dept, file.getId());

        file.setName(unique);
        file.setFolder(folder);
        file.setDeleted(false);
        file.setDeletedAt(null);

        return mapToDto(fileRepo.save(file));
    }

    @Override
    protected List<DepartmentFileInfoDto> applyList(DepartmentFolderEntity folder, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());
        if (folder != null) {
            return fileRepo.findByDepartmentAndFolderAndIsDeletedFalse(dept, folder)
                    .stream().map(this::mapToDto).toList();
        } else {
            return fileRepo.findByDepartmentAndIsDeletedFalse(dept)
                    .stream().map(this::mapToDto).toList();
        }
    }

    @Override
    protected DepartmentFileInfoDto applyRename(DepartmentFileEntity file, String newName, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());
        fileNameResolverService.ensureUniqueNameOrThrow(newName, file.getFolder(), dept, file.getId());
        file.setName(newName);
        return mapToDto(fileRepo.save(file));
    }

    @Override
    protected DepartmentFileInfoDto applyMove(DepartmentFileEntity file, DepartmentFolderEntity newFolder, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());
        fileNameResolverService.ensureUniqueNameOrThrow(file.getName(), file.getFolder(), dept, file.getId());
        file.setFolder(newFolder);
        return mapToDto(fileRepo.save(file));
    }

    @Override
    protected DepartmentFileInfoDto applyCopy(DepartmentFileEntity file, DepartmentFolderEntity folder, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());
        String unique = fileNameResolverService.ensureUniqueName(file.getName(), folder, dept, null);

        DepartmentFileEntity copy = new DepartmentFileEntity();
        copy.setName(unique);
        copy.setFolder(folder);
        copy.setCreatedBy(dCtx.user().getShortFio());
        copy.setCreatedAt(Instant.now());
        copy.setDeleted(false);
        copy.setFileType(file.getFileType());
        copy.setDepartment(dept);
        copy.setFileSize(file.getFileSize());

        String newKey = minioFileService.generateDepartmentFileKey(dept.getId());
        copy.setStorageKey(newKey);

        try {
            minioFileService.copyObjectServerSide(DEPARTMENT_BUCKET, file.getStorageKey(), newKey);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при server-side копировании файла", e);
        }

        return mapToDto(fileRepo.save(copy));
    }

    @Override
    protected DepartmentFileInfoDto mapToDto(DepartmentFileEntity file) {
        return departmentFileMapper.toDto(file);
    }
}
