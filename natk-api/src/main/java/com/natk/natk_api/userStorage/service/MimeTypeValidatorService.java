package com.natk.natk_api.userStorage.service;

import lombok.Getter;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class MimeTypeValidatorService {
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    @Getter
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "application/x-tika-ooxml",
            "text/plain",
            "image/png",
            "image/jpeg",
            "application/xml",
            "text/xml",
            "application/zip",
            "application/x-rar-compressed",
            "application/x-7z-compressed",
            "application/x-tar",
            "application/gzip"
    );

    private final Tika tika = new Tika();

    public void validate(byte[] fileData) {
        if (fileData.length > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }

        String mimeType = tika.detect(fileData);
        if (!ALLOWED_TYPES.contains(mimeType)) {
            throw new IllegalArgumentException("Unsupported file type: " + mimeType);
        }
    }

    public String detectMimeType(byte[] fileData) {
        return tika.detect(fileData);
    }
}
