package com.natk.natk_api.baseStorage.mapper;

import java.util.function.Function;

public abstract class AbstractStorageItemMapper<TFolder> {

    protected String resolvePath(
            TFolder folder, String rootName,
            Function<TFolder, Boolean> isDeletedFunc,
            Function<TFolder, TFolder> getParentFunc,
            Function<TFolder, String> buildPathFunc) {
        if (folder == null) return rootName;

        TFolder current = folder;
        while (current != null) {
            if (Boolean.TRUE.equals(isDeletedFunc.apply(current))) {
                return rootName;
            }
            current = getParentFunc.apply(current);
        }

        return buildPathFunc.apply(folder);
    }
}
