package com.natk.natk_api.baseStorage.dto;

import java.util.UUID;

public record CreateFolderDto(String name, UUID parentFolderId) {}
