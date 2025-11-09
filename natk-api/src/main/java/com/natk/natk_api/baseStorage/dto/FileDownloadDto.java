package com.natk.natk_api.baseStorage.dto;

public record FileDownloadDto(
        byte[] fileData,
        String originalName,
        String encodedName,
        String translitName
) {}
