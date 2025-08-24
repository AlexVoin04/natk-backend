package com.natk.natk_api.baseStorage.service;

import com.natk.natk_api.baseStorage.context.StorageContext;
import com.natk.natk_api.baseStorage.intarfece.BaseFolderDto;
import com.natk.natk_api.userStorage.dto.CreateFolderDto;
import com.natk.natk_api.userStorage.dto.FolderTreeDto;
import com.natk.natk_api.userStorage.dto.UpdateFolderDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public abstract class BaseFolderService<
        TFolder,
        TFolderRepo extends JpaRepository<TFolder, UUID>,
        TDto extends BaseFolderDto> {

    protected final TFolderRepo folderRepo;

    protected BaseFolderService(TFolderRepo folderRepo) {
        this.folderRepo = folderRepo;
    }

    @Transactional
    public TDto createFolder(CreateFolderDto dto, StorageContext ctx) {
        TFolder parent = dto.parentFolderId() != null ? findFolder(dto.parentFolderId(), ctx) : null;
        checkCreateAccess(parent, ctx);

        return doCreateFolder(dto.name(), parent, ctx);
    }

    @Transactional
    public TDto updateFolder(UUID id, UpdateFolderDto dto, StorageContext ctx) {
        TFolder folder = findFolder(id, ctx);
        checkUpdateAccess(folder, ctx);

        return applyUpdate(folder, dto, ctx);
    }

    @Transactional
    public void deleteFolder(UUID id, StorageContext ctx) {
        TFolder folder = findFolder(id, ctx);
        checkDeleteAccess(folder, ctx);

        applyDelete(folder);
        folderRepo.save(folder);
    }

    @Transactional
    public TDto restoreFolder(UUID folderId, UUID targetParentId, StorageContext ctx) {
        TFolder folder = findDeletedFolder(folderId, ctx);
        checkRestoreAccess(folder, ctx);

        TFolder parent = targetParentId != null ? findFolder(targetParentId, ctx) : null;

        return applyRestore(folder, parent);
    }

    @Transactional
    public List<TDto> listFolders(UUID parentFolderId, StorageContext ctx) {
        TFolder folder = parentFolderId != null ? findFolder(parentFolderId, ctx) : null;
        checkUpdateAccess(folder, ctx);

        return applyList(folder, ctx);
    }

    @Transactional
    public List<FolderTreeDto> getFolderTree(StorageContext ctx) {
        return applyTree(ctx);
    }

    // ==== abstract hooks ====
    protected abstract StorageContext getContext();
    protected abstract TFolder findFolder(UUID id, StorageContext ctx);
    protected abstract TFolder findDeletedFolder(UUID id, StorageContext ctx);
    protected abstract TFolder buildNewFolder(String name, TFolder parent, StorageContext ctx);

    protected abstract void checkCreateAccess(TFolder parent, StorageContext ctx);
    protected abstract void checkUpdateAccess(TFolder folder, StorageContext ctx);
    protected abstract void checkDeleteAccess(TFolder folder, StorageContext ctx);
    protected abstract void checkRestoreAccess(TFolder folder, StorageContext ctx);

    protected abstract TDto doCreateFolder(String name, TFolder parent, StorageContext context);

    protected abstract TDto applyUpdate(TFolder folder, UpdateFolderDto dto, StorageContext context);
    protected abstract void applyDelete(TFolder folder);
    protected abstract TDto applyRestore(TFolder folder, TFolder parent);
    protected abstract List<TDto> applyList(TFolder parent, StorageContext context);
    protected abstract List<FolderTreeDto> applyTree(StorageContext context);
}
