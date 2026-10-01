# Future AI-assisted input backlog

Status: The guarded tuition-intent classifier is implemented separately. The AI-assisted input and extraction
capabilities below remain planned and are not implemented.

## Student expense capture

- Accept a bill image or PDF and extract expense type, institution, amount, currency, recipient, reference and due date.
- Accept copied email text and extract the same structured fields.
- Keep the original document or text, extraction provenance and field-level confidence.
- Present extracted values as a draft that the user must review and confirm.
- Run recipient, corridor, currency, quote and approval checks after confirmation. Extraction must never bypass Policy Guard or Approval Mode.
- Keep manual entry as a fallback when extraction is unavailable or uncertain.

## Personal transaction capture

- Use connected bank events as the primary source.
- Allow the user to add a missing transaction when a bank event is unavailable or not recognized.
- Support cash income and cash expenses through an explicit Cash source.
- Allow receipt image/PDF upload and pasted notification or email text as AI-assisted drafts.
- Require amount, currency, date, direction and funding source before a transaction affects balances or reports.
- Run duplicate detection against connected bank events and existing manual entries.
- Show the funding source in transaction history, such as a bank account, wallet or Cash.
- Preserve source provenance: bank event, receipt extraction, pasted text or manual entry.

## Safety boundary

AI may extract and suggest fields. The user confirms uncertain fields. Deterministic services remain responsible for balances, duplicate handling, recipient verification, channel eligibility, policy decisions and payment execution.
