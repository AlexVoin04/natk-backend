package com.natk.natk_api.llms.dto;

import com.natk.natk_api.llms.QuestionType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record QuestionRequestDto(
        List<UUID> fileIds,
        Map<QuestionType, Integer> questionCounts
) {}
