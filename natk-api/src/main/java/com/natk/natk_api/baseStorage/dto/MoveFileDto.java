package com.natk.natk_api.baseStorage.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MoveFileDto(
        UUID newFolderId,
        @NotNull boolean moveToRoot // если true → переносим в корень
) {}
