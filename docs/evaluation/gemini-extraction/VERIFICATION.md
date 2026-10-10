# Extraction evaluation harness: implementation and verification

Date: 2026-10-10, Asia/Bangkok. Baseline application commit: `4650a59`.
The local commit containing this report adds evaluation tooling only.

## 1. Existing production path

`src/test/java/com/example/finance/GeminiExtractionEvaluationLiveIT.java` constructs the existing service through its public constructor at `src/main/java/com/example/finance/GeminiDocumentExtractionService.java:36` and calls `extract(MultipartFile, String)` at line 55. That method invokes the existing prompt at line 68 and production parser/whitelist at line 87; the parser starts at line 167. The original instruction is reachable through `prompt(String)` at line 99, so the runner records its SHA-256 by reflection without copying or modifying it.

No controller, database, save, receipt capability, approval or payment endpoint is invoked by the live evaluation. Four offline HTTP tests exercise the same service against a loopback stub. Their sentinel is not an API credential.

## 2. Files added (one line per file)

- `src/test/java/com/example/finance/ExtractionEvaluationFixtures.java` — deterministic JDK rendering and resource loading.
- `src/test/java/com/example/finance/ExtractionEvaluationFixtureGenerationIT.java` — explicitly selected fixture regeneration.
- `src/test/java/com/example/finance/ExtractionEvaluationScorer.java` — pure field and safety scoring.
- `src/test/java/com/example/finance/ExtractionEvaluationRunner.java` — sequential bounded execution and lazy opt-in gate.
- `src/test/java/com/example/finance/ExtractionEvaluationReport.java` — Markdown reports.
- `src/test/java/com/example/finance/GeminiExtractionEvaluationLiveIT.java` — opt-in production-service wiring and run metadata.
- `src/test/java/com/example/finance/ExtractionEvaluationFixturesTest.java` — fixture coverage and deterministic bytes.
- `src/test/java/com/example/finance/ExtractionEvaluationScorerTest.java` — canned correctness and safety cases.
- `src/test/java/com/example/finance/ExtractionEvaluationRunnerTest.java` — gate, cap, pacing and failure cases.
- `src/test/java/com/example/finance/ExtractionEvaluationProductionPathTest.java` — production parser with loopback responses.
- `src/test/resources/gemini-extraction-eval/ground-truth.json` — twenty synthetic templates and expected fields.
- `src/test/resources/gemini-extraction-eval/images/receipt-01.png` — clean receipt.
- `src/test/resources/gemini-extraction-eval/images/receipt-02.png` — low-resolution receipt.
- `src/test/resources/gemini-extraction-eval/images/receipt-03.png` — rotated receipt.
- `src/test/resources/gemini-extraction-eval/images/receipt-04.png` — several receipt amounts.
- `src/test/resources/gemini-extraction-eval/images/receipt-05.png` — Vietnamese receipt formats.
- `src/test/resources/gemini-extraction-eval/images/receipt-06.png` — US decimal and date formats.
- `src/test/resources/gemini-extraction-eval/images/receipt-07.png` — mixed Chinese receipt.
- `src/test/resources/gemini-extraction-eval/images/receipt-08.png` — missing receipt date and currency.
- `src/test/resources/gemini-extraction-eval/images/receipt-09.png` — receipt with injected instructions.
- `src/test/resources/gemini-extraction-eval/images/receipt-10.png` — non-document for receipt extraction.
- `src/test/resources/gemini-extraction-eval/images/bill-01.png` — clean school bill.
- `src/test/resources/gemini-extraction-eval/images/bill-02.png` — blurred bill.
- `src/test/resources/gemini-extraction-eval/images/bill-03.png` — skewed bill.
- `src/test/resources/gemini-extraction-eval/images/bill-04.png` — several bill amounts and dates.
- `src/test/resources/gemini-extraction-eval/images/bill-05.png` — Vietnamese bill formats.
- `src/test/resources/gemini-extraction-eval/images/bill-06.png` — US decimal and date formats.
- `src/test/resources/gemini-extraction-eval/images/bill-07.png` — mixed Chinese bill.
- `src/test/resources/gemini-extraction-eval/images/bill-08.png` — missing reference and ambiguous currency.
- `src/test/resources/gemini-extraction-eval/images/bill-09.png` — bill with injected instructions.
- `src/test/resources/gemini-extraction-eval/images/bill-10.png` — non-document for bill extraction.
- `docs/evaluation/gemini-extraction/README.md` — method, twenty-row fixture catalogue, metrics, commands and limitations.
- `docs/evaluation/gemini-extraction/VERIFICATION.md` — this offline evidence record.

## 3. Fixture list and scoring

