# ADR 001 Competition Demo Input Scope

- Status: Accepted
- Date: 2026-09-29
- Scope: Competition MVP demo only

## Context

The competition demo prioritizes one complete and safe journey from Personal Finance through Cross-border Tuition planning and controlled Payment Sandbox execution. Bulk statement import does not contribute directly to that critical demonstration path.

## Decision

CSV import and fixed-schema statement import are deferred from the competition MVP demo. Current demo validation relies on synthetic seeded data and clearly labelled Simulated Bank Events.

Image or screenshot bill capture and OCR are not implemented by this decision. They remain a separate future capability.

For the competition-demo scope only, this decision supersedes MVP FR003 in the binary requirements document. MVP FR003 remains part of the broader product requirements unless a later decision changes that product scope.

## Reason

The demo prioritizes the end-to-end Personal Finance and Cross-border Tuition safety journey over bulk data import. This keeps implementation and verification focused on transaction classification, proactive tuition insight, recipient and channel verification, deterministic cost calculations, approval, Policy Guard, Payment Sandbox, audit, and replay.

## Consequences

- Demo input comes from synthetic seeded data and Simulated Bank Events.
- No CSV upload or fixed-schema statement parser is presented as part of the competition MVP.
- No OCR, image bill capture, or screenshot ingestion is included in this task.
- Future implementation of any deferred input capability requires its own scope, tests, and acceptance criteria.
