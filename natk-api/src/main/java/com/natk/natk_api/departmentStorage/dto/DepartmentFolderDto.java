package com.natk.natk_api.departmentStorage.dto;

import com.natk.natk_api.baseStorage.intarfece.BaseFolderDto;

import java.util.UUID;

public record DepartmentFolderDto(
        UUID id,
        String name,
        UUID parentId,
        boolean isDeleted,
        String path,
        String user,
        boolean isPublic
) implements BaseFolderDto {}
