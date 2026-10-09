package com.vk.rag.controller;

import com.vk.rag.dto.SemanticSearchRequest;
import com.vk.rag.entity.SearchResult;
import com.vk.rag.service.SemanticSearchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SemanticSearchController {

    private final SemanticSearchService semanticSearchService;

    public SemanticSearchController(
            SemanticSearchService semanticSearchService) {
        this.semanticSearchService = semanticSearchService;
    }

    @PostMapping
    public List<SearchResult> search(
            @Valid @RequestBody SemanticSearchRequest request) {

        int limit = request.limit() == null ? 5 : request.limit();

        return semanticSearchService.search(
                request.query(),
                limit
        );
    }
}
