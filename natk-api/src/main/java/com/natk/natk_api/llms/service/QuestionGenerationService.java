package com.natk.natk_api.llms.service;

import com.natk.natk_api.llms.QuestionType;
import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.repository.UserFileRepository;
import com.natk.natk_api.users.model.UserEntity;
import com.natk.natk_api.users.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionGenerationService {
    private final UserFileRepository fileRepo;
    private final CurrentUserService currentUserService;

    public String generateQuestions(List<UUID> fileIds, Map<QuestionType, Integer> questionCounts) {
        UserEntity user = currentUserService.getCurrentUser();

        List<UserFileEntity> files = fileRepo.findAllByIdInAndCreatedBy(fileIds, user);

        if (files.isEmpty()) throw new IllegalArgumentException("No valid files found");

        // Генерация текста вопроса
        StringBuilder questionBuilder = new StringBuilder("Сгенерируй тест из следующих вопросов:\n");
        questionCounts.forEach((type, count) -> {
            questionBuilder.append("- ").append(type.name()).append(": ").append(count).append(" шт.\n");
        });
        String questionText = questionBuilder.toString();

        RestClient restClient = RestClient.builder()
                .baseUrl("http://natk-ai:8080")
                .build();

        var bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("question", questionText);

        for (UserFileEntity file : files) {
            ByteArrayResource resource = new ByteArrayResource(file.getFileData()) {
                @Override
                public String getFilename() {
                    return file.getName();
                }
            };
            bodyBuilder.part("files", resource)
                    .header("Content-Disposition",
                            "form-data; name=\"files\"; filename=\"" + file.getName() + "\"");
        }

        var multipartData = bodyBuilder.build();

        Map<String, String> response = restClient.post()
                .uri("/process-file/")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(multipartData)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<>() {});

        return response != null ? response.get("result") : null;
    }
}
