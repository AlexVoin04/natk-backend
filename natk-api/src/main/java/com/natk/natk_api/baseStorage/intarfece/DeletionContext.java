package com.natk.natk_api.baseStorage.intarfece;

import java.util.UUID;

public interface DeletionContext {
    void deleteFile(UUID fileId);
    void deleteFolder(UUID folderId);
}
