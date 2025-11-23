package com.natk.natk_api.minio;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class MinioMetrics {

    private final MeterRegistry meterRegistry;

    public void recordUpload(long size, long millis) {
        meterRegistry.timer("minio.upload.time").record(millis, TimeUnit.MILLISECONDS);
        meterRegistry.counter("minio.upload.bytes").increment(size);
    }

    public void recordError() {
        meterRegistry.counter("minio.upload.errors").increment();
    }

    public void recordDownload(long millis) {
        meterRegistry.timer("minio.download.time").record(millis, TimeUnit.MILLISECONDS);
    }
}
