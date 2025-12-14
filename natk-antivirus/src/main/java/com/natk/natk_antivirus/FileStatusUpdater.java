package com.natk.natk_antivirus;

import com.natk.common.messaging.ScanTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Service
public class FileStatusUpdater {

    private static final Logger log = LoggerFactory.getLogger(FileStatusUpdater.class);

    private final WebClient webClient;
    private final RetryTemplate retryTemplate;

    public FileStatusUpdater(
            @Value("${natk.api.url}") String apiUrl,
            RetryTemplate retryTemplate
    ) {
        this.retryTemplate = retryTemplate;
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .build();
    }

    private String generateEndpoint(UUID fileId, ScanTask.OriginType type){
        return switch (type) {
            case USER -> "/internal/scan/user/" + fileId;
            case DEPARTMENT -> "/internal/scan/department/" + fileId;
        };
    }

    public void markClean(UUID fileId, ScanTask.OriginType type) {
        String endpoint = generateEndpoint(fileId, type);
        safeCall(() -> webClient.post()
                .uri(endpoint + "/clean")
                .retrieve()
                .toBodilessEntity()
                .block());
    }

    public void markInfected(UUID fileId, String virusInfo, ScanTask.OriginType type) {
        String endpoint = generateEndpoint(fileId, type);
        safeCall(() -> webClient.post()
                .uri(endpoint + "/infected")
                .bodyValue(new VirusDto(virusInfo))
                .retrieve()
                .toBodilessEntity()
                .block());
    }

    public void markError(UUID fileId, String error, ScanTask.OriginType type) {
        String endpoint = generateEndpoint(fileId, type);
        safeCall(() ->webClient.post()
                .uri(endpoint + "/error")
                .bodyValue(new ErrorDto(error))
                .retrieve()
                .toBodilessEntity()
                .block());
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
        }, retryContext  -> {
            // Recovery callback — выполняется после всех попыток
            log.error("All retry attempts failed. Giving up.");
            return null;
        });
    }

    public record VirusDto(String virus) {}
    public record ErrorDto(String errorMessage) {}
}
