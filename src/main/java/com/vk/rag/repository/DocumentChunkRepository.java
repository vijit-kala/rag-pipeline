package com.vk.rag.repository;

import com.vk.rag.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    Long countByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);
}
