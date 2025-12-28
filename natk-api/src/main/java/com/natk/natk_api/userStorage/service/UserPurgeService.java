package com.natk.natk_api.userStorage.service;

import com.natk.natk_api.audit.enums.PurgeAuditType;
import com.natk.natk_api.audit.service.StoragePurgeAuditService;
import com.natk.natk_api.baseStorage.enums.BucketName;
import com.natk.natk_api.baseStorage.PurgeStats;
import com.natk.natk_api.baseStorage.context.StorageContext;
import com.natk.natk_api.baseStorage.context.UserContext;
import com.natk.natk_api.baseStorage.service.BasePurgeService;
import com.natk.natk_api.departmentStorage.dto.PurgeItemDto;
import com.natk.natk_api.minio.MinioFileService;
import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.userStorage.repository.UserFileRepository;
import com.natk.natk_api.userStorage.repository.UserFolderRepository;
import com.natk.natk_api.users.model.UserEntity;
import com.natk.natk_api.users.service.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserPurgeService extends BasePurgeService<UserFolderEntity, UserFileEntity> {

    private final UserFileRepository fileRepo;
    private final UserFolderRepository folderRepo;
    private final CurrentUserService currentUserService;
    private final StoragePurgeAuditService audit;

    public UserPurgeService(MinioFileService minio, UserFileRepository fileRepo, UserFolderRepository folderRepo, CurrentUserService currentUserService, StoragePurgeAuditService audit) {
        super(minio);
        this.fileRepo = fileRepo;
        this.folderRepo = folderRepo;
        this.currentUserService = currentUserService;
        this.audit = audit;
    }

    protected UserContext getContext(){
        return new UserContext(currentUserService.getCurrentUser());
    }

    @Transactional
    public void purgeFolder(UUID id) {
        UserContext ctx = getContext();
        PurgeStats stats = super.purgeFolder(id, ctx);

        audit.logUserPurge(ctx.user().getId(), id, PurgeAuditType.FOLDER, stats.files(), stats.folders());
    }

    @Transactional
    public void purgeFile(UUID id) {
        UserContext ctx = getContext();
        PurgeStats stats = super.purgeFile(id, ctx);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    public void afterCommit() {
                        audit.logUserPurge(ctx.user().getId(), id, PurgeAuditType.FILE, stats.files(), 0);
                    }
                }
        );
    }

    @Transactional
    public PurgeStats purgeMultiple(List<PurgeItemDto> items) {
        UserContext ctx = getContext();
        PurgeStats stats = super.purgeMultiple(items, ctx);
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        audit.logUserPurge(ctx.user().getId(), null, PurgeAuditType.BATCH, stats.files(), stats.folders());
                    }
                }
        );
        return stats;
    }

    @Override
    protected Optional<UserFileEntity> loadDeletedFileById(UUID id, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        return fileRepo.findByIdAndCreatedByAndIsDeletedTrue(id, user);
    }

    @Override
    protected Optional<UserFolderEntity> loadDeletedFolderById(UUID id, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        return folderRepo.findByIdAndUserAndIsDeletedTrue(id, user);
    }

    @Override
    protected List<UserFolderEntity> findDeletedChildFolders(UserFolderEntity parent, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        return folderRepo.findByUserAndParentFolderAndIsDeletedTrue(user, parent);
    }

    @Override
    protected List<UserFileEntity> findDeletedFilesInFolder(UserFolderEntity folder, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        if (folder == null) {
            return fileRepo.findByCreatedByAndFolderIsNullAndIsDeletedTrue(user);
        } else {
            return fileRepo.findByCreatedByAndFolderAndIsDeletedTrue(user, folder);
        }
    }

    @Override
    protected String extractStorageKey(UserFileEntity file, StorageContext ctx) {
        return file.getStorageKey();
    }

    @Override
    protected void deleteFileEntities(List<UserFileEntity> files, StorageContext ctx) {
        fileRepo.deleteAll(files);
    }

    @Override
    protected void deleteFolderEntities(List<UserFolderEntity> folders, StorageContext ctx) {
        folderRepo.deleteAll(folders);
    }

    @Override
    protected String bucketName() {
        return BucketName.USER_FILES.value();
    }

    @Override
    protected List<UserFolderEntity> findDeletedRootFolders(StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        return folderRepo.findByUserAndParentFolderIsNullAndIsDeletedTrue(user);
    }

    @Override
    protected void folderAccessForFile(UserFileEntity userFileEntity, StorageContext ctx) {}

    @Override
    protected void folderAccessForFolder(UserFolderEntity folder, StorageContext ctx) {}
}