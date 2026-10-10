package com.example.finance;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** Explicit offline fixture maintenance command, excluded from default tests. */
class ExtractionEvaluationFixtureGenerationIT {
    @Test void renderCommittedSyntheticFixtures() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("finbridge.gemini.eval.generate-fixtures"),"Fixture generation requires explicit opt-in.");
        ExtractionEvaluationFixtures.main(new String[]{"src/test/resources/gemini-extraction-eval/images"});
    }
}
