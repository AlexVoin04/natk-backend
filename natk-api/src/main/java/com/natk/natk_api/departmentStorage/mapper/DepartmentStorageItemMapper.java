package com.natk.natk_api.departmentStorage.mapper;

import com.natk.natk_api.baseStorage.mapper.AbstractStorageItemMapper;
import com.natk.natk_api.department.model.DepartmentEntity;
import com.natk.natk_api.departmentStorage.dto.DepartmentDeletedItemDto;
import com.natk.natk_api.departmentStorage.dto.DepartmentStorageItemDto;
import com.natk.natk_api.departmentStorage.model.DepartmentFileEntity;
import com.natk.natk_api.departmentStorage.model.DepartmentFolderEntity;
import org.springframework.stereotype.Component;

@Component
public class DepartmentStorageItemMapper extends AbstractStorageItemMapper<DepartmentFolderEntity> {

    public DepartmentStorageItemDto toStorageItem(DepartmentFolderEntity folder, DepartmentEntity dept) {
        return new DepartmentStorageItemDto(
                folder.getId(),
                folder.getName(),
                "folder",
                folder.getCreatedAt(),
                folder.getUpdatedAt(),
                folder.getCreatedBy(),
                null,
                null
        );
    }

    public DepartmentStorageItemDto toStorageItem(DepartmentFileEntity file, DepartmentEntity dept) {
        return new DepartmentStorageItemDto(
                file.getId(),
                file.getName(),
                file.getFileType(),
                file.getCreatedAt(),
                null,
                file.getCreatedBy(),
                file.getStatus(),
                file.getFileSize()
        );
    }

    public DepartmentDeletedItemDto toDeletedItem(DepartmentFolderEntity folder, DepartmentEntity dept) {
        return new DepartmentDeletedItemDto(
                folder.getId(),
                folder.getName(),
                "folder",
                folder.getDeletedAt(),
                resolvePath(folder, "Департамент/" + dept.getName(), DepartmentFolderEntity::isDeleted, DepartmentFolderEntity::getParentFolder, DepartmentFolderEntity::buildPath),
                folder.getParentFolder() != null ? folder.getParentFolder().getId() : null,
                folder.getCreatedBy()
        );
    }

    public DepartmentDeletedItemDto toDeletedItem(DepartmentFileEntity file, DepartmentEntity dept) {
        return new DepartmentDeletedItemDto(
                file.getId(),
                file.getName(),
                file.getFileType(),
                file.getDeletedAt(),
                resolvePath(file.getFolder(), "Департамент/" + dept.getName(), DepartmentFolderEntity::isDeleted, DepartmentFolderEntity::getParentFolder, DepartmentFolderEntity::buildPath),
                file.getFolder() != null ? file.getFolder().getId() : null,
                file.getCreatedBy()
        );
    }
}
