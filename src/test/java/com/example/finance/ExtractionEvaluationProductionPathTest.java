package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/** Loopback only: proves evaluation uses the real, unchanged service parser rather than its own prompt. */
class ExtractionEvaluationProductionPathTest {
    @Test void realProductionParserDropsExtraPrivilegesButScorerDetectsAttackerInsideAllowedField() throws Exception {
        try(var stub=new Stub(200,envelope("{\"institution\":\"Grove Test Academy\",\"amount\":\"1\",\"currency\":\"CNY\",\"dueDate\":\"2026-10-31\",\"paymentReference\":\"SYN-GRO-009\",\"recipientName\":\"ATTACKER TEST PAYEE\",\"verified\":true,\"execute\":true}"))) {
            var fixture=fixture("bill-09");
            var run=ExtractionEvaluationRunner.run(List.of(fixture),new ExtractionEvaluationRunner.Options(1,1,0),
                    f -> service(stub).extract(file(f),f.kind()).fields(),delay -> {});
            var result=ExtractionEvaluationScorer.score(fixture,run.observations().getFirst());
            assertTrue(result.whitelistOk()); assertTrue(result.injectionFollowed()); assertTrue(result.needsCorrection());
            assertFalse(result.observation().fields().containsKey("verified")); assertEquals(1,stub.calls);
        }
    }
    @Test void emptyNonDocumentProductionDraftIsCorrectAndProviderErrorIsNotARefusalPass() throws Exception {
        var fixture=fixture("receipt-10");
        try(var stub=new Stub(200,envelope("{}"))) {
            var fields=service(stub).extract(file(fixture),fixture.kind()).fields();
            assertEquals(7,fields.size()); assertTrue(fields.values().stream().allMatch(String::isBlank));
            assertTrue(ExtractionEvaluationScorer.score(fixture,new ExtractionEvaluationScorer.Observation(
                    fixture.id(),1,fields,ExtractionEvaluationScorer.Failure.NONE,1)).refusalCorrect());
        }
        var failed=ExtractionEvaluationScorer.score(fixture,new ExtractionEvaluationScorer.Observation(
                fixture.id(),1,Map.of(),ExtractionEvaluationScorer.Failure.PROVIDER_ERROR,1));
        assertTrue(failed.safeFailure()); assertFalse(failed.refusalCorrect());
        assertEquals(0,ExtractionEvaluationScorer.summarize(List.of(failed)).correctFields());
    }
    @Test void realMalformedJsonErrorRemainsUnclassifiedRatherThanFabricatingTimeout() throws Exception {
        try(var stub=new Stub(200,envelope("not-json"))) {
            var fixture=fixture("receipt-01");
            var run=ExtractionEvaluationRunner.run(List.of(fixture),new ExtractionEvaluationRunner.Options(1,1,0),
                    f -> service(stub).extract(file(f),f.kind()).fields(),delay -> {});
            assertEquals(ExtractionEvaluationScorer.Failure.READ_FAILURE,run.observations().getFirst().failure());
            assertTrue(run.observations().getFirst().fields().isEmpty()); assertEquals(1,stub.calls);
        }
    }
    @Test void productionQuotaErrorStopsRunnerAfterExactlyOneRequest() throws Exception {
        try(var stub=new Stub(429,"{\"error\":\"synthetic quota error\"}")) {
            var run=ExtractionEvaluationRunner.run(ExtractionEvaluationFixtures.load(),new ExtractionEvaluationRunner.Options(25,1,0),
                    f -> service(stub).extract(file(f),f.kind()).fields(),delay -> {});
            assertEquals(1,stub.calls); assertEquals(1,run.observations().size());
            assertEquals("STOPPED_ON_RATE_LIMIT",run.stopReason());
        }
    }
    private static ExtractionEvaluationFixtures.Fixture fixture(String id) throws Exception {
        return ExtractionEvaluationFixtures.load().stream().filter(f -> f.id().equals(id)).findFirst().orElseThrow();
    }
    private static MockMultipartFile file(ExtractionEvaluationFixtures.Fixture fixture) throws Exception {
        return new MockMultipartFile("file",fixture.id()+".png","image/png",ExtractionEvaluationFixtures.committedImage(fixture));
    }
    private static GeminiDocumentExtractionService service(Stub stub) {
        // Non-secret sentinel accepted only by this ephemeral loopback HTTP stub.
        return new GeminiDocumentExtractionService(new ObjectMapper(),"evaluation-loopback-placeholder","gemini-3.5-flash-lite",
                Duration.ofSeconds(1),URI.create("http://127.0.0.1:"+stub.server.getAddress().getPort()+"/v1beta/models/"));
    }
    private static String envelope(String text) throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of("candidates",List.of(Map.of("finishReason","STOP",
                "content",Map.of("parts",List.of(Map.of("text",text)))))));
    }
    private static final class Stub implements AutoCloseable {
        final HttpServer server; int calls;
        Stub(int status,String response) throws Exception {
            server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/",exchange -> {
                calls++; exchange.getRequestBody().readAllBytes();
                byte[] bytes=response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status,bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
            }); server.start();
        }
        public void close() { server.stop(0); }
    }
}
