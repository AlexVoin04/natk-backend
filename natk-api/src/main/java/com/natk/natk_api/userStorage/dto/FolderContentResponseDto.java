package com.natk.natk_api.userStorage.dto;

import java.util.List;
import java.util.UUID;

public record FolderContentResponseDto(
        UUID folderId,       // null — если корневая
        String path,
        List<StorageItemDto> items
) {}
