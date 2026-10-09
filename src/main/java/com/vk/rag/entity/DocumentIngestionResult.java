package com.vk.rag.entity;

public record DocumentIngestionResult(
        Long documentId,
        String title,
        int chunkCount
) {}
