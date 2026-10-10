# Package 2 TC07 entry-point inspection before implementation

State: read-only inspection complete, awaiting the user decision on which document date/scope to use. Package 1 is committed as d650cc4. No TC07 production code or schema edit has been made.

## Sources and actual entry points

PRD BR14 and TC07 require bill/image evidence to link to an existing transaction rather than a second expense. PRD section 5.1 explicitly says a bill image must not count as expense if the corresponding transaction exists; BR08 requires clarification on missing/conflicting data.

1. **AI extraction**: `/api/ai/extract-document` (`DocumentExtractionController:22-34`) returns draft fields only. Transaction receipt schema includes merchant, amount, currency and occurredAt. School bill schema includes institution, amount, currency, dueDate, paymentReference and recipientName. It does not save a transaction or evidence. It has no persistent receipt-file/evidence identity. Additional model output is ignored by whitelist.
2. **Receipt draft to expense**: `app.js:2163-2209` fills the manual transaction form. POST `/transactions/manual` (`PhaseOneController:198-208`) calls `TransactionService.recordManualTransaction` (`:211-233`) and then existing ingest. There is currently no server distinction between a normal manual entry and a confirmed image-derived receipt. This is the entry point that can create a second expense from a receipt.
3. **Bank event ingest**: `TransactionService:150-202` deduplicates by raw event reference and by account/direction/amount/merchant/minute fingerprint, plus currency. Those rules do not provide bill-to-transaction evidence links or the TC07 date window. They should remain unchanged for ordinary bank events.
4. **School bill creation**: POST `/student/expenses` (`PhaseOneController:409-430`) calls `CrossBorderService.addExpenseForReview` (`:473`); saves a row in international_bills and optional file metadata, not an expense. Schema (`schema.sql:116-144`) has due_date and metadata, no bill issue date or receipt payment date. Due date cannot safely be used as evidence transaction date.
5. **School bill edit/archive/cancel and selection**: operate on the obligation and its existing workflow; they are not expense-ingestion entry points and should retain their behavior.
6. **Sandbox receipt to transaction history**: `PhaseFourService:741` calls `TransactionService.recordSandboxPayment` (`:241-277`). The history row includes payment_action_id/payment_receipt_id/payment_quote_id/payment_channel_id. It deduplicates by receipt ID. Preserve this protected execution/idempotency path and its full landed-cost VND expense. A 20,000 CNY school bill does not equal the 70,760,800 VND source debit under TC07's same-currency/same-amount rule.
7. CSV is superseded for competition scope by ADR-001 and is not an entry point to add.

## Proposed deterministic boundary

Apply TC07 to the reviewed receipt-image save flow in Transactions, before normal expense ingestion. Use the user-confirmed occurredAt date and normalized merchant/payee, not school dueDate. Existing ordinary manual bank/event behavior remains unchanged. An exact eligible candidate receives additive evidence linkage with no money/account/budget mutation. An ambiguous candidate asks for clarification and creates no expense. No match follows the existing confirmed receipt-save path. Use additive evidence metadata/link storage for stable submission identity and audit link/ask/reject outcomes without changing existing financial columns or audit/status/reason semantics.

School bills remain obligations; existing bill -> plan -> receipt -> history references remain intact. If the team requires automatic linking of school bill uploads to historic expenses, it must first specify a source transaction/issue date and a cross-currency relationship rule. Do not infer either from dueDate or FX.

## Open decision sent to the user

Choose receipt linking in Transactions first (recommended), or expand to school-bill matching with the missing date/amount semantics specified. Per the prompt's stop condition for unstated business decisions, do not implement Package 2 until this is decided and do not skip ahead to Packages 3-5.

Proposed date window remains +/-3 calendar days, configurable, with one named default constant for team approval. The channel names have already been read from the PRD (Alipay Student Payment, Bank A International Transfer, Bank B Promotional Rate); no Package 3 change is yet made.

## Verification status

No TC07 test has run in this inspection, so TC07 is not marked PASS. No schema, production file, user-visible message, protected execution path or dependency changed. No external API/live Gemini/push/deploy. The five consecutive hit-target runs and final full regression belong to Package 4/final gate and have not been executed in this new task.
## Superseding scope decision on 10 October 2026

The user approved reviewed receipt-image linking in Transactions only. School-bill-to-historic-expense matching is roadmap, not this package. Out-of-window same-merchant purchases follow normal save, not clarification. Ambiguity is restricted to multiple candidates inside the window, a candidate linked to different evidence, or amount/currency/date matching with a different normalized merchant. Exactly one strong candidate links without new expense, but must expose This is a different purchase / Đây là khoản chi khác. No inference from school dueDate or FX.

## Protected dedupe conflict found before production edits

The new override requirement cannot be guaranteed through the unchanged manual save API:

- `TransactionService.java:231`: user-confirmed LocalDate is converted to `date.atStartOfDay()` for every manual entry.
- `TransactionService.java:171-174`: fingerprint is account + direction + amount + normalized lowercase merchant + occurrence minute. Reference is not part of this fingerprint. A duplicate returns the existing transaction ID.
- `TransactionService.java:168-169`: an already-used event reference independently returns its existing transaction.

