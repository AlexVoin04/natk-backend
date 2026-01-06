package com.natk.natk_api.baseStorage.service;

import com.natk.natk_api.baseStorage.StorageItemType;
import com.natk.natk_api.departmentStorage.dto.PurgeItemDto;

import java.util.Comparator;
import java.util.List;

public final class BulkDeleteValidator {

    private BulkDeleteValidator() {}

    public static void validate(List<PurgeItemDto> items) {
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("Items list is empty");

        for (int i = 0; i < items.size(); i++) {
            PurgeItemDto item = items.get(i);

            if (item == null)
                throw new IllegalArgumentException("Item at index " + i + " is null");

            if (item.id() == null)
                throw new IllegalArgumentException("Item id is null at index " + i);

            if (item.type() == null)
                throw new IllegalArgumentException("Item type is null at index " + i);

            StorageItemType.from(item.type());
        }
    }

    public static List<PurgeItemDto> order(List<PurgeItemDto> items) {
        return items.stream()
                .sorted(Comparator.comparing(
                        i -> StorageItemType.from(i.type()) == StorageItemType.FOLDER
                ))
                .toList();
    }
}
