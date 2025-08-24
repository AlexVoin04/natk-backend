package com.natk.natk_api.userStorage.service;

import com.natk.natk_api.baseStorage.context.StorageContext;
import com.natk.natk_api.baseStorage.context.UserContext;
import com.natk.natk_api.baseStorage.service.BaseFolderService;
import com.natk.natk_api.userStorage.dto.CreateFolderDto;
import com.natk.natk_api.userStorage.dto.FolderDto;
import com.natk.natk_api.userStorage.dto.FolderTreeDto;
import com.natk.natk_api.userStorage.dto.UpdateFolderDto;
import com.natk.natk_api.userStorage.mapper.UserFolderMapper;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import com.natk.natk_api.userStorage.repository.UserFolderRepository;
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
public class UserBaseFolderService extends BaseFolderService<UserFolderEntity, UserFolderRepository, FolderDto> {
    private final CurrentUserService currentUserService;
    private final UserFolderMapper userFolderMapper;
    private final FolderNameResolverService folderNameResolverService;

    protected UserBaseFolderService(
            UserFolderRepository folderRepo,
            CurrentUserService currentUserService,
            UserFolderMapper userFolderMapper,
            FolderNameResolverService folderNameResolverService) {
        super(folderRepo);
        this.currentUserService = currentUserService;
        this.folderNameResolverService = folderNameResolverService;
        this.userFolderMapper = userFolderMapper;
    }

    @Override
    protected UserContext getContext(){
        return new UserContext(currentUserService.getCurrentUser());
    }


    @Override
    protected UserFolderEntity findFolder(UUID id, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        return folderRepo.findByIdAndUserAndIsDeletedFalse(id, user)
                .orElseThrow(() -> new AccessDeniedException("Folder not found or not owned by user or deleted"));
    }

    @Override
    protected UserFolderEntity findDeletedFolder(UUID id, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();
        return folderRepo.findByIdAndUserAndIsDeletedTrue(id, user)
                .orElseThrow(() -> new AccessDeniedException("Folder not found or not owned by user or not deleted"));
    }

    @Override
    protected UserFolderEntity buildNewFolder(String name, UserFolderEntity parent, StorageContext ctx) {
        UserEntity user = ((UserContext) ctx).user();

        UserFolderEntity folder = new UserFolderEntity();
        folder.setName(name);
        folder.setUser(user);
        folder.setParentFolder(parent);
        folder.setDeleted(false);
        folder.setCreatedAt(Instant.now());
        folder.setUpdatedAt(null);

        return folder;
    }

    @Override
    protected void checkCreateAccess(UserFolderEntity parent, StorageContext ctx) {

    }

    @Override
    protected void checkUpdateAccess(UserFolderEntity userFolderEntity, StorageContext ctx) {

    }

    @Override
    protected void checkDeleteAccess(UserFolderEntity userFolderEntity, StorageContext ctx) {

    }

    @Override
    protected void checkRestoreAccess(UserFolderEntity userFolderEntity, StorageContext ctx) {

    }

    @Override
    protected FolderDto doCreateFolder(String name, UserFolderEntity parent, StorageContext context) {
        UserEntity user = ((UserContext) context).user();

        folderNameResolverService.ensureUniqueNameOrThrow(name, parent, user);

        UserFolderEntity folder = buildNewFolder(name, parent, context);
        return userFolderMapper.toDto(folderRepo.save(folder));
    }

    @Override
    protected FolderDto applyUpdate(UserFolderEntity folder, UpdateFolderDto dto, StorageContext context) {
        UserEntity user = ((UserContext) context).user();
        boolean modified = false;

        if (dto.newName() != null && !dto.newName().isBlank()) {
            folder.setName(dto.newName());
            modified = true;
        }

        if (Boolean.TRUE.equals(dto.moveToRoot())) {
            folderNameResolverService.ensureUniqueNameOrThrow(dto.newName(), null, user);
            folder.setParentFolder(null);
            modified = true;
        }

        else if (dto.newParentFolderId().isPresent()) {
            UUID newParentId = dto.newParentFolderId().get();

            UserFolderEntity newParent = folderRepo.findByIdAndUserAndIsDeletedFalse(newParentId, user)
                    .orElseThrow(() -> new AccessDeniedException("New parent folder not found or not owned by user or deleted"));

            if (isDescendant(folder, newParent)) {
                throw new IllegalArgumentException("Cannot move folder inside its descendant");
            }

            folderNameResolverService.ensureUniqueNameOrThrow(dto.newName(), newParent, user);
            folder.setParentFolder(newParent);
            modified = true;
        }

        if (!modified) {
            throw new IllegalArgumentException("No changes provided for folder update");
        }

        folderRepo.save(folder);

        return userFolderMapper.toDto(folder);
    }

    private boolean isDescendant(UserFolderEntity folder, UserFolderEntity target) {
        while (target != null) {
            if (target.getId().equals(folder.getId())) return true;
            target = target.getParentFolder();
        }
        return false;
    }

    @Override
    protected void applyDelete(UserFolderEntity folder) {
        folder.setDeleted(true);
        folder.setDeletedAt(Instant.now());
    }

    @Override
    protected FolderDto applyRestore(UserFolderEntity folder, UserFolderEntity targetParent) {
        if (isDescendant(folder, targetParent)) {
            throw new IllegalArgumentException("Cannot restore folder into its own descendant");
        }

        folder.setParentFolder(targetParent);
        folder.setDeleted(false);
        folder.setDeletedAt(null);
        folderRepo.save(folder);
        return userFolderMapper.toDto(folderRepo.save(folder));
    }

    @Override
    protected List<FolderDto> applyList(UserFolderEntity parent, StorageContext context) {
        UserEntity user = ((UserContext) context).user();
        return folderRepo.findByUserAndParentFolderAndIsDeletedFalse(user, parent).stream()
                .map(userFolderMapper::toDto)
                .toList();
    }

    @Override
    protected List<FolderTreeDto> applyTree(StorageContext context) {
        UserEntity user = ((UserContext) context).user();
        List<UserFolderEntity> allFolders = folderRepo.findByUserAndIsDeletedFalse(user);

        Map<UUID, List<UserFolderEntity>> childrenMap = allFolders.stream()
                .filter(f -> f.getParentFolder() != null)
                .collect(Collectors.groupingBy(f -> f.getParentFolder().getId()));

        List<UserFolderEntity> rootFolders = allFolders.stream()
                .filter(f -> f.getParentFolder() == null)
                .toList();

        return rootFolders.stream()
                .map(folder -> buildTree(folder, childrenMap, 0))
                .toList();
    }

    private FolderTreeDto buildTree(UserFolderEntity folder, Map<UUID, List<UserFolderEntity>> childrenMap, int depth) {
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
    public FolderDto createFolder(CreateFolderDto dto) {
        return super.createFolder(dto, getContext());
    }

    @Transactional
    public void deleteFolder(UUID folderId) {
        super.deleteFolder(folderId, getContext());
    }

    @Transactional
    public FolderDto updateFolder(UUID folderId, UpdateFolderDto dto) {
        return super.updateFolder(folderId, dto, getContext());
    }

    @Transactional
    public FolderDto restoreFolder(UUID folderId, UUID targetParentFolderId) {
        return super.restoreFolder(folderId, targetParentFolderId, getContext());
    }

    @Transactional
    public List<FolderDto> listFolders(UUID parentFolderId) {
        return super.listFolders(parentFolderId, getContext());
    }

    @Transactional
    public List<FolderTreeDto> getFolderTree() {
        return super.getFolderTree(getContext());
    }
}
