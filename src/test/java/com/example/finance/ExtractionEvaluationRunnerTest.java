package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import java.net.http.HttpTimeoutException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ExtractionEvaluationRunnerTest {
    @Test void noOptInSkipsWithoutInspectingKeyOrCallingClient() {
        var skip=ExtractionEvaluationRunner.skipReason(false,() -> { fail("Must not inspect key when opt-in is absent"); return false; });
        assertTrue(skip.orElseThrow().contains("zero model calls"));
    }
    @Test void optInWithoutKeySkipsCleanly() {
        assertTrue(ExtractionEvaluationRunner.skipReason(true,() -> false).orElseThrow().contains("absent"));
        assertTrue(ExtractionEvaluationRunner.skipReason(true,() -> true).isEmpty());
    }
    @Test void capEnforcedAgainstStubEvenWithRepeats() throws Exception {
        AtomicInteger calls=new AtomicInteger(); List<Long> sleeps=new ArrayList<>();
        var run=ExtractionEvaluationRunner.run(ExtractionEvaluationFixtures.load(),new ExtractionEvaluationRunner.Options(25,3,50),
                f -> { calls.incrementAndGet(); return Map.of(); },sleeps::add);
        assertEquals(25,calls.get()); assertEquals(25,run.observations().size()); assertEquals(60,run.plannedCalls());
        assertEquals(24,sleeps.size()); assertEquals(50,sleeps.getFirst());
        assertEquals("CALL_CAP_REACHED",run.stopReason()); assertEquals(2,run.observations().get(20).repeat());
    }
    @Test void defaultOneRunPerImageIsSequentialAndDoesNotSleepBeforeFirstCall() throws Exception {
        List<String> ids=new ArrayList<>(); AtomicInteger sleeps=new AtomicInteger();
        var run=ExtractionEvaluationRunner.run(ExtractionEvaluationFixtures.load(),new ExtractionEvaluationRunner.Options(25,1,0),
                f -> { ids.add(f.id()); return Map.of(); },delay -> sleeps.incrementAndGet());
        assertEquals(20,ids.size()); assertEquals("receipt-01",ids.getFirst()); assertEquals("bill-10",ids.getLast());
        assertEquals(19,sleeps.get()); assertEquals("COMPLETED",run.stopReason());
    }
    @Test void invalidCapRepeatsAndDelayFailBeforeClientCalls() {
        assertThrows(IllegalArgumentException.class,() -> new ExtractionEvaluationRunner.Options(26,1,0));
        assertThrows(IllegalArgumentException.class,() -> new ExtractionEvaluationRunner.Options(0,1,0));
        assertThrows(IllegalArgumentException.class,() -> new ExtractionEvaluationRunner.Options(25,0,0));
        assertThrows(IllegalArgumentException.class,() -> new ExtractionEvaluationRunner.Options(25,1,-1));
    }
    @Test void providerQuotaAndAuthenticationStopWithoutRetry() throws Exception {
        for(String error:List.of("Gemini quota or rate limit reached. Try again later.","Gemini rejected the API key. Check GEMINI_API_KEY in the server environment.")) {
            AtomicInteger calls=new AtomicInteger();
            var run=ExtractionEvaluationRunner.run(ExtractionEvaluationFixtures.load(),new ExtractionEvaluationRunner.Options(25,1,0),
                    f -> { calls.incrementAndGet(); throw new IllegalStateException(error); },delay -> {});
            assertEquals(1,calls.get()); assertEquals(1,run.observations().size()); assertTrue(run.stopReason().startsWith("STOPPED_ON_"));
        }
    }
    @Test void parseFailureTimeoutAndProviderErrorsAreCategorizedWithoutRetainingMessages() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        var run=ExtractionEvaluationRunner.run(ExtractionEvaluationFixtures.load(),new ExtractionEvaluationRunner.Options(3,1,0),f -> {
            switch(calls.incrementAndGet()) {
                case 1 -> throw new IllegalStateException("Gemini returned invalid structured data.");
                case 2 -> throw new HttpTimeoutException("arbitrary sensitive body must not persist");
                default -> throw new IllegalStateException("Gemini is temporarily unavailable. Try again later.");
            }
        },delay -> {});
        assertEquals(List.of(ExtractionEvaluationScorer.Failure.PARSE_FAILURE,ExtractionEvaluationScorer.Failure.TIMEOUT,ExtractionEvaluationScorer.Failure.PROVIDER_ERROR),
                run.observations().stream().map(ExtractionEvaluationScorer.Observation::failure).toList());
        assertTrue(run.observations().stream().allMatch(o -> o.fields().isEmpty()));
        assertFalse(run.toString().contains("sensitive"));
        assertEquals(ExtractionEvaluationScorer.Failure.READ_FAILURE,ExtractionEvaluationRunner.classify(
                new IllegalStateException("Could not read this receipt. Try a clearer JPG, PNG or WEBP image.")));
    }
    @Test void interruptedPacingDoesNotCreateAnUnexecutedPass() throws Exception {
        try {
            var run=ExtractionEvaluationRunner.run(ExtractionEvaluationFixtures.load(),new ExtractionEvaluationRunner.Options(25,1,1),
                    f -> Map.of(),delay -> { throw new InterruptedException(); });
            assertEquals(1,run.observations().size()); assertEquals("INTERRUPTED_BEFORE_CALL",run.stopReason());
        } finally { Thread.interrupted(); }
    }
}
