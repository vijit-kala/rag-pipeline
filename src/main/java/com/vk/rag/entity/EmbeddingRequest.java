package com.vk.rag.entity;

public record EmbeddingRequest(
    String model,
    String input
) {
}