Therefore two confirmed real purchases at the same merchant, amount, currency and calendar date will collapse when the normal save path is called twice, even with different references. Giving the override button a new reference alone will not fix it. Inventing a different timestamp or inserting a financial row directly would violate the task's safety intent and is not proposed.

Actual command:

```powershell
mvn '-Dtest=ReceiptSaveBoundaryInspectionTest,TransactionServiceTest#duplicateFingerprintDoesNotCreateAnotherTransaction' test
```

Result: **3 tests, 0 failures, 0 errors, 0 skips; BUILD SUCCESS**, 13:42:49 +07. Isolated H2 synthetic database, model disabled. Two new observation tests establish that same-date different references collapse to one row with timestamp midnight, whereas dates 30 days apart produce two rows. The unchanged existing fingerprint regression also passes. These are boundary-inspection results, not TC07 acceptance PASS. No receipt-link implementation, schema change or financial-code modification has been made.

The prompt says an explicit source marker must never bypass existing ingest/dedupe, while override must create a second genuine expense. It also requires stopping if protected semantics need changing. A narrow scope amendment is needed to reconcile these instructions; the user has been asked to decide.

## Proposed design if the narrow override amendment is approved

1. Server-issued receipt-origin capability, bound to the browser session after successful image extraction. Only allowlisted fields reach the review form. A client boolean alone is not proof of upload; invalid/foreign/stale capability is rejected rather than falling through to manual financial writes. This marker can request conservative matching but cannot approve payment or skip ordinary validations.
2. Evidence identity is the hash of normalized merchant, normalized amount, currency and confirmed calendar date, as explicitly required. Repeated confirmation resolves to one persisted evidence record/link. Candidate filtering excludes Income, Refund and Internal Transfer.
3. Additive evidence/link storage only; original financial columns retain their meaning. Linked evidence changes no amount, balance, budget or totals. All match/ask/reject/override outcomes get audit entries without altering existing audit or payment status meanings.
4. The explicit different-purchase choice receives a server-generated purchase-operation ID stored with the evidence. Repeating/double-clicking that operation reuses the same ID and transaction. It cannot originate in LLM output or a freely editable request field.
5. The only requested protected exception would be a receipt-override namespace for the ingestion identity. The original fingerprint/reference behavior stays unchanged for bank events and ordinary manual entries; payment approval, Sandbox receipt-id dedupe and financial math remain unchanged. Do not fabricate an occurrence time just to evade collision.
6. The existing demo has shared synthetic financial state and no auth. A session capability proves origin/choice, not user ownership of bank data. This limitation must be stated; no real per-user isolation claim is made.

## Current stop point

Await the narrow override-identity amendment (or a revised requirement requiring a genuinely different user-confirmed date/time). Package 2 is NOT complete and has no commit. Its inspection document and observational test remain uncommitted, separate from completed commits afaa761 and d650cc4. Packages 3, 4 and 5 and the final regression have not been started, to preserve required ordering. No push/deploy/external calls.

Prior screenshot prerequisites are confirmed: all three affected methods passed separately at 12:51:55, 12:52:35 and 12:53:48 +07 before afaa761. Evidence is in docs/ui-business-state/VERIFICATION.md, Recovery on continuation. The final new-task regression must still rerun those checks after Package 4, as specified.
## Approved narrow identity amendment and implementation plan

The user approved a separate ingestion namespace only for explicit different-purchase override. Ordinary manual, bank fingerprint/reference, and Sandbox receipt dedupe stay unchanged.

Successful receipt extraction issues a random server capability tied to the HTTP session, image hash, 20-minute expiry and reset lifetime. Confirmed fields are untrusted and validated. A capability locks its confirmed evidence and result on first save. A retry returns that result. The different-purchase operation ID is generated and stored only on the server; the browser submits the capability and explicit choice, never an operation ID. Each override evidence key combines the confirmed identity hash with this operation ID and links its own transaction. A new upload of the identical receipt after override re-evaluates candidates, finds two expenses, and asks.

Additive tables: receipt_submissions (capability, owner, origin, expiry, reviewed fields, result and server operation) and receipt_evidence (identity key, confirmed hash, transaction). All mutations use account and submission row locks inside transactions. Link/ask/reject audit does not mutate financial rows. Override uses the same validated manual ingestion pipeline with only a namespaced reference/fingerprint; timestamps remain the confirmed calendar date at midnight.

The capability proves session origin/choice, not ownership: there is no authentication and financial data is shared. Reset deletes receipt capabilities/evidence before transactions. Offline sample design is awaiting a separate user decision; it will not be silently added.
## Completion
The offline fixed seed sample was explicitly approved and implemented through the same capability path, gated by existing demo tools. Package 2 final gate passed 46/46 at 14:10:03 +07; see PACKAGE_2.md. Earlier stop points in this inspection are chronological and superseded, not the current status.