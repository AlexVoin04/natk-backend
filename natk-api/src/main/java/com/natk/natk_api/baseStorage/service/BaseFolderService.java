package com.natk.natk_api.baseStorage.service;

import com.natk.natk_api.baseStorage.context.StorageContext;
import com.natk.natk_api.baseStorage.dto.MoveFolderDto;
import com.natk.natk_api.baseStorage.dto.RenameFolderDto;
import com.natk.natk_api.baseStorage.intarfece.BaseFolderDto;
import com.natk.natk_api.baseStorage.intarfece.FolderAncestryRepository;
import com.natk.natk_api.userStorage.dto.CreateFolderDto;
import com.natk.natk_api.userStorage.dto.FolderTreeDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BaseFolderService<
        TFolder,
        TFolderRepo extends JpaRepository<TFolder, UUID> & FolderAncestryRepository,
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
    public TDto renameFolder(UUID id, RenameFolderDto dto, StorageContext ctx) {
        TFolder folder = findFolder(id, ctx);
        checkUpdateAccess(folder, ctx);
        return applyRename(folder, dto, ctx);
    }

    @Transactional
    public TDto moveFolder(UUID id, MoveFolderDto dto, StorageContext ctx) {
        TFolder folder = findFolder(id, ctx);
        checkUpdateAccess(folder, ctx);
        return applyMove(folder, dto, ctx);
    }

    @Transactional
    public void deleteFolder(UUID id, StorageContext ctx) {
        TFolder folder = findFolder(id, ctx);
        checkDeleteAccess(folder, ctx);
        applyDelete(folder);
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
        return applyList(folder, ctx);
    }

    @Transactional
    public List<FolderTreeDto> getFolderTree(StorageContext ctx) {
        return applyTree(ctx);
    }

    protected List<FolderTreeDto> buildFolderTree(
            List<TFolder> all,
            Function<TFolder, UUID> idExtractor,
            Function<TFolder, TFolder> parentExtractor,
            Function<TFolder, String> nameExtractor
    ) {
        Map<UUID, List<TFolder>> childrenMap = all.stream()
                .filter(f -> parentExtractor.apply(f) != null)
                .collect(Collectors.groupingBy(f -> idExtractor.apply(parentExtractor.apply(f))));

        List<TFolder> roots = all.stream()
                .filter(f -> parentExtractor.apply(f) == null)
                .toList();

        return roots.stream()
                .map(folder -> buildTree(folder, childrenMap, idExtractor, nameExtractor, 0))
                .toList();
    }

    private FolderTreeDto buildTree(
            TFolder folder,
            Map<UUID, List<TFolder>> childrenMap,
            Function<TFolder, UUID> idExtractor,
            Function<TFolder, String> nameExtractor,
            int depth
    ) {
        List<FolderTreeDto> children = childrenMap.getOrDefault(idExtractor.apply(folder), List.of()).stream()
                .map(child -> buildTree(child, childrenMap, idExtractor, nameExtractor, depth + 1))
                .toList();

        return new FolderTreeDto(
                idExtractor.apply(folder),
                nameExtractor.apply(folder),
                depth,
                children
        );
    }

    protected void validateNotMovingIntoSelfOrDescendant(UUID folderId, UUID candidateParentId) {
        if (candidateParentId == null) return; // перемещение в корень — ок

        if (folderId.equals(candidateParentId)) {
            throw new IllegalArgumentException("Cannot move folder into itself");
        }
        if (folderRepo.isAncestor(candidateParentId, folderId)) {
            throw new IllegalArgumentException("Cannot move folder inside its descendant");
        }
    }

    // ==== abstract hooks ====
    protected abstract TFolder findFolder(UUID id, StorageContext ctx);
    protected abstract TFolder findDeletedFolder(UUID id, StorageContext ctx);
    protected abstract TFolder buildNewFolder(String name, TFolder parent, StorageContext ctx);

    protected abstract void checkCreateAccess(TFolder parent, StorageContext ctx);
    protected abstract void checkUpdateAccess(TFolder folder, StorageContext ctx);
    protected abstract void checkDeleteAccess(TFolder folder, StorageContext ctx);
    protected abstract void checkRestoreAccess(TFolder folder, StorageContext ctx);

    protected abstract TDto doCreateFolder(String name, TFolder parent, StorageContext context);

    protected abstract TDto applyRename(TFolder folder, RenameFolderDto dto, StorageContext ctx);
    protected abstract TDto applyMove(TFolder folder, MoveFolderDto dto, StorageContext ctx);
    protected abstract void applyDelete(TFolder folder);
    protected abstract TDto applyRestore(TFolder folder, TFolder parent);
    protected abstract List<TDto> applyList(TFolder parent, StorageContext context);
    protected abstract List<FolderTreeDto> applyTree(StorageContext context);
}
