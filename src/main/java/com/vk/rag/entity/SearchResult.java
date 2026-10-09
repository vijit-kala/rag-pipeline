package com.vk.rag.entity;

public record SearchResult(
        Long chunkId,
        Long documentId,
        Integer chunkIndex,
        String content,
        double cosineDistance,
        String documentTitle,
        String sourceName
) {}
