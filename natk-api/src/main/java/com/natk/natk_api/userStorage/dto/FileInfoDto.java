package com.natk.natk_api.userStorage.dto;

import com.natk.natk_api.baseStorage.intarfece.BaseFileDto;

import java.time.Instant;
import java.util.UUID;

public record FileInfoDto(
        UUID id,
        String name,
        String fileType,
        Instant createdAt,
        FolderDto folder,
        boolean isDeleted,
        Instant deletedAt,
        String path,
        long fileSize,
        String storageKey
) implements BaseFileDto {}
