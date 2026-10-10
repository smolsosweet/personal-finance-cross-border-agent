# Pre-submission inspection and decision gate

Date: 10 October 2026. Application baseline for new packages: `afaa761`.

## Previous-task prerequisite

The notification/bill-selection patch is now a separate local commit, `afaa761`, with the exact requested message. Three previously blocked screenshot methods each passed on isolated retry without application or infrastructure changes. The full-suite invocation is still reported honestly as 399 tests, 395 passed, 3 screenshot-write errors, 1 live-smoke skip; its failed invocation is not relabeled as successful. All 179 tracked historical screenshots were restored. Generated images and pre-run evidence folders were excluded from the commit. No push or deploy.

## Required sources inspected

- Repository PRD: `Product_Requirements_Personal_Finance_Cross_Border_Agent.docx`, read from its document XML without modifying it. BR07 requires untrusted input, BR08 asks/refuses on missing/conflicting data, BR14 and TC07 require linking duplicate bill evidence rather than another expense.
- `docs/decisions/ADR-001-demo-input-scope.md`: CSV deferred for competition MVP; historical OCR exclusion needs a later ADR reflecting the current extraction implementation.
- README: currently Gemini-only and explicitly synthetic/shared/no-auth; extraction is review before save.
- `docs/DEMO_READINESS.md`: historical Ollama rehearsal guidance, not an accurate current startup guide. Package 5 must retain history while providing current instructions.
- PRD's named channels are explicit: Alipay Student Payment, Bank A International Transfer, Bank B Promotional Rate. This resolves the source-name uncertainty for future Package 3; it is not evidence that the presentation package has been implemented.

## Package 1 Step 1: complete outgoing-request inventory

There are two network adapters in production. No browser direct-Gemini path was found in the application code search.

### Intent routing and contextual conversation

Path: `FinanceChatService.java:82-83` -> `ModelConversationContext.with(turn.summary(),...)` -> `GeminiIntentClient.classify` (`:52-60`). The summary is built at `SessionConversationService.java:295-296`.

`GeminiIntentClient.java:85-89` sends exactly three root JSON fields:

1. `systemInstruction.parts[0].text`: fixed `LlmIntentContract.INSTRUCTIONS`, plus a fixed follow-up explanation and backend-generated enum values when a session summary is present. The values are `topic`, `channel`, `lastIntent`, `pending` (`ModelConversationContext.java:3-48`). No transcript or financial records are appended here.
2. `contents[0]`: `role=user`, one `parts[0].text` containing the current user message. No earlier USER or ASSISTANT message is currently included.
3. `generationConfig`: `candidateCount=1`, `maxOutputTokens=512`, `responseMimeType=application/json`, `responseJsonSchema=LlmIntentContract.schema()` (strict four-field contract).

The synthetic API key is in the HTTP header in the local stub test; production secrets are not inspected, requested or logged. Current user text is sent as supplied, so this inventory does not claim that amounts or identifiers typed by a user are automatically redacted.

Captured-request evidence: existing `GeminiIntentClientTest.headerSchemaAndEnumOnlySessionContextUseSharedContract`, specifically `:40` asserts the outgoing system text contains `topic=BUDGET`. It also checks the root shape, actual current text, strict schema and absence of object account data in the synthetic scenario.

### Document extraction: transaction receipt and student bill

Path: `DocumentExtractionController.java:22-34` -> `GeminiDocumentExtractionService.extract` (`:55-87`).

Request contains `contents` and `generationConfig` only:

- `contents[0].role=user`.
- `parts[0].text`: the fixed instruction selected by validated `kind` (`transaction` or `student-bill`); no profile/session/financial context interpolated.
- `parts[1].inlineData`: uploaded file MIME (`image/jpeg`, `image/png`, or `image/webp`) and exact base64 file bytes.
- `generationConfig`: `candidateCount=1`, `temperature=0`, `maxOutputTokens=512`, `responseMimeType=application/json`, and kind-specific string-field schema.
- Transaction output names: merchant, amount, currency, occurredAt, paymentMethod, category, reference.
- Student-bill output names: institution, amount, currency, dueDate, paymentReference, recipientName.

The instruction/response schema names are not backend financial values. The uploaded file itself can contain financial information; document upload is the explicit extraction exception in the attached policy. Missing/invalid output values are normalized/blanked by `validate`; only allowed draft fields are returned. Unknown fields are ignored by the whitelist, not assigned to matching/payment status. Additional injected-output and financial-snapshot regressions requested in Step 3 have not been added or run yet.

Captured-request evidence: existing `GeminiDocumentExtractionServiceTest.transactionReceiptIsSentToGeminiAndValidatedForExistingReviewForm` verifies fixed prompt, MIME, exact image bytes and stub request; `educationBillAlsoUsesGeminiVisionAndLeavesMissingFieldsBlank` covers the second kind.

Command actually run:

```powershell
mvn '-Dtest=GeminiIntentClientTest#headerSchemaAndEnumOnlySessionContextUseSharedContract,GeminiDocumentExtractionServiceTest#transactionReceiptIsSentToGeminiAndValidatedForExistingReviewForm+educationBillAlsoUsesGeminiVisionAndLeavesMissingFieldsBlank' test
```

Result: **3 tests, 0 failures, 0 errors, 0 skips; BUILD SUCCESS** (12:54:01 +07). All requests went to loopback HTTP stubs. Live model/external API requests: **0**.

## Package 1 stop condition

The attached prompt expressly says to stop if an existing test depends on the broader old context. Such dependencies exist:

- `GeminiIntentClientTest.java:40`: requires `topic=BUDGET` in captured outbound system instruction.
- `SessionConversationIntegrationTest.java:42`: requires topic enum context for the follow-up.
- `SessionConversationIntegrationTest.java:285`: requires `context_v1` and `topic=BUDGET` in Gemini request body.
- `RunwayIntegrationTest.java:38,197`: requires channel/runway enum input.

Replacing this with prior USER messages contradicts those existing expectations. No policy implementation, assertion edit or Package 1 commit has been made. No subsequent package was started.

Recommendation presented to the user: keep the current question plus allowlisted enum context, excluding transcripts and backend replies; add request-boundary and extraction-safety tests. Enum context is smaller and cannot include arbitrary sensitive historical user text. This is a proposed scope amendment, awaiting the user's decision; it is not treated as permission already granted.

## PM follow-up items and limits

- Five consecutive `launcherNeverStealsPaginationTaps` executions belong to Package 4, which is not started. Earlier targeted and final full runs passed the unchanged class, but the first full run failed once; no stability conclusion is drawn.
- Code shows the notification counter is based on the currently rendered DOM. Another session's financial changes are fetched when the bell opens; no server-event subscription or background notification polling exists. Thus this task does not claim a live counter before opening. A two-window runtime check of that pre-open counter has not been executed here. It is an unresolved product/acceptance item, not silently fixed.
- No new schema/dependency/user-visible string change in this inspection. No protected code changes for the new packages.
- Proposed constants remain undecided: prior USER window 3 would apply only if that approach is chosen; TC07 date window +/-3 days remains a team proposal. PRD provides the three channel names, but Package 3 still needs implementation and tests.
- Human-only submission checks remain deployed SHA confirmation and a bilingual walkthrough on the deployed app. No Render manipulation was performed.
## Superseding decision
The user subsequently approved minimal server-derived enum context and explicitly replaced Package 1 Step 2. The original stop condition above is historical. Package 1 has now been verified: see PACKAGE_1.md. No prior USER window is introduced; the proposed constant 3 is withdrawn.