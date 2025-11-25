package com.natk.common.messaging;

import java.util.UUID;

public record ScanTask(
        UUID fileId,
        String storageKey,
        UUID userId
) {}
