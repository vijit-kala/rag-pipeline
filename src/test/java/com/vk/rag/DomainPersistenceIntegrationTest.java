package com.vk.rag;

import com.vk.rag.entity.Document;
import com.vk.rag.entity.DocumentChunk;
import com.vk.rag.repository.DocumentChunkRepository;
import com.vk.rag.repository.DocumentRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Transactional
public class DomainPersistenceIntegrationTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Test
    void shouldSaveDocumentAndRetrieveChunksInOrder() {

        // 1. Create and save a document
        Document document = new Document();
        document.setTitle("Spring Boot RAG Guide");
        document.setSourceName("rag-guide.txt");
        document.setContentType("text/plain");

        Document savedDocument = documentRepository.save(document);

        assertThat(savedDocument.getId()).isNotNull();

        // 2. Create two chunks associated with the document
        DocumentChunk secondChunk = new DocumentChunk();
        secondChunk.setDocument(savedDocument);
        secondChunk.setChunkIndex(1);
        secondChunk.setContent("Embeddings represent text as vectors.");

        DocumentChunk firstChunk = new DocumentChunk();
        firstChunk.setDocument(savedDocument);
        firstChunk.setChunkIndex(0);
        firstChunk.setContent("RAG combines retrieval with generation.");

        // Intentionally save in reverse order
        documentChunkRepository.save(secondChunk);
        documentChunkRepository.save(firstChunk);

        // 3. Retrieve chunks in their defined order
        List<DocumentChunk> chunks =
                documentChunkRepository
                        .findByDocumentIdOrderByChunkIndexAsc(
                                savedDocument.getId()
                        );

        // 4. Verify persistence and ordering
        assertThat(chunks).hasSize(2);

        assertThat(chunks.get(0).getChunkIndex()).isZero();
        assertThat(chunks.get(0).getContent())
                .isEqualTo("RAG combines retrieval with generation.");

        assertThat(chunks.get(1).getChunkIndex()).isEqualTo(1);
        assertThat(chunks.get(1).getContent())
                .isEqualTo("Embeddings represent text as vectors.");

        assertThat(chunks.get(0).getDocument().getId())
                .isEqualTo(savedDocument.getId());
    }
}
