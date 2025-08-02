package com.natk.natk_api.userStorage.dto;

public record FileDownloadDto(
        byte[] fileData,
        String originalName,
        String encodedName,
        String translitName
) {}
