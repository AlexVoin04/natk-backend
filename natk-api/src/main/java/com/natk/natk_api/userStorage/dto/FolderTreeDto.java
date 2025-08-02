package com.natk.natk_api.userStorage.dto;

import java.util.List;
import java.util.UUID;

public record FolderTreeDto(
        UUID id,
        String name,
        int depth, // глубина - уровень вложенности внутри структуры
        List<FolderTreeDto> children
) {}
