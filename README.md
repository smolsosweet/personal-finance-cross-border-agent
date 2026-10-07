# FinBridge

FinBridge is a guarded personal-finance and cross-border education-payment assistant. Transaction categorization, balances, budgets, eligibility checks, and payment-policy decisions remain deterministic backend responsibilities. AI only classifies supported chat requests or suggests fields from a bill image; it cannot save a transaction, change balances, approve, or execute a payment.

## Gemini-only AI

Both the chatbot and bill/receipt extraction use the Gemini API from the backend. The web page does not choose another model or send API keys to the browser.

Set `GEMINI_API_KEY` in the server environment. Never put the key in source code, `.properties`, browser JavaScript, a URL, or a committed `.env` file.

### Run on Windows

Requirements: JDK 21 and Maven 3.9+. Set the key in your PowerShell session, then launch:

```powershell
$secureKey = Read-Host 'Gemini API key (hidden input)' -AsSecureString
$env:GEMINI_API_KEY = [System.Net.NetworkCredential]::new('', $secureKey).Password
Remove-Variable secureKey
.\scripts\start-gemini.ps1 -Port 8080
```

The launcher starts FinBridge with the Gemini profile and keeps the key in the child process environment, not command-line arguments. Startup itself sends no model request. Keep the terminal open and visit `http://localhost:8080`. Press Ctrl+C in that terminal to stop the application.

### Render environment

Set `GEMINI_API_KEY` as a secret environment variable in the Render service. The Docker image starts the `hosting` Spring profile, which configures Gemini for both chat and document extraction. `FINBRIDGE_LLM_MODEL` and `FINBRIDGE_DOCUMENT_AI_MODEL` can select the respective Gemini model; defaults are `gemini-3.5-flash-lite`.

The current `hosting` profile is a shared synthetic demo using in-memory H2 data and no user login. It is not ready for storing private financial data for multiple real users; Gemini-only provider setup does not add authentication or per-user data isolation.

## Bill and receipt flow

In **Transactions → Add transaction → Read receipt with AI**, or when adding an education bill, choose an image and click **Read**. That action sends the selected image to Gemini. The validated extraction fills the existing review form; the user can edit it and must still save through the existing transaction flow. The backend retains existing validation, duplicate detection, persistence, categorization, and budget logic. Beneficiary details must still be checked against the original bill.

## Verification

Build and run the test suite:

```powershell
mvn -B test
```

Gemini adapter contract tests use local HTTP stubs and do not require a live API key or consume Gemini quota. A real Gemini call requires a valid server-side `GEMINI_API_KEY` and can consume the project's quota.

Project guides:

- [Shared demo deployment](docs/SHARED_DEMO_DEPLOYMENT.md)
- [Assistant workspace](docs/ASSISTANT_WORKSPACE.md)
- [Payment workflow](docs/PAYMENT_WORKFLOW.md)
- [AI personal-finance verification](docs/AI_PERSONAL_FINANCE_VERIFICATION.md)
- [Contextual conversation verification](docs/CONTEXTUAL_CONVERSATION_VERIFICATION.md)
