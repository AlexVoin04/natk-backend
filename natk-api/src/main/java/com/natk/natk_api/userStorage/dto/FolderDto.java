package com.natk.natk_api.userStorage.dto;

import com.natk.natk_api.baseStorage.intarfece.BaseFolderDto;

import java.util.UUID;

public record FolderDto(
        UUID id,
        String name,
        UUID parentId,
        boolean isDeleted,
        String path
) implements BaseFolderDto {}