package com.natk.natk_api.llms.service;

import com.natk.natk_api.llms.AiServiceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AiServiceClient {

    private final RestClient restClient;
    private final AiServiceProperties aiServiceProperties;

    /**
     * Отправляет POST-запрос в AI-сервис и извлекает результат.
     */
    public String generateQuestions(MultiValueMap<String, HttpEntity<?>> multipartData) {
        try {
            Map<String, String> response = restClient.post()
                    .uri(aiServiceProperties.getUrl() + "/generate-questions/")
                    .contentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA)
                    .body(multipartData)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            return response != null ? response.get("result") : null;
        }catch (HttpServerErrorException | HttpClientErrorException ex) {
            throw new IllegalArgumentException("Error accessing natk-ai: " + ex.getResponseBodyAsString());
        }
    }
}
