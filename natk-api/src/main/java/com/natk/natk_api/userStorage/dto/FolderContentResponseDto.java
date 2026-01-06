package com.natk.natk_api.userStorage.dto;

import java.util.List;
import java.util.UUID;

public record FolderContentResponseDto<TItemDto>(
        UUID folderId,       // null — если корневая
        String path,
        String[] pathIds,   // ["all", "id1", "id2"]
        String[] pathNames, // ["Все файлы", "Папка1", "Папка2"]
        List<TItemDto> items
) {}
