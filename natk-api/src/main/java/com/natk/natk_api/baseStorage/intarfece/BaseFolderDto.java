package com.natk.natk_api.baseStorage.intarfece;

import java.util.UUID;

public interface BaseFolderDto {
    UUID id();
    String name();
    UUID parentId();
    boolean isDeleted();
    String path();
}
