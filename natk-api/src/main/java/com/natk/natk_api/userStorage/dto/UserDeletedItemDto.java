package com.natk.natk_api.userStorage.dto;

import com.natk.natk_api.baseStorage.intarfece.BaseDeletedItemDto;

import java.time.Instant;
import java.util.UUID;

public record UserDeletedItemDto(
        UUID id,
        String name,
        String type,
        Instant deletedAt,
        String path,
        UUID parentFolder
) implements BaseDeletedItemDto {}
