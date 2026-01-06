package com.natk.natk_api.departmentStorage.service;

import com.natk.natk_api.audit.enums.PurgeAuditType;
import com.natk.natk_api.audit.service.StoragePurgeAuditService;
import com.natk.natk_api.baseStorage.enums.BucketName;
import com.natk.natk_api.baseStorage.PurgeStats;
import com.natk.natk_api.baseStorage.context.DepartmentContext;
import com.natk.natk_api.baseStorage.context.StorageContext;
import com.natk.natk_api.baseStorage.service.BasePurgeService;
import com.natk.natk_api.department.model.DepartmentEntity;
import com.natk.natk_api.department.permission.DepartmentAccessService;
import com.natk.natk_api.departmentStorage.dto.PurgeItemDto;
import com.natk.natk_api.departmentStorage.model.DepartmentFileEntity;
import com.natk.natk_api.departmentStorage.model.DepartmentFolderEntity;
import com.natk.natk_api.departmentStorage.repository.DepartmentFileRepository;
import com.natk.natk_api.departmentStorage.repository.DepartmentFolderRepository;
import com.natk.natk_api.exception.FileOrFolderNotFoundOrNoAccessException;
import com.natk.natk_api.minio.MinioFileService;
import com.natk.natk_api.users.model.UserEntity;
import com.natk.natk_api.users.service.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DepartmentPurgeService extends BasePurgeService<DepartmentFolderEntity, DepartmentFileEntity> {

    private final DepartmentFileRepository fileRepo;
    private final DepartmentFolderRepository folderRepo;
    private final StoragePurgeAuditService audit;
    private final CurrentUserService currentUserService;
    private final DepartmentAccessService departmentAccessService;

    public DepartmentPurgeService(MinioFileService minio, DepartmentFileRepository fileRepo, DepartmentFolderRepository folderRepo, DepartmentAccessService departmentAccessService, StoragePurgeAuditService audit, CurrentUserService currentUserService) {
        super(minio);
        this.fileRepo = fileRepo;
        this.folderRepo = folderRepo;
        this.audit = audit;
        this.currentUserService = currentUserService;
        this.departmentAccessService = departmentAccessService;
    }

    protected StorageContext getContext(UUID departmentId) {
        UserEntity user = currentUserService.getCurrentUser();
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(departmentId);
        if (!departmentAccessService.hasAnyAccess(user, dept.getId())) {
            throw new FileOrFolderNotFoundOrNoAccessException();
        }
        return new DepartmentContext(user, dept);
    }

    @Transactional
    public void purgeFolder(UUID id, UUID departmentId) {
        StorageContext ctx = getContext(departmentId);
        DepartmentContext dCtx = (DepartmentContext) ctx;
        PurgeStats stats = super.purgeFolder(id, ctx);

        audit.logDepartmentPurge(
                dCtx.user().getId(),
                departmentId,
                id,
                PurgeAuditType.FOLDER,
                stats.files(),
                stats.folders()
        );
    }

    @Transactional
    public void purgeFile(UUID id, UUID departmentId) {
        StorageContext ctx = getContext(departmentId);
        DepartmentContext dCtx = (DepartmentContext) ctx;
        PurgeStats stats = super.purgeFile(id, ctx);

        audit.logDepartmentPurge(
                dCtx.user().getId(),
                departmentId,
                id,
                PurgeAuditType.FILE,
                stats.files(),
                0
        );
    }

    @Transactional
    public PurgeStats purgeMultiple(List<PurgeItemDto> items, UUID departmentId) {
        StorageContext ctx = getContext(departmentId);
        DepartmentContext dCtx = (DepartmentContext) ctx;
        PurgeStats stats = super.purgeMultiple(items, ctx);
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        audit.logDepartmentPurge(
                                dCtx.user().getId(),
                                departmentId,
                                null,
                                PurgeAuditType.BATCH,
                                stats.files(),
                                stats.folders()
                        );
                    }
                }
        );
        return stats;
    }

    @Override
    protected Optional<DepartmentFileEntity> loadDeletedFileById(UUID id, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = dCtx.department();
        return fileRepo.findByIdAndDepartmentAndIsDeletedTrue(id, dept);
    }

    @Override
    protected Optional<DepartmentFolderEntity> loadDeletedFolderById(UUID id, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = dCtx.department();
        return folderRepo.findByIdAndDepartmentAndIsDeletedTrue(id, dept);
    }

    @Override
    protected List<DepartmentFolderEntity> findDeletedChildFolders(DepartmentFolderEntity parent, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = dCtx.department();
        return folderRepo.findByDepartmentAndParentFolderAndIsDeletedTrue(dept, parent);
    }

    @Override
    protected List<DepartmentFileEntity> findDeletedFilesInFolder(DepartmentFolderEntity folder, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = dCtx.department();
        return fileRepo.findByDepartmentAndFolderAndIsDeletedTrue(dept, folder);
    }

    @Override
    protected String extractStorageKey(DepartmentFileEntity file, StorageContext ctx) {
        return file.getStorageKey();
    }

    @Override
    protected void deleteFileEntities(List<DepartmentFileEntity> files, StorageContext ctx) {
        fileRepo.deleteAll(files);
    }

    @Override
    protected void deleteFolderEntities(List<DepartmentFolderEntity> folders, StorageContext ctx) {
        folderRepo.deleteAll(folders);
    }

    @Override
    protected String bucketName() {
        return BucketName.DEPARTMENTS_FILES.value();
    }

    @Override
    protected List<DepartmentFolderEntity> findDeletedRootFolders(StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = dCtx.department();
        return folderRepo.findByDepartmentAndParentFolderIsNullAndIsDeletedTrue(dept);
    }

    @Override
    protected void folderAccessForFile(DepartmentFileEntity file, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        if(!departmentAccessService.hasFolderAccess(dCtx.user(), file.getFolder())){
            throw new AccessDeniedException("No rights to delete");
        }
    }

    @Override
    protected void folderAccessForFolder(DepartmentFolderEntity folder, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        if(!departmentAccessService.hasFolderAccess(dCtx.user(), folder.getParentFolder())){
            throw new AccessDeniedException("No rights to delete");
        }
    }
}
