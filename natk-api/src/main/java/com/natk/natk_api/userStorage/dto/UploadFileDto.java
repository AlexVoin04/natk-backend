package com.natk.natk_api.userStorage.dto;

import jakarta.annotation.Nullable;

import java.util.UUID;

public record UploadFileDto(String name, @Nullable UUID folderId, byte[] fileData) {}