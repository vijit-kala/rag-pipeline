package com.vk.rag.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

@Service
public class DocumentTextExtractorService {

    public String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Uploaded file must not be empty"
            );
        }

        String filename = file.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException(
                    "Uploaded file must have a filename"
            );
        }

        String normalizedFilename =
                filename.toLowerCase(Locale.ROOT);

        try {
            if (normalizedFilename.endsWith(".txt")) {
                return new String(
                        file.getBytes(),
                        java.nio.charset.StandardCharsets.UTF_8
                );
            }

            if (normalizedFilename.endsWith(".pdf")) {
                try (PDDocument document =
                             Loader.loadPDF(file.getBytes())) {

                    return new PDFTextStripper()
                            .getText(document);
                }
            }

            throw new IllegalArgumentException(
                    "Unsupported file type. Upload a .txt or .pdf file"
            );

        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "Could not extract text from the uploaded file",
                    exception
            );
        }
    }
}
