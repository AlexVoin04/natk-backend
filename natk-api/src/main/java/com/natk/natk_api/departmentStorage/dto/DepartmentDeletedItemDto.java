package com.natk.natk_api.departmentStorage.dto;

import com.natk.natk_api.baseStorage.intarfece.BaseDeletedItemDto;

import java.time.Instant;
import java.util.UUID;

public record DepartmentDeletedItemDto(
        UUID id,
        String name,
        String type,
        Instant deletedAt,
        String path,
        UUID parentFolder,
        String user
) implements BaseDeletedItemDto {}
