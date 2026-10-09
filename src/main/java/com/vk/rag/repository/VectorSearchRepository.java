package com.vk.rag.repository;

import com.vk.rag.entity.SearchResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public class VectorSearchRepository {

    private final JdbcTemplate jdbcTemplate;

    public VectorSearchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SearchResult> searchSimilar(
            float[] queryEmbedding,
            int limit) {

        if (queryEmbedding == null || queryEmbedding.length != 768) {
            throw new IllegalArgumentException(
                    "Query embedding must contain 768 dimensions"
            );
        }

        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException(
                    "Search limit must be between 1 and 100"
            );
        }

        String vectorValue = Arrays.toString(queryEmbedding);

        String sql = """
                SELECT
                    dc.id AS chunk_id,
                    dc.document_id,
                    dc.chunk_index,
                    dc.content,
                    dc.embedding <=> ?::vector AS cosine_distance,
                    d.title AS document_title,
                    d.source_name
                FROM document_chunks dc
                JOIN documents d
                    ON d.id = dc.document_id
                WHERE dc.embedding IS NOT NULL
                ORDER BY dc.embedding <=> ?::vector
                LIMIT ?
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SearchResult(
                        rs.getLong("chunk_id"),
                        rs.getLong("document_id"),
                        rs.getInt("chunk_index"),
                        rs.getString("content"),
                        rs.getDouble("cosine_distance"),
                        rs.getString("document_title"),
                        rs.getString("source_name")
                ),
                vectorValue,
                vectorValue,
                limit
        );
    }
}
