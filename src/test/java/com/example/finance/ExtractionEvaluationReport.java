package com.example.finance;

import java.util.*;

/** Markdown report from scored observations only; never reads credentials or provider bodies. */
final class ExtractionEvaluationReport {
    static String markdown(Map<String,Object> metadata, List<ExtractionEvaluationScorer.ImageScore> scores,
                           ExtractionEvaluationRunner.Run run) {
        StringBuilder out = new StringBuilder("# Gemini extraction evaluation\n\n");
        metadata.forEach((key,value) -> out.append("- ").append(key).append(": ").append(value).append('\n'));
        out.append("\n## Overall\n\n");
        table(out,Map.of("All attempted calls",scores));
        out.append("\n## By kind\n\n"); table(out,group(scores,false));
        out.append("\n## By category\n\n"); table(out,group(scores,true));
        out.append("\n## Per-field exact accuracy\n\n| Kind / field | Correct / assessed | Exact accuracy | Hallucinated / truth-absent |\n| --- | --- | --- | --- |\n");
        Map<String,List<ExtractionEvaluationScorer.FieldResult>> fields = new TreeMap<>();
        for (var score:scores) for (var field:score.fields())
            fields.computeIfAbsent(score.fixture().kind()+" / "+field.field(),key -> new ArrayList<>()).add(field);
        fields.forEach((key,values) -> {
            long correct=values.stream().filter(ExtractionEvaluationScorer.FieldResult::correct).count();
            long absent=values.stream().filter(f -> ExtractionEvaluationScorer.blank(f.expected())).count();
            long hallucinated=values.stream().filter(ExtractionEvaluationScorer.FieldResult::hallucinated).count();
            out.append("| ").append(key).append(" | ").append(correct).append('/').append(values.size())
                    .append(" | ").append(rate(correct,values.size())).append(" | ").append(hallucinated).append('/').append(absent).append(" |\n");
        });
        var summary=ExtractionEvaluationScorer.summarize(scores);
        out.append("\n## Failure handling, injection and refusal\n\n")
                .append("- Safe failure: ").append(summary.safeFailures()).append('/').append(summary.failures())
                .append(" (").append(rate(summary.safeFailures(),summary.failures())).append("); errors ending without draft fields.\n")
                .append("- Unsafe output boundary violations: ").append(summary.unsafeOutputs()).append(".\n")
                .append("- Non-document refusal/empty correctness: ").append(summary.correctRefusals()).append('/').append(summary.refusalCalls())
                .append(" (").append(rate(summary.correctRefusals(),summary.refusalCalls())).append("); infrastructure/parser errors are inconclusive, never successful refusals.\n")
                .append("- Injection marker followed: ").append(summary.injectionFollowed()).append('/').append(summary.injectionCalls())
                .append("; injection calls ending in errors: ").append(summary.injectionErrors()).append(".\n\n")
                .append("| Image / repeat | Injection outcome | Whitelist | Expected extraction exact |\n| --- | --- | --- | --- |\n");
        for (var score:scores) if (!score.fixture().attackValues().isEmpty()) {
            String outcome=score.injectionFollowed()?"FOLLOWED":score.observation().failure()!=ExtractionEvaluationScorer.Failure.NONE
                    ?"NOT OBSERVED (call failed)":"MARKERS ABSENT";
            out.append("| ").append(score.fixture().id()).append(" / ").append(score.observation().repeat()).append(" | ")
                    .append(outcome).append(" | ").append(score.whitelistOk()?"PASS":"FAIL")
                    .append(" | ").append(!score.needsCorrection()).append(" |\n");
        }
        out.append("\n## Latency and execution\n\n")
                .append("- p50: ").append(summary.p50Ms()).append(" ms; p95: ").append(summary.p95Ms()).append(" ms (nearest rank, errors included).\n")
                .append("- Attempted extraction calls: ").append(summary.calls()).append(" / planned ").append(run.plannedCalls()).append(".\n")
                .append("- Unexecuted planned calls: ").append(run.plannedCalls()-summary.calls()).append("; stop reason: ").append(run.stopReason()).append(".\n");
        out.append("\n## Per-image results\n\n| Image / repeat | Kind | Category | Expected behavior | Field exact | Hallucinations | Needs correction | Failure | Refusal correct | Whitelist | Latency ms |\n| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |\n");
        for(var score:scores) {
            long correct=score.fields().stream().filter(ExtractionEvaluationScorer.FieldResult::correct).count();
            long hallucinations=score.fields().stream().filter(ExtractionEvaluationScorer.FieldResult::hallucinated).count();
            out.append("| ").append(score.fixture().id()).append(" / ").append(score.observation().repeat()).append(" | ")
                    .append(score.fixture().kind()).append(" | ").append(score.fixture().category()).append(" | ")
                    .append(score.fixture().expectedBehavior()).append(" | ").append(correct).append('/').append(score.fields().size())
                    .append(" | ").append(hallucinations).append(" | ").append(score.needsCorrection()).append(" | ")
                    .append(score.observation().failure()).append(" | ").append("REFUSE_OR_EMPTY".equals(score.fixture().expectedBehavior())
                            ?score.observation().failure()==ExtractionEvaluationScorer.Failure.NONE?score.refusalCorrect():"INCONCLUSIVE":"N/A")
                    .append(" | ").append(score.whitelistOk()).append(" | ").append(score.observation().latencyMs()).append(" |\n");
        }
        out.append("\n## Failures: expected versus actual\n\n");
        int failures=0;
        for (var score:scores) {
            boolean refusalFailed="REFUSE_OR_EMPTY".equals(score.fixture().expectedBehavior()) && !score.refusalCorrect();
            if (!score.needsCorrection() && score.whitelistOk() && !score.injectionFollowed() && !refusalFailed
                    && score.observation().failure()==ExtractionEvaluationScorer.Failure.NONE) continue;
            failures++;
            out.append("### ").append(score.fixture().id()).append(" / repeat ").append(score.observation().repeat()).append("\n\n")
                    .append("Failure category: ").append(score.observation().failure()).append(".\n\n")
                    .append("| Field | Expected | Actual | Correct |\n| --- | --- | --- | --- |\n");
            for (var field:score.fields()) out.append("| ").append(field.field()).append(" | ").append(cell(field.expected()))
                    .append(" | ").append(cell(field.actual())).append(" | ").append(field.correct()).append(" |\n");
            out.append("\nReturned draft fields: ").append(cell(score.observation().fields().toString())).append("\n\n");
        }
        if(failures==0) out.append("No failures observed in executed calls. Unexecuted calls are not passes.\n");
        out.append("\n## Limitations\n\n")
                .append("Small synthetic sample, single model, not statistically significant, not a benchmark; results describe this test set only.\n\n")
                .append("These are drafts after the existing production parser/whitelist, not raw model JSON. Extra provider fields removed by production cannot be observed; whitelist PASS applies only at this boundary. Receipt accuracy covers merchant, amount, currency and occurredAt; paymentMethod/category/reference remain visible in results and injection/refusal checks but are not accuracy-scored.\n\n")
                .append("A safe error is not accurate extraction. Production masks some JSON/network/timeout causes as READ_FAILURE; the harness does not guess them. Safe failure means empty output at the service boundary, not a proof of all downstream financial safety. Unknown attack variants are not covered by marker detection.\n\n")
                .append("Accuracy includes null-truth fields; read per-field and refusal results rather than relying only on the aggregate. A failed call never earns field-accuracy credit even for null-truth fields. Correction rate uses document calls only, including failed document calls. Repeat mode weighs observations, not unique images. Correct refusal means a successful empty draft; exceptions are inconclusive for refusal and are scored separately as safe failures. Latency includes validation/network and failures but excludes configured pacing.\n\n")
                .append("The configured model may be an alias; the resolved provider model version is not exposed by the service. Rendering is deterministic for the same JDK, fonts and platform; committed PNG bytes are used for cross-machine evaluation. No application/UI/payment end-to-end acceptance or production-data claim is made. No prompt was tuned.\n");
        return out.toString();
    }

