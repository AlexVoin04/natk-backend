package com.natk.natk_api.baseStorage.intarfece;

import java.util.UUID;

public interface FolderNameResolver<TFolder, TContext> {
    void ensureUniqueNameOrThrow(String desiredName, TFolder parentFolder, TContext ctx, UUID excludeFolderId);
    void ensureUniqueNameOrThrow(String desiredName, TFolder parentFolder, TContext ctx);
    String ensureUniqueName(String desiredName, TFolder parentFolder, TContext ctx, UUID excludeFolderId);
}
