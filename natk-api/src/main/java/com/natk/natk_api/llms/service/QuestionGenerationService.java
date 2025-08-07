package com.natk.natk_api.llms.service;

import com.natk.natk_api.llms.QuestionType;
import com.natk.natk_api.userStorage.model.UserFileEntity;
import com.natk.natk_api.userStorage.repository.UserFileRepository;
import com.natk.natk_api.users.model.UserEntity;
import com.natk.natk_api.users.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionGenerationService {
    private final UserFileRepository fileRepo;
    private final CurrentUserService currentUserService;
//    private final PdfConverter pdfConverter = new PdfConverter();

    private final RestClient restClient = RestClient.builder()
            .baseUrl("http://natk-ai:8080")
            .build();

    public String generateQuestions(List<UUID> fileIds, Map<QuestionType, Integer> questionCounts) {
        UserEntity user = currentUserService.getCurrentUser();

        List<UserFileEntity> userFiles = getUserFiles(fileIds, user);

        String questionText = buildPrompt(questionCounts);
        MultiValueMap<String, HttpEntity<?>> multipartData = buildMultipartRequest(userFiles, questionText);

        return sendRequest(multipartData);
    }


    /**
     * Загружает файлы пользователя по ID и проверяет их принадлежность.
     */
    private List<UserFileEntity> getUserFiles(List<UUID> fileIds, UserEntity user) {
        List<UserFileEntity> files = fileRepo.findAllByIdInAndCreatedBy(fileIds, user);
        if (files.isEmpty()) {
            throw new IllegalArgumentException("No valid files found for the current user");
        }
        return files;
    }

    /**
     * Формирует текст запроса на генерацию вопросов.
     */
    private String buildPrompt(Map<QuestionType, Integer> questionCounts) {
        StringBuilder prompt = new StringBuilder("Сгенерируй тест из следующих вопросов:\n");
        questionCounts.forEach((type, count) ->
                prompt.append("- ").append(type.name()).append(": ").append(count).append(" шт.\n")
        );
        return prompt.toString();
    }

    /**
     * Формирует multipart тело с вопросом и файлами.
     */
    private MultiValueMap<String, HttpEntity<?>> buildMultipartRequest(List<UserFileEntity> files, String questionText) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("question", questionText);

        for (UserFileEntity file : files) {
            byte[] fileData;

            if (!file.getName().toLowerCase().endsWith(".pdf")) {
                try {
                    fileData = normalizeToPdf(file);  // вызов внешнего сервиса
                } catch (Exception e) {
                    throw new IllegalArgumentException("Failed to convert file to PDF: " + file.getName(), e);
                }
            } else {
                fileData = file.getFileData();
            }

            ByteArrayResource resource = new ByteArrayResource(fileData) {
                @Override
                public String getFilename() {
                    return file.getName();
                }
            };
            bodyBuilder.part("files", resource)
                    .header("Content-Disposition",
                            "form-data; name=\"files\"; filename=\"" + resource.getFilename() + "\"");
        }

        return bodyBuilder.build();
    }

    private byte[] normalizeToPdf(UserFileEntity file) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", new ByteArrayResource(file.getFileData()) {
            @Override
            public String getFilename() {
                return file.getName();
            }
        });

        return restClient.post()
                .uri("http://natk-pdf:8080/convert")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(builder.build())
                .retrieve()
                .body(byte[].class);
    }

    /**
     * Отправляет POST-запрос в AI-сервис и извлекает результат.
     */
    private String sendRequest(MultiValueMap<String, HttpEntity<?>> multipartData) {
        try {
            Map<String, String> response = restClient.post()
                    .uri("/process-file/")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipartData)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            return response != null ? response.get("result") : null;
        }catch (HttpServerErrorException | HttpClientErrorException ex) {
            throw new IllegalArgumentException("Error accessing natk-ai: " + ex.getResponseBodyAsString());
        }
    }
}
