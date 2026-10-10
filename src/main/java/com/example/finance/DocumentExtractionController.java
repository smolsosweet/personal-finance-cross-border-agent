package com.example.finance;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Gemini-only extraction for the existing review-before-save document flow. */
@RestController
public class DocumentExtractionController {
    private final GeminiDocumentExtractionService gemini;
    private final ReceiptLinkService receipts;

    @org.springframework.beans.factory.annotation.Autowired
    public DocumentExtractionController(GeminiDocumentExtractionService gemini, ReceiptLinkService receipts) {
        this.gemini=gemini; this.receipts=receipts;
    }

    public DocumentExtractionController(GeminiDocumentExtractionService gemini) {
        this.gemini = gemini; this.receipts=null;
    }

    @PostMapping(path = "/api/ai/extract-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> extractWithOrigin(@RequestParam("file") MultipartFile file,
                                               @RequestParam("kind") String kind,
                                               jakarta.servlet.http.HttpServletRequest request) throws java.io.IOException {
        var response=extract(file,kind);
        if (response.getStatusCode().is2xxSuccessful() && "transaction".equals(kind)) {
            String capability=receipts.issue(request.getSession(),file.getBytes());
            return ResponseEntity.ok().header("X-FinBridge-Receipt-Capability",capability).body(response.getBody());
        }
        return response;
    }

    public ResponseEntity<?> extract(@RequestParam("file") MultipartFile file,
                                     @RequestParam("kind") String kind) {
        if (file == null || file.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("message", "Choose an image first."));
        if (!"transaction".equals(kind) && !"student-bill".equals(kind))
            return ResponseEntity.badRequest().body(Map.of("message", "Unsupported document type."));
        try {
            return ResponseEntity.ok(gemini.extract(file, kind));
        } catch (IllegalStateException unavailable) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("message", unavailable.getMessage()));
        }
    }
}
