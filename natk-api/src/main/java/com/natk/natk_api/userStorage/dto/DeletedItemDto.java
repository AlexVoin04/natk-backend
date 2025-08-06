package com.natk.natk_api.userStorage.dto;

import java.time.Instant;
import java.util.UUID;

public record DeletedItemDto(
        UUID id,
        String name,
        String type,
        Instant deletedAt,
        String path,
        UUID parentFolder
) {}
