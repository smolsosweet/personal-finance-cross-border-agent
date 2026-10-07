package com.example.finance;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Provider selection for the existing review-before-save document flow. */
@RestController
public class DocumentExtractionController {
    private final Map<String, DocumentExtractionService> providers;
    private final String defaultProvider;

    public DocumentExtractionController(List<DocumentExtractionService> services,
            @Value("${finbridge.document-ai.provider:local}") String defaultProvider) {
        this.providers = services.stream().collect(Collectors.toUnmodifiableMap(
                DocumentExtractionService::provider, Function.identity()));
        this.defaultProvider = "gemini".equalsIgnoreCase(defaultProvider) ? "gemini" : "local";
    }

    @PostMapping(path = "/api/ai/extract-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> extract(@RequestParam("file") MultipartFile file,
                                     @RequestParam("kind") String kind,
                                     @RequestParam(required=false) String provider) {
        if (file == null || file.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("message", "Choose an image first."));
        if (!"transaction".equals(kind) && !"student-bill".equals(kind))
            return ResponseEntity.badRequest().body(Map.of("message", "Unsupported document type."));
        String selected = provider == null || provider.isBlank() ? defaultProvider : provider.trim().toLowerCase();
        if (!selected.equals("gemini") && !selected.equals("local"))
            return ResponseEntity.badRequest().body(Map.of("message", "Choose Gemini or local AI."));
        DocumentExtractionService service = providers.get(selected);
        if (service == null)
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", selected.equals("local")
                            ? "Local bill AI is not available. Start the PaddleOCR service and Ollama, then try again."
                            : "Gemini bill AI is not available in this application."));
        try {
            return ResponseEntity.ok(service.extract(file, kind));
        } catch (IllegalStateException unavailable) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", unavailable.getMessage()));
        }
    }
}
