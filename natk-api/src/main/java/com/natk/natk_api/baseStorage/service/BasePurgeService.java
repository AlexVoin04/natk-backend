package com.natk.natk_api.baseStorage.service;

import com.natk.natk_api.baseStorage.PurgeStats;
import com.natk.natk_api.baseStorage.context.StorageContext;
import com.natk.natk_api.departmentStorage.dto.PurgeItemDto;
import com.natk.natk_api.minio.MinioFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public abstract class BasePurgeService<TFolder, TFile> {

    protected final MinioFileService minio;
    protected static final int BATCH_SIZE = 100;

    protected abstract Optional<TFile> loadDeletedFileById(UUID id, StorageContext ctx);
    protected abstract Optional<TFolder> loadDeletedFolderById(UUID id, StorageContext ctx);

    /**
     * Возвращает непосредственных удалённых дочерних папок (могут быть пустыми).
     */
    protected abstract List<TFolder> findDeletedChildFolders(TFolder parent, StorageContext ctx);

    /**
     * Возвращает файлы, помеченные удалёнными, непосредственно в папке (folder == null — корень).
     */
    protected abstract List<TFile> findDeletedFilesInFolder(TFolder folder, StorageContext ctx);

    /**
     * Извлечь ключ в object storage из сущности файла (или null).
     */
    protected abstract String extractStorageKey(TFile file, StorageContext ctx);

    /**
     * Удалить файлы (записи) из БД — можно deleteAll
     */
    protected abstract void deleteFileEntities(List<TFile> files, StorageContext ctx);

    /**
     * Удалить папки (записи) из БД — в порядке, который передается (дочерние сначала).
     */
    protected abstract void deleteFolderEntities(List<TFolder> folders, StorageContext ctx);

    /**
     * Название бакета (user-files / department-files)
     */
    protected abstract String bucketName();

    protected abstract List<TFolder> findDeletedRootFolders(StorageContext ctx);

    protected abstract void folderAccessForFile(TFile file, StorageContext ctx);
    protected abstract void folderAccessForFolder(TFolder folder, StorageContext ctx);

    @Transactional
    public PurgeStats purgeFile(UUID id, StorageContext ctx) {
        TFile file = loadDeletedFileById(id, ctx)
                .orElseThrow(() -> new IllegalArgumentException("File not found in bin"));

        folderAccessForFile(file, ctx);

        String key = extractStorageKey(file, ctx);
        deleteFileEntities(List.of(file), ctx);

        registerMinioDeleteAfterCommit(List.of(key));
        return new PurgeStats(1, 0);
    }

    @Transactional
    public PurgeStats purgeFolder(UUID id, StorageContext ctx) {
        TFolder root = loadDeletedFolderById(id, ctx)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found in bin"));

        folderAccessForFolder(root, ctx);

        // Собираем все файлы в subtree для удаления из MinIO
        List<TFile> filesAcc = new ArrayList<>();
        List<TFolder> foldersAcc = new ArrayList<>();

        collectDeletedSubtree(root, filesAcc, foldersAcc, ctx);

        // Получаем ключи для MinIO
        List<String> keys = filesAcc.stream()
                .map(f -> extractStorageKey(f, ctx))
                .filter(Objects::nonNull)
                .toList();

        deleteFileEntities(filesAcc, ctx);
        deleteFolderEntities(foldersAcc, ctx);

        // После коммита удаляем файлы из MinIO
        registerMinioDeleteAfterCommit(keys);
        return new PurgeStats(filesAcc.size(), foldersAcc.size());
    }

    // DFS только для файлов
    private void collectFilesRecursive(TFolder folder, List<TFile> filesAcc, StorageContext ctx) {
        filesAcc.addAll(findDeletedFilesInFolder(folder, ctx));
        for (TFolder child : findDeletedChildFolders(folder, ctx)) {
            collectFilesRecursive(child, filesAcc, ctx);
        }
    }

    @Transactional
    public PurgeStats purgeMultiple(List<PurgeItemDto> items, StorageContext ctx) {
        int files = 0;
        int folders = 0;

        for (PurgeItemDto i : items) {
            PurgeStats s = "file".equalsIgnoreCase(i.type())
                    ? purgeFile(i.id(), ctx)
                    : purgeFolder(i.id(), ctx);

            files += s.files();
            folders += s.folders();
        }

        return new PurgeStats(files, folders);
    }

    private void registerMinioDeleteAfterCommit(List<String> keys) {
        if (keys.isEmpty()) return;

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        for (int i = 0; i < keys.size(); i += BATCH_SIZE) {
                            List<String> batch = keys.subList(i, Math.min(i + BATCH_SIZE, keys.size()));
                            try {
                                minio.deleteFiles(bucketName(), batch);
                            } catch (Exception ex) {
                                // можно добавить retry / DLQ / лог
                            }
                        }
                    }
                }
        );
    }

    //TODO: есть каскадное удаление
    /**
     * DFS: сначала рекурсивно обходим дочерние папки, затем добавляем их в foldersAcc (post-order).
     * Для каждой папки добавляем её файлы в filesAcc.
     */
    private void collectDeletedSubtree(TFolder folder, List<TFile> filesAcc, List<TFolder> foldersAcc, StorageContext ctx) {
        List<TFolder> children =
                (folder == null)
                        ? findDeletedRootFolders(ctx)
                        : findDeletedChildFolders(folder, ctx);

        for (TFolder child : children) {
            collectDeletedSubtree(child, filesAcc, foldersAcc, ctx);
        }

        filesAcc.addAll(findDeletedFilesInFolder(folder, ctx));

        if (folder != null) {
            foldersAcc.add(folder); // post-order
        }
    }
}
