package com.natk.natk_api.purgeStorage;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class StorageLifecycleScheduler {
    private final StorageCleanupService storageCleanupService;

    /**
     * Запускаем каждый день в 3:00 ночи
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledPurge() {
        storageCleanupService.purgeDeletedItemsOlderThan(Duration.ofDays(30));
    }
}
