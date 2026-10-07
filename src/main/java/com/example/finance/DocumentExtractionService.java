package com.example.finance;

import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

/** Provider boundary for receipt and education-bill extraction. Results are suggestions only. */
public interface DocumentExtractionService {
    record Result(Map<String, String> fields, Double confidence, String provider) {}
    String provider();
    Result extract(MultipartFile file, String kind);
}
