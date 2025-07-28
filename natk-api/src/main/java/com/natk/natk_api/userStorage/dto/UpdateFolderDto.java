package com.natk.natk_api.userStorage.dto;

import java.util.UUID;

public record UpdateFolderDto(String newName, UUID newParentFolderId) {}
