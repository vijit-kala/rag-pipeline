package com.vk.rag.service;

import com.vk.rag.entity.RagAnswer;
import com.vk.rag.entity.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RagServiceTest {

    private SemanticSearchService semanticSearchService;
    private OllamaChatService ollamaChatService;
    private RagService ragService;

    private static final String QUESTION =
            "How does this application store embeddings?";

    @BeforeEach
    void setUp() {
        semanticSearchService = mock(SemanticSearchService.class);
        ollamaChatService = mock(OllamaChatService.class);

        ragService = new RagService(
                semanticSearchService,
                ollamaChatService,
                0.40,  // max cosine distance
                5,     // default retrieval limit
                20     // maximum retrieval limit
        );
    }

    @Test
    void ask_shouldIncludeOnlyRelevantChunks() {
        // Arrange: lower cosine distance means a closer match.
        SearchResult relevantChunk = new SearchResult(
                15L, 8L, 0,
                "Embeddings are stored in PostgreSQL using pgvector.",
                0.33,
                "RAG Architecture Guide",
                "rag-architecture.txt"
        );

        SearchResult irrelevantChunk = new SearchResult(
                14L, 7L, 2,
                "Unrelated content about Spring Boot.",
                0.46,
                "Spring Boot Notes",
                "spring-notes.txt"
        );

        when(semanticSearchService.search(QUESTION, 5))
                .thenReturn(List.of(relevantChunk, irrelevantChunk));

        when(ollamaChatService.generateAnswer(
                anyString(), anyString()))
                .thenReturn("Embeddings are stored using pgvector.");

        // Act
        RagAnswer result = ragService.ask(QUESTION, 5);

        // Assert
        assertEquals(QUESTION, result.question());
        assertEquals(
                "Embeddings are stored using pgvector.",
                result.answer()
        );
        assertEquals(1, result.sources().size());
        assertEquals(15L, result.sources().get(0).chunkId());

        // Verify the LLM receives the relevant content.
        verify(ollamaChatService).generateAnswer(
                anyString(),
                argThat(prompt ->
                        prompt.contains("Embeddings are stored in PostgreSQL")
                                && !prompt.contains(
                                "Unrelated content about Spring Boot."
                        )
                )
        );
    }

    @Test
    void ask_shouldNotCallLlmWhenAllChunksAreIrrelevant() {
        // Arrange
        SearchResult irrelevantChunk = new SearchResult(
                14L, 7L, 2,
                "Unrelated content.",
                0.75,
                "Unrelated Document",
                "unrelated.txt"
        );

        when(semanticSearchService.search(QUESTION, 5))
                .thenReturn(List.of(irrelevantChunk));

        // Act
        RagAnswer result = ragService.ask(QUESTION, 5);

        // Assert
        assertEquals(QUESTION, result.question());
        assertEquals(
                "I could not find sufficiently relevant information "
                        + "in the stored documents.",
                result.answer()
        );
        assertTrue(result.sources().isEmpty());

        // No relevant context means no LLM call.
        verifyNoInteractions(ollamaChatService);
    }

    @Test
    void ask_shouldIncludeMultipleRelevantChunks() {
        // Arrange
        SearchResult firstChunk = new SearchResult(
                15L, 8L, 0,
                "PostgreSQL stores the document chunks.",
                0.25,
                "RAG Architecture Guide",
                "rag-architecture.txt"
        );

        SearchResult secondChunk = new SearchResult(
                16L, 8L, 1,
                "pgvector stores the embeddings.",
                0.39,
                "RAG Architecture Guide",
                "rag-architecture.txt"
        );

        when(semanticSearchService.search(QUESTION, 5))
                .thenReturn(List.of(firstChunk, secondChunk));

        when(ollamaChatService.generateAnswer(
                anyString(), anyString()))
                .thenReturn("PostgreSQL and pgvector store the data.");

        // Act
        RagAnswer result = ragService.ask(QUESTION, 5);

        // Assert
        assertEquals(2, result.sources().size());
        assertEquals(15L, result.sources().get(0).chunkId());
        assertEquals(16L, result.sources().get(1).chunkId());

        verify(ollamaChatService).generateAnswer(
                anyString(),
                argThat(prompt ->
                        prompt.contains("PostgreSQL stores the document chunks.")
                                && prompt.contains(
                                "pgvector stores the embeddings."
                        )
                )
        );
    }

    @Test
    void ask_shouldRejectChunkExactlyAtThresholdPlusSmallMargin() {
        // Arrange: 0.4001 is just outside the configured 0.40 threshold.
        SearchResult borderlineChunk = new SearchResult(
                17L, 9L, 0,
                "Borderline content.",
                0.4001,
                "Borderline Document",
                "borderline.txt"
        );

        when(semanticSearchService.search(QUESTION, 5))
                .thenReturn(List.of(borderlineChunk));

        // Act
        RagAnswer result = ragService.ask(QUESTION, 5);

        // Assert
        assertTrue(result.sources().isEmpty());
        verifyNoInteractions(ollamaChatService);
    }

    @Test
    void ask_shouldAcceptChunkExactlyAtThreshold() {
        SearchResult chunk = new SearchResult(
                18L, 10L, 0,
                "The application uses PostgreSQL and pgvector.",
                0.40,
                "Database Guide",
                "database-guide.txt"
        );

        when(semanticSearchService.search(QUESTION, 5))
                .thenReturn(List.of(chunk));

        when(ollamaChatService.generateAnswer(anyString(), anyString()))
                .thenReturn("The application uses PostgreSQL and pgvector.");

        RagAnswer result = ragService.ask(QUESTION, 5);

        assertEquals(1, result.sources().size());
        assertEquals(18L, result.sources().get(0).chunkId());

        verify(ollamaChatService).generateAnswer(
                anyString(), anyString()
        );
    }

    @Test
    void ask_shouldReturnInsufficientContextWhenSearchReturnsNoChunks() {
        when(semanticSearchService.search(QUESTION, 5))
                .thenReturn(List.of());

        RagAnswer result = ragService.ask(QUESTION, 5);

        assertEquals(
                "I could not find sufficiently relevant information "
                        + "in the stored documents.",
                result.answer()
        );
        assertTrue(result.sources().isEmpty());

        verifyNoInteractions(ollamaChatService);
    }

    @Test
    void ask_shouldRejectBlankQuestionBeforeSearching() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ragService.ask("   ", 5)
        );

        verifyNoInteractions(semanticSearchService, ollamaChatService);
    }

    @Test
    void ask_shouldRejectInvalidRetrievalLimit() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ragService.ask(QUESTION, 0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ragService.ask(QUESTION, 21)
        );

        verifyNoInteractions(semanticSearchService, ollamaChatService);
    }

    @Test
    void ask_shouldNotReturnSourcesForIrrelevantChunks() {
        SearchResult relevant = new SearchResult(
                20L, 11L, 0,
                "PostgreSQL stores document chunks.",
                0.30,
                "Storage Guide",
                "storage.txt"
        );

        SearchResult irrelevant = new SearchResult(
                21L, 12L, 0,
                "Completely unrelated content.",
                0.80,
                "Other Guide",
                "other.txt"
        );

        when(semanticSearchService.search(QUESTION, 5))
                .thenReturn(List.of(relevant, irrelevant));

        when(ollamaChatService.generateAnswer(anyString(), anyString()))
                .thenReturn("PostgreSQL stores document chunks.");

        RagAnswer result = ragService.ask(QUESTION, 5);

        assertEquals(1, result.sources().size());
        assertEquals(20L, result.sources().get(0).chunkId());
    }
}