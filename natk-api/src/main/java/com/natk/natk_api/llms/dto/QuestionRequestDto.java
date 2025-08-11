package com.natk.natk_api.llms.dto;

import com.natk.natk_api.llms.QuestionType;
import com.natk.natk_api.llms.service.ProviderType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record QuestionRequestDto(
        @NotNull(message = "fileIds must not be null")
        @NotEmpty(message = "fileIds must not be empty")
        List<UUID> fileIds,

        @NotNull(message = "questionCounts must not be null")
        @NotEmpty(message = "questionCounts must not be empty")
        Map<QuestionType, Integer> questionCounts,

        @NotNull(message = "provider must not be null")
        ProviderType provider
) {}
