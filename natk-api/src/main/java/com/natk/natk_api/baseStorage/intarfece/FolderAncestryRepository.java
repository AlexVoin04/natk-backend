package com.natk.natk_api.baseStorage.intarfece;

import java.util.UUID;

public interface FolderAncestryRepository {
    /**
     * Возвращает true, если node ancestorId встречается в цепочке родителей, начиная от childId вверх.
     * То есть: true, если ancestorId является предком childId.
     */
    boolean isAncestor(UUID childId, UUID ancestorId);
}
