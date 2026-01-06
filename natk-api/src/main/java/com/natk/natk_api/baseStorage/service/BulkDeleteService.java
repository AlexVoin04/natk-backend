package com.natk.natk_api.baseStorage.service;

import com.natk.natk_api.baseStorage.StorageItemType;
import com.natk.natk_api.baseStorage.dto.BulkDeleteResult;
import com.natk.natk_api.baseStorage.intarfece.DeletionContext;
import com.natk.natk_api.departmentStorage.dto.PurgeItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BulkDeleteService {

    @Transactional
    public BulkDeleteResult deleteMultiple(List<PurgeItemDto> items, DeletionContext ctx) {
        BulkDeleteValidator.validate(items);
        List<PurgeItemDto> ordered = BulkDeleteValidator.order(items);


        List<UUID> success = new ArrayList<>();
        Map<UUID, String> failed = new LinkedHashMap<>();

        for (PurgeItemDto item : ordered) {
            try {
                StorageItemType type = StorageItemType.from(item.type());

                switch (type) {
                    case FILE -> ctx.deleteFile(item.id());
                    case FOLDER -> ctx.deleteFolder(item.id());
                }

                success.add(item.id());
            }
            catch (Exception ex) {
                failed.put(item.id(), extractMessage(ex));
            }
        }

        return new BulkDeleteResult(success, failed);
    }

    private String extractMessage(Exception ex) {
        if (ex.getMessage() != null)
            return ex.getMessage();
        return ex.getClass().getSimpleName();
    }
}
