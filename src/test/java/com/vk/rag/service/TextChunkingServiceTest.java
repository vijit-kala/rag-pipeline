package com.vk.rag.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextChunkingServiceTest {

    @Test
    void shouldReturnEmptyListForNullOrBlankText() {
        TextChunkingService service = new TextChunkingService(10, 2);

        assertThat(service.chunkText(null)).isEmpty();
        assertThat(service.chunkText("   ")).isEmpty();
    }

    @Test
    void shouldReturnSingleChunkWhenTextFits() {
        TextChunkingService service = new TextChunkingService(10, 2);

        List<String> chunks = service.chunkText("Hello world");

        assertThat(chunks).containsExactly("Hello worl", "rld");
    }

    @Test
    void shouldPreserveOverlapBetweenChunks() {
        TextChunkingService service = new TextChunkingService(6, 2);

        List<String> chunks = service.chunkText("abcdefghijkl");

        assertThat(chunks).containsExactly(
                "abcdef",
                "efghij",
                "ijkl"
        );
    }

    @Test
    void shouldTrimWhitespaceAroundInputAndChunks() {
        TextChunkingService service = new TextChunkingService(10, 2);

        List<String> chunks = service.chunkText("   abcdefghijkl   ");

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allSatisfy(chunk ->
                assertThat(chunk).doesNotStartWith(" ")
                        .doesNotEndWith(" ")
        );
    }

    @Test
    void shouldRejectInvalidConfiguration() {
        assertThatThrownBy(() -> new TextChunkingService(0, 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new TextChunkingService(10, 10))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new TextChunkingService(10, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}