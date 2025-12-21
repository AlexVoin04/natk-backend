package com.natk.natk_api.baseStorage.service;

import com.natk.natk_api.baseStorage.context.StorageContext;
import com.natk.natk_api.userStorage.dto.FolderContentResponseDto;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public abstract class BaseStorageService<
        TFolder,
        TFile,
        TItemDto,
        TDeletedDto
        > {

    @Transactional(readOnly = true)
    public FolderContentResponseDto<TItemDto> getStorageItems(UUID folderId, StorageContext ctx) {
        TFolder folder = resolveFolderOrRoot(folderId, ctx);

        List<TItemDto> folderItems = fetchActiveFolders(folder, ctx).stream()
                .map(f -> mapFolderToItem(f, ctx))
                .toList();

        List<TItemDto> fileItems = fetchActiveFiles(folder, ctx).stream()
                .map(f -> mapFileToItem(f, ctx))
                .toList();

        List<TItemDto> combined = sortByFolderThenName(folderItems, fileItems);

        String path = resolvePathForResponse(folder, ctx);
        var breadcrumb = generateBreadcrumbPath(folder, ctx);

        return new FolderContentResponseDto<>(
                folder == null ? null : extractFolderId(folder),
                path,
                breadcrumb.pathIds(),
                breadcrumb.names(),
                combined
        );
    }

    @Transactional(readOnly = true)
    public List<TDeletedDto> getDeletedItems(StorageContext ctx) {
        List<TDeletedDto> folders = findDeletedFolders(ctx).stream()
                .map(f -> mapFolderToDeleted(f, ctx))
                .toList();

        List<TDeletedDto> files = findDeletedFiles(ctx).stream()
                .map(f -> mapFileToDeleted(f, ctx))
                .toList();

        return Stream.concat(folders.stream(), files.stream())
                .sorted(Comparator.comparing(this::extractDeletedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    protected abstract TFolder resolveFolderOrRoot(UUID folderId, StorageContext ctx);

    protected abstract List<TFolder> fetchActiveFolders(TFolder parent, StorageContext ctx);
    protected abstract List<TFile> fetchActiveFiles(TFolder parent, StorageContext ctx);

    protected abstract List<TFolder> findDeletedFolders(StorageContext ctx);
    protected abstract List<TFile> findDeletedFiles(StorageContext ctx);

    protected abstract TItemDto mapFolderToItem(TFolder folder, StorageContext ctx);
    protected abstract TItemDto mapFileToItem(TFile file, StorageContext ctx);

    protected abstract TDeletedDto mapFolderToDeleted(TFolder folder, StorageContext ctx);
    protected abstract TDeletedDto mapFileToDeleted(TFile file, StorageContext ctx);

    /**
     * Нужно вернуть id папки (generic access) — конкретные реализации знают, как это делать.
     */
    protected abstract UUID extractFolderId(TFolder folder);

    protected abstract Instant extractDeletedAt(TDeletedDto dto);

    /**
     * Возвращает строку path для отображения (например "Все файлы" или "Департамент/Имя/...")
     */
    protected abstract String resolvePathForResponse(TFolder folder, StorageContext ctx);

    protected abstract boolean isFile(TItemDto dto);
    protected abstract String extractName(TItemDto dto);

    protected abstract TFolder getParentFolder(TFolder folder);

    private List<TItemDto> sortByFolderThenName(List<TItemDto> folders, List<TItemDto> files) {
        Comparator<TItemDto> byTypeThenName = Comparator
                .comparing(this::isFile)
                .thenComparing(this::extractName, String.CASE_INSENSITIVE_ORDER);

        return Stream.concat(folders.stream(), files.stream())
                .sorted(byTypeThenName)
                .toList();
    }

    private record BreadcrumbPath(String[] pathIds, String[] names) {}

    private BreadcrumbPath generateBreadcrumbPath(TFolder folder, StorageContext ctx) {
        List<String> pathIdsList = new ArrayList<>();
        List<String> pathNamesList = new ArrayList<>();

        pathIdsList.add(null);
        pathNamesList.add("Все файлы");

        // собираем путь ОТ КОРНЯ К ТЕКУЩЕЙ ПАПКЕ (реверсный обход)
        List<String> tempIds = new ArrayList<>();
        List<String> tempNames = new ArrayList<>();

        TFolder current = folder;
        while (current != null) {
            tempIds.add(extractFolderId(current).toString());
            tempNames.add(extractName(mapFolderToItem(current, ctx)));
            current = getParentFolder(current);
        }

        Collections.reverse(tempIds);
        Collections.reverse(tempNames);

        pathIdsList.addAll(tempIds);
        pathNamesList.addAll(tempNames);

        return new BreadcrumbPath(
                pathIdsList.toArray(String[]::new),
                pathNamesList.toArray(String[]::new)
        );
    }
}
