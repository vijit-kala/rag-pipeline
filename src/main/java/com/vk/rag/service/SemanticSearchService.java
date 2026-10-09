package com.vk.rag.service;

import com.vk.rag.entity.SearchResult;
import com.vk.rag.repository.VectorSearchRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SemanticSearchService {

    private final OllamaEmbeddingService embeddingService;
    private final VectorSearchRepository vectorSearchRepository;

    public SemanticSearchService(
            OllamaEmbeddingService embeddingService,
            VectorSearchRepository vectorSearchRepository) {
        this.embeddingService = embeddingService;
        this.vectorSearchRepository = vectorSearchRepository;
    }

    public List<SearchResult> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException(
                    "Search query must not be blank"
            );
        }

        float[] queryEmbedding = embeddingService.embed(query);

        return vectorSearchRepository.searchSimilar(
                queryEmbedding,
                limit
        );
    }
}
