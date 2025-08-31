package com.natk.natk_api.baseStorage.service;

import com.natk.natk_api.baseStorage.context.StorageContext;
import com.natk.natk_api.userStorage.dto.FileDownloadDto;
import com.natk.natk_api.userStorage.dto.UpdateFileDto;
import com.natk.natk_api.userStorage.dto.UploadFileDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public abstract class BaseFileService<TFile, TFolder,
        TFileRepo extends JpaRepository<TFile, UUID>,
        TFolderRepo extends JpaRepository<TFolder, UUID>
        ,TDto> {

    protected final TFileRepo fileRepo;
    protected final TFolderRepo folderRepo;

    protected BaseFileService(TFileRepo fileRepo, TFolderRepo folderRepo) {
        this.fileRepo = fileRepo;
        this.folderRepo = folderRepo;
    }

    @Transactional
    public TDto uploadFile(UploadFileDto dto, StorageContext ctx) {
        TFolder folder = dto.folderId() != null ? findFolder(dto.folderId(), ctx) : null;
        checkUploadAccess(folder, ctx);
        return applyUploadFile(dto, folder, ctx);
    }

    @Transactional(readOnly = true)
    public TFile getFileEntity(UUID fileId, StorageContext ctx) {
        TFile file = findFile(fileId, ctx);
        checkReadAccess(file, ctx);
        return file;
    }

    @Transactional(readOnly = true)
    public FileDownloadDto getFileDownloadData(UUID fileId, StorageContext ctx) {
        TFile file = findFile(fileId, ctx);
        checkReadAccess(file, ctx);
        return applyDownload(file, ctx);
    }

    @Transactional(readOnly = true)
    public TDto getFile(UUID fileId, StorageContext ctx) {
        TFile file = findFile(fileId, ctx);
        checkReadAccess(file, ctx);
        return mapToDto(file);
    }

    //TODO: проверка, что файл уже удалён?
    @Transactional
    public void deleteFile(UUID fileId, StorageContext ctx) {
        TFile file = findFile(fileId, ctx);
        checkDeleteAccess(file, ctx);
        applyDelete(file);
    }

    @Transactional
    public TDto restoreFile(UUID fileId, UUID targetFolderId, StorageContext ctx) {
        TFile file = findDeletedFile(fileId, ctx);
        checkRestoreAccess(file, ctx);

        TFolder folder = targetFolderId != null ? findFolder(targetFolderId, ctx) : null;
        return applyRestore(file, folder, ctx);
    }

    @Transactional(readOnly = true)
    public List<TDto> listFiles(UUID folderId, StorageContext ctx) {
        TFolder folder = folderId != null ? findFolder(folderId, ctx) : null;
        return applyList(folder, ctx);
    }

    @Transactional
    public TDto updateFile(UUID fileId, UpdateFileDto dto, StorageContext ctx) {
        TFile file = findFile(fileId, ctx);
        checkUpdateAccess(file, ctx);
        return applyUpdate(file, dto, ctx);
    }

    @Transactional
    public TDto copyFile(UUID fileId, UUID targetFolderId, StorageContext ctx) {
        TFile file = findFile(fileId, ctx);
        checkReadAccess(file, ctx);

        TFolder folder = targetFolderId != null ? findFolder(targetFolderId, ctx) : null;
        return applyCopy(file, folder, ctx);
    }

    // ==== abstract hooks ====
    protected abstract TFile findFile(UUID id, StorageContext ctx);
    protected abstract TFile findDeletedFile(UUID id, StorageContext ctx);
    protected abstract TFolder findFolder(UUID id, StorageContext ctx);

    protected abstract void checkReadAccess(TFile file, StorageContext ctx);
    protected abstract void checkUploadAccess(TFolder folder, StorageContext ctx);
    protected abstract void checkDeleteAccess(TFile file, StorageContext ctx);
    protected abstract void checkRestoreAccess(TFile file, StorageContext ctx);
    protected abstract void checkUpdateAccess(TFile file, StorageContext ctx);

    protected abstract TDto applyUploadFile(UploadFileDto dto, TFolder folder, StorageContext ctx);
    protected abstract FileDownloadDto applyDownload(TFile file, StorageContext ctx);
    protected abstract void applyDelete(TFile file);
    protected abstract TDto applyRestore(TFile file, TFolder folder, StorageContext ctx);
    protected abstract List<TDto> applyList(TFolder folder, StorageContext ctx);
    protected abstract TDto applyUpdate(TFile file, UpdateFileDto dto, StorageContext ctx);
    protected abstract TDto applyCopy(TFile file, TFolder folder, StorageContext ctx);

    protected abstract TDto mapToDto(TFile file);
}