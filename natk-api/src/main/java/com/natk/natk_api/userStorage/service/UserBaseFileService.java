package com.natk.natk_api.userStorage.service;

import com.natk.natk_api.baseStorage.FileStatus;
import com.natk.natk_api.baseStorage.MagicValidationResult;
import com.natk.natk_api.baseStorage.context.StorageContext;
import com.natk.natk_api.baseStorage.context.UserContext;
import com.natk.natk_api.baseStorage.service.BaseFileService;
import com.natk.natk_api.baseStorage.dto.FileDownloadDto;
import com.natk.natk_api.baseStorage.service.MimeTypeValidatorService;
import com.natk.natk_api.baseStorage.service.TransliterationService;
import com.natk.natk_api.exception.FileOrFolderNotFoundOrNoAccessException;
import com.natk.natk_api.minio.MinioFileService;
import com.natk.common.messaging.ScanTask;
import com.natk.natk_api.rabbit.ScanTaskPublisher;
import com.natk.natk_api.userStorage.dto.FileInfoDto;
import com.natk.natk_api.baseStorage.dto.UploadFileDto;
import com.natk.natk_api.userStorage.mapper.UserFileMapper;
import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.userStorage.repository.UserFileRepository;
import com.natk.natk_api.userStorage.repository.UserFolderRepository;
import com.natk.natk_api.users.model.UserEntity;
import com.natk.natk_api.users.service.CurrentUserService;
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
public class UserBaseFileService extends BaseFileService<UserFileEntity, UserFolderEntity, UserFileRepository, UserFolderRepository, FileInfoDto> {

    private final CurrentUserService currentUserService;
    private final UserFileMapper fileMapper;
    private final UserFileNameResolverService fileNameResolverService;
    private final MimeTypeValidatorService mimeTypeValidatorService;
    private final TransliterationService transliterationService;
    private final MinioFileService minioFileService;
    private final ScanTaskPublisher scanTaskPublisher;
    private static final String USER_BUCKET = "user-files";

    public UserBaseFileService(
            UserFileRepository fileRepo,
            UserFolderRepository folderRepo,
            CurrentUserService currentUserService,
            UserFileMapper fileMapper,
            UserFileNameResolverService fileNameResolverService,
            MimeTypeValidatorService mimeTypeValidatorService,
            TransliterationService transliterationService,
            MinioFileService minioFileService,
            ScanTaskPublisher scanTaskPublisher
    ) {
        super(fileRepo, folderRepo);
        this.currentUserService = currentUserService;
        this.fileMapper = fileMapper;
        this.fileNameResolverService = fileNameResolverService;
        this.mimeTypeValidatorService = mimeTypeValidatorService;
        this.transliterationService = transliterationService;
        this.minioFileService = minioFileService;
        this.scanTaskPublisher = scanTaskPublisher;
    }

    protected UserContext getContext(){
        return new UserContext(currentUserService.getCurrentUser());
    }

    @Transactional
    public FileInfoDto uploadFile(UploadFileDto dto){
        return super.uploadFile(dto, getContext());
    }

    @Transactional
    public FileDownloadDto getFileDownloadData(UUID fileId){
        return super.getFileDownloadData(fileId, getContext());
    }

    @Transactional
    public FileInfoDto getFile(UUID fileId){
        return super.getFile(fileId, getContext());
    }

    @Transactional
    public void deleteFile(UUID fileId){
        super.deleteFile(fileId, getContext());
    }

    @Transactional
    public FileInfoDto restoreFile(UUID fileId, UUID targetFolderId){
        return super.restoreFile(fileId, targetFolderId, getContext());
    }

    @Transactional
    public List<FileInfoDto> listFiles(UUID folderId){
        return super.listFiles(folderId, getContext());
    }

    @Transactional
    public FileInfoDto renameFile(UUID fileId, String newName) {
        return super.renameFile(fileId, newName, getContext());
    }

    @Transactional
    public FileInfoDto moveFile(UUID fileId, UUID newFolderId, Boolean moveToRoot) {
        return super.moveFile(fileId, newFolderId, moveToRoot, getContext());
    }

    @Transactional
    public FileInfoDto copyFile(UUID fileId, UUID targetFolderId){
        return super.copyFile(fileId, targetFolderId, getContext());
    }

    @Override
    protected UserFileEntity findFile(UUID id, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        return fileRepo.findByIdAndCreatedBy(id, user)
                .orElseThrow(FileOrFolderNotFoundOrNoAccessException::new);
    }

    @Override
    protected UserFileEntity findDeletedFile(UUID id, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        return fileRepo.findByIdAndCreatedByAndIsDeletedTrue(id, user)
                .orElseThrow(FileOrFolderNotFoundOrNoAccessException::new);
    }

    @Override
    protected UserFolderEntity findFolder(UUID id, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        return folderRepo.findByIdAndUserAndIsDeletedFalse(id, user)
                .orElseThrow(FileOrFolderNotFoundOrNoAccessException::new);
    }

