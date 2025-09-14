package com.natk.natk_api.userStorage.mapper;

import com.natk.natk_api.baseStorage.mapper.AbstractStorageItemMapper;
import com.natk.natk_api.userStorage.dto.UserDeletedItemDto;
import com.natk.natk_api.userStorage.dto.UserStorageItemDto;
import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import org.springframework.stereotype.Component;

@Component
public class UserStorageItemMapper extends AbstractStorageItemMapper<UserFolderEntity> {

    public UserStorageItemDto toStorageItem(UserFolderEntity folder) {
        return new UserStorageItemDto(
                folder.getId(),
                folder.getName(),
                "folder",
                folder.getCreatedAt(),
                folder.getUpdatedAt()
        );
    }

    public UserStorageItemDto toStorageItem(UserFileEntity file) {
        return new UserStorageItemDto(
                file.getId(),
                file.getName(),
                file.getFileType(),
                file.getCreatedAt(),
                null // файлы не обновляются
        );
    }

    public UserDeletedItemDto toDeletedItem(UserFolderEntity folder) {
        return new UserDeletedItemDto(
                folder.getId(),
                folder.getName(),
                "folder",
                folder.getDeletedAt(),
                resolvePath(
                        folder,
                        "Облако",
                        UserFolderEntity::isDeleted,
                        UserFolderEntity::getParentFolder,
                        UserFolderEntity::buildPath
                ),
                folder.getParentFolder() != null ? folder.getParentFolder().getId() : null
        );
    }

    public UserDeletedItemDto toDeletedItem(UserFileEntity file) {
        return new UserDeletedItemDto(
                file.getId(),
                file.getName(),
                file.getFileType(),
                file.getDeletedAt(),
                resolvePath(
                        file.getFolder(),
                        "Облако",
                        UserFolderEntity::isDeleted,
                        UserFolderEntity::getParentFolder,
                        UserFolderEntity::buildPath
                ),
                file.getFolder() != null ? file.getFolder().getId() : null
        );
    }
}
