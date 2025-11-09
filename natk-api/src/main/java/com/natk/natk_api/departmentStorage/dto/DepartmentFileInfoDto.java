package com.natk.natk_api.departmentStorage.dto;

import com.natk.natk_api.baseStorage.intarfece.BaseFileDto;

import java.time.Instant;
import java.util.UUID;

public record DepartmentFileInfoDto(
        UUID id,
        String name,
        String fileType,
        Instant createdAt,
        DepartmentFolderDto folder,
        boolean isDeleted,
        Instant deletedAt,
        String path,
        String user
) implements BaseFileDto {}
