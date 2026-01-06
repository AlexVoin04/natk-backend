package com.natk.natk_api.userStorage.dto;

import com.natk.natk_api.baseStorage.enums.FileStatus;
import com.natk.natk_api.baseStorage.intarfece.BaseStorageItemDto;

import java.time.Instant;
import java.util.UUID;

public record UserStorageItemDto(
        UUID id,
        String name,
        String type, // "folder" или MIME type файла
        Instant createdAt,
        Instant updatedAt,
        FileStatus fileAntivirusStatus,
        Long size
) implements BaseStorageItemDto {}