The [fixture catalogue](README.md#fixture-set) lists every ID, kind, category and expected behavior. There are ten `transaction` and ten `student-bill` inputs: sixteen `EXTRACT`, two `EXTRACT_IGNORING_INJECTION`, and two `REFUSE_OR_EMPTY`. All twenty PNGs were generated and decoded successfully. Generation is byte-deterministic for the same templates, JDK, fonts and platform; evaluation reads the committed bytes.

Amounts compare with `BigDecimal.compareTo`; currency is trimmed and uppercased; dates compare as ISO `LocalDate`. Text uses Unicode NFD, removes combining marks, lowercases with `Locale.ROOT`, maps Vietnamese U+0111 to `d`, and collapses Unicode whitespace. Punctuation remains significant. Null truth matches absent/null/blank output; populated null-truth fields are hallucinations. Failed calls never earn exact-accuracy credit, even for null truth.

The report includes correction rate, safe failures, injection markers, whitelist results, non-document refusal, nearest-rank latency p50/p95 and attempted-call counts. Provider/parser errors are **INCONCLUSIVE** for non-document refusal, not successful refusals. The service masks some timeout and malformed-JSON causes as one read error; these stay `READ_FAILURE` rather than a guessed cause. Observations are post-production-validation drafts, not raw Google responses. See [scoring rules](README.md#scoring-rules) for denominators and limitations.

## 4. Commands and observed results

### Fixture generation

```powershell
mvn -B '-Dtest=ExtractionEvaluationFixtureGenerationIT' '-Dfinbridge.gemini.eval.generate-fixtures=true' test
```

First attempt: 1 error because the chosen font lacked the Vietnamese currency glyph. Only the newly added renderer was corrected to use deterministic per-line font fallback. Second attempt: **1 test, 0 failures, 0 errors, 0 skips**, twenty generated PNGs. Completed 20:51:50 +07:00.

### New offline tests

```powershell
mvn -B '-Dtest=ExtractionEvaluationScorerTest,ExtractionEvaluationRunnerTest,ExtractionEvaluationFixturesTest,ExtractionEvaluationProductionPathTest' test
```

Completed 20:55:46 +07:00: **29 tests, 0 failures, 0 errors, 0 skips**.

| Class | Tests | Failures | Errors | Skips |
| --- | ---: | ---: | ---: | ---: |
| ExtractionEvaluationFixturesTest | 3 | 0 | 0 | 0 |
| ExtractionEvaluationScorerTest | 14 | 0 | 0 | 0 |
| ExtractionEvaluationRunnerTest | 8 | 0 | 0 | 0 |
| ExtractionEvaluationProductionPathTest | 4 | 0 | 0 | 0 |

### Explicit live entry point with opt-in disabled

```powershell
mvn -B '-Dtest=GeminiExtractionEvaluationLiveIT' '-Dfinbridge.gemini.eval=false' test
```

Completed 20:56:09 +07:00: **1 test, 0 failures, 0 errors, 1 skip**. The opt-in gate skips before accessing the key. Missing-key behavior was separately tested with a stub BooleanSupplier, without reading any environment key.

### Default regression, live Gemini disabled

```powershell
$env:FINBRIDGE_GEMINI_LIVE_SMOKE='false'
mvn -B '-Dsurefire.excludes=**/PaginationGeometryDiagnosticTest.java,**/ScreenshotEvidenceCollectorTest.java' test
```

Completed 21:03:39 +07:00, duration 6m50s: **462 tests, 460 passed, 1 failure, 0 errors, 1 skip; BUILD FAILURE**. All 29 new offline tests passed within this run. The skipped test was the existing opt-in `GeminiProviderLiveSmokeTest`. Neither new `*IT` entry point is selected by default.

The two exclusions are pre-existing **untracked** diagnostic/review classes, not maintained regression tests. The screenshot collector writes into review documentation; it was not run so those unrelated artifacts could be preserved. No test assertion, existing source file, skip annotation or application CSS was changed.

Failure: `UiReviewFollowupPlaywrightTest.launcherNeverStealsPaginationTaps`, line 70: `student-bill hit target at 768 ratio 0.2`, expected true but got false. This is the previously recorded tablet-width intermittent issue. This run does not establish its root cause or prove launcher overlap.

### Required isolated rerun, exactly once

```powershell
mvn -B '-Dtest=UiReviewFollowupPlaywrightTest#launcherNeverStealsPaginationTaps' test
```

Completed 21:04:13 +07:00: **1 test, 0 failures, 0 errors, 0 skips; BUILD SUCCESS**. This does not erase the full-suite failure; the suite is not described as fully green. The existing issue remains outside this extraction-harness task.

Full-run and isolated-rerun logs remain respectively at `target/gemini-extraction-eval-default-suite.log` and `target/gemini-extraction-eval-pagination-rerun.log`; they are not committed. This report preserves their counts outside `target/`.

## 5. Live run for the operator

In a PowerShell session where the application's own key is already configured:

```powershell
cd D:\personal-finance-cross-border-agent
mvn -B '-Dtest=GeminiExtractionEvaluationLiveIT' '-Dfinbridge.gemini.eval=true' '-Dfinbridge.gemini.eval.cap=25' '-Dfinbridge.gemini.eval.repeat=1' '-Dfinbridge.gemini.eval.delay-ms=1500' test
```

This attempts twenty images, one each, with sequential pacing and no retry; quota/authentication/interruption can stop it sooner. The hard limit is 25 attempts even when repeats are requested. CI is rejected. No web server needs to be started.

Reports: `target/gemini-extraction-eval/<UTC timestamp>-<UUID>/report.md` and sibling `raw-results.json`. Do not commit raw model outputs. Real Gemini extraction accuracy and latency remain **NOT MEASURED** until this operator run; canned test outcomes are not substitutes.

## 6. Protected scope and completion

- Live Gemini calls made during implementation: **0**. No API key was read, printed, requested, created or committed.
- `src/main`: no diff; production prompts, parser, financial logic, schema and defaults unchanged.
- Existing tracked tests: no diff. `pom.xml`: no diff; no dependencies added.
- All staged files belong to this task and are additions. Pre-existing untracked review/diagnostic files were left untracked and preserved.
- One local task commit is requested; no push or deployment is part of this task.
- Tooling verification passed. The unrelated existing tablet-width regression remains intermittent. Real-model quality measurements are deliberately left to the operator, with small-sample and post-parser limitations documented.
