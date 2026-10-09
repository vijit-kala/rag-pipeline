package com.vk.rag.controller;

import com.vk.rag.dto.DocumentIngestionRequest;
import com.vk.rag.dto.DocumentIngestionResponse;
import com.vk.rag.entity.DocumentDetails;
import com.vk.rag.entity.DocumentIngestionResult;
import com.vk.rag.entity.DocumentSummary;
import com.vk.rag.service.DocumentService;
import com.vk.rag.service.DocumentTextExtractorService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentTextExtractorService documentTextExtractorService;

    public DocumentController(DocumentService documentService, DocumentTextExtractorService documentTextExtractorService) {
        this.documentService = documentService;
        this.documentTextExtractorService = documentTextExtractorService;
    }

    @GetMapping
    public ResponseEntity<List<DocumentSummary>> listDocuments() {
        return ResponseEntity.ok(documentService.listDocuments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDetails> getDocument(
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(documentService.getDocument(id));
    }

    @PostMapping
    public ResponseEntity<DocumentIngestionResponse> ingestDocument(
            @Valid @RequestBody DocumentIngestionRequest request) {

        DocumentIngestionResult result =
                documentService.ingestText(
                        request.title(),
                        request.sourceName(),
                        request.contentType(),
                        request.text()
                );

        DocumentIngestionResponse response =
                new DocumentIngestionResponse(
                        result.documentId(),
                        result.title(),
                        result.chunkCount()
                );

        return ResponseEntity
                .created(URI.create("/api/documents/" + result.documentId()))
                .body(response);
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<DocumentIngestionResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title) {

        String extractedText =
                documentTextExtractorService.extractText(file);

        String sourceName = file.getOriginalFilename();

        String contentType = file.getContentType();

        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        DocumentIngestionResult result =
                documentService.ingestText(
                        title,
                        sourceName,
                        contentType,
                        extractedText
                );

        DocumentIngestionResponse response =
                new DocumentIngestionResponse(
                        result.documentId(),
                        result.title(),
                        result.chunkCount()
                );

        return ResponseEntity
                .created(URI.create(
                        "/api/documents/" + result.documentId()
                ))
                .body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable("id") Long id) {

        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
}