    private static Map<String,List<ExtractionEvaluationScorer.ImageScore>> group(List<ExtractionEvaluationScorer.ImageScore> scores, boolean category) {
        Map<String,List<ExtractionEvaluationScorer.ImageScore>> result=new TreeMap<>();
        for(var score:scores) result.computeIfAbsent(category?score.fixture().category():score.fixture().kind(),key -> new ArrayList<>()).add(score);
        return result;
    }
    private static void table(StringBuilder out, Map<String,List<ExtractionEvaluationScorer.ImageScore>> groups) {
        out.append("| Group | Calls | Field exact | Hallucinated / absent | Needs correction / documents | Safe failures / errors | Refusal correct | p50 / p95 ms |\n| --- | --- | --- | --- | --- | --- | --- | --- |\n");
        groups.forEach((key,values) -> {
            var s=ExtractionEvaluationScorer.summarize(values);
            out.append("| ").append(key).append(" | ").append(s.calls()).append(" | ").append(rate(s.correctFields(),s.totalFields()))
                    .append(" (").append(s.correctFields()).append('/').append(s.totalFields()).append(") | ")
                    .append(rate(s.hallucinations(),s.absentFields())).append(" (").append(s.hallucinations()).append('/').append(s.absentFields()).append(") | ")
                    .append(rate(s.correctionDrafts(),s.documentCalls())).append(" (").append(s.correctionDrafts()).append('/').append(s.documentCalls()).append(") | ")
                    .append(rate(s.safeFailures(),s.failures())).append(" (").append(s.safeFailures()).append('/').append(s.failures()).append(") | ")
                    .append(rate(s.correctRefusals(),s.refusalCalls())).append(" | ").append(s.p50Ms()).append(" / ").append(s.p95Ms()).append(" |\n");
        });
    }
    static String rate(long numerator,long denominator) {
        return denominator==0?"N/A":String.format(Locale.ROOT,"%.1f%%",100.0*numerator/denominator);
    }
    private static String cell(String value) {
        return value==null?"(null)":value.isBlank()?"(empty)":value.replace("|","\\|").replace("\r"," ").replace("\n"," ");
    }
}
