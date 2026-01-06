package com.natk.natk_api.baseStorage.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BulkDeleteResult(
        List<UUID> success,
        Map<UUID, String> failed
) {}
