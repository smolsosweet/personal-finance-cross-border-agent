package com.example.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.mock.web.MockMultipartFile;

/** Not a *Test: never selected by the default Maven/CI suite. Explicit opt-in and existing env key required. */
class GeminiExtractionEvaluationLiveIT {
    @Test void evaluateExistingProductionExtractor() throws Exception {
        Assumptions.assumeFalse("true".equalsIgnoreCase(System.getenv("CI"))
                || "true".equalsIgnoreCase(System.getenv("GITHUB_ACTIONS")),"Live evaluation is excluded from CI.");
        var skip=ExtractionEvaluationRunner.skipReason(Boolean.getBoolean("finbridge.gemini.eval"),
                () -> System.getenv("GEMINI_API_KEY")!=null && !System.getenv("GEMINI_API_KEY").isBlank());
        if(skip.isPresent()) { System.out.println(skip.get()); Assumptions.assumeTrue(false,skip.get()); }

        // Read only the same config properties as the application's existing extraction service.
        StandardEnvironment environment=new StandardEnvironment();
        environment.getPropertySources().addLast(new ResourcePropertySource("classpath:application.properties"));
        String profiles=environment.getProperty("spring.profiles.active","");
        for(String profile:profiles.split(",")) if(profile.trim().matches("[A-Za-z0-9_-]+")) {
            String resource="classpath:application-"+profile.trim()+".properties";
            if(new org.springframework.core.io.ClassPathResource("application-"+profile.trim()+".properties").exists())
                environment.getPropertySources().addBefore("class path resource [application.properties]",new ResourcePropertySource(resource));
        }
        String model=environment.getProperty("finbridge.document-ai.model","gemini-3.5-flash-lite");
        if(!model.matches("gemini-[A-Za-z0-9._-]+")) throw new IllegalArgumentException("Invalid configured document model; zero model calls.");
        Duration connectTimeout=org.springframework.boot.convert.DurationStyle.detectAndParse(
                environment.getProperty("finbridge.document-ai.connect-timeout","10s"));
        var options=new ExtractionEvaluationRunner.Options(Integer.getInteger("finbridge.gemini.eval.cap",25),
                Integer.getInteger("finbridge.gemini.eval.repeat",1),Long.getLong("finbridge.gemini.eval.delay-ms",1500L));
        var fixtures=ExtractionEvaluationFixtures.load();
        Map<String,byte[]> images=new LinkedHashMap<>();
        for(var fixture:fixtures) images.put(fixture.id(),ExtractionEvaluationFixtures.committedImage(fixture));
        ObjectMapper mapper=new ObjectMapper();
        var service=new GeminiDocumentExtractionService(mapper,System.getenv("GEMINI_API_KEY"),model,connectTimeout);
        Path root=Path.of("target","gemini-extraction-eval").toAbsolutePath().normalize();
        String runId=Instant.now().toString().replace(':','-')+"-"+UUID.randomUUID();
        Path directory=Files.createDirectories(root.resolve(runId));
        Map<String,Object> metadata=new LinkedHashMap<>();
        metadata.put("runId",runId); metadata.put("startedAtUtc",Instant.now().toString());
        metadata.put("commit",git("rev-parse","HEAD")); metadata.put("trackedWorktree",git("status","--porcelain","--untracked-files=no"));
        metadata.put("configuredModel",model); metadata.put("instructionSha256",instructionHashes());
        metadata.put("groundTruthSha256",ExtractionEvaluationFixtures.sha256(
                Files.readAllBytes(Path.of("src/test/resources/gemini-extraction-eval/ground-truth.json"))));
        metadata.put("callCap",options.callCap()); metadata.put("repeat",options.repeat()); metadata.put("delayMs",options.delayMs());
        metadata.put("resultBoundary","production validated draft; raw provider response is not exposed");
        Map<String,String> imageHashes=new LinkedHashMap<>();
        images.forEach((id,bytes) -> imageHashes.put(id,ExtractionEvaluationFixtures.sha256(bytes)));
        metadata.put("imageSha256",imageHashes);
        System.out.println("Starting bounded synthetic extraction evaluation: "+fixtures.size()+" images, cap "+options.callCap()+"; no payment actions.");
        var run=ExtractionEvaluationRunner.run(fixtures,options,fixture -> service.extract(
                new MockMultipartFile("file",fixture.id()+".png","image/png",images.get(fixture.id())),fixture.kind()).fields(),Thread::sleep);
        metadata.put("finishedAtUtc",Instant.now().toString()); metadata.put("attemptedCalls",run.observations().size());
        metadata.put("plannedCalls",run.plannedCalls()); metadata.put("stopReason",run.stopReason());
        mapper.writerWithDefaultPrettyPrinter().writeValue(directory.resolve("raw-results.json").toFile(),Map.of("metadata",metadata,"results",run.observations()));
        String report=ExtractionEvaluationReport.markdown(metadata,ExtractionEvaluationScorer.scoreAll(fixtures,run.observations()),run);
        Files.writeString(directory.resolve("report.md"),report,StandardCharsets.UTF_8);
        System.out.println("Evaluation calls: "+run.observations().size()+". Report: "+directory.resolve("report.md"));
    }

    private static Map<String,String> instructionHashes() {
        Map<String,String> hashes=new LinkedHashMap<>();
        try {
            var prompt=GeminiDocumentExtractionService.class.getDeclaredMethod("prompt",String.class); prompt.setAccessible(true);
            for(String kind:List.of("transaction","student-bill"))
                hashes.put(kind,ExtractionEvaluationFixtures.sha256(((String)prompt.invoke(null,kind)).getBytes(StandardCharsets.UTF_8)));
        } catch(ReflectiveOperationException | RuntimeException unavailable) { hashes.put("status","not captured"); }
        return hashes;
    }
    private static String git(String... arguments) {
        try {
            List<String> command=new ArrayList<>(List.of("git")); command.addAll(List.of(arguments));
            Process process=new ProcessBuilder(command).redirectErrorStream(true).start();
            String result=new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8).trim();
            return process.waitFor()==0?(result.isEmpty()?"clean":result):"not captured";
        } catch(Exception unavailable) { return "not captured"; }
    }
}
