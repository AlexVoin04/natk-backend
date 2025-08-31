package com.natk.natk_api.userStorage.service;

import com.natk.natk_api.baseStorage.context.StorageContext;
import com.natk.natk_api.baseStorage.context.UserContext;
import com.natk.natk_api.baseStorage.dto.MoveFolderDto;
import com.natk.natk_api.baseStorage.dto.RenameFolderDto;
import com.natk.natk_api.baseStorage.service.BaseFolderService;
import com.natk.natk_api.userStorage.dto.CreateFolderDto;
import com.natk.natk_api.userStorage.dto.FolderDto;
import com.natk.natk_api.userStorage.dto.FolderTreeDto;
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
import java.util.UUID;

@Service
public class UserBaseFolderService extends BaseFolderService<UserFolderEntity, UserFolderRepository, FolderDto> {
    private final CurrentUserService currentUserService;
    private final UserFolderMapper userFolderMapper;
    private final UserFolderNameResolverService folderNameResolverService;

    protected UserBaseFolderService(
            UserFolderRepository folderRepo,
            CurrentUserService currentUserService,
            UserFolderMapper userFolderMapper,
            UserFolderNameResolverService folderNameResolverService) {
        super(folderRepo);
        this.currentUserService = currentUserService;
        this.folderNameResolverService = folderNameResolverService;
        this.userFolderMapper = userFolderMapper;
    }

    protected UserContext getContext(){
        return new UserContext(currentUserService.getCurrentUser());
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
    public FolderDto renameFolder(UUID folderId, RenameFolderDto dto) {
        return super.renameFolder(folderId, dto, getContext());
    }

    @Transactional
    public FolderDto moveFolder(UUID folderId, MoveFolderDto dto) {
        return super.moveFolder(folderId, dto, getContext());
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
    protected FolderDto applyRename(UserFolderEntity folder, RenameFolderDto dto, StorageContext context) {
        UserEntity user = ((UserContext) context).user();
        folderNameResolverService.ensureUniqueNameOrThrow(dto.newName(), folder.getParentFolder(), user, folder.getId());
        folder.setName(dto.newName());
        folderRepo.save(folder);
        return userFolderMapper.toDto(folder);
    }

    @Override
    protected FolderDto applyMove(UserFolderEntity folder, MoveFolderDto dto, StorageContext context) {
        UserEntity user = ((UserContext) context).user();

        if (dto.moveToRoot()) {
            folderNameResolverService.ensureUniqueNameOrThrow(folder.getName(), null, user, folder.getId());
            folder.setParentFolder(null);
        } else if (dto.newParentFolderId() != null) {
            UserFolderEntity newParent = folderRepo
                    .findByIdAndUserAndIsDeletedFalse(dto.newParentFolderId(), user)
                    .orElseThrow(() -> new AccessDeniedException("New parent folder not found"));

            validateNotMovingIntoSelfOrDescendant(folder.getId(), newParent.getId());

            folderNameResolverService.ensureUniqueNameOrThrow(folder.getName(), newParent, user, folder.getId());
            folder.setParentFolder(newParent);
        } else {
            throw new IllegalArgumentException("No new parent folder specified");
        }

        return userFolderMapper.toDto(folderRepo.save(folder));
    }

    @Override
    protected void applyDelete(UserFolderEntity folder) {
        folder.setDeleted(true);
        folder.setDeletedAt(Instant.now());
        folderRepo.save(folder);
    }

    @Override
    protected FolderDto applyRestore(UserFolderEntity folder, UserFolderEntity parent, StorageContext context) {
        UserEntity user = ((UserContext) context).user();
        if (parent != null) {
            validateNotMovingIntoSelfOrDescendant(folder.getId(), parent.getId());
        }

        String uniqueName = folderNameResolverService.ensureUniqueName(folder.getName(), parent, user, folder.getId());

        folder.setName(uniqueName);
        folder.setParentFolder(parent);
        folder.setDeleted(false);
        folder.setDeletedAt(null);
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
        List<UserFolderEntity> all = folderRepo.findByUserAndIsDeletedFalse(user);

        return buildFolderTree(
                all,
                UserFolderEntity::getId,
                UserFolderEntity::getParentFolder,
                UserFolderEntity::getName
        );
    }
}
