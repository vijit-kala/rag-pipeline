package com.vk.rag.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Arrays;

@Repository
public class ChunkEmbeddingRepository {

    private final JdbcTemplate jdbcTemplate;

    public ChunkEmbeddingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void saveEmbedding(Long chunkId, float[] embedding) {
        if (embedding == null || embedding.length != 768) {
            throw new IllegalArgumentException(
                    "Embedding must contain exactly 768 dimensions"
            );
        }

        String vectorValue = Arrays.toString(embedding);

        int updatedRows = jdbcTemplate.update(
                """
                UPDATE document_chunks
                SET embedding = ?::vector
                WHERE id = ?
                """,
                vectorValue,
                chunkId
        );

        if (updatedRows != 1) {
            throw new IllegalStateException(
                    "Expected to update one chunk, but updated "
                            + updatedRows
            );
        }
    }
}
