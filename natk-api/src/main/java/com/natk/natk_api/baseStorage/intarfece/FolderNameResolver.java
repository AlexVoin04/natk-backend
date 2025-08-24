package com.natk.natk_api.baseStorage.intarfece;

public interface FolderNameResolver<TFolder, TContext> {
    void ensureUniqueNameOrThrow(String desiredName, TFolder parentFolder, TContext ctx);
}
