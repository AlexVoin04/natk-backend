package com.natk.natk_api.baseStorage.service;

import com.natk.natk_api.baseStorage.context.StorageContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public abstract class BaseFileService<TFile, TFolder, TFileRepo extends JpaRepository<TFile, UUID>> {

    protected final TFileRepo fileRepo;

    protected BaseFileService(TFileRepo fileRepo) {
        this.fileRepo = fileRepo;
    }

    @Transactional
    public TFile uploadFile(String name, byte[] data, UUID folderId, StorageContext ctx) {
        TFolder folder = folderId != null ? findFolder(folderId, ctx) : null;
        checkUploadAccess(folder, ctx);

        TFile file = buildNewFile(name, data, folder, ctx);
        return fileRepo.save(file);
    }

    @Transactional
    public void deleteFile(UUID id, StorageContext ctx) {
        TFile file = findFileOrThrow(id, ctx);
        checkDeleteAccess(file, ctx);

        applyDelete(file);
        fileRepo.save(file);
    }

    @Transactional
    public TFile restoreFile(UUID id, UUID targetFolderId, StorageContext ctx) {
        TFile file = findFileOrThrow(id, ctx);
        checkRestoreAccess(file, ctx);

        TFolder folder = targetFolderId != null ? findFolder(targetFolderId, ctx) : null;
        applyRestore(file, folder);
        return fileRepo.save(file);
    }

    @Transactional
    public TFile renameFile(UUID id, String newName, StorageContext ctx) {
        TFile file = findFileOrThrow(id, ctx);
        checkUpdateAccess(file, ctx);

        applyRename(file, newName);
        return fileRepo.save(file);
    }

    // ==== abstract hooks ====
    protected abstract TFile findFileOrThrow(UUID id, StorageContext ctx);
    protected abstract TFolder findFolder(UUID id, StorageContext ctx);
    protected abstract TFile buildNewFile(String name, byte[] data, TFolder folder, StorageContext ctx);

    protected abstract void checkUploadAccess(TFolder folder, StorageContext ctx);
    protected abstract void checkDeleteAccess(TFile file, StorageContext ctx);
    protected abstract void checkRestoreAccess(TFile file, StorageContext ctx);
    protected abstract void checkUpdateAccess(TFile file, StorageContext ctx);

    protected abstract void applyDelete(TFile file);
    protected abstract void applyRestore(TFile file, TFolder folder);
    protected abstract void applyRename(TFile file, String newName);
}