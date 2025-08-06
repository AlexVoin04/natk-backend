package com.natk.natk_api.userStorage.mapper;

import com.natk.natk_api.userStorage.dto.DeletedItemDto;
import com.natk.natk_api.userStorage.dto.StorageItemDto;
import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import org.springframework.stereotype.Component;

@Component
public class StorageItemMapper {

    public StorageItemDto toStorageItem(UserFolderEntity folder) {
        return new StorageItemDto(
                folder.getId(),
                folder.getName(),
                "folder",
                folder.getCreatedAt(),
                folder.getUpdatedAt()
        );
    }

    public StorageItemDto toStorageItem(UserFileEntity file) {
        return new StorageItemDto(
                file.getId(),
                file.getName(),
                file.getFileType(),
                file.getCreatedAt(),
                null // файлы не обновляются
        );
    }

    public DeletedItemDto toDeletedItem(UserFolderEntity folder) {
        return new DeletedItemDto(
                folder.getId(),
                folder.getName(),
                "folder",
                folder.getDeletedAt(),
                resolvePath(folder.getParentFolder()),
                folder.getParentFolder() != null ? folder.getParentFolder().getId() : null
        );
    }

    public DeletedItemDto toDeletedItem(UserFileEntity file) {
        return new DeletedItemDto(
                file.getId(),
                file.getName(),
                file.getFileType(),
                file.getDeletedAt(),
                resolvePath(file.getFolder()),
                file.getFolder() != null ? file.getFolder().getId() : null
        );
    }

    private String resolvePath(UserFolderEntity folder) {
        if (folder == null) return "Облако";

        UserFolderEntity current = folder;
        while (current != null) {
            if (current.isDeleted()) {
                return "Облако";
            }
            current = current.getParentFolder();
        }

        return folder.buildPath();
    }
}
