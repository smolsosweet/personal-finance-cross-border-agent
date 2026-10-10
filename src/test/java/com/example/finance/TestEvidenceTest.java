package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TestEvidenceTest {
    @Test void generatedArtifactsAreConfinedToAUniqueTargetRun() {
        Path image=TestEvidence.path("docs/business-ux/example.png");
        assertTrue(image.startsWith(Path.of("target","test-evidence").toAbsolutePath().normalize()));
        assertEquals(image,TestEvidence.path("docs/business-ux/example.png"));
        assertTrue(image.toString().matches(".*[0-9a-f]{8}-[0-9a-f-]{27}.*"));
        assertThrows(IllegalArgumentException.class,()->TestEvidence.path("../../docs/tracked.png"));
        assertThrows(IllegalArgumentException.class,()->TestEvidence.path(Path.of("docs","tracked.png").toAbsolutePath().toString()));
    }
}