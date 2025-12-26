package com.natk.natk_api.departmentStorage.dto;

import com.natk.natk_api.baseStorage.FileStatus;
import com.natk.natk_api.baseStorage.intarfece.BaseStorageItemDto;

import java.time.Instant;
import java.util.UUID;

public record DepartmentStorageItemDto(
        UUID id,
        String name,
        String type,
        Instant createdAt,
        Instant updatedAt,
        String user,
        FileStatus fileAntivirusStatus,
        Long size
) implements BaseStorageItemDto {}
