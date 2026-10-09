package com.vk.rag.entity;

import java.util.List;

public record EmbeddingResponse(
        List<List<Double>> embeddings
) {
}
