package com.vk.rag.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
public class OllamaChatService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OllamaChatService(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${rag.embedding.base-url:http://localhost:11434}")
            String baseUrl,
            @Value("${rag.chat.model:qwen2.5:3b}")
            String model) {

        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.model = model;
    }

    public String generateAnswer(String systemPrompt, String userPrompt) {

        Map<String, Object> request = Map.of(
                "model", model,
                "stream", false,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", systemPrompt
                        ),
                        Map.of(
                                "role", "user",
                                "content", userPrompt
                        )
                )
        );

        JsonNode response = restClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Ollama returned an empty chat response"
            );
        }

        JsonNode content = response.path("message").path("content");

        if (!content.isTextual() || content.asText().isBlank()) {
            throw new IllegalStateException(
                    "Ollama returned an empty or invalid chat response"
            );
        }

        return content.asText().trim();
    }
}
