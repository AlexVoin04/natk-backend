package com.natk.natk_api.baseStorage.dto;

public record SignedUrlResponse(
        String url,
        String fileName,
        String mimeType
) {}
