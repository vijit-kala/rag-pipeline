package com.vk.rag.service;

import com.vk.rag.entity.*;
import com.vk.rag.exception.DocumentNotFoundException;
import com.vk.rag.repository.ChunkEmbeddingRepository;
import com.vk.rag.repository.DocumentChunkRepository;
import com.vk.rag.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final TextChunkingService textChunkingService;

    private final ChunkEmbeddingRepository chunkEmbeddingRepository;
    private final OllamaEmbeddingService ollamaEmbeddingService;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            TextChunkingService textChunkingService,
            ChunkEmbeddingRepository chunkEmbeddingRepository,
            OllamaEmbeddingService ollamaEmbeddingService) {

        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.textChunkingService = textChunkingService;
        this.chunkEmbeddingRepository = chunkEmbeddingRepository;
        this.ollamaEmbeddingService = ollamaEmbeddingService;
    }

    @Transactional
    public DocumentIngestionResult ingestText(String title, String sourceName, String contentType, String text) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be blank");
        }

        if (sourceName == null || sourceName.isBlank()) {
            throw new IllegalArgumentException("Source name must not be blank");
        }

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Document text must not be blank");
        }

        // Create document metadata
        Document document = new Document();
        document.setTitle(title);
        document.setSourceName(sourceName);
        document.setContentType(contentType);

        Document savedDocument = documentRepository.save(document);

        // Split the input text
        List<String> chunks = textChunkingService.chunkText(text);

        if (chunks.isEmpty()) {
            throw new IllegalArgumentException(
                    "Document produced no chunks"
            );
        }

        // Associate each chunk with its document
        List<DocumentChunk> entities = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocument(savedDocument);
            chunk.setChunkIndex(i);
            chunk.setContent(chunks.get(i));

            entities.add(chunk);
        }

        // Persist all chunks
        List<DocumentChunk> savedChunks =
                documentChunkRepository.saveAll(entities);

        for (DocumentChunk chunk : savedChunks) {
            float[] embedding =
                    ollamaEmbeddingService.embed(chunk.getContent());

            chunkEmbeddingRepository.saveEmbedding(
                    chunk.getId(),
                    embedding
            );
        }

        // Return a concise result
        return new DocumentIngestionResult(
                savedDocument.getId(),
                savedDocument.getTitle(),
                entities.size()
        );
    }

    @Transactional(readOnly = true)
    public List<DocumentSummary> listDocuments() {
        return documentRepository.findAllDocumentSummaries()
                .stream()
                .map(row -> new DocumentSummary(
                        row.getId(),
                        row.getTitle(),
                        row.getSourceName(),
                        row.getContentType(),
                        toOffsetDateTime(row.getCreatedAt()),
                        toOffsetDateTime(row.getUpdatedAt()),
                        row.getChunkCount()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentDetails getDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found with ID: " + documentId
                ));

        long chunkCount =
                documentChunkRepository.countByDocumentId(documentId);

        return new DocumentDetails(
                document.getId(),
                document.getTitle(),
                document.getSourceName(),
                document.getContentType(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                chunkCount
        );
    }

    @Transactional
    public void deleteDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found with ID: " + documentId
                ));

        documentChunkRepository.deleteByDocumentId(documentId);
        documentRepository.delete(document);
    }

    private OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null
                ? null
                : instant.atOffset(ZoneOffset.UTC);
    }
}
