package com.natk.natk_antivirus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Service
public class FileStatusUpdater {

    private static final Logger log = LoggerFactory.getLogger(FileStatusUpdater.class);

    private final RestClient client;

    public FileStatusUpdater(@Value("${natk.api.url}") String apiUrl) {
        this.client = RestClient.builder()
                .baseUrl(apiUrl)
                .build();
    }

    public void markClean(UUID fileId, String storageKey) {
//        client.post()
//                .uri("/internal/scan/{id}/clean", fileId)
//                .body(new CleanDto(storageKey))
//                .retrieve()
//                .toBodilessEntity();
        log.info("Marking file as clean: fileId={}, storageKey={}", fileId, storageKey);
    }

    public void markInfected(UUID fileId, String virusInfo) {
        /*client.post()
                .uri("/internal/scan/{id}/infected", fileId)
                .body(new VirusDto(virusInfo))
                .retrieve()
                .toBodilessEntity();*/
        log.info("Marking file as infected: fileId={}, virus={}", fileId, virusInfo);
    }

    public void markError(UUID fileId, String error) {
//        client.post()
//                .uri("/internal/scan/{id}/error", fileId)
//                .body(new ErrorDto(error))
//                .retrieve()
//                .toBodilessEntity();
        log.info("Marking file as error: fileId={}, error={}", fileId, error);
    }

    public record CleanDto(String storageKey) {}
    public record VirusDto(String virus) {}
    public record ErrorDto(String errorMessage) {}
}
