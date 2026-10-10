# Package 3 — competition display and simulated-source wording

Default finbridge.demo.presentation=competition projects Student finance to China/CNY bills and the three PRD channels: ALIPAY / Alipay Student Payment, BANK_A / Bank A International Transfer, BANK_B / Bank B Promotional Rate. The seed has two eligible channels in that projection; Bank B has no executable action.

finbridge.demo.presentation=extension restores the complete UI data, including Vietcombank, Techcombank, MoMo and US/USD/Australia/AUD. No records are deleted. The projection preserves the backend's quote objects, ranking order, global rank values, eligibility, costs, fees and FX. It is presentation only, NOT an access-control restriction: backend APIs and chat routing retain extension capabilities. Global rank badges can consequently retain gaps after filtering; no ranking behavior was changed.

## Implementation
- DemoPresentationScope contains the one named closed channel set and pure list projections.
- PhaseOneController applies it to displayed bills/quotes and the selected bill view without changing the workspace selection.
- Both create/edit country selectors show only China in competition mode.
- Synthetic account badges and channel headings/filters/recovery explanations now say simulated source/channel in VI and EN. Stored CONNECTED/DISCONNECTED enums, status values, CSS selectors and eligibility data are untouched.
- RecipientMatchPresentation changes display phrases for preserved backend/audit seed wording. No recipient-match logic changed.
- GlobalAssistantQueries, LivingExpenseRunwayService, PersonalFinanceInsights and FinanceWorkspaceService changes are user-facing text literals only.
- GLOBAL_ASSISTANT_WORKFLOW wording updated. Historical inventory/audit reports retain their observed original quotes rather than rewriting evidence. Unrelated untracked team review documents are untouched.

## Actual gate
79 tests, zero failures/errors/skips, BUILD SUCCESS 2026-10-10 14:22:08 +07.
Command: mvn -Dtest=DemoPresentationPlaywrightTest,DemoExtensionPresentationTest,PhaseThreeIntegrationTest,StudentExpenseIntegrationTest,FinanceWorkspaceIntegrationTest,RecipientMatchPresentationTest,GlobalAssistantIntegrationTest,RunwayIntegrationTest test
Log: target/pre-submission-package3-accepted.log.
Default browser checks: exactly 3 channel cards, Bank B non-executable, only China creation option, honest account labels in EN/VI, 390px and 1440px simulated layout. Backend projection equality checks prove ranking/cost/eligibility data retained. Extension MockMvc checks retain all six channels and additional countries.

Existing extension tests explicitly configured, no assertion removed/weakened:
BusinessUxPlaywrightTest, BilingualLayoutAcceptancePlaywrightTest, PaymentSourcePlaywrightE2ETest, StudentExpensePlaywrightE2ETest, PhaseThreeIntegrationTest, StudentExpenseIntegrationTest. Two former connected-label assertions changed only to the new paired wording. Screenshot-producing legacy walkthroughs will execute after Package 4 so they cannot overwrite committed images.

No schema/dependency/Policy Guard/Sandbox/approval/FX/fee/matching/intent-contract changes. No live Gemini, push or deploy.

