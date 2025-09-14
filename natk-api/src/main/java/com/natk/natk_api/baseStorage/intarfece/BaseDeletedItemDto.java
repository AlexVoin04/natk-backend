package com.natk.natk_api.baseStorage.intarfece;

import java.time.Instant;
import java.util.UUID;

public interface BaseDeletedItemDto {
    UUID id();
    String name();
    String type();
    Instant deletedAt();
    String path();
    UUID parentFolder();
}
