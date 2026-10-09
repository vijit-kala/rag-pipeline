package com.vk.rag.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TextChunkingService {

    private final int chunkSize;
    private final int chunkOverlap;

    public TextChunkingService(@Value("${rag.chunking.chunk-size:1000}") int chunkSize,
                               @Value("${rag.chunking.chunk-overlap:150}") int chunkOverlap) {
        if(chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be greater than zero");
        }

        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException(
                    "Chunk overlap must be non-negative and smaller than chunk size"
            );
        }

        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        String normalizedText = text.trim();

        if (normalizedText.isEmpty()) {
            return chunks;
        }

        int start = 0;

        while (start < normalizedText.length()) {
            int end = Math.min(
                    start + chunkSize,
                    normalizedText.length()
            );

            String chunk = normalizedText.substring(start, end).trim();

            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            if (end == normalizedText.length()) {
                break;
            }

            start = end - chunkOverlap;
        }

        return chunks;
    }
}
