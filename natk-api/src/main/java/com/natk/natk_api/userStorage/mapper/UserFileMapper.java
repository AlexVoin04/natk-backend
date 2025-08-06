package com.natk.natk_api.userStorage.mapper;

import com.natk.natk_api.userStorage.dto.FileInfoDto;
import com.natk.natk_api.userStorage.dto.FolderDto;
import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.model.UserFolderEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserFileMapper {
    private final UserFolderMapper userFolderMapper;

    public FileInfoDto toDto(UserFileEntity entity) {
        UserFolderEntity folder = entity.getFolder();
        FolderDto folderDto = folder != null ? userFolderMapper.toDto(folder) : null;
        String path = folder != null ? folderDto.path()  : "Все файлы";

        return new FileInfoDto(
                entity.getId(),
                entity.getName(),
                entity.getFileType(),
                entity.getCreatedAt(),
                folderDto,
                entity.isDeleted(),
                entity.getDeletedAt(),
                path
        );
    }
}
