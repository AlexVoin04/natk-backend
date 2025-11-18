package com.natk.natk_api.baseStorage.dto;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;

public record FileDownloadDto(
        StreamingResponseBody body,
        String originalName,
        String encodedName,
        String translitName
) {}
