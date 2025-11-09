package com.natk.natk_api.baseStorage.intarfece;

import java.time.Instant;
import java.util.UUID;

public interface BaseStorageItemDto {
    UUID id();
    String name();
    String type();
    Instant createdAt();
    Instant updatedAt();
}

