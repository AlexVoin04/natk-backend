package com.natk.natk_api.userStorage.mapper;

import com.natk.natk_api.userStorage.dto.FolderDto;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import org.springframework.stereotype.Component;

@Component
public class UserFolderMapper {

    public FolderDto toDto(UserFolderEntity entity) {
        String path = entity.getParentFolder() != null ? entity.buildPath() : "Все файлы" + "/" + entity.getName();

        return new FolderDto(
                entity.getId(),
                entity.getName(),
                entity.getParentFolder() != null ? entity.getParentFolder().getId() : null,
                entity.isDeleted(),
                path
        );
    }
}
