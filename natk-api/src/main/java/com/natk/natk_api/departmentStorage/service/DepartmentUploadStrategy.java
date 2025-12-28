package com.natk.natk_api.departmentStorage.service;

import com.natk.common.messaging.ScanTask;
import com.natk.natk_api.baseStorage.enums.BucketName;
import com.natk.natk_api.baseStorage.enums.FileStatus;
import com.natk.natk_api.baseStorage.MagicValidationResult;
import com.natk.natk_api.baseStorage.context.DepartmentContext;
import com.natk.natk_api.baseStorage.context.StorageContext;
import com.natk.natk_api.baseStorage.intarfece.UploadStrategy;
import com.natk.natk_api.baseStorage.service.MimeTypeValidatorService;
import com.natk.natk_api.department.model.DepartmentEntity;
import com.natk.natk_api.department.permission.DepartmentAccessService;
import com.natk.natk_api.departmentStorage.model.DepartmentFileEntity;
import com.natk.natk_api.departmentStorage.model.DepartmentFolderEntity;
import com.natk.natk_api.departmentStorage.repository.DepartmentFileRepository;
import com.natk.natk_api.minio.MinioFileService;
import com.natk.natk_api.rabbit.ScanTaskPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DepartmentUploadStrategy implements UploadStrategy<
        DepartmentFileEntity,
        DepartmentFolderEntity,
        DepartmentFileRepository,
        DepartmentEntity> {

    private final DepartmentFileRepository repo;
    private final DepartmentFileNameResolverService resolver;
    private final MimeTypeValidatorService mime;
    private final MinioFileService minio;
    private final ScanTaskPublisher publisher;
    private final DepartmentAccessService accessService;

    @Override
    public DepartmentEntity getOwner(StorageContext ctx) {
        return accessService.getDepartmentOrThrow(((DepartmentContext) ctx).departmentId());
    }

    @Override
    public void ensureUniqueNameOrThrow(String name, DepartmentFolderEntity folder, DepartmentEntity dept) {
        resolver.ensureUniqueNameOrThrow(name, folder, dept);
    }

    @Override
    public MagicValidationResult validateMime(InputStream data, String name) {
        return mime.validate(data, name);
    }

    @Override
    public String generateIncomingKey(DepartmentEntity dept) {
        return minio.generateIncomingDepartmentFileKey(dept.getId());
    }

    @Override
    public void uploadToMinio(InputStream stream, long size, String key, String mimeType) {
        minio.uploadFile(stream, size, BucketName.INCOMING.value(), key, mimeType);
    }

    @Override
    public ScanTask buildScanTask(DepartmentFileEntity file, String key, StorageContext ctx, DepartmentEntity dept) {
        var user = ((DepartmentContext) ctx).user();
        return new ScanTask(file.getId(), key, user.getId(), ScanTask.OriginType.DEPARTMENT, dept.getId());
    }

    @Override
    public DepartmentFileEntity buildNewFileEntity(String name, String mime, long size,
                                                   DepartmentFolderEntity folder, String storageKey,
                                                   StorageContext ctx, DepartmentEntity dept) {

        var dc = (DepartmentContext) ctx;

        DepartmentFileEntity f = new DepartmentFileEntity();
        f.setName(name);
        f.setFolder(folder);
        f.setDepartment(dept);
        f.setFileType(mime);
        f.setFileSize(size);
        f.setCreatedBy(dc.user().getShortFio());
        f.setCreatedAt(Instant.now());
        f.setStorageKey(storageKey);
        f.setStatus(FileStatus.UPLOADED_PENDING_SCAN);
        f.setDeleted(false);
        return f;
    }

    @Override
    public void persistFile(DepartmentFileEntity file) { repo.save(file); }

    @Override
    public DepartmentFileEntity reloadAfterSave(DepartmentFileEntity file) { return file; }

    @Override
    public DepartmentFileRepository getRepo() { return repo; }
}
