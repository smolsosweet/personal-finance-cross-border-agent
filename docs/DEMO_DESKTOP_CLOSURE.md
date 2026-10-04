# FinBridge desktop demo closure

## Scope

This note closes the controlled desktop demo preparation that followed the
2026-10-04 Render acceptance audit. It does not claim production readiness or
physical-device mobile acceptance.

## Audited deployment

- URL: https://finbridge-shared-demo.onrender.com
- Audited live commit shown in the supplied Render deployment history:
  d6a68a6
- Full cloud evidence and limitations:
  docs/acceptance/2026-10-04-render/AUDIT.md
- The closure patch must receive a new Git commit. That new SHA is considered
  deployed only after Render shows it as Live and the short smoke check passes.

## Closure changes

- Bank B explanations are fully localized in Vietnamese while preserving the
  existing English response.
- Asking about Bank B remains read-only and does not create a payment plan,
  receipt, or Sandbox transaction.
- Delegated low-risk actions now have explicit regression coverage for:
  - aggregate daily amount limit;
  - daily completed-action frequency limit.
- Corridor and currency tampering already had backend regression coverage, so
  no duplicate tests were added.

## Verification

Commands executed on the final local source state:

- mvn "-Dtest=PhaseFourIntegrationTest,SessionConversationIntegrationTest" test
- mvn test

Actual results:

- Targeted: 35 tests, 0 failures, 0 errors, 0 skipped.
- Full suite: 265 tests, 0 failures, 0 errors, 0 skipped.
- The full suite includes the repository's local Playwright tests. It does not
  replace the separate evidence gathered against the deployed Render URL.

## Availability finding

The supplied logs correlate the first observed 503 with a service shutdown and
later startup. A second graceful shutdown also occurred before the other 503,
but the corresponding Render Event and subsequent startup sequence were not
provided. The evidence supports a hosting lifecycle interruption; it does not
identify whether the trigger was idle spin-down, deploy, health-check recovery,
or another platform event.

Operational handling for the desktop demo:

1. Open the URL before the presentation and wait for HTTP 200.
2. Run one read-only Gemini question as a preflight.
3. Keep the verified local launcher and a recorded walkthrough available as
   fallbacks.
4. Do not describe a Codex tool capacity message as a Gemini API incident.

## Release gate

The desktop demo is ready for controlled use after all of the following are
observed on Render:

1. Render shows the closure commit SHA as Live.
2. HTTPS loads successfully.
3. A Vietnamese Bank B explanation contains no English eligibility sentence.
4. One read-only AI question succeeds.
5. A tuition draft remains AWAITING_APPROVAL with no receipt before approval.

Physical-device mobile support remains unverified until the chat, keyboard,
runway form, plan review, and approval controls are checked on a real phone.
The demo remains synthetic, shared, resettable, and unsuitable for real
financial data or real-money payments.
