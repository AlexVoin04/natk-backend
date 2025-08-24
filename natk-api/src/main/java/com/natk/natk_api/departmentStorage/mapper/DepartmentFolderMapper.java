package com.natk.natk_api.departmentStorage.mapper;

import com.natk.natk_api.departmentStorage.dto.DepartmentFolderDto;
import com.natk.natk_api.departmentStorage.model.DepartmentFolderEntity;
import org.springframework.stereotype.Component;

@Component
public class DepartmentFolderMapper {

    public DepartmentFolderDto toDto(DepartmentFolderEntity entity) {
        String path = entity.getParentFolder() != null
                ? entity.buildPath()
                : "Департамент/" + entity.getDepartment().getName() + "/" + entity.getName();

        return new DepartmentFolderDto(
                entity.getId(),
                entity.getName(),
                entity.getParentFolder() != null ? entity.getParentFolder().getId() : null,
                entity.isDeleted(),
                path,
                entity.getCreatedBy(),
                entity.isPublic()
        );
    }
}
