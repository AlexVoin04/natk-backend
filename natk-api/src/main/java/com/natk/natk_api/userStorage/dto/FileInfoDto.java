package com.natk.natk_api.userStorage.dto;

import com.natk.natk_api.userStorage.model.UserFolderEntity;

import java.time.Instant;
import java.util.UUID;

public record FileInfoDto(UUID id, String name, String fileType, Instant createdAt, FolderDto folder, boolean isDeleted, Instant deletedAt, String path) {}
