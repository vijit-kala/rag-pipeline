package com.vk.rag.dto;

public record DocumentIngestionResponse(
        Long documentId,
        String title,
        int chunkCount
) {
}
