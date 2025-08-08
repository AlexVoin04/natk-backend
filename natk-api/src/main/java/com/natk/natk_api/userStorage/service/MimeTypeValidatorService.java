package com.natk.natk_api.userStorage.service;

import lombok.Getter;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
public class MimeTypeValidatorService {
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    @Getter
    public enum MimeType {
        PDF("application/pdf", false),
        DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", true),
        XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", true),
        XLS("application/vnd.ms-excel", true),
        OOXML("application/x-tika-ooxml", true),
        TXT("text/plain", true),
        PNG("image/png", true),
        JPEG("image/jpeg", true),
        XML("application/xml", true),
        TEXT_XML("text/xml", true),
        ZIP("application/zip", false),
        RAR("application/x-rar-compressed", false),
        SEVEN_Z("application/x-7z-compressed", false),
        TAR("application/x-tar", false),
        GZIP("application/gzip", false);

        private final String type;
        private final boolean convertibleToPdf;

        MimeType(String type, boolean convertibleToPdf) {
            this.type = type;
            this.convertibleToPdf = convertibleToPdf;
        }

        public static Optional<MimeType> fromType(String type) {
            return Arrays.stream(values())
                    .filter(mt -> mt.type.equalsIgnoreCase(type))
                    .findFirst();
        }
    }

    private final Tika tika = new Tika();

    public void validate(byte[] fileData) {
        if (fileData.length > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }

        String mimeType = tika.detect(fileData);
        if (MimeType.fromType(mimeType).isEmpty()) {
            throw new IllegalArgumentException("Unsupported file type: " + mimeType);
        }
    }

    public String detectMimeType(byte[] fileData) {
        return tika.detect(fileData);
    }

    public boolean isConvertibleToPdf(byte[] fileData) {
        String mimeType = detectMimeType(fileData);
        return MimeType.fromType(mimeType)
                .map(MimeType::isConvertibleToPdf)
                .orElse(false);
    }
}
