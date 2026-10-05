# Global Assistant Workflow — contract and acceptance

Baseline: 6096bbf. Synthetic shared demo, session-owned conversation.
Four-field intent schema stays strict. Gemini classifies the operation only;
backend catalogues resolve actual objects. No tool calling or model financial values.

| Capability | Supported scope / source | Clarification and acceptance |
|---|---|---|
| Current balances | Connected, VERIFIED payment_source_accounts joined by account_id to sandbox_accounts | One account or explicit all; unknown/ambiguous accounts produce session-token choices. Totals per currency, exclude planner, recipients and fee accounts. Never deduct buffer. |
| Spending / budgets | Existing current demo-month aggregation | No arbitrary period, account/category slice or another person's data. Preserve actual reporting period and currency separation. |
| Tuition comparison / channel explanation | Existing verified bill and synthetic channel/quote catalogue | Explicit bill reference, compatible session bill, relevant hint, single candidate; otherwise choices. Bank B remains discussion-only and unavailable. |
| Tuition projection | Verified unpaid bill, resolved eligible VND source and existing quote calculations | ESTIMATE, current valid quote, no payment. Paid bill reports completion and asks whether current balance or another bill is intended; no double subtraction. |
| Living-expense estimate | Existing runway calculation, one unpaid bill/source/channel and confirmed monthly VND form | No inferred baseline or planner double-counting. Missing objects selectable in chat; expired quote blocks only the scenario. |
| Plan/payment status | Actual backend plan/bill status | Named plan/bill overrides context; ambiguous plans require choice. Approval banner only for awaiting approval; completed shows receipt. |
| Historical receipt balance | Completed plan's immutable Sandbox receipt | Transaction ID, timestamp, source and immediate post-payment balance. Never label as current. No current FX quote needed. |
| Explicit draft | Backend-resolved session bill/version, source account and exact eligible quote | Explicit current request required. Choices never authorize draft or payment. Draft B may be created while workspace selects A; shared selections stay unchanged. Review and approval use the plan's locked references. |

Resolution uses backend names, institution aliases, bill references, plan/receipt IDs.
Frontend choice tokens are session-owned and revision/expiry checked. IDs returned by
the model are forbidden by the unchanged four-field schema. Chat choices never update
shared account/channel selections. Current-balance, history and prospective scopes are
remembered separately. Provider failures preserve valid context and create no drafts.

All supported operations are available from Dashboard, Transactions, Student finance,
and Tasks & Payments. Screen names change suggestions only; requests use the same endpoint.
The fixed English/Vietnamese corpus lives in GlobalAssistantCorpus (test-only).
Default tests use mocked classification; the opt-in local Gemini browser test is separate
and enforces a persisted maximum of 20 submissions.

User-authorized scope extension: the existing workspace draft entry point retains its
selected-bill check. A separate conversation entry point uses the same plan builder
with an explicit verified bill version, source and quote, without requiring that bill
to be selected in the shared workspace. Only object binding/selection changes.

Protected blocks: PhaseFourService financial plan fields, Policy Guard, approve,
execute, idempotency, account/ledger writes and receipt methods; CrossBorderService,
LivingExpenseRunwayService calculation, strict parser, database schema, dependencies,
requirements and provider implementations. Only PhaseFourService assistant intent dispatch
and fallback text, plus the explicitly authorized draft object binding, may change.
