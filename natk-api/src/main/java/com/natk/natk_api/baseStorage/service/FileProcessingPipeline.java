package com.natk.natk_api.baseStorage.service;

import com.natk.natk_api.baseStorage.ProcessingResult;
import com.natk.natk_api.clamav.ClamAVClientService;
import org.apache.commons.io.input.TeeInputStream;
import org.springframework.stereotype.Service;
import xyz.capybara.clamav.commands.scan.result.ScanResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Service
public class FileProcessingPipeline {

    private static final int HEADER_LIMIT = 8192; // 8 KB

    private final ExecutorService fileProcessingExecutor;
    private final ClamAVClientService clamavClient;
    private final MimeTypeValidatorService mimeValidator;

    public FileProcessingPipeline(
            ExecutorService fileProcessingExecutor,
            ClamAVClientService clamavClient,
            MimeTypeValidatorService mimeValidator
    ) {
        this.fileProcessingExecutor = fileProcessingExecutor;
        this.clamavClient = clamavClient;
        this.mimeValidator = mimeValidator;
    }

    /**
     * Обрабатывает поток файла:
     * - Читает первые HEADER_LIMIT байт для MIME
     * - Передаёт поток в MinIO
     * - Отправляет поток в ClamAV для асинхронного сканирования
     */
    public ProcessingResult process(InputStream inputStream, String fileName) throws IOException {

        // Буфер для первых N байт (для MAGIC / MIME определения)
        byte[] header = readHeader(inputStream);

        // Поток, который получит ClamAV
        PipedInputStream clamIn = new PipedInputStream(64 * 1024);
        PipedOutputStream clamOut = new PipedOutputStream(clamIn);

        // Tee для разделения потока: MinIO поток + ClamAV поток
        TeeInputStream tee = new TeeInputStream(inputStream, clamOut, false);

        Future<ScanResult> clamFuture = fileProcessingExecutor.submit(() -> {
            try {
                return clamavClient.scan(clamIn);
            } finally {
                clamIn.close();
            }
        });

        // MIME определяем по содержимому и заголовку
        String mime = mimeValidator.detectMimeType(header, fileName);

        return new ProcessingResult(header, mime, tee, clamFuture, clamOut);
    }

    /** Читает строго первые N байт, не трогая остальной поток */
    private byte[] readHeader(InputStream input) throws IOException {
        ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int total = 0;

        while (total < HEADER_LIMIT) {
            int read = input.read(buf, 0, Math.min(buf.length, HEADER_LIMIT - total));
            if (read == -1) break;
            headerBuffer.write(buf, 0, read);
            total += read;
        }

        return headerBuffer.toByteArray();
    }
}
