package com.vk.rag.entity;

public record SourceCitation(
        Long documentId,
        String documentTitle,
        String sourceName,
        Long chunkId,
        Integer chunkIndex,
        String content,
        double cosineDistance
) {
}
