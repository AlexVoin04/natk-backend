package com.natk.natk_api.userStorage.dto;

import java.util.UUID;

public record UploadFileDto(String name, UUID folderId, byte[] fileData) {}