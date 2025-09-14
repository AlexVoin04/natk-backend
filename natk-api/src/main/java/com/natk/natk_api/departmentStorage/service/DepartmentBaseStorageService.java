package com.natk.natk_api.departmentStorage.service;

import com.natk.natk_api.baseStorage.context.DepartmentContext;
import com.natk.natk_api.baseStorage.context.StorageContext;
import com.natk.natk_api.baseStorage.service.BaseStorageService;
import com.natk.natk_api.department.model.DepartmentEntity;
import com.natk.natk_api.department.permission.DepartmentAccessService;
import com.natk.natk_api.departmentStorage.dto.DepartmentDeletedItemDto;
import com.natk.natk_api.departmentStorage.dto.DepartmentStorageItemDto;
import com.natk.natk_api.departmentStorage.mapper.DepartmentStorageItemMapper;
import com.natk.natk_api.departmentStorage.model.DepartmentFileEntity;
import com.natk.natk_api.departmentStorage.model.DepartmentFolderEntity;
import com.natk.natk_api.departmentStorage.repository.DepartmentFileRepository;
import com.natk.natk_api.departmentStorage.repository.DepartmentFolderRepository;
import com.natk.natk_api.userStorage.dto.FolderContentResponseDto;
import com.natk.natk_api.users.model.UserEntity;
import com.natk.natk_api.users.service.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DepartmentBaseStorageService extends BaseStorageService<
        DepartmentFolderEntity,
        DepartmentFileEntity,
        DepartmentStorageItemDto,
        DepartmentDeletedItemDto
        > {

    private final DepartmentFolderRepository folderRepo;
    private final DepartmentFileRepository fileRepo;
    private final DepartmentStorageItemMapper mapper;
    private final CurrentUserService currentUserService;
    private final DepartmentAccessService departmentAccessService;

    public DepartmentBaseStorageService(
            DepartmentFolderRepository folderRepo,
            DepartmentFileRepository fileRepo,
            DepartmentStorageItemMapper mapper,
            CurrentUserService currentUserService,
            DepartmentAccessService departmentAccessService
    ) {
        this.folderRepo = folderRepo;
        this.fileRepo = fileRepo;
        this.mapper = mapper;
        this.currentUserService = currentUserService;
        this.departmentAccessService = departmentAccessService;
    }

    protected StorageContext getContext(UUID departmentId) {
        UserEntity user = currentUserService.getCurrentUser();
        return new DepartmentContext(user, departmentId);
    }

    @Transactional(readOnly = true)
    public FolderContentResponseDto<DepartmentStorageItemDto> getStorageItems(UUID folderId, UUID departmentId) {
        return super.getStorageItems(folderId, getContext(departmentId));
    }

    @Transactional(readOnly = true)
    public List<DepartmentDeletedItemDto> getDeletedItems(UUID departmentId) {
        return super.getDeletedItems(getContext(departmentId));
    }

    @Override
    protected DepartmentFolderEntity resolveFolderOrRoot(UUID folderId, StorageContext ctx) {
        if (folderId == null) return null;
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());
        return folderRepo.findByIdAndDepartmentAndIsDeletedFalse(folderId, dept)
                .orElseThrow(() -> new AccessDeniedException("Folder not found"));
    }

    @Override
    protected List<DepartmentFolderEntity> fetchActiveFolders(DepartmentFolderEntity parent, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());
        UserEntity user = dCtx.user();

        return folderRepo.findByDepartmentAndParentFolderAndIsDeletedFalse(dept, parent).stream()
                .filter(f -> f.isPublic() || departmentAccessService.hasFolderAccess(user, f))
                .collect(Collectors.toList());
    }

    @Override
    protected List<DepartmentFileEntity> fetchActiveFiles(DepartmentFolderEntity parent, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());
        UserEntity user = dCtx.user();

        List<DepartmentFileEntity> files = (parent == null)
                ? fileRepo.findByDepartmentAndFolderIsNullAndIsDeletedFalse(dept)
                : fileRepo.findByDepartmentAndFolderAndIsDeletedFalse(dept, parent);

        return files.stream()
                .filter(f -> {
                    DepartmentFolderEntity folder = f.getFolder();
                    return folder == null || folder.isPublic() || departmentAccessService.hasFolderAccess(user, folder);
                })
                .toList();
    }

    @Override
    protected List<DepartmentFolderEntity> findDeletedFolders(StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());
        return folderRepo.findByDepartmentAndIsDeletedTrueOrderByDeletedAtDesc(dept);
    }

    @Override
    protected List<DepartmentFileEntity> findDeletedFiles(StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());
        return fileRepo.findByDepartmentAndIsDeletedTrueOrderByDeletedAtDesc(dept);
    }

    @Override
    protected DepartmentStorageItemDto mapFolderToItem(DepartmentFolderEntity folder, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());
        return mapper.toStorageItem(folder, dept);
    }

    @Override
    protected DepartmentStorageItemDto mapFileToItem(DepartmentFileEntity file, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());
        return mapper.toStorageItem(file, dept);
    }

    @Override
    protected DepartmentDeletedItemDto mapFolderToDeleted(DepartmentFolderEntity folder, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());
        return mapper.toDeletedItem(folder, dept);
    }

    @Override
    protected DepartmentDeletedItemDto mapFileToDeleted(DepartmentFileEntity file, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());
        return mapper.toDeletedItem(file, dept);
    }

    @Override
    protected UUID extractFolderId(DepartmentFolderEntity folder) {
        return folder.getId();
    }

    @Override
    protected Instant extractDeletedAt(DepartmentDeletedItemDto dto) {
        return dto.deletedAt();
    }

    @Override
    protected String resolvePathForResponse(DepartmentFolderEntity folder, StorageContext ctx) {
        if (folder == null) {
            DepartmentContext dCtx = (DepartmentContext) ctx;
            DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());
            return "Департамент/" + dept.getName();
        }

        DepartmentFolderEntity current = folder;
        while (current != null) {
            if (current.isDeleted()) {
                DepartmentContext dCtx = (DepartmentContext) ctx;
                DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());
                return "Департамент/" + dept.getName();
            }
            current = current.getParentFolder();
        }

        return folder.buildPath();
    }

    @Override
    protected boolean isFile(DepartmentStorageItemDto dto) {
        return !"folder".equals(dto.type());
    }

    @Override
    protected String extractName(DepartmentStorageItemDto dto) {
        return dto.name();
    }
}