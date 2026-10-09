package com.vk.rag.entity;

import java.time.OffsetDateTime;

public record DocumentDetails(
        Long id,
        String title,
        String sourceName,
        String contentType,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        long chunkCount
) {
}
