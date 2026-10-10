package com.example.finance;

import java.net.http.HttpTimeoutException;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Sequential bounded runner. Environment access and production wiring live in the explicit IT entry point. */
final class ExtractionEvaluationRunner {
    static final int MAX_CALLS = 25;
    record Options(int callCap, int repeat, long delayMs) {
        Options {
            if (callCap < 1 || callCap > MAX_CALLS) throw new IllegalArgumentException("Call cap must be 1..25");
            if (repeat < 1 || repeat > MAX_CALLS) throw new IllegalArgumentException("Repeat must be 1..25");
            if (delayMs < 0 || delayMs > 60000) throw new IllegalArgumentException("Delay must be 0..60000 ms");
        }
    }
    @FunctionalInterface interface Client { Map<String,String> extract(ExtractionEvaluationFixtures.Fixture fixture) throws Exception; }
    @FunctionalInterface interface Sleeper { void sleep(long delayMs) throws InterruptedException; }
    record Run(List<ExtractionEvaluationScorer.Observation> observations, int plannedCalls, String stopReason) {}

    static Optional<String> skipReason(boolean optedIn, BooleanSupplier keyPresent) {
        if (!optedIn) return Optional.of("Skipped: finbridge.gemini.eval=true is required; zero model calls.");
        if (!keyPresent.getAsBoolean()) return Optional.of("Skipped: the application's GEMINI_API_KEY is absent; zero model calls.");
        return Optional.empty();
    }

    static Run run(List<ExtractionEvaluationFixtures.Fixture> fixtures, Options options, Client client, Sleeper sleeper) {
        List<ExtractionEvaluationScorer.Observation> observations = new ArrayList<>();
        String reason = "COMPLETED";
        outer: for (int repeat=1; repeat<=options.repeat(); repeat++) {
            for (var fixture : fixtures) {
                if (observations.size() >= options.callCap()) { reason="CALL_CAP_REACHED"; break outer; }
                if (!observations.isEmpty()) {
                    try { sleeper.sleep(options.delayMs()); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); reason="INTERRUPTED_BEFORE_CALL"; break outer; }
                }
                long start = System.nanoTime();
                Map<String,String> fields = Map.of();
                var failure = ExtractionEvaluationScorer.Failure.NONE;
                try {
                    fields = client.extract(fixture);
                    if (fields == null) { fields=Map.of(); failure=ExtractionEvaluationScorer.Failure.PARSE_FAILURE; }
                } catch (Exception error) {
                    failure=classify(error);
                    if (error instanceof InterruptedException) Thread.currentThread().interrupt();
                }
                observations.add(new ExtractionEvaluationScorer.Observation(fixture.id(),repeat,fields,failure,
                        (System.nanoTime()-start)/1_000_000));
                if (failure==ExtractionEvaluationScorer.Failure.RATE_LIMIT || failure==ExtractionEvaluationScorer.Failure.AUTHENTICATION
                        || failure==ExtractionEvaluationScorer.Failure.INTERRUPTED) {
                    reason="STOPPED_ON_"+failure; break outer;
                }
            }
        }
        return new Run(List.copyOf(observations),fixtures.size()*options.repeat(),reason);
    }

    static ExtractionEvaluationScorer.Failure classify(Exception error) {
        if (error instanceof HttpTimeoutException) return ExtractionEvaluationScorer.Failure.TIMEOUT;
        if (error instanceof InterruptedException) return ExtractionEvaluationScorer.Failure.INTERRUPTED;
        String message=error.getMessage()==null?"":error.getMessage();
        if (message.contains("quota or rate limit")) return ExtractionEvaluationScorer.Failure.RATE_LIMIT;
        if (message.contains("rejected the API key")) return ExtractionEvaluationScorer.Failure.AUTHENTICATION;
        if (message.contains("temporarily unavailable") || message.contains("rejected the image request"))
            return ExtractionEvaluationScorer.Failure.PROVIDER_ERROR;
        if (message.contains("interrupted")) return ExtractionEvaluationScorer.Failure.INTERRUPTED;
        if (message.contains("invalid structured data") || message.contains("complete extraction")
                || message.contains("no extracted fields")) return ExtractionEvaluationScorer.Failure.PARSE_FAILURE;
        // The production service masks several errors (including timeout/malformed JSON) with one message.
        // Do not guess a cause or persist the exception/provider body.
        return ExtractionEvaluationScorer.Failure.READ_FAILURE;
    }
}
