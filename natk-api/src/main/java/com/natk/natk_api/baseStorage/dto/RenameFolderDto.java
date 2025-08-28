package com.natk.natk_api.baseStorage.dto;

import jakarta.validation.constraints.NotBlank;

public record RenameFolderDto(
        @NotBlank String newName
) {}
