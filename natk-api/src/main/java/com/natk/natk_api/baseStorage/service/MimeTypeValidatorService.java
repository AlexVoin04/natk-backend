package com.natk.natk_api.baseStorage.service;

import com.natk.natk_api.baseStorage.MagicValidationResult;
import lombok.Getter;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;

@Service
public class MimeTypeValidatorService {
    private static final int MAGIC_BYTES_LIMIT = 8192;
    private static final long MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50 MB
    private final Tika tika = new Tika();

    @Getter
    public enum MimeType {
        PDF("application/pdf", false),
        DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", true),
        XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", true),
        XLS("application/vnd.ms-excel", true),
        PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", true),
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

    public MagicValidationResult validate(InputStream is, String fileName) {
        try {
            byte[] header = is.readNBytes(MAGIC_BYTES_LIMIT);

            validate(header);
            String mime = detectMimeType(header, fileName);

            return new MagicValidationResult(header, mime);
        }catch (Exception e){
            throw new RuntimeException("Failed to detect Magic mime type", e);
        }
    }

    public void validate(byte[] fileData) {
        if (fileData.length > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 50MB limit");
        }

        String mimeType = tika.detect(fileData);
        if (MimeType.fromType(mimeType).isEmpty()) {
            throw new IllegalArgumentException("Unsupported file type: " + mimeType);
        }
    }

    public String detectMimeType(byte[] fileData, String fileName) {
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName); // указать имя файла с расширением
        try (TikaInputStream stream = TikaInputStream.get(fileData)) {
            return tika.getDetector().detect(stream, metadata).toString();
        }catch (IOException e){
            throw new RuntimeException("Failed to detect mime type", e);
        }
    }

    public boolean isConvertibleToPdf(byte[] fileData, String fileName) {
        String mimeType = detectMimeType(fileData, fileName);
        return MimeType.fromType(mimeType)
                .map(MimeType::isConvertibleToPdf)
                .orElse(false);
    }
}