## Team review: visible wording
New and changed dictionary entries are included below verbatim as paired EN/VI strings. Internal enum keys remain internal; their display values are simulation labels.
~~~javascript
  'Simulated balances follow synthetic bank events. You control balances entered manually, including cash.': 'Số dư mô phỏng cập nhật theo sự kiện ngân hàng mô phỏng. Bạn tự quản lý số dư nhập thủ công, gồm cả tiền mặt.',
  "Manual money source: no connected payment channel": "Nguồn nhập tay: không có kênh thanh toán mô phỏng",
  "No verified payment channel connected": "Chưa có kênh thanh toán mô phỏng khớp danh bạ",
  'Use manual entry for cash or a source outside the simulated ledger. It is included in your personal total and marked as manually maintained.': 'Dùng nhập thủ công cho tiền mặt hoặc nguồn ngoài sổ mô phỏng. Nguồn này được tính vào tổng cá nhân và được đánh dấu là tự quản lý.',
  'Update this balance after reconciling your cash or unsupported account. Simulated bank sources cannot be edited here.': 'Cập nhật số dư sau khi đối soát tiền mặt hoặc tài khoản chưa hỗ trợ. Không thể sửa nguồn ngân hàng mô phỏng tại đây.',
  'CONNECTED': 'NGUỒN MÔ PHỎNG',
  'SIMULATED PAYMENT CHANNELS': 'KÊNH THANH TOÁN MÔ PHỎNG',
  'YOUR SIMULATED PAYMENT SOURCES': 'NGUỒN THANH TOÁN MÔ PHỎNG',
  'YOUR SIMULATED PAYMENT CHANNELS': 'KÊNH THANH TOÁN MÔ PHỎNG',
  'Simulated channels': 'Kênh mô phỏng',
  'All simulated channels': 'Tất cả kênh mô phỏng',
  'No simulated channels match this filter.': 'Không có kênh mô phỏng phù hợp bộ lọc.',
  'Simulated payment sources are shown. Choose the account whose balance should be used for comparison and payment.': 'Hiển thị đầy đủ các tài khoản mô phỏng. Chọn tài khoản có số dư sẽ được dùng để so sánh và thanh toán.',
  'These channels are reference options without a configured simulated source. They cannot be executed.': 'Đây là kênh tham khảo chưa có nguồn tiền mô phỏng được cấu hình. Không thể thực thi.',
  'No linked simulated source': 'Chưa liên kết nguồn mô phỏng',
  'DISCONNECTED': 'Nguồn mô phỏng không khả dụng',
  'Account disconnected: unavailable for payments': 'Nguồn mô phỏng không khả dụng để thanh toán',
  'Simulated channel comparison': 'So sánh kênh mô phỏng',
  'Executable simulated channels': 'Kênh mô phỏng có thể thực thi',
  'Each simulated account has its own payment channel. Balance readiness is calculated against that channel\'s landed cost and the safety buffer.': 'Mỗi tài khoản mô phỏng có một kênh thanh toán riêng. Khả năng đủ tiền được tính theo tổng chi phí của chính kênh đó và vùng an toàn.',
  'Simulated source': 'Nguồn mô phỏng',
  'Unavailable simulated source': 'Nguồn mô phỏng không khả dụng',
  'Simulated account balances can only be changed by synthetic bank events and approved Sandbox payments': 'Số dư tài khoản mô phỏng chỉ thay đổi bởi sự kiện ngân hàng mô phỏng và thanh toán Sandbox đã phê duyệt',
  'Simulated bank sources cannot be archived from this workspace': 'Không thể lưu trữ nguồn ngân hàng mô phỏng từ workspace này',
  'Reference only: no linked Bank B source in the simulation': 'Chỉ tham khảo: chưa liên kết nguồn Bank B trong mô phỏng',
  'Simulated source unavailable: unavailable for payments': 'Nguồn mô phỏng không khả dụng để thanh toán',
  "The source account is not connected, verified, or enabled for this corridor.": "Nguồn mô phỏng chưa khả dụng, chưa khớp danh bạ hoặc chưa bật cho hành lang này.",
  'Simulated source available, but this channel does not support': 'Nguồn mô phỏng khả dụng, nhưng kênh này chưa hỗ trợ',
 CONNECTED:'Simulated source', DISCONNECTED:'Unavailable simulated source', VERIFIED:'Verified', MANUAL:'Manual', CASH:'Cash', LINKED:'Linked',
  'Simulated source available, but this demo channel does not support': 'Nguồn mô phỏng khả dụng, nhưng kênh mô phỏng này chưa hỗ trợ',
  '. Adding the source again will not enable this route. Choose an eligible channel.': '. Thêm lại nguồn tiền không mở được tuyến này. Hãy chọn một kênh đủ điều kiện.'
~~~

Backend-only wording pairs:
- No eligible simulated source with a recorded balance is available. / Chưa có nguồn tài khoản mô phỏng khả dụng với số dư được ghi nhận.
- Select one eligible simulated VND source account in Student finance. / Chọn rõ một nguồn tài khoản VND mô phỏng khả dụng trong Tài chính du học.
- No eligible simulated source account is selected. / Chưa có nguồn tài khoản mô phỏng khả dụng và được cho phép.
- Simulated account balances can only be changed by synthetic bank events and approved Sandbox payments / Số dư tài khoản mô phỏng chỉ thay đổi bởi sự kiện ngân hàng mô phỏng và thanh toán Sandbox đã phê duyệt.
- Simulated bank sources cannot be archived from this workspace / Không thể lưu trữ nguồn ngân hàng mô phỏng từ workspace này.

Channel-set sign-off: PRD explicitly names the three above. No channel names were guessed. Date window +/-3 remains the other constant for team approval. No message-history window exists after the Package 1 amendment.