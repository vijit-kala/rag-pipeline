package com.vk.rag.controller;

import com.vk.rag.dto.RagRequest;
import com.vk.rag.entity.RagAnswer;
import com.vk.rag.service.RagService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public RagAnswer ask(@Valid @RequestBody RagRequest request) {
        int limit = request.limit() == null ? 5 : request.limit();

        return ragService.ask(request.question(), limit);
    }
}
