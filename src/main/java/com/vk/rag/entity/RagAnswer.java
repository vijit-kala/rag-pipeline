package com.vk.rag.entity;

import java.util.List;

public record RagAnswer(
        String question,
        String answer,
        List<SourceCitation> sources
) {}
