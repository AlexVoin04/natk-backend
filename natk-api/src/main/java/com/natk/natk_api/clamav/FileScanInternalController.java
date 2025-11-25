package com.natk.natk_api.clamav;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/scan")
@RequiredArgsConstructor
@Slf4j
public class FileScanInternalController {

    private final FileScanService scanService;

    @PostMapping("/{id}/clean")
    public ResponseEntity<Void> markClean(@PathVariable UUID id, @RequestBody CleanDto dto) {
        try {
            scanService.markClean(id);
        } catch (Exception e) {
            log.error("Error in markClean {}", id, e);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/infected")
    public ResponseEntity<Void> markInfected(@PathVariable UUID id, @RequestBody VirusDto dto) {
        try {
            scanService.markInfected(id, dto.virus());
        } catch (Exception e) {
            log.error("Error in markInfected {}", id, e);
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/error")
    public ResponseEntity<Void> markError(@PathVariable UUID id, @RequestBody ErrorDto dto) {
        try {
            scanService.markError(id, dto.errorMessage());
        } catch (Exception e) {
            log.error("Error in markError {}", id, e);
        }
        return ResponseEntity.noContent().build();
    }

    public record CleanDto(String storageKey) {}
    public record VirusDto(String virus) {}
    public record ErrorDto(String errorMessage) {}
}
