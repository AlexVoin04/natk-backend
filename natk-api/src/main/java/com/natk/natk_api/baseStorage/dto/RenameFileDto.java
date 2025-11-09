package com.natk.natk_api.baseStorage.dto;

import jakarta.validation.constraints.NotBlank;

public record RenameFileDto(
        @NotBlank String newName
) {
}
