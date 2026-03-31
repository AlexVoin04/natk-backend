package com.natk.natk_api.departmentStorage.repository;

import com.natk.natk_api.baseStorage.enums.FileStatus;

import java.time.Instant;
import java.util.UUID;

public record DepartmentStorageSearchRow(
        UUID id,
        String name,
        String type,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        FileStatus fileAntivirusStatus,
        Long size
) {}
