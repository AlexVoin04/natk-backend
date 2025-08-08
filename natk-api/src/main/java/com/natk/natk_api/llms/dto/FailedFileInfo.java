package com.natk.natk_api.llms.dto;

import java.util.UUID;

public record FailedFileInfo(UUID id, String name, String reason) {
}

