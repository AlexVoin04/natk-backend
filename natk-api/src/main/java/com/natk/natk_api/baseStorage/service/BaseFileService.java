package com.natk.natk_api.baseStorage.service;

import com.natk.natk_api.baseStorage.MagicValidationResult;
import com.natk.natk_api.baseStorage.context.StorageContext;
import com.natk.natk_api.baseStorage.dto.FileDownloadDto;
import com.natk.natk_api.baseStorage.dto.SignedUrlResponse;
import com.natk.natk_api.baseStorage.dto.UploadFileDto;
import com.natk.natk_api.baseStorage.intarfece.UploadStrategy;
import com.natk.natk_api.rabbit.ScanTaskPublisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.List;
import java.util.UUID;

public abstract class BaseFileService<TFile, TFolder,
        TFileRepo extends JpaRepository<TFile, UUID>,
        TFolderRepo extends JpaRepository<TFolder, UUID>
        ,TDto, TOwner> {

    protected final TFileRepo fileRepo;
    protected final TFolderRepo folderRepo;
    protected final ScanTaskPublisher scanTaskPublisher;

    protected BaseFileService(TFileRepo fileRepo, TFolderRepo folderRepo, ScanTaskPublisher scanTaskPublisher) {
        this.fileRepo = fileRepo;
        this.folderRepo = folderRepo;
        this.scanTaskPublisher = scanTaskPublisher;
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
    public TDto renameFile(UUID fileId, String newName, StorageContext ctx) {
        TFile file = findFile(fileId, ctx);
        checkUpdateAccess(file, ctx);
        return applyRename(file, newName, ctx);
    }

    @Transactional
    public TDto moveFile(UUID fileId, UUID targetFolderId, Boolean moveToRoot, StorageContext ctx) {
        TFile file = findFile(fileId, ctx);
        checkRestoreAccess(file, ctx);

        TFolder folder = null;
        if (Boolean.FALSE.equals(moveToRoot) && targetFolderId != null) {
            folder = findFolder(targetFolderId, ctx);
        }

        return applyMove(file, folder, ctx);
    }

    @Transactional
    public TDto copyFile(UUID fileId, UUID targetFolderId, StorageContext ctx) {
        TFile file = findFile(fileId, ctx);
        checkReadAccess(file, ctx);

        TFolder folder = targetFolderId != null ? findFolder(targetFolderId, ctx) : null;
        return applyCopy(file, folder, ctx);
    }

    @Transactional(readOnly = true)
    public SignedUrlResponse getFileSignedUrl(UUID fileId, int expirySeconds, StorageContext ctx){
        TFile file = findFile(fileId, ctx);
        checkReadAccess(file, ctx);

        String url = generatePresignedUrl(file, expirySeconds, ctx);

        return new SignedUrlResponse(
                url,
                extractFileName(file),
                extractMimeType(file)
        );
    }

    protected TDto applyUploadFile(UploadFileDto dto, TFolder folder, StorageContext ctx) {
        UploadStrategy<TFile, TFolder, TFileRepo,  TOwner> strategy = getUploadStrategy();
        TOwner owner = strategy.getOwner(ctx);

        // 1. Проверка имени
        strategy.ensureUniqueNameOrThrow(dto.name(), folder, owner);

        // 2. MIME
        MagicValidationResult res = strategy.validateMime(dto.fileData(), dto.name());

        // 3. Объединяем header + файл
        InputStream fullStream = new SequenceInputStream(
                new ByteArrayInputStream(res.header()),
                dto.fileData()
        );

        // 4. Генерируем ключ
        String key = strategy.generateIncomingKey(owner);

        // 5. Создаём сущность
        TFile file = strategy.buildNewFileEntity(
                dto.name(),
                res.mimeType(),
                dto.size(),
                folder,
                key,
                ctx,
                owner
        );

        // 6. Загрузка в minio
        strategy.uploadToMinio(fullStream, dto.size(), key, res.mimeType());

        // 7. Сохраняем файл
        strategy.persistFile(file);
        TFile saved = strategy.reloadAfterSave(file);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    scanTaskPublisher.publish(strategy.buildScanTask(saved, key, ctx, owner));
                }
            });
        } else {
            scanTaskPublisher.publish(strategy.buildScanTask(saved, key, ctx, owner));
        }
        return mapToDto(saved);
    }

    protected abstract String extractFileName(TFile file);
    protected abstract String extractMimeType(TFile file);
    protected abstract String generatePresignedUrl(TFile file, int expirySeconds, StorageContext ctx);

    protected abstract TFile findFile(UUID id, StorageContext ctx);
    protected abstract TFile findDeletedFile(UUID id, StorageContext ctx);
    protected abstract TFolder findFolder(UUID id, StorageContext ctx);
    protected abstract UploadStrategy<TFile, TFolder, TFileRepo, TOwner> getUploadStrategy();

    protected abstract void checkReadAccess(TFile file, StorageContext ctx);
    protected abstract void checkUploadAccess(TFolder folder, StorageContext ctx);
    protected abstract void checkDeleteAccess(TFile file, StorageContext ctx);
    protected abstract void checkRestoreAccess(TFile file, StorageContext ctx);
    protected abstract void checkUpdateAccess(TFile file, StorageContext ctx);

    protected abstract TDto applyMove(TFile file, TFolder newFolder, StorageContext ctx);
    protected abstract TDto applyRename(TFile file, String newName, StorageContext ctx);
//    protected abstract TDto applyUploadFile(UploadFileDto dto, TFolder folder, StorageContext ctx);
    protected abstract FileDownloadDto applyDownload(TFile file, StorageContext ctx);
    protected abstract void applyDelete(TFile file);
    protected abstract TDto applyRestore(TFile file, TFolder folder, StorageContext ctx);
    protected abstract List<TDto> applyList(TFolder folder, StorageContext ctx);
    protected abstract TDto applyCopy(TFile file, TFolder folder, StorageContext ctx);

    protected abstract TDto mapToDto(TFile file);
}