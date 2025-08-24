package com.natk.natk_api.departmentStorage.service;

import com.natk.natk_api.baseStorage.context.DepartmentContext;
import com.natk.natk_api.baseStorage.context.StorageContext;
import com.natk.natk_api.baseStorage.service.BaseFolderService;
import com.natk.natk_api.department.model.DepartmentEntity;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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


    @Override
    protected StorageContext getContext() {
        UserEntity user = currentUserService.getCurrentUser();
        UUID departmentId = departmentAccessService.getCurrentDepartmentId();
        return new DepartmentContext(user, departmentId);
    }

    @Override
    protected DepartmentFolderEntity findFolder(UUID id, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());

        DepartmentFolderEntity folder = folderRepo.findByIdAndDepartmentAndIsDeletedFalse(id, dept)
                .orElseThrow(() -> new AccessDeniedException("Folder not found"));

        if (!folder.isPublic() && !departmentAccessService.hasAccess(dCtx.user(), folder)) {
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
    protected void checkCreateAccess(DepartmentFolderEntity parent, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        if (!departmentAccessService.canManage(dCtx.user(), dCtx.departmentId())) {
            throw new AccessDeniedException("No rights to create folder");
        }
    }

    @Override
    protected void checkUpdateAccess(DepartmentFolderEntity departmentFolderEntity, StorageContext ctx) {
        DepartmentContext dCtx = (DepartmentContext) ctx;
        if (!departmentAccessService.canManage(dCtx.user(), dCtx.departmentId())) {
            throw new AccessDeniedException("No rights to update folder");
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

    @Override
    protected DepartmentFolderDto applyUpdate(DepartmentFolderEntity folder, UpdateFolderDto dto, StorageContext context) {
        boolean modified = false;
        DepartmentContext dCtx = (DepartmentContext) context;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());

        if (dto.newName() != null && !dto.newName().isBlank()) {
            folderNameResolverService.ensureUniqueNameOrThrow(dto.newName(), folder.getParentFolder(), dept);
            folder.setName(dto.newName());
            modified = true;
        }

        if (dto.newParentFolderId().isPresent()) {
            DepartmentFolderEntity newParent = folderRepo.findByIdAndDepartmentAndIsDeletedFalse(dto.newParentFolderId().get(), dept)
                    .orElseThrow(() -> new AccessDeniedException("New parent not found"));

            if (isDescendant(folder, newParent)) {
                throw new IllegalArgumentException("Cannot move folder inside its descendant");
            }

            folderNameResolverService.ensureUniqueNameOrThrow(dto.newName(), newParent, dept);
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
                .filter(f -> f.isPublic() || departmentAccessService.hasAccess(dCtx.user(), f))
                .map(folderMapper::toDto)
                .toList();
    }

    @Override
    protected List<FolderTreeDto> applyTree(StorageContext context) {
        DepartmentContext dCtx = (DepartmentContext) context;
        DepartmentEntity dept = departmentAccessService.getDepartmentOrThrow(dCtx.departmentId());

        List<DepartmentFolderEntity> all = folderRepo.findByDepartmentAndIsDeletedFalse(dept);

        // фильтруем только доступные
        all = all.stream()
                .filter(f -> f.isPublic() || departmentAccessService.hasAccess(dCtx.user(), f))
                .toList();

        Map<UUID, List<DepartmentFolderEntity>> childrenMap = all.stream()
                .filter(f -> f.getParentFolder() != null)
                .collect(Collectors.groupingBy(f -> f.getParentFolder().getId()));

        List<DepartmentFolderEntity> roots = all.stream()
                .filter(f -> f.getParentFolder() == null)
                .toList();

        return roots.stream()
                .map(folder -> buildTree(folder, childrenMap, 0))
                .toList();
    }

    private FolderTreeDto buildTree(DepartmentFolderEntity folder, Map<UUID, List<DepartmentFolderEntity>> childrenMap, int depth) {
        List<FolderTreeDto> children = childrenMap.getOrDefault(folder.getId(), List.of()).stream()
                .map(child -> buildTree(child, childrenMap, depth + 1))
                .toList();

        return new FolderTreeDto(
                folder.getId(),
                folder.getName(),
                depth,
                children
        );
    }

    @Transactional
    public DepartmentFolderDto createFolder(CreateFolderDto dto) {
        return super.createFolder(dto, getContext());
    }

    @Transactional
    public void deleteFolder(UUID folderId) {
        super.deleteFolder(folderId, getContext());
    }

    @Transactional
    public DepartmentFolderDto updateFolder(UUID folderId, UpdateFolderDto dto) {
        return super.updateFolder(folderId, dto, getContext());
    }

    @Transactional
    public DepartmentFolderDto restoreFolder(UUID folderId, UUID targetParentFolderId) {
        return super.restoreFolder(folderId, targetParentFolderId, getContext());
    }

    @Transactional
    public List<DepartmentFolderDto> listFolders(UUID parentFolderId) {
        return super.listFolders(parentFolderId, getContext());
    }

    @Transactional
    public List<FolderTreeDto> getFolderTree() {
        return super.getFolderTree(getContext());
    }
}
