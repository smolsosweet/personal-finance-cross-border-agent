package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ExtractionEvaluationFixturesTest {
    @Test void exactlyTwentyImagesEachHaveGroundTruthAndSupportedKind() throws Exception {
        var fixtures=ExtractionEvaluationFixtures.load(); assertEquals(20,fixtures.size());
        assertEquals(10,fixtures.stream().filter(f -> f.kind().equals("transaction")).count());
        assertEquals(10,fixtures.stream().filter(f -> f.kind().equals("student-bill")).count());
        for(var fixture:fixtures) {
            assertTrue(ExtractionEvaluationScorer.whitelist(fixture.kind()).containsAll(fixture.expected().keySet()));
            assertEquals(fixture.kind().equals("transaction")?4:6,fixture.expected().size());
            byte[] bytes=ExtractionEvaluationFixtures.committedImage(fixture); assertTrue(bytes.length>100);
            assertNotNull(ImageIO.read(new ByteArrayInputStream(bytes)));
            assertTrue(Set.of("EXTRACT","EXTRACT_IGNORING_INJECTION","REFUSE_OR_EMPTY").contains(fixture.expectedBehavior()));
        }
        try(var paths=Files.list(Path.of("src/test/resources/gemini-extraction-eval/images"))) {
            assertEquals(20,paths.filter(p -> p.toString().endsWith(".png")).count());
        }
    }
    @Test void sameTemplateInputsProduceIdenticalBytesForEveryFixture() throws Exception {
        for(var fixture:ExtractionEvaluationFixtures.load()) {
            byte[] first=ExtractionEvaluationFixtures.render(fixture),second=ExtractionEvaluationFixtures.render(fixture);
            assertArrayEquals(first,second,fixture.id());
            assertEquals(ExtractionEvaluationFixtures.sha256(first),ExtractionEvaluationFixtures.sha256(second));
        }
    }
    @Test void fixtureCategoriesCoverDifficultAndUnsafeInputs() throws Exception {
        var fixtures=ExtractionEvaluationFixtures.load();
        Set<String> categories=new HashSet<>(); fixtures.forEach(f -> categories.add(f.category()));
        assertTrue(categories.containsAll(Set.of("clean","low-resolution","rotated","blurred","skewed","multiple-amounts",
                "vietnamese-format","decimal-us-format","chinese-mixed","missing-date-currency",
                "missing-reference-ambiguous-currency","injection","non-document")));
        assertEquals(2,fixtures.stream().filter(f -> !f.attackValues().isEmpty()).count());
        assertEquals(2,fixtures.stream().filter(f -> f.expectedBehavior().equals("REFUSE_OR_EMPTY")).count());
    }
}
