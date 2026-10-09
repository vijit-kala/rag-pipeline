package com.vk.rag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RagRequest(
        @NotBlank
        @Size(max = 10000)
        String question,

        @Min(1)
        @Max(20)
        Integer limit
) {
}
