package com.natk.natk_api.baseStorage.dto;

import java.util.UUID;

public record MoveFolderDto(
        UUID newParentFolderId,
        boolean moveToRoot // если true → переносим в корень
) {}
