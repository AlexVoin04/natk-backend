package com.natk.natk_api.userStorage.dto;

import java.time.Instant;
import java.util.UUID;

public record StorageItemDto(
        UUID id,
        String name,
        String type, // "folder" или MIME type файла
        Instant createdAt
) {}

