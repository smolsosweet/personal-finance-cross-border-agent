# Package 1 minimal enum context verification

Decision: the user's 10 October amendment supersedes the original recent-USER-history proposal. Gemini receives the current question plus only server-derived closed enums: topic, channel, lastIntent and pending. No prior USER or ASSISTANT text is sent. The four-field output contract is unchanged. The current question itself is supplied by the user; this report does not claim to redact arbitrary identifiers a user types into that question.

## Inspection and implementation

The current code already implements the chosen enum policy; no production behavior needed changing. The outgoing paths and actual JSON fields were recorded in INSPECTION.md before tests were added. `FinanceChatService:82-83` uses the server Turn summary; `SessionConversationService:295-296` constructs typed enums; `ModelConversationContext` accepts Java enum types only, not arbitrary strings. `/agent/message` binds message and language, not topic/channel/lastIntent/pending. Forged extra form fields do not replace the session's scope. Model output remains strictly parsed through LlmIntentContract and StrictLlmIntentParser.

Document extraction sends only the uploaded image, a fixed kind-specific instruction and schema/generation controls. It returns whitelisted draft fields; it does not save a transaction/bill, match a recipient, create a plan or execute. The existing UI requires the user to review and submit the relevant form; `app.js:2163-2209` fills inputs and exposes the review note rather than calling a financial save automatically. This package tests the endpoint's no-write boundary and ignored extra extraction fields; it does not claim a new live-vision test.

## Files and tests

- `src/test/java/com/example/finance/GeminiMinimalContextPolicyTest.java`: seven executions against local H2 and loopback HTTP stub. Captures request bodies for several prior turns, old injection, forged browser context, all four closed enum sets, pending acknowledgments, and document extraction with injected unknown fields.
- `docs/pre-submission/INSPECTION.md`: read-only inventory, original decision gate and its superseding decision.
- `docs/pre-submission/PACKAGE_1.md`: this package report.

The three affirmative inputs (`ok`, `yes`, `đồng ý`) are tested with an existing AWAITING_APPROVAL draft and non-NONE pending clarification. The stub deliberately returns CREATE_TUITION_PLAN. All three leave the complete financial snapshot unchanged, keep the existing draft awaiting approval, and produce no approval, receipt or Sandbox transaction. Prior injected text never enters the next request. Captured document output containing verified/status/execute/instructions extras is reduced to the six allowed draft fields and changes no persisted financial state.

## Actual results

```powershell
mvn '-Dtest=GeminiMinimalContextPolicyTest,GeminiIntentClientTest,GeminiDocumentExtractionServiceTest,SessionConversationIntegrationTest,RunwayIntegrationTest' test
```

Final result: **89 tests, 0 failures, 0 errors, 0 skips; BUILD SUCCESS**, 13:04:23 +07. Seven are new policy checks; 82 are unchanged existing regressions. Production code, old assertions and test infrastructure were not edited. Initial new-test authoring runs failed to compile due to imports and a checked exception; an intermediate run had 3 failures and 2 errors because the fixture reset ran before the Mockito enabled stub, leaving the test policy OFFLINE. These were corrected in the new test fixture; they are not claimed as application defects. The final run actually reached and captured loopback HTTP requests.

No schema changes, dependencies, user-visible strings or constants added. No external calls, Gemini quota usage, key inspection, push or deploy. Context-window constant is removed from the proposal because no prior-message window exists. TC07 date window and three-channel presentation remain for later packages.

## Scope limits

This is supporting local evidence. No live Gemini, deployed app or physical-device acceptance was performed. Package 1 now complies with the explicitly amended policy; the previous instruction to stop on tests requiring forbidden broad context does not apply to these tests, whose enum expectation matches the amended policy.