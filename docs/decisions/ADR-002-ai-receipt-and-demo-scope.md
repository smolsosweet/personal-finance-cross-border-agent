# ADR-002: Gemini boundary, reviewed receipts, and competition presentation

- Status: Accepted
- Date: 2026-10-10
- Scope: FinBridge shared synthetic competition demo

## Context

FinBridge combines personal-finance views with a controlled tuition-payment demonstration. It needs a useful assistant and receipt review flow without allowing a model, a browser request, or a demo-only convenience feature to make a financial decision.

## Decisions

### Gemini boundary

Gemini is the only current model provider. It is used for intent routing and for extraction of fields into a review draft from a user-selected image. A model never verifies a recipient, creates a financial transaction, creates or approves a payment, executes a payment, changes a balance, or overrides Policy Guard.

Intent routing sends the current question exactly as typed plus server-derived closed enums only: `topic`, `channel`, `lastIntent`, and `pending`. It does not send conversation history, assistant/backend replies, financial facts, IDs, plans, receipts, notifications, accounts, or recipient data. The current message is intentionally not transformed; therefore it can itself contain user-entered information. Enum values outside server-owned sets are not forwarded. The four-field response contract remains mandatory.

Document extraction sends only the selected image and a fixed extraction instruction. Its structured output is untrusted and can only fill a review draft. Deterministic validation and the simulated School Registry determine all subsequent state.

The demo uses sample data only. No claim is made that Gemini, a registry match, or a displayed label verifies a real school, account, or invoice.

### TC07 reviewed receipt linking

Receipt linking is deterministic server logic. A reviewed receipt links to exactly one existing expense only when currency, BigDecimal amount, normalized merchant, and calendar date within the configured window match. The default window is `ReceiptLinkService.DEFAULT_DATE_WINDOW_DAYS = 3`; `finbridge.receipt.date-window-days` accepts 0 through 31. A link changes no financial total.

Ambiguous candidates require an explicit user choice. An explicit **different purchase** choice uses a server-generated override identity bound to the evidence submission and session capability. It permits one additional normal-ingestion expense, remains idempotent on replay/double click, and leaves bank-event, ordinary-manual-entry, and Sandbox dedupe unchanged.

The session capability proves image/sample origin and the explicit choice in this unauthenticated shared workspace. It does not prove a person's identity or ownership. The pre-existing rule that ordinary same-merchant, same-amount, same-day manual entries collapse remains a known limitation. Matching school bills to historic expenses and cross-currency matching are roadmap work.

The fixed synthetic sample receipt is a demo-only stand-in for an unavailable model. It is exposed only by the server-side `app.demo-tools-enabled` gate, receives a normal server capability, and traverses the same review/save/link/override path. It is not an AI capability and opening it creates no financial entry.

### Competition presentation

The default `competition` display projects Student Finance to the Vietnam-to-China VND-to-CNY story and exactly three retained channel records: Alipay Student Payment, Bank A International Transfer, and reference-only Bank B Promotional Rate. The two eligible channels retain their existing cost, fee, FX, eligibility, and ranking data; Bank B remains non-executable.

Other retained bills, countries and channel data can be shown with `finbridge.demo.presentation=extension`. This projection is not access control and does not alter backend eligibility, pricing, routing, or payment rules.

### Inputs and future offline option

ADR-001 supersedes PRD MVP03/FR003 CSV import for the competition demo. CSV remains out of scope. Ollama/Qwen are not available in the current build; an offline provider is a possible future option that would need its own design, tests, and acceptance criteria.

### Test evidence

Generated screenshots and browser evidence write below a unique `target/test-evidence/<UUID>/` directory rather than tracked documentation files. A tablet-width pagination hit-test remains a known intermittent test issue: the observed failing points were below the viewport, no launcher overlap was observed, and a smooth-scroll synchronization race is only a leading hypothesis. The test and CSS are unchanged; the issue affects only the 768px automated layout check.

## Consequences

- Finance state, policy, approval and execution retain a deterministic server boundary.
- The demo remains intentionally synthetic and shared, unsuitable for production financial data.
- The date window and three competition channel names require team sign-off.
- Production work requires authentication, durable storage, independently governed recipient verification, partner integrations, and a separate compliance and operations plan.