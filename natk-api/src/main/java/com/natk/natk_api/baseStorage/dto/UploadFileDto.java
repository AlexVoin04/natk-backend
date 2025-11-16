package com.natk.natk_api.baseStorage.dto;

import jakarta.annotation.Nullable;

import java.util.UUID;

public record UploadFileDto(String name, @Nullable UUID folderId, byte[] fileData, long size) {}