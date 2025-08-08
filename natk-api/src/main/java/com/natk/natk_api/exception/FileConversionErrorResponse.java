package com.natk.natk_api.exception;

import com.natk.natk_api.llms.dto.FailedFileInfo;

import java.util.List;

public record FileConversionErrorResponse(
        int status,
        String error,
        String message,
        List<FailedFileInfo> failedFiles
) {}
