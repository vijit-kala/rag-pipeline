package com.vk.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentIngestionRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        @NotBlank
        @Size(max = 500)
        String sourceName,

        @Size(max = 100)
        String contentType,

        @NotBlank
        @Size(max = 1_000_000)
        String text
) {
}
