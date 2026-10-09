package com.vk.rag.repository;

import com.vk.rag.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query(value = """
        SELECT
            d.id AS "id",
            d.title AS "title",
            d.source_name AS "sourceName",
            d.content_type AS "contentType",
            d.created_at AS "createdAt",
            d.updated_at AS "updatedAt",
            COUNT(dc.id) AS "chunkCount"
        FROM documents d
        LEFT JOIN document_chunks dc
            ON dc.document_id = d.id
        GROUP BY
            d.id,
            d.title,
            d.source_name,
            d.content_type,
            d.created_at,
            d.updated_at
        ORDER BY d.id
        """, nativeQuery = true)
    List<DocumentSummaryProjection> findAllDocumentSummaries();
}
