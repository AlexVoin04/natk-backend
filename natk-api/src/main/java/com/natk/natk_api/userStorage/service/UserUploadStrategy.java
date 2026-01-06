package com.natk.natk_api.userStorage.service;

import com.natk.common.messaging.ScanTask;
import com.natk.natk_api.baseStorage.enums.FileStatus;
import com.natk.natk_api.baseStorage.MagicValidationResult;
import com.natk.natk_api.baseStorage.context.StorageContext;
import com.natk.natk_api.baseStorage.context.UserContext;
import com.natk.natk_api.baseStorage.intarfece.UploadStrategy;
import com.natk.natk_api.baseStorage.service.MimeTypeValidatorService;
import com.natk.natk_api.minio.MinioFileService;
import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.userStorage.repository.UserFileRepository;
import com.natk.natk_api.users.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserUploadStrategy implements UploadStrategy<
        UserFileEntity,
        UserFolderEntity,
        UserFileRepository,
        UserEntity> {

    private final UserFileRepository repo;
    private final UserFileNameResolverService resolver;
    private final MimeTypeValidatorService mime;
    private final MinioFileService minio;

    @Override
    public UserEntity getOwner(StorageContext ctx) {
        return ((UserContext) ctx).user();
    }

    @Override
    public void ensureUniqueNameOrThrow(String name, UserFolderEntity folder, UserEntity user) {
        resolver.ensureUniqueNameOrThrow(name, folder, user);
    }

    @Override
    public MagicValidationResult validateMime(InputStream data, String name) {
        return mime.validate(data, name);
    }

    @Override
    public String generateIncomingKey(UserEntity user) {
        return minio.generateIncomingUserFileKey(user.getId());
    }

    @Override
    public void uploadToMinio(InputStream stream, long size, String key, String mimeType) {
        minio.uploadFile(stream, size, "incoming", key, mimeType);
    }

    @Override
    public ScanTask buildScanTask(UserFileEntity file, String key, StorageContext ctx, UserEntity user) {
        return new ScanTask(file.getId(), key, user.getId(), ScanTask.OriginType.USER, null);
    }

    @Override
    public UserFileEntity buildNewFileEntity(String name, String mime, long size,
                                                    UserFolderEntity folder, String storageKey,
                                                    StorageContext ctx, UserEntity user) {

        UserFileEntity f = new UserFileEntity();
        f.setName(name);
        f.setFolder(folder);
        f.setFileType(mime);
        f.setCreatedBy(user);
        f.setCreatedAt(Instant.now());
        f.setDeleted(false);
        f.setDeletedAt(null);
        f.setFileSize(size);
        f.setStorageKey(storageKey);
        f.setStatus(FileStatus.UPLOADED_PENDING_SCAN);
        return f;
    }

    @Override
    public void persistFile(UserFileEntity file) { repo.save(file); }

    @Override
    public UserFileEntity reloadAfterSave(UserFileEntity file) { return file; }

    @Override
    public UserFileRepository getRepo() { return repo; }
}
