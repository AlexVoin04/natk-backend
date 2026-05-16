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
import java.util.Set;

@Service
public class MimeTypeValidatorService {
    private static final int MAGIC_BYTES_LIMIT = 8192;
    private static final long MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50 MB
    private final Tika tika = new Tika();

    @Getter
    public enum MimeType {
        PDF("application/pdf", false),
        DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", true),
        DOC("application/msword", true),
        XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", true),
        XLS("application/vnd.ms-excel", true),
        PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", true),
        PPT("application/vnd.ms-powerpoint", true),
        ODT("application/vnd.oasis.opendocument.text", true),
        ODS("application/vnd.oasis.opendocument.spreadsheet", true),
        ODP("application/vnd.oasis.opendocument.presentation", true),
        RTF("application/rtf", true),
        TXT("text/plain", true),
        CSV("text/csv", true),
        HTML("text/html", true),
        OOXML("application/x-tika-ooxml", true),
        TEXT_XML("text/xml", true),
        JSON("application/json", true),
        XML("application/xml", true),

        PNG("image/png", true),
        JPEG("image/jpeg", true),
        JPG("image/jpg", true),
        GIF("image/gif", true),
        WEBP("image/webp", true),
        BMP("image/bmp", true),
        SVG("image/svg+xml", true),

        ZIP("application/zip", false),
        RAR("application/x-rar-compressed", false),
        SEVEN_Z("application/x-7z-compressed", false),
        TAR("application/x-tar", false),
        GZIP("application/gzip", false),

        MP3("audio/mpeg", false),
        WAV("audio/wav", false),
        MP4("video/mp4", false),
        MOV("video/quicktime", false);

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

    private static final Set<String> FORBIDDEN_MIME_PREFIXES = Set.of(
            "application/x-dosexec",
            "application/x-msdownload",
            "application/x-sh",
            "application/x-bat",
            "application/x-powershell",
            "application/java-archive",
            "application/x-mach-binary",
            "application/x-elf"
    );

    public MagicValidationResult validate(InputStream is, String fileName) {
        try {
            byte[] header = is.readNBytes(MAGIC_BYTES_LIMIT);

            String mime = detectMimeType(header, fileName);
            validateMime(mime);

            return new MagicValidationResult(header, mime);
        }catch (Exception e){
            throw new RuntimeException("Failed to detect Magic mime type", e);
        }
    }

    private void validateMime(String detectedMime) {
        String base = normalizeMime(detectedMime);

        boolean forbidden = FORBIDDEN_MIME_PREFIXES.stream()
                .anyMatch(base::startsWith);

        if (forbidden) {
            throw new IllegalArgumentException("Executable files are not allowed: " + detectedMime);
        }
    }

    private String normalizeMime(String mime) {
        if (mime == null) {
            return "";
        }
        return mime.split(";")[0].trim().toLowerCase();
    }

    public void validate(byte[] fileData, String fileName) {
        if (fileData.length > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 200MB limit");
        }

        String mimeType = detectMimeType(fileData, fileName);
        validateMime(mimeType);
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
