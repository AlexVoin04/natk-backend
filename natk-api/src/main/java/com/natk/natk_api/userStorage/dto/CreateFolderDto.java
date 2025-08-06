package com.natk.natk_api.userStorage.dto;

import java.util.UUID;

public record CreateFolderDto(String name, UUID parentFolderId) {}
