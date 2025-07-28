package com.natk.natk_api.userStorage.dto;

import java.util.UUID;

public record UpdateFileDto(String newName, UUID newFolderId) {}