    @Override
    protected void checkUploadAccess(UserFolderEntity folder, StorageContext ctx) {}
    @Override
    protected void checkReadAccess(UserFileEntity file, StorageContext ctx) {}
    @Override
    protected void checkUpdateAccess(UserFileEntity file, StorageContext ctx) {}
    @Override
    protected void checkDeleteAccess(UserFileEntity file, StorageContext ctx) {}
    @Override
    protected void checkRestoreAccess(UserFileEntity file, StorageContext ctx) {}

    @Override
    protected FileInfoDto applyUploadFile(UploadFileDto dto, UserFolderEntity folder, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();

        fileNameResolverService.ensureUniqueNameOrThrow(dto.name(), folder, user);

        MagicValidationResult res = mimeTypeValidatorService.validate(dto.fileData(), dto.name());

        String quarantineKey = minioFileService.generateIncomingUserFileKey(user.getId());

        InputStream fullStream = new SequenceInputStream(
                new ByteArrayInputStream(res.header()),
                dto.fileData()
        );

        UserFileEntity file = new UserFileEntity();
        file.setName(dto.name());
        file.setFolder(folder);
        file.setFileType(res.mimeType());
        file.setCreatedBy(user);
        file.setCreatedAt(Instant.now());
        file.setDeleted(false);
        file.setDeletedAt(null);
        file.setFileSize(dto.size());
        file.setStorageKey(quarantineKey);
        file.setStatus(FileStatus.UPLOADED_PENDING_SCAN);

        minioFileService.uploadFile(fullStream, dto.size(), "incoming", quarantineKey, res.mimeType());

        fileRepo.save(file);

        scanTaskPublisher.publish(new ScanTask(
                file.getId(),
                quarantineKey,
                user.getId()
        ));

        return fileMapper.toDto(file);
    }

    @Override
    protected FileDownloadDto applyDownload(UserFileEntity file, StorageContext ctx) {

        String originalName = file.getName();
        String encoded = UriUtils.encode(originalName, StandardCharsets.UTF_8);
        String translit = transliterationService.transliterate(originalName);
        StreamingResponseBody body = outputStream -> {
            try (InputStream stream = minioFileService.downloadFile(USER_BUCKET, file.getStorageKey())) {
                stream.transferTo(outputStream);
            }
        };
        return new FileDownloadDto(body, originalName, encoded, translit);
    }

    @Override
    protected void applyDelete(UserFileEntity file) {
        file.setDeleted(true);
        file.setDeletedAt(Instant.now());
        fileRepo.save(file);
    }

    @Override
    protected FileInfoDto applyRestore(UserFileEntity file, UserFolderEntity folder, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        String unique = fileNameResolverService.ensureUniqueName(file.getName(), folder, user, file.getId());

        file.setName(unique);
        file.setFolder(folder);
        file.setDeleted(false);
        file.setDeletedAt(null);

        return fileMapper.toDto(fileRepo.save(file));
    }

    @Override
    protected List<FileInfoDto> applyList(UserFolderEntity folder, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        return fileRepo.findByCreatedByAndFolderAndIsDeletedFalse(user, folder)
                .stream().map(fileMapper::toDto).toList();
    }

    @Override
    protected FileInfoDto applyRename(UserFileEntity file, String newName, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        fileNameResolverService.ensureUniqueNameOrThrow(newName, file.getFolder(), user, file.getId());
        file.setName(newName);
        return fileMapper.toDto(fileRepo.save(file));
    }

    @Override
    protected FileInfoDto applyMove(UserFileEntity file, UserFolderEntity newFolder, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        fileNameResolverService.ensureUniqueNameOrThrow(file.getName(), newFolder, user, file.getId());
        file.setFolder(newFolder);
        return fileMapper.toDto(fileRepo.save(file));
    }

    @Override
    protected FileInfoDto applyCopy(UserFileEntity file, UserFolderEntity folder, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        String unique = fileNameResolverService.ensureUniqueName(file.getName(), folder, user, null);

        UserFileEntity copy = new UserFileEntity();
        copy.setName(unique);
        copy.setFolder(folder);
        copy.setCreatedBy(user);
        copy.setCreatedAt(Instant.now());
        copy.setDeleted(false);
        copy.setFileType(file.getFileType());
        copy.setFileSize(file.getFileSize());
        copy.setStatus(file.getStatus());

        String newKey  = minioFileService.generateUserFileKey(user.getId());
        copy.setStorageKey(newKey);

        try {
            minioFileService.copyObjectServerSide(USER_BUCKET, file.getStorageKey(), newKey);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при server-side копировании файла", e);
        }

        return fileMapper.toDto(fileRepo.save(copy));
    }

    @Override
    protected FileInfoDto mapToDto(UserFileEntity file) {
        return fileMapper.toDto(file);
    }
}
