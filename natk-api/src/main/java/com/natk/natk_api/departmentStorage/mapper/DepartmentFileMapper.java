package com.natk.natk_api.departmentStorage.mapper;

import com.natk.natk_api.departmentStorage.dto.DepartmentFileInfoDto;
import com.natk.natk_api.departmentStorage.dto.DepartmentFolderDto;
import com.natk.natk_api.departmentStorage.model.DepartmentFileEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DepartmentFileMapper {
    private final DepartmentFolderMapper folderMapper;

    public DepartmentFileInfoDto toDto(DepartmentFileEntity entity) {
        DepartmentFolderDto folderDto = entity.getFolder() != null
                ? folderMapper.toDto(entity.getFolder())
                : null;

        String path = folderDto != null ? folderDto.path()
                : "Департамент/" + entity.getDepartment().getName() + "/Все файлы";

        return new DepartmentFileInfoDto(
                entity.getId(),
                entity.getName(),
                entity.getFileType(),
                entity.getCreatedAt(),
                folderDto,
                entity.isDeleted(),
                entity.getDeletedAt(),
                path,
                entity.getCreatedBy()
        );
    }
}
