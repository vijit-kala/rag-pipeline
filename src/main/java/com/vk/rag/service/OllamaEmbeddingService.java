package com.vk.rag.service;

import com.vk.rag.entity.EmbeddingRequest;
import com.vk.rag.entity.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class OllamaEmbeddingService {

    private final RestClient restClient;
    private final String model;
    private final int dimensions;

    public OllamaEmbeddingService(
            RestClient.Builder restClientBuilder,
            @Value("${rag.embedding.base-url}") String baseUrl,
            @Value("${rag.embedding.model}") String model,
            @Value("${rag.embedding.dimensions}") int dimensions) {

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();

        this.model = model;
        this.dimensions = dimensions;
    }

    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Text to embed must not be blank"
            );
        }

        EmbeddingRequest request = new EmbeddingRequest(model, text);

        EmbeddingResponse response = restClient.post()
                .uri("/api/embed")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(EmbeddingResponse.class);

        if (response == null
                || response.embeddings() == null
                || response.embeddings().isEmpty()) {
            throw new IllegalStateException(
                    "Ollama returned no embeddings"
            );
        }

        List<Double> values = response.embeddings().get(0);

        if (values == null || values.size() != dimensions) {
            throw new IllegalStateException(
                    "Expected " + dimensions
                            + " embedding dimensions, but received "
                            + (values == null ? 0 : values.size())
            );
        }

        float[] embedding = new float[values.size()];

        for (int i = 0; i < values.size(); i++) {
            embedding[i] = values.get(i).floatValue();
        }

        return embedding;
    }

}
