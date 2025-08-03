package com.natk.natk_api.userStorage.dto;

import java.util.Optional;
import java.util.UUID;

public record UpdateFolderDto(
        String newName,
        Optional<UUID> newParentFolderId,
        Boolean moveToRoot // если true — устанавливаем parentFolder = null
) {}
