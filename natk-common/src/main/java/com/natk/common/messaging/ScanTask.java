package com.natk.common.messaging;

import java.util.UUID;

public record ScanTask(
        UUID fileId,
        String storageKey,
        UUID userId,
        OriginType originType,
        UUID departmentId // null для user files
) {
    public enum OriginType { USER, DEPARTMENT }
}
