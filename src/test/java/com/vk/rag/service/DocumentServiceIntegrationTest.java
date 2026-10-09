package com.vk.rag.service;

import com.vk.rag.entity.Document;
import com.vk.rag.entity.DocumentChunk;
import com.vk.rag.entity.DocumentIngestionResult;
import com.vk.rag.repository.DocumentChunkRepository;
import com.vk.rag.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class DocumentServiceIntegrationTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldIngestDocumentAndPersistChunksInOrder() {
        String text = "A".repeat(1800);

        DocumentIngestionResult result =
                documentService.ingestText(
                        "RAG Introduction",
                        "rag-intro.txt",
                        "text/plain",
                        text
                );

        assertThat(result.documentId()).isNotNull();
        assertThat(result.title()).isEqualTo("RAG Introduction");
        assertThat(result.chunkCount()).isEqualTo(2);

        Document document = documentRepository
                .findById(result.documentId())
                .orElseThrow();

        List<DocumentChunk> chunks =
                documentChunkRepository
                        .findByDocumentIdOrderByChunkIndexAsc(
                                document.getId()
                        );

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).getChunkIndex()).isZero();
        assertThat(chunks.get(1).getChunkIndex()).isEqualTo(1);

        assertThat(chunks.get(0).getContent()).hasSize(1000);
        assertThat(chunks.get(1).getContent()).hasSize(950);

        assertThat(chunks.get(0).getDocument().getId())
                .isEqualTo(document.getId());
    }

    @Test
    void shouldRejectBlankDocumentText() {
        assertThatThrownBy(() ->
                documentService.ingestText(
                        "Empty document",
                        "empty.txt",
                        "text/plain",
                        " "
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Document text must not be blank");
    }


    @Test
    void shouldPersistEmbeddingForEachChunk() {
        DocumentIngestionResult result =
                documentService.ingestText(
                        "Embedding Test",
                        "embedding-test.txt",
                        "text/plain",
                        "Spring Boot builds Java applications. ".repeat(40)
                );

        Integer chunksWithoutEmbeddings = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM document_chunks
                WHERE document_id = ?
                  AND embedding IS NULL
                """,
                Integer.class,
                result.documentId()
        );

        assertThat(chunksWithoutEmbeddings).isZero();

        Integer embeddedChunks = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM document_chunks
                WHERE document_id = ?
                  AND embedding IS NOT NULL
                """,
                Integer.class,
                result.documentId()
        );

        assertThat(embeddedChunks).isEqualTo(result.chunkCount());
    }

    @Test
    void shouldDeleteDocumentAndCascadeDeleteChunksAndEmbeddings() {
        DocumentIngestionResult result = documentService.ingestText(
                "Cascade Delete Test",
                "cascade-delete-test.txt",
                "text/plain",
                "Testing PostgreSQL cascading deletes. ".repeat(40)
        );

        Long documentId = result.documentId();

        assertThat(documentChunkRepository.countByDocumentId(documentId))
                .isEqualTo((long) result.chunkCount());

        jdbcTemplate.update(
                "DELETE FROM documents WHERE id = ?",
                documentId
        );

        assertThat(documentRepository.existsById(documentId)).isFalse();

        assertThat(documentChunkRepository.countByDocumentId(documentId))
                .isZero();

        Integer remainingEmbeddings = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM document_chunks
                WHERE document_id = ?
                  AND embedding IS NOT NULL
                """,
                Integer.class,
                documentId
        );

        assertThat(remainingEmbeddings).isZero();
    }

    @Test
    void shouldReturnDocumentSummariesWithCorrectChunkCounts() {
        DocumentIngestionResult result = documentService.ingestText(
                "Summary Projection Test",
                "summary-test.txt",
                "text/plain",
                "Testing document summaries. ".repeat(40)
        );

        var summaries = documentRepository.findAllDocumentSummaries();

        var summary = summaries.stream()
                .filter(row -> row.getId().equals(result.documentId()))
                .findFirst()
                .orElseThrow();

        assertThat(summary.getTitle())
                .isEqualTo("Summary Projection Test");

        assertThat(summary.getSourceName())
                .isEqualTo("summary-test.txt");

        assertThat(summary.getChunkCount())
                .isEqualTo((long) result.chunkCount());

        assertThat(summary.getCreatedAt()).isNotNull();
    }

}