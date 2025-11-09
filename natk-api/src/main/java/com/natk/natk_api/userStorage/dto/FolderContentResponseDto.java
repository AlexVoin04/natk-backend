package com.natk.natk_api.userStorage.dto;

import java.util.List;
import java.util.UUID;

public record FolderContentResponseDto<TItemDto>(
        UUID folderId,       // null — если корневая
        String path,
        List<TItemDto> items
) {}
