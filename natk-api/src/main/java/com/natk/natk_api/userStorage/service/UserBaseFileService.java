package com.natk.natk_api.userStorage.service;

import com.natk.natk_api.baseStorage.context.StorageContext;
import com.natk.natk_api.baseStorage.context.UserContext;
import com.natk.natk_api.baseStorage.service.BaseFileService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class UserBaseFileService extends BaseFileService<UserFileEntity, UserFolderEntity, UserFileRepository, UserFolderRepository, FileInfoDto> {

    private final CurrentUserService currentUserService;
    private final UserFileMapper fileMapper;
    private final FileNameResolverService fileNameResolverService;
    private final MimeTypeValidatorService mimeTypeValidatorService;
    private final TransliterationService transliterationService;

    public UserBaseFileService(
            UserFileRepository fileRepo,
            UserFolderRepository folderRepo,
            CurrentUserService currentUserService,
            UserFileMapper fileMapper,
            FileNameResolverService fileNameResolverService,
            MimeTypeValidatorService mimeTypeValidatorService,
            TransliterationService transliterationService
    ) {
        super(fileRepo, folderRepo);
        this.currentUserService = currentUserService;
        this.fileMapper = fileMapper;
        this.fileNameResolverService = fileNameResolverService;
        this.mimeTypeValidatorService = mimeTypeValidatorService;
        this.transliterationService = transliterationService;
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
    public List<FileInfoDto> listFiles(UUID folderId, UUID targetFolderId){
        return super.listFiles(folderId, getContext());
    }

    @Transactional
    public FileInfoDto updateFile(UUID fileId, UpdateFileDto dto){
        return super.updateFile(fileId, dto, getContext());
    }

    @Transactional
    public FileInfoDto copyFile(UUID fileId, UUID targetFolderId){
        return super.copyFile(fileId, targetFolderId, getContext());
    }

    @Override
    protected UserFileEntity findFile(UUID id, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        return fileRepo.findByIdAndCreatedBy(id, user)
                .orElseThrow(() -> new AccessDeniedException("File not found or not owned by user"));
    }

    @Override
    protected UserFileEntity findDeletedFile(UUID id, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        return fileRepo.findByIdAndCreatedByAndIsDeletedTrue(id, user)
                .orElseThrow(() -> new AccessDeniedException("Deleted file not found or not owned by user"));
    }

    @Override
    protected UserFolderEntity findFolder(UUID id, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        return folderRepo.findByIdAndUserAndIsDeletedFalse(id, user)
                .orElseThrow(() -> new AccessDeniedException("Folder not found or not owned by user"));
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
        mimeTypeValidatorService.validate(dto.fileData());
        String mimeType = mimeTypeValidatorService.detectMimeType(dto.fileData());

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

        return fileMapper.toDto(fileRepo.save(file));
    }

    @Override
    protected FileDownloadDto applyDownload(UserFileEntity file, StorageContext ctx) {
        String originalName = file.getName();
        String encoded = UriUtils.encode(originalName, StandardCharsets.UTF_8);
        String translit = transliterationService.transliterate(originalName);
        return new FileDownloadDto(file.getFileData(), originalName, encoded, translit);
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
        String unique = fileNameResolverService.generateUniqueFileName(file.getName(), folder, user);

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
    protected FileInfoDto applyUpdate(UserFileEntity file, UpdateFileDto dto, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        if (dto.newName() != null && !dto.newName().isBlank()) {
            fileNameResolverService.ensureUniqueNameOrThrow(dto.newName(), file.getFolder(), user);
            file.setName(dto.newName());
        }

        //TODO: проверять имя
        if (Boolean.TRUE.equals(dto.moveToRoot())) {
            file.setFolder(null);
        } else if (dto.newFolderId() != null) {
            file.setFolder(findFolder(dto.newFolderId(), ctx));
        }
        return fileMapper.toDto(fileRepo.save(file));
    }

    @Override
    protected FileInfoDto applyCopy(UserFileEntity file, UserFolderEntity folder, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        String unique = fileNameResolverService.generateUniqueFileName(file.getName(), folder, user);

        UserFileEntity copy = new UserFileEntity();
        copy.setName(unique);
        copy.setFolder(folder);
        copy.setCreatedBy(user);
        copy.setCreatedAt(Instant.now());
        copy.setDeleted(false);
        copy.setFileType(file.getFileType());
        copy.setFileData(file.getFileData());

        return fileMapper.toDto(fileRepo.save(copy));
    }

    @Override
    protected FileInfoDto mapToDto(UserFileEntity file) {
        return fileMapper.toDto(file);
    }
}
