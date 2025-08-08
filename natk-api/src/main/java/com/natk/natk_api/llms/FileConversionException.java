package com.natk.natk_api.llms;

import com.natk.natk_api.llms.dto.FailedFileInfo;
import lombok.Getter;

import java.util.List;

@Getter
public class FileConversionException extends RuntimeException {
    private final List<FailedFileInfo> failedFiles;

    public FileConversionException(List<FailedFileInfo> failedFiles) {
        super("Some files could not be converted to PDF");
        this.failedFiles = failedFiles;
    }

}
