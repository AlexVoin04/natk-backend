package com.natk.natk_api.baseStorage;

import xyz.capybara.clamav.commands.scan.result.ScanResult;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Future;

public record ProcessingResult(byte[] header, String mime, InputStream minioStream, Future<ScanResult> clamAvFuture,
                               PipedOutputStream clamOut) implements Closeable {

    @Override
    public void close() {
        try {
            clamOut.close();
        } catch (IOException ignored) {
        }
        try {
            minioStream.close();
        } catch (IOException ignored) {
        }
    }
}
