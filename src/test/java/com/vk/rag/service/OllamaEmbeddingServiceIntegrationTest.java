package com.vk.rag.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OllamaEmbeddingServiceIntegrationTest {

    @Autowired
    private OllamaEmbeddingService embeddingService;

    @Test
    void shouldGenerate768DimensionalEmbedding() {
        float[] embedding = embeddingService.embed(
                "Spring Boot is used to build Java backend applications."
        );

        assertThat(embedding).hasSize(768);
        assertThat(embedding).doesNotContain(0.0f);
    }

    @Test
    void shouldRejectBlankText() {
        assertThatThrownBy(() -> embeddingService.embed(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Text to embed must not be blank");
    }
}