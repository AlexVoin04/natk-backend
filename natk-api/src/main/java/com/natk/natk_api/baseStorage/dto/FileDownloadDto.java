package com.natk.natk_api.baseStorage.dto;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public record FileDownloadDto(
        StreamingResponseBody body,
        String originalName,
        String encodedName,
        String translitName,
        MediaType fileType
) {}
