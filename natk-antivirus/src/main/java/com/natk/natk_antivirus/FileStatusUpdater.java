package com.natk.natk_antivirus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Service
public class FileStatusUpdater {

    private static final Logger log = LoggerFactory.getLogger(FileStatusUpdater.class);

    private final RestClient client;
    private final RetryTemplate retryTemplate;

    public FileStatusUpdater(
            @Value("${natk.api.url}") String apiUrl,
            RetryTemplate retryTemplate
    ) {
        this.retryTemplate = retryTemplate;
        this.client = RestClient.builder()
                .baseUrl(apiUrl)
                .build();
    }

    public void markClean(UUID fileId, String storageKey) {
        safeCall(() ->
                client.post()
                        .uri("/internal/scan/{id}/clean", fileId)
                        .body(new CleanDto(storageKey))
                        .retrieve()
                        .toBodilessEntity()
        );
    }

    public void markInfected(UUID fileId, String virusInfo) {
        safeCall(() ->
                client.post()
                        .uri("/internal/scan/{id}/infected", fileId)
                        .body(new VirusDto(virusInfo))
                        .retrieve()
                        .toBodilessEntity()
        );
    }

    public void markError(UUID fileId, String error) {
        safeCall(() ->
                client.post()
                        .uri("/internal/scan/{id}/error", fileId)
                        .body(new ErrorDto(error))
                        .retrieve()
                        .toBodilessEntity()
        );
    }

    private void safeCall(Runnable action) {
        retryTemplate.execute(context -> {
            try {
                action.run();
                return null;
            } catch (Exception e) {
                log.warn("Attempt {} failed: {}", context.getRetryCount() + 1, e.getMessage());
                throw e;
            }
        }, context -> {
            // Recovery callback — выполняется после всех попыток
            log.error("All retry attempts failed. Giving up.");
            return null;
        });
    }

    public record CleanDto(String storageKey) {}
    public record VirusDto(String virus) {}
    public record ErrorDto(String errorMessage) {}
}
