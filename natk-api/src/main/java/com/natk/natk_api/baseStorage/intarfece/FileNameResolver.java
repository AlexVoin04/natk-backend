package com.natk.natk_api.baseStorage.intarfece;

import java.util.UUID;

public interface FileNameResolver<TFolder, TOwner> {
    void ensureUniqueNameOrThrow(String desiredName, TFolder parentFolder, TOwner owner, UUID excludeFileId);
    void ensureUniqueNameOrThrow(String desiredName, TFolder parentFolder, TOwner owner);
    String ensureUniqueName(String desiredName, TFolder parentFolder, TOwner owner, UUID excludeFileId);
}
