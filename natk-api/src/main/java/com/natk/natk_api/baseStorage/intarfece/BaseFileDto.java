package com.natk.natk_api.baseStorage.intarfece;

import java.time.Instant;
import java.util.UUID;

public interface BaseFileDto {
    UUID id();
    String name();
    String fileType();
    Instant createdAt();
    BaseFolderDto folder();
    boolean isDeleted();
    Instant deletedAt();
    String path();
}
