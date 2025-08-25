package com.natk.natk_api.departmentStorage.service;

import com.natk.natk_api.baseStorage.context.DepartmentContext;
import com.natk.natk_api.baseStorage.context.StorageContext;
import com.natk.natk_api.baseStorage.service.BaseFolderService;
import com.natk.natk_api.department.model.DepartmentEntity;
import com.natk.natk_api.department.permission.DepartmentAccessService;
import com.natk.natk_api.departmentStorage.dto.DepartmentFolderDto;
import com.natk.natk_api.departmentStorage.mapper.DepartmentFolderMapper;
import com.natk.natk_api.departmentStorage.model.DepartmentFolderEntity;
import com.natk.natk_api.departmentStorage.repository.DepartmentFolderRepository;
import com.natk.natk_api.userStorage.dto.CreateFolderDto;
import com.natk.natk_api.userStorage.dto.FolderTreeDto;
import com.natk.natk_api.userStorage.dto.UpdateFolderDto;
import com.natk.natk_api.users.model.UserEntity;
import com.natk.natk_api.users.service.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DepartmentBaseFolderService extends BaseFolderService<DepartmentFolderEntity, DepartmentFolderRepository, DepartmentFolderDto> {

    private final CurrentUserService currentUserService;
    private final DepartmentFolderMapper folderMapper;
    private final DepartmentFolderNameResolverService folderNameResolverService;
    private final DepartmentAccessService departmentAccessService;

    public DepartmentBaseFolderService(
            DepartmentFolderRepository folderRepo,
            CurrentUserService currentUserService,
            DepartmentFolderMapper folderMapper,
            DepartmentFolderNameResolverService folderNameResolverService,
            DepartmentAccessService departmentAccessService) {
        super(folderRepo);
        this.currentUserService = currentUserService;
        this.folderMapper = folderMapper;
        this.folderNameResolverService = folderNameResolverService;
        this.departmentAccessService = departmentAccessService;
    }

    @Transactional
    public DepartmentFolderDto createFolder(UUID departmentId, CreateFolderDto dto) {
        return super.createFolder(dto, getContext(departmentId));
    }

    @Transactional
    public void deleteFolder(UUID departmentId, UUID folderId) {
        super.deleteFolder(folderId, getContext(departmentId));
    }

    @Transactional
    public DepartmentFolderDto updateFolder(UUID departmentId, UUID folderId, UpdateFolderDto dto) {
        return super.updateFolder(folderId, dto, getContext(departmentId));
    }

    @Transactional
    public DepartmentFolderDto restoreFolder(UUID departmentId, UUID folderId, UUID targetParentFolderId) {
        return super.restoreFolder(folderId, targetParentFolderId, getContext(departmentId));
    }

    @Transactional
    public List<DepartmentFolderDto> listFolders(UUID departmentId, UUID parentFolderId) {
        return super.listFolders(parentFolderId, getContext(departmentId));
    }

    @Transactional
    public List<FolderTreeDto> getFolderTree(UUID departmentId) {
        return super.getFolderTree(getContext(departmentId));
    }

    protected StorageContext getContext(UUID departmentId) {
        UserEntity user = currentUserService.getCurrentUser();
        return new DepartmentContext(user, departmentId);
    }

    @Override
    protected DepartmentFolderEntity findFolder(UUID id, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());

        DepartmentFolderEntity folder = folderRepo.findByIdAndDepartmentAndIsDeletedFalse(id, dept)
                .orElseThrow(() -> new AccessDeniedException("Folder not found"));

        if (!folder.isPublic() && !departmentAccessService.hasFolderAccess(dCtx.user(), folder)) {
            throw new AccessDeniedException("Access denied");
        }
        return folder;
    }

    @Override
    protected DepartmentFolderEntity findDeletedFolder(UUID id, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());

        return folderRepo.findByIdAndDepartmentAndIsDeletedTrue(id, dept)
                .orElseThrow(() -> new AccessDeniedException("Deleted folder not found"));
    }

    @Override
    protected DepartmentFolderEntity buildNewFolder(String name, DepartmentFolderEntity parent, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());

        DepartmentFolderEntity folder = new DepartmentFolderEntity();
        folder.setName(name);
        folder.setDepartment(dept);
        folder.setParentFolder(parent);
        folder.setCreatedBy(dCtx.user().getShortFio());
        folder.setPublic(false);
        folder.setDeleted(false);
        folder.setCreatedAt(Instant.now());
        return folder;
    }

    @Override
    protected void checkCreateAccess(DepartmentFolderEntity departmentFolderEntity, StorageContext ctx) {
        //Проверка уж есть в findFolder
    }

    @Override
    protected void checkUpdateAccess(DepartmentFolderEntity departmentFolderEntity, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        if (!departmentAccessService.canManage(dCtx.user(), dCtx.departmentId())) {
            throw new AccessDeniedException("No access rights to the folder");
        }
    }

    @Override
    protected void checkDeleteAccess(DepartmentFolderEntity departmentFolderEntity, StorageContext ctx) {
        checkUpdateAccess(departmentFolderEntity, ctx);
    }

    @Override
    protected void checkRestoreAccess(DepartmentFolderEntity departmentFolderEntity, StorageContext ctx) {
        checkUpdateAccess(departmentFolderEntity, ctx);
    }

    @Override
    protected DepartmentFolderDto doCreateFolder(String name, DepartmentFolderEntity parent, StorageContext context) {
        DepartmentContext dCtx = (DepartmentContext) context;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());

        folderNameResolverService.ensureUniqueNameOrThrow(name, parent, dept);

        DepartmentFolderEntity folder = buildNewFolder(name, parent, context);
        return folderMapper.toDto(folderRepo.save(folder));
    }

    //TODO: разделить переименование и перемещение
    @Override
    protected DepartmentFolderDto applyUpdate(DepartmentFolderEntity folder, UpdateFolderDto dto, StorageContext context) {
        boolean modified = false;
        DepartmentContext dCtx = (DepartmentContext) context;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());

        if (dto.newName() != null && !dto.newName().isBlank()) {
            folder.setName(dto.newName());
            modified = true;
        }

        if (Boolean.TRUE.equals(dto.moveToRoot()) && folder.getParentFolder() != null) {
            folderNameResolverService.ensureUniqueNameOrThrow(dto.newName(), null, dept);
            folder.setParentFolder(null);
            modified = true;
        }

        if (Boolean.FALSE.equals(dto.moveToRoot()) && dto.newParentFolderId().isPresent()) {
            DepartmentFolderEntity newParent = folderRepo.findByIdAndDepartmentAndIsDeletedFalse(dto.newParentFolderId().get(), dept)
                    .orElseThrow(() -> new AccessDeniedException("New parent not found"));

            if (isDescendant(folder, newParent)) {
                throw new IllegalArgumentException("Cannot move folder inside its descendant");
            }

            folderNameResolverService.ensureUniqueNameOrThrow(folder.getName(), newParent, dept);
            folder.setParentFolder(newParent);
            modified = true;
        }

        if (!modified) throw new IllegalArgumentException("No changes provided");

        return folderMapper.toDto(folderRepo.save(folder));
    }

    private boolean isDescendant(DepartmentFolderEntity folder, DepartmentFolderEntity target) {
        while (target != null) {
            if (target.getId().equals(folder.getId())) return true;
            target = target.getParentFolder();
        }
        return false;
    }

    @Override
    protected void applyDelete(DepartmentFolderEntity departmentFolderEntity) {
        departmentFolderEntity.setDeleted(true);
        departmentFolderEntity.setDeletedAt(Instant.now());
        folderRepo.save(departmentFolderEntity);
    }

    @Override
    protected DepartmentFolderDto applyRestore(DepartmentFolderEntity folder, DepartmentFolderEntity parent) {
        if (isDescendant(folder, parent)) {
            throw new IllegalArgumentException("Cannot restore into descendant");
        }
        folder.setParentFolder(parent);
        folder.setDeleted(false);
        folder.setDeletedAt(null);
        return folderMapper.toDto(folderRepo.save(folder));
    }

    @Override
    protected List<DepartmentFolderDto> applyList(DepartmentFolderEntity parent, StorageContext context) {
        DepartmentContext dCtx = (DepartmentContext) context;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());

        return folderRepo.findByDepartmentAndParentFolderAndIsDeletedFalse(dept, parent).stream()
                .filter(f -> f.isPublic() || departmentAccessService.hasFolderAccess(dCtx.user(), f))
                .map(folderMapper::toDto)
                .toList();
    }

    @Override
    protected List<FolderTreeDto> applyTree(StorageContext context) {
        DepartmentContext dCtx = (DepartmentContext) context;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());

        List<DepartmentFolderEntity> all = folderRepo.findByDepartmentAndIsDeletedFalse(dept).stream()
                .filter(f -> f.isPublic() || departmentAccessService.hasFolderAccess(dCtx.user(), f))
                .toList();

        return buildFolderTree(
                all,
                DepartmentFolderEntity::getId,
                DepartmentFolderEntity::getParentFolder,
                DepartmentFolderEntity::getName
        );
    }
}
