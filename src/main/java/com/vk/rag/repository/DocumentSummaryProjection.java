package com.vk.rag.repository;

import java.time.Instant;

public interface DocumentSummaryProjection {

    Long getId();

    String getTitle();

    String getSourceName();

    String getContentType();

    Instant getCreatedAt();

    Instant getUpdatedAt();

    Long getChunkCount();
}
