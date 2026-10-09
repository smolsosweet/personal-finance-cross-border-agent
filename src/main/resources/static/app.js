const translationsVi = new Map(Object.entries({
  'SHARED DEMO · SYNTHETIC DATA': 'DEMO DÙNG CHUNG · DỮ LIỆU MÔ PHỎNG',
  'Finance and Payment Sandbox data are shared by the team. Reset affects everyone. Restart restores the seed data. No real money or bank connection.': 'Dữ liệu tài chính và Payment Sandbox dùng chung cho cả team. Reset ảnh hưởng mọi người. Restart đưa dữ liệu về seed. Không dùng tiền thật hoặc kết nối ngân hàng.',
  'About this estimate': 'Giải thích về ước tính',
  'LIVING_EXPENSE_RUNWAY': 'Thời gian đủ tiền sinh hoạt',
  'Living-expense scenario updated; estimate only, no payment.': 'Đã cập nhật kịch bản sinh hoạt; chỉ ước tính, chưa thanh toán.',
  'Living-expense scenario · ESTIMATE': 'Kịch bản sinh hoạt · ƯỚC TÍNH',
  'Include rent, food, transport and utilities. This input confirms an estimate only; it does not change budgets or authorize payments.': 'Bao gồm tiền thuê nhà, ăn uống, đi lại và tiện ích. Chỉ xác nhận dữ liệu ước tính; không sửa ngân sách hoặc cấp quyền thanh toán.',
  'Monthly living expense': 'Chi sinh hoạt mỗi tháng',
  'Confirmed monthly expense': 'Mức chi tháng đã xác nhận',
  'Confirm estimate input': 'Xác nhận mức chi để ước tính',
  'Clear monthly assumption': 'Xóa giả định chi hàng tháng',
  'Previous estimates are historical snapshots. Ask again or confirm the form for current data. VND only; no automatic currency conversion.': 'Ước tính trước là ảnh chụp dữ liệu tại thời điểm trả lời. Hỏi lại hoặc xác nhận form để lấy dữ liệu hiện tại. Chỉ dùng VND; không tự đổi tiền tệ.',
  'Ask FinBridge': 'Hỏi FinBridge',
  'Transactions': 'Giao dịch',
  'Review incoming activity, confirm categories and find past payments.': 'Xem giao dịch mới, xác nhận danh mục và tìm khoản đã chi.',
  '+ Add transaction': '+ Thêm giao dịch',
  'NEW TRANSACTION': 'GIAO DỊCH MỚI',
  'Add transaction': 'Thêm giao dịch',
  'Manual entry': 'Nhập thủ công',
  'Read receipt with AI': 'Đọc bill bằng AI',
  'Gemini reads this image to prefill the bill. Always verify beneficiary details against the official bill; extraction does not verify the recipient.': 'Gemini sẽ đọc ảnh để điền hóa đơn. Luôn đối chiếu người thụ hưởng với hóa đơn chính thức; tính năng đọc bill không xác minh người nhận.',
  'Local AI': 'AI cục bộ', 'Gemini': 'Gemini', 'confidence': 'độ tin cậy',
  'Upload a bill image to Gemini to prefill details. Confirm the beneficiary against the official school bill; extraction does not verify the recipient.': 'Ảnh hóa đơn sẽ được gửi tới Gemini để điền thông tin. Hãy đối chiếu người nhận với hóa đơn chính thức; AI không xác minh người thụ hưởng.',
  'Receipt image': 'Ảnh hóa đơn',
  'Read receipt': 'Đọc hóa đơn',
  'AI recognized these details. Please review them before saving.': 'AI đã nhận diện thông tin. Vui lòng kiểm tra trước khi lưu.',
  'AI recognized these details. Please review and edit them before saving.': 'AI đã nhận diện thông tin. Vui lòng kiểm tra và chỉnh sửa trước khi lưu.',
  'Choose an image first.': 'Vui lòng chọn ảnh trước.',
  'Choose a JPG, PNG or WEBP image up to 5 MB.': 'Chọn ảnh JPG, PNG hoặc WEBP tối đa 5 MB.',
  'Reading image…': 'Đang đọc ảnh…',
  'Receipt AI is not configured. Set GEMINI_API_KEY on the server.': 'Máy chủ chưa có GEMINI_API_KEY để đọc hóa đơn.',
  'Gemini rejected the API key. Check GEMINI_API_KEY in the server environment.': 'Gemini từ chối API key. Hãy kiểm tra GEMINI_API_KEY trên máy chủ.',
  'Gemini quota or rate limit reached. Try again later.': 'Gemini đã hết hạn mức hoặc đang giới hạn yêu cầu. Hãy thử lại sau.',
  'Gemini is temporarily unavailable. Try again later.': 'Gemini tạm thời không khả dụng. Hãy thử lại sau.',
  'Could not read this receipt. Try a clearer JPG, PNG or WEBP image.': 'Không đọc được hóa đơn. Hãy thử ảnh JPG, PNG hoặc WEBP rõ hơn.',
  'Read bill': 'Đọc hóa đơn',
  'AI/OCR extraction is not configured on this server yet. No fields were extracted.': 'Máy chủ chưa cấu hình OCR/AI đọc hóa đơn. Chưa có thông tin nào được nhận diện.',
  'Review and save transaction': 'Kiểm tra và lưu giao dịch',
  'Merchant': 'Đơn vị bán hàng',
  'Date': 'Ngày giao dịch',
  'Payment method': 'Phương thức thanh toán', 'Payment method (optional)': 'Phương thức thanh toán (không bắt buộc)',
  'Invoice / reference number (optional)': 'Mã hóa đơn / tham chiếu (không bắt buộc)',
  'AI/OCR extraction is not configured on this server yet. No fields were extracted.': 'Máy chủ chưa cấu hình OCR/AI đọc hóa đơn. Chưa có thông tin nào được nhận diện.',

  'Conversation topic': 'Chủ đề hội thoại',
  'CURRENT_BALANCE': 'Số dư tài khoản hiện tại',
  'RECEIPT_BALANCE': 'Số dư lịch sử trong biên nhận',
  'Waiting for clarification': 'Cần làm rõ',
  'ACCOUNT': 'Chọn tài khoản', 'BILL': 'Chọn hóa đơn', 'PLAN': 'Chọn kế hoạch',
  'CHANNEL': 'Chọn kênh', 'TOPIC': 'Chọn chủ đề', 'MONTHLY_EXPENSE': 'Xác nhận mức chi sinh hoạt',
  'Payment completed. View its receipt; no further approval is required for this payment.': 'Thanh toán đã hoàn tất. Xem biên nhận; giao dịch này không cần phê duyệt thêm.',
  'Student payment completed': 'Thanh toán du học đã hoàn tất',
  'Student payment plan needs controlled execution': 'Kế hoạch thanh toán du học cần thực thi có kiểm soát',
  'Payment completed; view the immutable Sandbox receipt.': 'Thanh toán đã hoàn tất; xem biên nhận Sandbox không thể chỉnh sửa.',
  'Plan invalidated. Review the changed bill or quote before creating a new draft.': 'Kế hoạch đã vô hiệu. Kiểm tra hóa đơn hoặc báo giá đã thay đổi trước khi tạo draft mới.',
  'Plan blocked. Review the policy reason before continuing.': 'Kế hoạch bị chặn. Kiểm tra lý do từ Policy Guard trước khi tiếp tục.',
  'Plan canceled. No new execution is available for this plan.': 'Kế hoạch đã hủy. Kế hoạch này không thể thực thi thêm.',
  'Reload the authoritative plan state. Chat does not approve or execute payments.': 'Tải lại trạng thái chính thức của kế hoạch. Chat không phê duyệt hay thực thi thanh toán.',
  'Update conversation context': 'Cập nhật ngữ cảnh hội thoại',
  'Updating conversation context…': 'Đang cập nhật ngữ cảnh hội thoại…',
  'Choose the topic in the assistant': 'Chọn chủ đề trong trợ lý',
  'NONE': 'Chưa chọn', 'SPENDING': 'Chi tiêu', 'BUDGET': 'Ngân sách',
  'TUITION_AFFORDABILITY': 'Dự kiến sau học phí', 'TUITION_CHANNELS': 'Kênh học phí',
  'CHANNEL_UNAVAILABLE': 'Điều kiện sử dụng kênh', 'TUITION_PLAN': 'Kế hoạch học phí', 'TUITION_STATUS': 'Trạng thái học phí',
  'Close assistant': 'Đóng trợ lý',
  'Ask about tuition': 'Hỏi về học phí',
  'Explain this plan': 'Giải thích kế hoạch này',
  'Current screen': 'Màn hình đang xem',
  'Choose a question, then press Send. Opening the assistant does not create or approve a payment.': 'Chọn câu hỏi rồi bấm Gửi. Mở trợ lý không tạo hay phê duyệt thanh toán.',
  'Spending this month': 'Chi tiêu tháng này',
  'Remaining budgets': 'Ngân sách còn lại',
  'Can I afford tuition?': 'Tôi có đủ tiền đóng học phí không?',
  'Compare tuition channels': 'So sánh kênh đóng học phí',
  'Why is Bank B unavailable?': 'Vì sao Bank B không khả dụng?',
  'Displayed plan facts': 'Thông tin từ kế hoạch đang xem',
  'These figures come from the displayed plan. Financial questions use recorded data and estimates; they do not approve this plan.': 'Các số liệu này lấy từ kế hoạch đang xem. Câu hỏi tài chính sử dụng dữ liệu đã ghi nhận và ước tính; không phê duyệt kế hoạch này.',
  'This plan requires explicit approval on the payment review screen. Creating a plan or sending a chat message does not execute it.': 'Kế hoạch này cần bạn phê duyệt rõ ràng tại màn hình thanh toán. Tạo kế hoạch hay gửi tin nhắn không thực thi thanh toán.',
  'View payment plan': 'Xem kế hoạch thanh toán',
  'Ask about spending, budgets or tuition': 'Hỏi về chi tiêu, ngân sách hoặc học phí',
  'Your question': 'Câu hỏi của bạn',
  'You': 'Bạn',
  'Ask about spending, budgets or tuition. I can prepare a tuition plan for you to review; payments always need explicit approval.': 'Bạn có thể hỏi về chi tiêu, ngân sách hoặc học phí. Tôi có thể tạo kế hoạch học phí để bạn xem xét; thanh toán luôn cần bạn phê duyệt rõ ràng.',
  'Beneficiary': 'Người thụ hưởng',
  'Emergency Stop is active': 'Đã bật Dừng khẩn cấp',
  'READ-ONLY PERSONAL FINANCE': 'TÀI CHÍNH CÁ NHÂN · CHỈ ĐỌC',
  'Ask about your money': 'Hỏi về tài chính của bạn',
  "Ask about this month's spending, remaining budgets, or a tuition affordability projection. Answers use recorded demo data.": 'Hỏi về chi tiêu tháng này, ngân sách còn lại hoặc dự kiến sau học phí. Câu trả lời dùng dữ liệu demo đã ghi nhận.',
  'Where did I spend the most this month?': 'Tháng này tôi chi nhiều nhất vào đâu?',
  'Processing your question…': 'Đang xử lý câu hỏi…',
  'AI understands the question. Backend code calculates the figures. Asking a question does not authorize a payment.': 'AI hiểu câu hỏi. Backend tính số liệu. Đặt câu hỏi không cấp quyền thanh toán.',
  'FINANCE ASSISTANT': 'TRỢ LÝ TÀI CHÍNH',
  'Overview': 'Tổng quan',
  'Dashboard': 'Tổng quan',
  'Proactive Feed': 'Thông tin chủ động',
  'Budget': 'Ngân sách',
  'Student finance': 'Tài chính du học',
  'Agent & Sandbox': 'Tác vụ & Sandbox',
  'Agent & Payments': 'Tác vụ & Thanh toán',
  'Transactions': 'Giao dịch',
  'Transaction history': 'Lịch sử giao dịch',
  'All transactions': 'Tất cả giao dịch',
  'Categories': 'Danh mục',
  'Demo bank feed': 'Nguồn ngân hàng demo',
  'DEMO ONLY': 'CHỈ DÙNG DEMO',
  'Create demo transaction': 'Tạo giao dịch demo',
  'Just created': 'Vừa tạo',
  'PHASE 5 OF 5': 'GIAI ĐOẠN 5/5',
  'Controlled execution': 'Thực thi có kiểm soát',
  'Workspace': 'Không gian làm việc',
  'Finance dashboard': 'Bảng điều khiển tài chính',
  '● SYNTHETIC DATA': '● DỮ LIỆU MÔ PHỎNG',
  'PERSONAL FINANCE · PHASE 5': 'TÀI CHÍNH CÁ NHÂN · GIAI ĐOẠN 5',
  'Good morning,': 'Chào buổi sáng,',
  'Your deterministic finance dashboard updates after every simulated event.': 'Bảng điều khiển tài chính theo quy tắc được cập nhật sau mỗi sự kiện mô phỏng.',
  'PERSONAL FINANCE WORKSPACE': 'KHÔNG GIAN TÀI CHÍNH CÁ NHÂN',
  'Your money today,': 'Tài chính hôm nay của',
  'See what you own, what is already planned and what remains safe to allocate.': 'Xem tổng tiền hiện có, các khoản đã lên kế hoạch và số tiền còn có thể phân bổ an toàn.',
  'Summary': 'Tóm tắt',
  'Accounts & cash': 'Tài khoản & tiền mặt',
  'Plans & budgets': 'Kế hoạch & ngân sách',
  'Total personal balance': 'Tổng số dư cá nhân',
  'VND · active personal sources': 'VND · các nguồn tiền cá nhân đang hoạt động',
  'Reserved for next 30 days': 'Giữ trước cho 30 ngày tới',
  'Recurring and one-time plans': 'Kế hoạch định kỳ và một lần',
  'Available to allocate': 'Có thể phân bổ',
  'After plans and 3,000,000 VND safety buffer': 'Sau các kế hoạch và vùng đệm an toàn 3.000.000 VND',
  'Transactions waiting for your input': 'Giao dịch đang chờ bạn xử lý',
  'ACTION CENTER': 'TRUNG TÂM HÀNH ĐỘNG',
  'What needs your attention': 'Việc cần bạn chú ý',
  'Each item opens the workspace where you can resolve it.': 'Mỗi mục mở đúng khu vực để bạn xử lý.',
  'Nothing urgent. Your current plans and transactions are in order.': 'Không có việc khẩn cấp. Các kế hoạch và giao dịch hiện đang ổn.',
  'Education payments': 'Thanh toán giáo dục',
  'Verify a provider bill and compare eligible payment channels.': 'Xác minh hóa đơn nhà cung cấp và so sánh các kênh thanh toán hợp lệ.',
  'Open student finance →': 'Mở tài chính du học →',
  'Controlled payment plans': 'Kế hoạch thanh toán có kiểm soát',
  'Review approvals, Sandbox receipts and the Audit Log.': 'Xem phê duyệt, biên nhận Sandbox và Nhật ký kiểm toán.',
  'Open payments →': 'Mở thanh toán →',
  'CURRENT DATASET': 'DỮ LIỆU HIỆN TẠI',
  'Cash-flow snapshot': 'Tóm tắt dòng tiền',
  'Internal transfers remain excluded from income and expense totals.': 'Chuyển khoản nội bộ không được tính vào tổng thu nhập và chi tiêu.',
  'Monthly budget': 'Ngân sách tháng',
  'Review plans and budgets →': 'Xem kế hoạch và ngân sách →',
  'PERSONAL MONEY SOURCES': 'NGUỒN TIỀN CÁ NHÂN',
  'Accounts and cash': 'Tài khoản và tiền mặt',
  'Connected balances follow incoming bank events. You control balances entered manually, including cash.': 'Số dư đã kết nối cập nhật theo sự kiện ngân hàng. Bạn tự quản lý số dư nhập thủ công, gồm cả tiền mặt.',
  '+ Add money source': '+ Thêm nguồn tiền',
  'Personal ledger': 'Sổ tài chính cá nhân',
  "YOUR MONEY SOURCES": "NGUỒN TIỀN CỦA BẠN",
  "The same account balances are used across Overview, planning, chat and payments. Currencies remain separate.": "Tổng quan, kế hoạch, chat và thanh toán dùng chung số dư tài khoản. Hiển thị riêng từng tiền tệ.",
  "Each source is counted once. Recipient accounts are excluded; manual cash is labelled and cannot execute online payments.": "Mỗi nguồn tiền chỉ được tính một lần. Không cộng tài khoản người nhận; tiền mặt nhập tay được ghi rõ và không dùng để thanh toán trực tuyến.",
  "Money available for planning": "Tiền dành cho kế hoạch",
  "Planning uses the same accounts shown above, including manually maintained cash. Reservations do not move money.": "Kế hoạch dùng cùng các tài khoản phía trên, gồm tiền mặt nhập tay. Khoản giữ trước không phải giao dịch chuyển tiền.",
  "Total balance · VND": "Tổng số dư · VND",
  "All active personal sources · VND": "Các nguồn tiền cá nhân đang hoạt động · VND",
  "Available after reservations": "Còn lại sau khoản giữ trước",
  "Shared account ledger": "Sổ tài khoản chung",
  "One account ledger powers Overview, planning, chat and Payment Sandbox. Bank events and approved payments update these balances.": "Tổng quan, kế hoạch, chat và Payment Sandbox dùng một sổ tài khoản. Sự kiện ngân hàng và thanh toán đã phê duyệt cập nhật các số dư này.",
  "Manually maintained": "Nhập và cập nhật thủ công",
  "Read-only · updated by bank events and approved payments": "Chỉ đọc · cập nhật bởi sự kiện ngân hàng và thanh toán đã phê duyệt",
  "Scroll down": "Cuộn xuống",
  "Manual money source: no connected payment channel": "Nguồn nhập tay: chưa có kênh thanh toán kết nối",
  "No verified payment channel connected": "Chưa kết nối kênh thanh toán đã xác minh",
  'These balances drive the Overview. Payment Sandbox balances remain separate so the same money is never counted twice.': 'Các số dư này dùng cho Tổng quan. Số dư Payment Sandbox được tách riêng để không cộng trùng tiền.',
  'Read-only · updated by bank events': 'Chỉ đọc · cập nhật bởi sự kiện ngân hàng',
  'Edit balance': 'Sửa số dư',
  'NEW MONEY SOURCE': 'NGUỒN TIỀN MỚI',
  'Add an account or cash': 'Thêm tài khoản hoặc tiền mặt',
  'Use manual entry for cash or a source that is not connected. It is included in your personal total and marked as manually maintained.': 'Dùng nhập thủ công cho tiền mặt hoặc nguồn chưa kết nối. Nguồn này được tính vào tổng cá nhân và được đánh dấu là tự quản lý.',
  'Name': 'Tên',
  'Institution': 'Tổ chức',
  'Type': 'Loại',
  'Checking': 'Tài khoản thanh toán',
  'Savings': 'Tiết kiệm',
  'Cash': 'Tiền mặt',
  'E-wallet': 'Ví điện tử',
  'Reference / last digits': 'Tham chiếu / số cuối',
  'Current balance (VND)': 'Số dư hiện tại (VND)',
  'Add money source': 'Thêm nguồn tiền',
  'EDIT MANUAL SOURCE': 'SỬA NGUỒN NHẬP THỦ CÔNG',
  'Update this balance after reconciling your cash or unsupported account. Connected accounts cannot be edited here.': 'Cập nhật số dư sau khi đối soát tiền mặt hoặc tài khoản chưa hỗ trợ. Không thể sửa tài khoản đã kết nối tại đây.',
  'Save balance': 'Lưu số dư',
  'FORWARD PLAN': 'KẾ HOẠCH TƯƠNG LAI',
  'Plans and budgets': 'Kế hoạch và ngân sách',
  'Plan a one-time expense, recurring bill or savings target. Only active plans marked “reserve money” reduce the available amount.': 'Lập kế hoạch cho khoản chi một lần, hóa đơn định kỳ hoặc mục tiêu tiết kiệm. Chỉ kế hoạch đang hoạt động có bật giữ tiền mới làm giảm số tiền có thể phân bổ.',
  '+ Add plan': '+ Thêm kế hoạch',
  'Active plans': 'Kế hoạch đang hoạt động',
  'Reserved · next 30 days': 'Giữ trước · 30 ngày tới',
  'Weekly guide': 'Gợi ý theo tuần',
  'Upcoming and saved plans': 'Kế hoạch sắp tới và đã lưu',
  'Completed and archived records stay visible but stop affecting projections.': 'Bản ghi hoàn tất và lưu trữ vẫn hiển thị nhưng không ảnh hưởng dự báo.',
  'Tracking only': 'Chỉ theo dõi',
  'Edit': 'Sửa',
  'Mark complete': 'Đánh dấu hoàn tất',
  'Monthly category budgets': 'Ngân sách danh mục theo tháng',
  'Weekly guidance is derived from monthly limits, preventing conflicting targets.': 'Gợi ý theo tuần được suy ra từ hạn mức tháng để tránh các mục tiêu mâu thuẫn.',
  'Monthly limit': 'Hạn mức tháng',
  '+ Add budget': '+ Thêm ngân sách',
  'NEW CATEGORY BUDGET': 'NGÂN SÁCH DANH MỤC MỚI',
  'Add a monthly budget': 'Thêm ngân sách tháng',
  'Choose an active spending category. A category can have one monthly budget; update that budget instead of creating a duplicate.': 'Chọn danh mục chi tiêu đang hoạt động. Mỗi danh mục chỉ có một ngân sách tháng; hãy cập nhật thay vì tạo trùng.',
  'Monthly category budget added. The weekly guide was recalculated.': 'Đã thêm ngân sách danh mục tháng và tính lại gợi ý theo tuần.',
  'Reopen plan': 'Mở lại kế hoạch',
  'Plan reopened. Reserved money was recalculated.': 'Đã mở lại kế hoạch và tính lại số tiền giữ trước.',
  'Update': 'Cập nhật',
  'NEW FINANCE PLAN': 'KẾ HOẠCH TÀI CHÍNH MỚI',
  'Plan upcoming money': 'Lập kế hoạch dòng tiền sắp tới',
  'A plan is a forecast. It never executes a payment. Payment approval remains in Agent & Payments.': 'Kế hoạch chỉ là dự báo và không thực thi thanh toán. Việc phê duyệt thanh toán vẫn nằm trong Tác vụ & Thanh toán.',
  'Plan name': 'Tên kế hoạch',
  'Plan type': 'Loại kế hoạch',
  'One-time expense': 'Khoản chi một lần',
  'Recurring bill': 'Hóa đơn định kỳ',
  'Savings goal': 'Mục tiêu tiết kiệm',
  'Category': 'Danh mục',
  'Amount (VND)': 'Số tiền (VND)',
  'Cadence': 'Chu kỳ',
  'Once': 'Một lần',
  'Weekly': 'Hàng tuần',
  'Monthly': 'Hàng tháng',
  'Next due date': 'Ngày đến hạn tiếp theo',
  'Funding source': 'Nguồn tiền',
  'Decide later': 'Chọn sau',
  'Reserve this money in available-to-allocate calculations': 'Giữ trước khoản này khi tính số tiền có thể phân bổ',
  'Notes': 'Ghi chú',
  'Add plan': 'Thêm kế hoạch',
  'EDIT FINANCE PLAN': 'SỬA KẾ HOẠCH TÀI CHÍNH',
  'Saving recalculates the 30-day reserved amount immediately.': 'Khi lưu, số tiền giữ trước trong 30 ngày được tính lại ngay.',
  'Save plan': 'Lưu kế hoạch',
  'Money source added. Manual balances are included in your personal overview.': 'Đã thêm nguồn tiền. Số dư nhập thủ công được tính vào tổng quan cá nhân.',
  'Manual money source updated.': 'Đã cập nhật nguồn tiền thủ công.',
  'Manual money source archived. Historical plans are preserved.': 'Đã lưu trữ nguồn tiền thủ công. Các kế hoạch lịch sử vẫn được giữ lại.',
  'Plan added. Reserved money and available balance were recalculated.': 'Đã thêm kế hoạch. Số tiền giữ trước và số dư có thể phân bổ đã được tính lại.',
  'Plan updated and the 30-day projection was recalculated.': 'Đã cập nhật kế hoạch và tính lại dự báo 30 ngày.',
  'Plan marked complete. Reserved money was released.': 'Đã đánh dấu kế hoạch hoàn tất và giải phóng số tiền giữ trước.',
  'Plan archived. It no longer affects projections.': 'Đã lưu trữ kế hoạch. Kế hoạch không còn ảnh hưởng dự báo.',
  'Monthly budget updated. The weekly guide was recalculated.': 'Đã cập nhật ngân sách tháng và tính lại gợi ý theo tuần.',
  'Synthetic workspace data reset.': 'Đã đặt lại dữ liệu mô phỏng trong không gian làm việc.',
  'CONNECTED': 'ĐÃ KẾT NỐI',
  'MANUAL': 'THỦ CÔNG',
  'CASH': 'TIỀN MẶT',
  'COMPLETED': 'HOÀN TẤT',
  '↺ Reset demo data': '↺ Đặt lại dữ liệu demo',
  'Checking balance': 'Số dư tài khoản thanh toán',
  'VND · synthetic account': 'VND · tài khoản mô phỏng',
  'Income in dataset': 'Thu nhập trong dữ liệu',
  'Internal transfers excluded': 'Không tính chuyển khoản nội bộ',
  'Expenses in dataset': 'Chi tiêu trong dữ liệu',
  'Refunds tracked separately': 'Hoàn tiền được theo dõi riêng',
  'Needs review': 'Cần xem xét',
  'Medium or low confidence': 'Độ tin cậy trung bình hoặc thấp',
  'SIMULATED BANK EVENT': 'SỰ KIỆN NGÂN HÀNG MÔ PHỎNG',
  'DEMO TRANSACTION LAB': 'PHÒNG THÍ NGHIỆM GIAO DỊCH DEMO',
  'DEMO BANK EVENT SOURCE': 'NGUỒN SỰ KIỆN NGÂN HÀNG DEMO',
  'Simulate an incoming bank event': 'Mô phỏng sự kiện đến từ ngân hàng',
  'This developer-only tool represents an event received after a transaction happened in the banking app. Production users do not create transactions here.': 'Công cụ dành cho demo này mô phỏng sự kiện được nhận sau khi giao dịch đã xảy ra trong ứng dụng ngân hàng. Người dùng production không tạo giao dịch tại đây.',
  'FinBridge receives synthetic bank data, rejects duplicates, normalizes it and applies deterministic categorization rules. The transaction then appears in History or Needs review.': 'FinBridge nhận dữ liệu ngân hàng mô phỏng, loại bỏ bản trùng, chuẩn hóa và áp dụng quy tắc phân loại. Giao dịch sau đó xuất hiện trong Lịch sử hoặc Cần xử lý.',
  'Receive coffee purchase': 'Nhận giao dịch mua cà phê',
  'Highlands Coffee is categorized automatically as Food & Drinks': 'Highlands Coffee được tự động phân loại vào Ăn uống',
  'Receive campus purchase': 'Nhận giao dịch mua tại trường',
  'Campus Store enters Needs review with a suggested category': 'Campus Store được đưa vào Cần xử lý cùng danh mục đề xuất',
  'Receive unknown QR payment': 'Nhận thanh toán QR không xác định',
  'The payment already happened; FinBridge asks its purpose for bookkeeping': 'Thanh toán đã xảy ra; FinBridge hỏi mục đích để hoàn thiện sổ thu chi',
  'Receive salary event': 'Nhận sự kiện trả lương',
  'Receive refund event': 'Nhận sự kiện hoàn tiền',
  'Create a test transaction': 'Tạo giao dịch để kiểm thử',
  'Each button creates one new synthetic bank event and places it at the top of Transaction history. The merchants below are fixed examples for testing, not your latest transactions.': 'Mỗi nút tạo một sự kiện ngân hàng mô phỏng mới và đặt nó ở đầu Lịch sử giao dịch. Các đơn vị bán hàng bên dưới là ví dụ cố định để kiểm thử, không phải giao dịch mới nhất của bạn.',
  'What happens when I click?': 'Điều gì xảy ra khi tôi bấm?',
  'A new demo event is normalized, classified using deterministic rules, then added to the transaction list. Use these scenarios to see each safety and review path.': 'Một sự kiện demo mới được chuẩn hóa, phân loại bằng quy tắc xác định, rồi thêm vào danh sách giao dịch. Dùng các kịch bản này để xem từng luồng an toàn và xem xét.',
  'Test categorization confidence': 'Kiểm tra độ tin cậy phân loại',
  'Try how the app handles a clear, uncertain, or unknown merchant.': 'Thử cách ứng dụng xử lý đơn vị bán hàng rõ ràng, chưa chắc chắn hoặc không xác định.',
  'Create coffee purchase': 'Tạo giao dịch mua cà phê',
  'Example: Highlands Coffee → automatically categorizes as Food & Drinks': 'Ví dụ: Highlands Coffee → tự động phân loại là Ăn uống',
  'Create campus purchase': 'Tạo giao dịch mua tại trường',
  'Example: Campus Store → asks you to confirm the suggested category': 'Ví dụ: Campus Store → yêu cầu bạn xác nhận danh mục đề xuất',
  'Create unknown QR payment': 'Tạo thanh toán QR không xác định',
  'Example: Unknown QR → asks for the transaction purpose': 'Ví dụ: QR không xác định → hỏi mục đích giao dịch',
  'Test money movement': 'Kiểm tra dòng tiền',
  'Try how totals change for income, your own transfer, and a refund.': 'Thử cách các tổng số thay đổi với thu nhập, chuyển tiền của chính bạn và hoàn tiền.',
  'Create demo salary': 'Tạo lương demo',
  'Adds 900,000 VND to income and checking balance': 'Thêm 900.000 VND vào thu nhập và tài khoản thanh toán',
  'Move money to savings': 'Chuyển tiền vào tiết kiệm',
  'Moves 200,000 VND between your accounts; income and expenses stay unchanged': 'Chuyển 200.000 VND giữa các tài khoản của bạn; thu nhập và chi tiêu không đổi',
  'Create purchase refund': 'Tạo hoàn tiền mua hàng',
  'Records an 85,000 VND refund separately from income': 'Ghi nhận hoàn tiền 85.000 VND riêng với thu nhập',
  'Test each confidence path': 'Kiểm tra từng mức độ tin cậy',
  'Deterministic merchant rules run immediately after Phase 1 normalization.': 'Quy tắc nhận diện đơn vị bán hàng chạy ngay sau bước chuẩn hóa của Giai đoạn 1.',
  'High confidence': 'Độ tin cậy cao',
  'Highlands · auto category': 'Highlands · tự động phân loại',
  'Medium confidence': 'Độ tin cậy trung bình',
  'Campus Store · confirm popup': 'Campus Store · popup xác nhận',
  'Low confidence': 'Độ tin cậy thấp',
  'Unknown QR · ask purpose': 'QR không xác định · hỏi mục đích',
  'Income': 'Thu nhập',
  'Demo salary · 99%': 'Lương demo · 99%',
  'Internal Transfer': 'Chuyển khoản nội bộ',
  'Checking → savings · 99%': 'Thanh toán → tiết kiệm · 99%',
  'Refund': 'Hoàn tiền',
  'Matched description · 95%': 'Mô tả khớp · 95%',
  'Rules: ≥90% auto-categorize · 60–89% confirm suggestion · below 60% ask transaction purpose. No LLM is used for balances or categorization in this phase.': 'Quy tắc: ≥90% tự động phân loại · 60–89% xác nhận đề xuất · dưới 60% hỏi mục đích giao dịch. Không dùng LLM để tính số dư hoặc phân loại.',
  'TRANSACTION INBOX': 'HỘP GIAO DỊCH CẦN XỬ LÝ',
  'Needs your review': 'Cần bạn xem xét',
  'These transactions already happened at the bank. Your answer only completes their purpose and category for budgets and reports.': 'Các giao dịch này đã xảy ra tại ngân hàng. Câu trả lời của bạn chỉ hoàn thiện mục đích và danh mục cho ngân sách và báo cáo.',
  'How decisions work': 'Cách hệ thống ra quyết định',
  '90% or higher is categorized automatically · 60–89% asks you to confirm · below 60% asks for purpose and category.': 'Từ 90% tự động phân loại · 60–89% yêu cầu xác nhận · dưới 60% hỏi mục đích và danh mục.',
  'You are all caught up. New uncertain bank transactions will appear here.': 'Bạn đã xử lý xong. Giao dịch ngân hàng chưa chắc chắn mới sẽ xuất hiện tại đây.',
  'PURPOSE NEEDED': 'CẦN MỤC ĐÍCH',
  'CONFIRM SUGGESTION': 'XÁC NHẬN ĐỀ XUẤT',
  'What was this payment for?': 'Khoản thanh toán này dùng cho việc gì?',
  'Suggested category': 'Danh mục đề xuất',
  'You can accept it or choose another category.': 'Bạn có thể chấp nhận hoặc chọn danh mục khác.',
  'Choose a category': 'Chọn danh mục',
  'Create a new category…': 'Tạo danh mục mới…',
  'New category name': 'Tên danh mục mới',
  'Save categorization': 'Lưu phân loại',
  'CATEGORY MANAGEMENT': 'QUẢN LÝ DANH MỤC',
  'Your transaction categories': 'Danh mục giao dịch của bạn',
  'System categories keep reports consistent. Add personal categories when the built-in list does not describe your spending.': 'Danh mục hệ thống giúp báo cáo nhất quán. Hãy thêm danh mục cá nhân khi danh sách có sẵn chưa mô tả đúng khoản chi.',
  'New personal category': 'Danh mục cá nhân mới',
  'Add category': 'Thêm danh mục',
  'System categories': 'Danh mục hệ thống',
  'Always available and cannot be archived.': 'Luôn khả dụng và không thể lưu trữ.',
  'Personal categories': 'Danh mục cá nhân',
  'Rename a category to correct its display name. The new name is applied to its transactions and reports. Archiving removes it from future selection.': 'Đổi tên danh mục để chỉnh cách hiển thị. Tên mới được áp dụng cho giao dịch và báo cáo. Lưu trữ sẽ loại danh mục khỏi các lựa chọn mới.',
  'Archiving removes a category from future selection while preserving transaction history.': 'Lưu trữ sẽ loại danh mục khỏi lựa chọn mới nhưng vẫn giữ nguyên lịch sử giao dịch.',
  'No personal categories yet.': 'Chưa có danh mục cá nhân.',
  'Rename': 'Đổi tên',
  'New name': 'Tên mới',
  'Save name': 'Lưu tên',
  'Archive': 'Lưu trữ',
  'Restore': 'Khôi phục',
  'Example: Lunch with classmates': 'Ví dụ: Ăn trưa cùng bạn học',
  'Example: Pet care': 'Ví dụ: Chăm sóc thú cưng',
  'PROACTIVE FEED': 'THÔNG TIN CHỦ ĐỘNG',
  'Worth your attention': 'Nội dung cần bạn chú ý',
  'Every insight shows the deterministic evidence behind it.': 'Mỗi thông tin đều hiển thị bằng chứng theo quy tắc.',
  'Evidence:': 'Bằng chứng:',
  'BUDGET SUMMARY': 'TỔNG QUAN NGÂN SÁCH',
  'Monthly category limits': 'Hạn mức theo danh mục hàng tháng',
  'Only confirmed and auto-categorized expenses are allocated.': 'Chỉ phân bổ các khoản chi đã xác nhận hoặc tự động phân loại.',
  'SHARED DEMO PROFILE': 'HỒ SƠ DEMO DÙNG CHUNG',
  'Demo accounts': 'Tài khoản demo',
  'CROSS BORDER STUDENT FINANCE · SYNTHETIC DATA': 'TÀI CHÍNH DU HỌC XUYÊN BIÊN GIỚI · DỮ LIỆU MÔ PHỎNG',
  'Study expense planning': 'Lập kế hoạch chi phí du học',
  'EDUCATION BILLS': 'HÓA ĐƠN GIÁO DỤC',
  'Manage and verify provider bills': 'Quản lý và xác minh hóa đơn nhà cung cấp',
  '← Scroll horizontally to view bills and providers →': '← Cuộn ngang để xem hóa đơn và nhà cung cấp →',
  'Verification belongs to each bill. Select a verified active bill before comparing payment channels.': 'Mỗi hóa đơn có trạng thái xác minh riêng. Hãy chọn hóa đơn đang hoạt động và đã xác minh trước khi so sánh kênh thanh toán.',
  'Select bill': 'Chọn hóa đơn',
  'Unable to confirm the result. Reload to check the current data before trying again.': 'Không thể xác nhận kết quả. Hãy tải lại trang để kiểm tra dữ liệu hiện tại trước khi thử lại.',
  'Search': 'Tìm kiếm',
  'Bill, provider, reference or beneficiary': 'Hóa đơn, nhà cung cấp, mã tham chiếu hoặc người thụ hưởng',
  'Status': 'Trạng thái',
  'All statuses': 'Tất cả trạng thái',
  'Active': 'Đang hoạt động',
  'Paid': 'Đã thanh toán',
  'Archived': 'Đã lưu trữ',
  'Cancelled': 'Đã hủy',
  'Verification': 'Xác minh',
  'All verification states': 'Tất cả trạng thái xác minh',
  'Verified beneficiary': 'Người thụ hưởng đã xác minh',
  'Needs verification': 'Cần xác minh',
  'Sort': 'Sắp xếp',
  'Due date: nearest': 'Hạn gần nhất',
  'Newest added': 'Mới thêm gần đây',
  'Amount: highest': 'Số tiền cao nhất',
  'Provider: A–Z': 'Nhà cung cấp: A–Z',
  'Clear filters': 'Xóa bộ lọc',
  'Bill and provider': 'Hóa đơn và nhà cung cấp',
  'Beneficiary': 'Người thụ hưởng',
  'Amount and due date': 'Số tiền và hạn thanh toán',
  'State': 'Tình trạng',
  'Actions': 'Thao tác',
  'VERIFIED BENEFICIARY': 'NGƯỜI THỤ HƯỞNG ĐÃ XÁC MINH',
  'NEEDS VERIFICATION': 'CẦN XÁC MINH',
  'Only verified active bills can be selected for comparison.': 'Chỉ hóa đơn đang hoạt động và đã xác minh mới có thể được chọn để so sánh.',
  'No education bills match these filters.': 'Không có hóa đơn giáo dục phù hợp với bộ lọc.',
  'Beneficiary legal name': 'Tên pháp lý của người thụ hưởng',
  'Receiving bank or payment provider': 'Ngân hàng nhận hoặc nhà cung cấp thanh toán',
  'SWIFT/BIC or bank routing code': 'Mã SWIFT/BIC hoặc mã định tuyến ngân hàng',
  'Beneficiary account / provider ID': 'Tài khoản người thụ hưởng / mã nhà cung cấp',
  'Receiving bank': 'Ngân hàng nhận',
  'SWIFT/BIC or routing': 'SWIFT/BIC hoặc mã định tuyến',
  'Recipient account': 'Tài khoản nhận',
  'TRUSTED EDUCATION BENEFICIARY REGISTRY · SYNTHETIC': 'DANH BẠ NGƯỜI THỤ HƯỞNG GIÁO DỤC ĐÁNG TIN CẬY · MÔ PHỎNG',
  'Beneficiary verified': 'Đã xác minh người thụ hưởng',
  'Beneficiary mismatch': 'Người thụ hưởng không khớp',
  'No matching trusted beneficiary profile': 'Không có hồ sơ người thụ hưởng đáng tin cậy khớp',
  'Provider, beneficiary, receiving bank, account, corridor and currency match the trusted demo registry': 'Nhà cung cấp, người thụ hưởng, ngân hàng nhận, tài khoản, hành lang và tiền tệ khớp danh bạ demo đáng tin cậy',
  'One or more beneficiary fields do not match the trusted education-provider registry': 'Một hoặc nhiều trường người thụ hưởng không khớp danh bạ nhà cung cấp giáo dục đáng tin cậy',
  'School or education provider': 'Trường hoặc nhà cung cấp giáo dục',
  'Add and verify bill': 'Thêm và xác minh hóa đơn',
  'Enter beneficiary details from the official bill or authenticated provider portal. The demo verifies them against a synthetic trusted registry; it does not contact the bank or school.': 'Nhập thông tin người thụ hưởng từ hóa đơn chính thức hoặc cổng nhà cung cấp đã xác thực. Bản demo đối chiếu với danh bạ mô phỏng đáng tin cậy và không liên hệ ngân hàng hoặc trường.',
  'Changing the bill or beneficiary invalidates every pending plan and approval linked to the previous details.': 'Thay đổi hóa đơn hoặc người thụ hưởng sẽ vô hiệu mọi kế hoạch và phê duyệt đang chờ gắn với thông tin cũ.',
  'This tab is only for bills issued by a verified school or education provider. Personal transfers to friends or family belong to the future Transfers module.': 'Tab này chỉ dành cho hóa đơn do trường hoặc nhà cung cấp giáo dục đã xác minh phát hành. Chuyển tiền cho bạn bè hoặc người thân thuộc module Chuyển tiền trong tương lai.',
  '+ Add student bill': '+ Thêm hóa đơn du học',
  'Manage several student bills, verify the selected recipient and compare simulated payment channels. No provider API or real payment is connected.': 'Quản lý nhiều khoản phí du học, xác minh người nhận của khoản đang chọn và so sánh các kênh thanh toán mô phỏng. Không kết nối API nhà cung cấp hoặc tiền thật.',
  '+ Add expense': '+ Thêm khoản phí',
  'STUDENT EXPENSES': 'CÁC KHOẢN PHÍ DU HỌC',
  'VERIFIED EDUCATION BILLS': 'HÓA ĐƠN GIÁO DỤC ĐÃ XÁC MINH',
  'Select an active bill to compare': 'Chọn hóa đơn đang hoạt động để so sánh',
  'The selected school or provider bill controls recipient verification, quote amounts and the action plan.': 'Hóa đơn trường hoặc nhà cung cấp đang chọn quyết định việc xác minh người nhận, số tiền báo giá và kế hoạch hành động.',
  'Select an expense to compare': 'Chọn khoản phí để so sánh',
  'The selected bill controls recipient verification, quote amounts and the action plan.': 'Khoản đang chọn quyết định việc xác minh người nhận, số tiền báo giá và kế hoạch hành động.',
  'Selected': 'Đang chọn',
  'Compare this expense': 'So sánh khoản này',
  'Compare': 'So sánh',
  'Edit': 'Sửa',
  'Archive': 'Lưu trữ',
  'Cancel bill': 'Hủy khoản phí',
  'Restore': 'Khôi phục',
  'ARCHIVED': 'ĐÃ LƯU TRỮ',
  'CANCELLED': 'ĐÃ HỦY',
  'PAID': 'ĐÃ THANH TOÁN',
  'Payment completed · receipt and Audit Log are immutable': 'Đã thanh toán · biên nhận và Audit Log không thể chỉnh sửa',
  'SELECTED EXPENSE': 'KHOẢN PHÍ ĐANG CHỌN',
  'Institution': 'Đơn vị thu',
  'Destination': 'Điểm đến',
  'Destination country': 'Quốc gia đích',
  'CONNECTED PAYMENT CHANNELS': 'KÊNH THANH TOÁN ĐÃ KẾT NỐI',
  'Compare the essentials': 'So sánh thông tin cần thiết',
  'Each compact card shows the information needed to decide. Scroll horizontally, then open details for the complete fee and quote breakdown.': 'Mỗi thẻ gọn hiển thị thông tin cần để ra quyết định. Cuộn ngang rồi mở chi tiết để xem đầy đủ phí và báo giá.',
  '← Scroll horizontally to compare channels →': '← Cuộn ngang để so sánh các kênh →',
  'Priority and order': 'Ưu tiên và thứ tự',
  'This choice determines both recommendation rank and display order.': 'Lựa chọn này quyết định cả thứ hạng đề xuất và thứ tự hiển thị.',
  'Remaining': 'Còn lại',
  'Details': 'Chi tiết',
  'Create plan': 'Tạo kế hoạch',
  'SIMULATED QUOTE DETAIL': 'CHI TIẾT BÁO GIÁ MÔ PHỎNG',
  'Funding account': 'Tài khoản nguồn',
  'These are not linked to your accounts and cannot be selected or executed.': 'Các kênh này chưa liên kết với tài khoản của bạn nên không thể chọn hoặc thực thi.',
  'Reference saving only': 'Chỉ là mức tiết kiệm tham khảo',
  'NEW STUDENT EXPENSE': 'KHOẢN PHÍ DU HỌC MỚI',
  'NEW EDUCATION BILL': 'HÓA ĐƠN GIÁO DỤC MỚI',
  'Add a school or provider bill': 'Thêm hóa đơn của trường hoặc nhà cung cấp',
  'Only enter bills issued by a verified school or education provider. Personal payments to friends or family are outside this tab.': 'Chỉ nhập hóa đơn do trường hoặc nhà cung cấp giáo dục đã xác minh phát hành. Thanh toán cá nhân cho bạn bè hoặc người thân không thuộc tab này.',
  'EDIT STUDENT BILL': 'SỬA HÓA ĐƠN DU HỌC',
  'Changing this bill invalidates every pending plan and approval linked to its previous details.': 'Thay đổi hóa đơn này sẽ vô hiệu mọi kế hoạch và phê duyệt đang chờ gắn với thông tin cũ.',
  'Add a bill or fee': 'Thêm hóa đơn hoặc khoản phí',
  'Enter the verified fields from your bill. An attachment is stored as demo evidence only; this version does not run OCR.': 'Nhập các trường đã kiểm tra từ hóa đơn. File đính kèm chỉ được lưu làm bằng chứng demo; phiên bản này chưa chạy OCR.',
  'Expense type': 'Loại chi phí',
  'Education expense type': 'Loại chi phí giáo dục',
  'Expense name': 'Tên khoản phí',
  'Bill name': 'Tên hóa đơn',
  'Institution or provider': 'Trường hoặc đơn vị cung cấp',
  'Verified school or education provider': 'Trường hoặc nhà cung cấp giáo dục đã xác minh',
  'Currency': 'Tiền tệ',
  'China': 'Trung Quốc',
  'United States': 'Hoa Kỳ',
  'Australia': 'Úc',
  'Recipient account': 'Tài khoản người nhận',
  'Verified recipient account': 'Tài khoản người nhận đã xác minh',
  'Payment reference': 'Mã tham chiếu thanh toán',
  'Attachment (optional)': 'File đính kèm (không bắt buộc)',
  'PDF, JPG or PNG · maximum 5 MB · no OCR in this version': 'PDF, JPG hoặc PNG · tối đa 5 MB · phiên bản này chưa có OCR',
  'Add and compare expense': 'Thêm và so sánh khoản phí',
  'Add and compare bill': 'Thêm và so sánh hóa đơn',
  'Save and invalidate old plans': 'Lưu và vô hiệu kế hoạch cũ',
  'Tuition': 'Học phí',
  'Dormitory': 'Ký túc xá',
  'Insurance': 'Bảo hiểm',
  'Student insurance': 'Bảo hiểm du học',
  'Visa': 'Thị thực',
  'Visa fee': 'Phí thị thực',
  'Living expense': 'Sinh hoạt phí',
  'Provider-billed living expense': 'Sinh hoạt phí do nhà cung cấp thu',
  'Other': 'Khác',
  'Other education fee': 'Chi phí giáo dục khác',
  'Tuition planning': 'Lập kế hoạch học phí',
  'One fixed corridor and one verified bill. No provider API or real payment is connected.': 'Một hành lang cố định và một hóa đơn đã xác minh. Không kết nối API nhà cung cấp hoặc thanh toán thật.',
  'Rank eligible channels by': 'Xếp hạng kênh đủ điều kiện theo',
  'Cheaper': 'Rẻ hơn',
  'Faster': 'Nhanh hơn',
  'Safer': 'An toàn hơn',
  'Student': 'Sinh viên',
  'Corridor': 'Hành lang',
  'Currencies': 'Tiền tệ',
  'Preference': 'Ưu tiên',
  'TUITION BILL': 'HÓA ĐƠN HỌC PHÍ',
  'Due date': 'Hạn thanh toán',
  'Reference': 'Mã tham chiếu',
  'Recipient': 'Người nhận',
  'Evidence': 'Bằng chứng',
  'SCHOOL REGISTRY': 'DANH BẠ NHÀ TRƯỜNG',
  'VERIFIED EDUCATION PROVIDER REGISTRY': 'DANH BẠ NHÀ CUNG CẤP GIÁO DỤC ĐÃ XÁC MINH',
  'Recipient verified': 'Đã xác minh người nhận',
  'Recipient mismatch': 'Người nhận không khớp',
  'No matching account': 'Không có tài khoản khớp',
  'No matching education-provider account': 'Không có tài khoản nhà cung cấp giáo dục khớp',
  'Bill institution, recipient account, corridor and currency match the School Registry': 'Trường, tài khoản nhận, hành lang và tiền tệ trên hóa đơn khớp Danh bạ Nhà trường',
  'Bill recipient does not match the School Registry': 'Người nhận trên hóa đơn không khớp Danh bạ Nhà trường',
  'Bill institution, recipient account, corridor and currency match the verified Education Provider Registry': 'Trường hoặc nhà cung cấp, tài khoản nhận, hành lang và tiền tệ trên hóa đơn khớp Danh bạ Nhà cung cấp Giáo dục đã xác minh',
  'Bill recipient is not registered for this school or education provider': 'Tài khoản người nhận chưa được đăng ký cho trường hoặc nhà cung cấp giáo dục này',
  'Eligible channel comparison': 'So sánh kênh đủ điều kiện',
  'Eligibility is applied before preference and price. Bank B remains reference-only even when its synthetic rate is lower.': 'Điều kiện sử dụng được xét trước ưu tiên và giá. Bank B chỉ để tham khảo dù tỷ giá mô phỏng thấp hơn.',
  'Available payment balance': 'Số dư thanh toán hiện có',
  'Payment Sandbox · checked again before approval': 'Payment Sandbox · được kiểm tra lại trước khi phê duyệt',
  'Remaining after payment': 'Còn lại sau thanh toán',
  'Insufficient balance': 'Không đủ số dư',
  'Unavailable for corridor': 'Không hỗ trợ hành lang này',
  'Below safety buffer': 'Dưới vùng số dư an toàn',
  'Safety buffer preserved': 'Vẫn giữ được vùng số dư an toàn',
  '↻ Refresh synthetic quotes': '↻ Làm mới báo giá mô phỏng',
  'Eligible': 'Đủ điều kiện',
  'Unavailable': 'Không khả dụng',
  'Student profile includes an eligible Alipay education account': 'Hồ sơ sinh viên có tài khoản giáo dục Alipay đủ điều kiện',
  'Student profile includes an active Bank A account': 'Hồ sơ sinh viên có tài khoản Bank A đang hoạt động',
  'Unavailable: the student does not have a Bank B account': 'Không khả dụng: sinh viên không có tài khoản Bank B',
  'Landed cost': 'Tổng chi phí thực trả',
  'Rate': 'Tỷ giá',
  'Source amount': 'Số tiền nguồn',
  'Transfer fee': 'Phí chuyển tiền',
  'FX markup': 'Phụ phí tỷ giá',
  'Expected received': 'Dự kiến nhận',
  'Settlement': 'Thời gian quyết toán',
  'Latest safe date': 'Ngày an toàn cuối cùng',
  'Safety score': 'Điểm an toàn',
  'Quote expired · refresh required': 'Báo giá đã hết hạn · cần làm mới',
  'Create Approval Mode plan': 'Tạo kế hoạch cần phê duyệt',
  'No select or execute action': 'Không thể chọn hoặc thực thi',
  'CONTROLLED EXECUTION · PAYMENT SANDBOX': 'THỰC THI CÓ KIỂM SOÁT · PAYMENT SANDBOX',
  'Conversation, policy and action plan': 'Hội thoại, chính sách và kế hoạch hành động',
  'The conversation creates plans only. Policy Guard is deterministic and the backend alone can execute sandbox actions.': 'Hội thoại chỉ tạo kế hoạch. Policy Guard chạy theo quy tắc và chỉ backend mới có thể thực thi trong sandbox.',
  'ACTIVE': 'ĐANG HOẠT ĐỘNG',
  'PAUSED': 'ĐÃ TẠM DỪNG',
  'APPROVAL MODE': 'CHẾ ĐỘ PHÊ DUYỆT',
  'DELEGATED MODE': 'CHẾ ĐỘ ỦY QUYỀN',
  'TUITION INSIGHT': 'THÔNG TIN HỌC PHÍ',
  'Selected tuition plan is ready for review': 'Kế hoạch học phí đã chọn đang chờ xem xét',
  'Selected plan': 'Kế hoạch đã chọn',
  'Permission mode': 'Chế độ quyền hạn',
  'Approval': 'Phê duyệt',
  'Delegated': 'Ủy quyền',
  'Delegated limit': 'Hạn mức ủy quyền',
  'Safety buffer': 'Số dư an toàn',
  'Runtime': 'Chế độ chạy',
  'Enable offline fallback': 'Bật chế độ dự phòng ngoại tuyến',
  '■ Emergency Stop': '■ Dừng khẩn cấp',
  'Resume Agent': 'Tiếp tục tác vụ',
  'GROUNDED CONVERSATION': 'HỘI THOẠI DỰA TRÊN DỮ LIỆU',
  'Ask Atlas': 'Hỏi Atlas',
  'Deterministic offline responses': 'Phản hồi ngoại tuyến theo quy tắc',
  'Guarded AI intent · deterministic answers': 'AI chỉ hiểu ý định · câu trả lời theo dữ liệu xác định',
  'Deterministic offline fallback': 'Chế độ dự phòng ngoại tuyến theo quy tắc',
  'ONLINE · GUARDED INTENT': 'TRỰC TUYẾN · Ý ĐỊNH ĐƯỢC KIỂM SOÁT',
  'OFFLINE FALLBACK': 'DỰ PHÒNG NGOẠI TUYẾN',
  'This request cannot change payment safety controls or bypass approval.': 'Yêu cầu này không thể thay đổi kiểm soát an toàn thanh toán hoặc bỏ qua phê duyệt.',
  'Please clarify whether you want to compare tuition channels, check tuition status, or prepare a tuition-payment plan.': 'Hãy cho biết bạn muốn so sánh kênh học phí, kiểm tra trạng thái học phí hay chuẩn bị kế hoạch thanh toán học phí.',
  'AI is temporarily unavailable; use the guided tuition flow to compare channels or create a plan.': 'AI tạm thời không khả dụng; hãy dùng luồng học phí có hướng dẫn để so sánh kênh hoặc tạo kế hoạch.',
  'To prepare the tuition-payment plan, select a source account.': 'Để chuẩn bị kế hoạch thanh toán học phí, hãy chọn một tài khoản nguồn.',
  'Bank B has a lower quoted rate but is unavailable for your verified profile and cannot be selected.': 'Bank B có tỷ giá báo thấp hơn nhưng không khả dụng với hồ sơ đã xác minh và không thể được chọn.',
  'Send': 'Gửi',
  'Create 250,000 VND low-risk action': 'Tạo tác vụ rủi ro thấp 250.000 VND',
  'Test malicious instruction': 'Kiểm tra chỉ dẫn độc hại',
  'STRUCTURED ACTION PLAN': 'KẾ HOẠCH HÀNH ĐỘNG CÓ CẤU TRÚC',
  'Latest plan': 'Kế hoạch mới nhất',
  'Choose an eligible tuition channel or create a low-risk action.': 'Chọn kênh học phí đủ điều kiện hoặc tạo tác vụ rủi ro thấp.',
  'Debit': 'Ghi nợ',
  'Destination': 'Đích đến',
  'Channel / quote': 'Kênh / báo giá',
  'Permission': 'Quyền hạn',
  'Idempotency': 'Chống thực thi trùng',
  'Impact:': 'Tác động:',
  'Risk:': 'Rủi ro:',
  'DEADLINE RISK · settlement time plus the one-day safety margin may miss the bill due date.': 'RỦI RO QUÁ HẠN · thời gian quyết toán cộng biên an toàn một ngày có thể trễ hạn hóa đơn.',
  'Approve exact plan & execute': 'Phê duyệt đúng kế hoạch và thực thi',
  'Retry with same idempotency key': 'Thử lại với cùng khóa chống trùng',
  'MULTI-CURRENCY PAYMENT SANDBOX': 'PAYMENT SANDBOX ĐA TIỀN TỆ',
  'Latest receipt': 'Biên nhận mới nhất',
  'SIMULATED': 'MÔ PHỎNG',
  'No sandbox payment has executed.': 'Chưa có thanh toán sandbox nào được thực thi.',
  'TRANSACTION ID': 'MÃ GIAO DỊCH',
  'VND debit': 'Ghi nợ VND',
  'Converted at stored quote': 'Quy đổi theo báo giá đã lưu',
  'Fee deduction': 'Khấu trừ phí',
  'CNY credit': 'Ghi có CNY',
  'VND credit': 'Ghi có VND',
  'SANDBOX LEDGER': 'SỔ CÁI SANDBOX',
  'Balances': 'Số dư',
  'IMMUTABLE DEMO EVIDENCE': 'BẰNG CHỨNG DEMO BẤT BIẾN',
  'Audit Log': 'Nhật ký kiểm toán',
  'Time / actor': 'Thời gian / tác nhân',
  'Event': 'Sự kiện',
  'Correlation': 'Liên kết',
  'Status': 'Trạng thái',
  'Reason': 'Lý do',
  'TRANSACTION DASHBOARD': 'BẢNG ĐIỀU KHIỂN GIAO DỊCH',
  'TRANSACTION HISTORY': 'LỊCH SỬ GIAO DỊCH',
  'Recent transactions': 'Giao dịch gần đây',
  'Newest transactions appear first. Search or filter the list to focus on what you want to review.': 'Giao dịch mới nhất xuất hiện trước. Tìm kiếm hoặc lọc danh sách để tập trung vào nội dung bạn muốn xem xét.',
  'Search': 'Tìm kiếm',
  'Search merchant or evidence': 'Tìm đơn vị bán hàng hoặc bằng chứng',
  'All transaction types': 'Tất cả loại giao dịch',
  'Review status': 'Trạng thái xử lý',
  'All processing states': 'Tất cả trạng thái xử lý',
  'Automatically categorized': 'Đã tự động phân loại',
  'Confirmed by you': 'Bạn đã xác nhận',
  'Needs confirmation': 'Cần xác nhận',
  'Needs purpose': 'Cần nhập mục đích',
  'All categories': 'Tất cả danh mục',
  'Sort': 'Sắp xếp',
  'Newest first': 'Mới nhất trước',
  'Largest amount': 'Số tiền lớn nhất',
  'Smallest amount': 'Số tiền nhỏ nhất',
  'Clear filters': 'Xóa bộ lọc',
  'Showing 5 transactions per page': 'Hiển thị 5 giao dịch mỗi trang',
  'No transactions match these filters.': 'Không có giao dịch nào khớp các bộ lọc này.',
  'Previous': 'Trang trước',
  'Next': 'Trang sau',
  'Normalized and categorized ledger': 'Sổ giao dịch đã chuẩn hóa và phân loại',
  'Category, confidence, evidence and review state remain visible.': 'Danh mục, độ tin cậy, bằng chứng và trạng thái xem xét luôn hiển thị.',
  'Merchant / evidence': 'Đơn vị bán hàng / bằng chứng',
  'Type': 'Loại',
  'Category': 'Danh mục',
  'Confidence': 'Độ tin cậy',
  'Amount': 'Số tiền',
  'Action': 'Thao tác',
  'Purpose required': 'Cần nhập mục đích',
  'Review': 'Xem xét',
  'Undo': 'Hoàn tác',
  'Expense': 'Chi tiêu',
  'Food & Drinks': 'Ăn uống',
  'Transport': 'Di chuyển',
  'Utilities': 'Tiện ích',
  'Shopping': 'Mua sắm',
  'AUTO': 'TỰ ĐỘNG',
  'CONFIRMED': 'ĐÃ XÁC NHẬN',
  'CONFIRMATION REQUIRED': 'CẦN XÁC NHẬN',
  'PURPOSE REQUIRED': 'CẦN MỤC ĐÍCH',
  'AWAITING APPROVAL': 'CHỜ PHÊ DUYỆT',
  'COMPLETED': 'HOÀN TẤT',
  'BLOCKED': 'ĐÃ CHẶN',
  'SYNTHETIC DATA · DETERMINISTIC POLICY GUARD · PAYMENT SANDBOX · PHASE 5 ONLY': 'DỮ LIỆU MÔ PHỎNG · POLICY GUARD THEO QUY TẮC · PAYMENT SANDBOX · CHỈ GIAI ĐOẠN 5',
  'LOW CONFIDENCE · PURPOSE NEEDED': 'ĐỘ TIN CẬY THẤP · CẦN MỤC ĐÍCH',
  'MEDIUM CONFIDENCE · CONFIRM CATEGORY': 'ĐỘ TIN CẬY TRUNG BÌNH · XÁC NHẬN DANH MỤC',
  'Category or purpose': 'Danh mục hoặc mục đích',
  'Cancel': 'Hủy',
  'Confirm': 'Xác nhận',
  'Category confirmed and dashboard updated.': 'Đã xác nhận danh mục và cập nhật bảng điều khiển.',
  'Category change undone.': 'Đã hoàn tác thay đổi danh mục.',
  'Conversation updated from deterministic demo data.': 'Đã cập nhật hội thoại từ dữ liệu demo theo quy tắc.',
  'Conversation updated through the guarded intent boundary.': 'Đã cập nhật hội thoại qua lớp hiểu ý định được kiểm soát.',
  'Synthetic FX quotes refreshed for five minutes.': 'Đã làm mới báo giá FX mô phỏng trong năm phút.',
  'Emergency Stop active. New actions receive AGENT PAUSED.': 'Dừng khẩn cấp đang bật. Các tác vụ mới sẽ nhận trạng thái TÁC VỤ ĐÃ TẠM DỪNG.',
  'Agent resumed.': 'Tác vụ đã hoạt động trở lại.',
  'Synthetic Phase 4 data reset.': 'Đã đặt lại dữ liệu mô phỏng Giai đoạn 4.',
  'YOUR CONNECTED PAYMENT SOURCES': 'NGUỒN THANH TOÁN ĐÃ KẾT NỐI',
  'YOUR CONNECTED PAYMENT CHANNELS': 'KÊNH THANH TOÁN ĐÃ KẾT NỐI',
  'Accounts and payment options': 'Tài khoản và phương án thanh toán',
  'Each card combines one personal account with its linked payment channel, balance, complete cost and amount remaining after payment.': 'Mỗi thẻ gộp một tài khoản cá nhân với kênh thanh toán liên kết, số dư, tổng chi phí và số tiền còn lại sau thanh toán.',
  'Connected channels': 'Kênh đã kết nối',
  'One account per channel': 'Mỗi kênh dùng một tài khoản',
  'All connected channels': 'Tất cả kênh đã kết nối',
  'Quote expired': 'Báo giá đã hết hạn',
  'Lowest landed cost': 'Tổng chi phí thấp nhất',
  'Highest remaining balance': 'Số dư còn lại cao nhất',
  'Fastest settlement': 'Quyết toán nhanh nhất',
  'Highest safety score': 'Điểm an toàn cao nhất',
  'Personal funding account': 'Tài khoản cá nhân dùng thanh toán',
  'Available balance': 'Số dư hiện có',
  'No connected channels match this filter.': 'Không có kênh đã kết nối phù hợp bộ lọc.',
  'Personal accounts': 'Tài khoản cá nhân',
  'Every connected demo account is shown. Choose the account whose balance should be used for comparison and payment.': 'Hiển thị đầy đủ các tài khoản demo đã kết nối. Chọn tài khoản có số dư sẽ được dùng để so sánh và thanh toán.',
  'All accounts': 'Tất cả tài khoản',
  'Ready for this corridor': 'Sẵn sàng cho hành lang này',
  'Needs attention': 'Cần xử lý',
  'Recommended': 'Đề xuất',
  'Highest balance': 'Số dư cao nhất',
  'Lowest balance': 'Số dư thấp nhất',
  'Name A–Z': 'Tên A–Z',
  'Ready': 'Sẵn sàng',
  'Ready for eligible cross-border channels': 'Sẵn sàng cho các kênh xuyên biên giới hợp lệ',
  'This corridor is not supported by the wallet': 'Ví này chưa hỗ trợ hành lang thanh toán này',
  'Savings account excluded from cross-border payments': 'Tài khoản tiết kiệm không dùng cho thanh toán xuyên biên giới',
  '✓ Selected for comparison': '✓ Đang dùng để so sánh',
  'Use this account': 'Dùng tài khoản này',
  'Not available for this corridor': 'Không khả dụng cho hành lang này',
  'No accounts match this filter.': 'Không có tài khoản phù hợp bộ lọc.',
  'Selected payment balance': 'Số dư thanh toán đã chọn',
  'MARKET SUGGESTIONS · REFERENCE ONLY': 'GỢI Ý THỊ TRƯỜNG · CHỈ THAM KHẢO',
  'Potentially better alternatives': 'Các lựa chọn có thể có lợi hơn',
  'These channels may be cheaper or otherwise attractive, but they are not linked to your personal accounts. Connect and verify an account before they can become executable.': 'Các kênh này có thể rẻ hơn hoặc có lợi thế khác, nhưng chưa liên kết với tài khoản cá nhân. Cần kết nối và xác minh tài khoản trước khi có thể thực thi.',
  'Not connected': 'Chưa kết nối',
  'than the cheapest currently eligible option': 'so với lựa chọn hợp lệ rẻ nhất hiện tại',
  'Reference only · no payment action': 'Chỉ tham khảo · không có thao tác thanh toán',
  'Source account': 'Tài khoản nguồn',
  'DISCONNECTED': 'Mất kết nối',
  'Account disconnected: unavailable for payments': 'Tài khoản mất kết nối: không thể dùng để thanh toán',
  'Source currency is not supported for this payment corridor': 'Tiền tệ của tài khoản không hỗ trợ hành lang thanh toán này',
  'Cross-border payments are not enabled for this source': 'Nguồn tiền này chưa được phép thanh toán xuyên biên giới',
  'EWALLET': 'Ví điện tử',
  'Manual entry': 'Nhập thủ công',
  'Ready to pay now': 'Sẵn sàng thanh toán ngay',
  'Insufficient balance': 'Không đủ số dư',
  'Unavailable for this corridor': 'Không khả dụng cho hành lang này',
  'Ready to pay': 'Sẵn sàng thanh toán',
  'Can cover the cheapest eligible option and keep the 3,000,000 VND safety buffer.': 'Đủ chi trả lựa chọn hợp lệ rẻ nhất và vẫn giữ vùng an toàn 3.000.000 VND.',
  'Selected for payment': 'Đang chọn để thanh toán',
  'Selected for comparison · cannot fund this bill': 'Đang dùng để so sánh · chưa đủ cho hóa đơn này',
  'Use for payment': 'Dùng để thanh toán',
  'Show accounts ready to pay': 'Xem tài khoản đủ tiền',
  'Select an account ready to pay': 'Chọn tài khoản đủ tiền',
  'Education wallet': 'Ví giáo dục',
  'Linked channel': 'Kênh liên kết',
  'Connected channel comparison': 'So sánh kênh đã kết nối',
  'Each channel uses its own linked account. Balance sufficiency is calculated separately after rate, fees and safety buffer.': 'Mỗi kênh sử dụng đúng tài khoản đã liên kết. Khả năng đủ tiền được tính riêng sau tỷ giá, phí và vùng an toàn.',
  'Connected executable channels': 'Kênh đã kết nối có thể thực thi',
  'One linked account per channel': 'Mỗi kênh liên kết với một tài khoản',
  'Linked funding account': 'Tài khoản nguồn liên kết',
  'Insufficient balance in linked account': 'Tài khoản liên kết không đủ số dư',
  'Each connected account has its own payment channel. Balance readiness is calculated against that channel\'s landed cost and the safety buffer.': 'Mỗi tài khoản đã kết nối có một kênh thanh toán riêng. Khả năng đủ tiền được tính theo tổng chi phí của chính kênh đó và vùng an toàn.',
  'No supported payment channel is linked to this account.': 'Chưa có kênh thanh toán được hỗ trợ liên kết với tài khoản này.',
  'Everyday account': 'Tài khoản thanh toán',
  'Savings account': 'Tài khoản tiết kiệm',
  'E-wallet': 'Ví điện tử'
}));

Object.entries({
  'Payment receipt': 'Biên nhận thanh toán', 'You approved this education payment.': 'Bạn đã phê duyệt khoản thanh toán giáo dục này.',
  'Amount credited in Sandbox': 'Số tiền đã ghi có trong Sandbox',
  'This payment completed in the Sandbox. Its approved details and receipt are preserved.': 'Thanh toán đã hoàn tất trong Sandbox. Thông tin đã phê duyệt và biên nhận được giữ nguyên.',
  'The receipt records the exact locked plan. Its beneficiary, amounts and quote cannot be edited.': 'Biên nhận ghi đúng kế hoạch đã khóa. Người thụ hưởng, số tiền và báo giá không thể chỉnh sửa.',
  'Review and approve payment': 'Kiểm tra và phê duyệt thanh toán',
  'Creating a plan does not move money. Review its locked details before approving a synthetic payment.': 'Tạo kế hoạch chưa chuyển tiền. Hãy kiểm tra thông tin đã khóa trước khi phê duyệt thanh toán mô phỏng.',
  'Back to Student finance': 'Quay lại Tài chính du học',
  'Emergency Stop is active. New payments are blocked; completed payments are not reversed.': 'Dừng khẩn cấp đang bật. Thanh toán mới bị chặn; khoản đã hoàn tất không được hoàn tác.',
  'No payment plan selected': 'Chưa chọn kế hoạch thanh toán',
  'Choose a verified bill and an eligible channel in Student finance, then select Create plan.': 'Chọn hóa đơn đã xác minh và kênh hợp lệ trong Tài chính du học, sau đó bấm Tạo kế hoạch.',
  'Review plan': 'Kiểm tra kế hoạch', 'Sandbox execution': 'Thực thi Sandbox', 'Receipt': 'Biên nhận',
  'Your approval is required for this education payment.': 'Khoản thanh toán giáo dục này bắt buộc bạn phê duyệt.',
  'Delegated mode cannot bypass this approval. Amount, beneficiary, account, channel and quote are locked to this plan.': 'Chế độ ủy quyền không bỏ qua bước phê duyệt này. Số tiền, người thụ hưởng, tài khoản, kênh và báo giá đã được khóa theo kế hoạch.',
  'Payment blocked': 'Thanh toán bị chặn',
  'Return to the bill or refresh quotes before creating a new plan.': 'Quay lại kiểm tra hóa đơn hoặc làm mới báo giá trước khi tạo kế hoạch mới.',
  'Internal transfer and recipient': 'Chuyển nội bộ và người nhận',
  'Review transfer details': 'Xem chi tiết chuyển tiền',
  'No transfer was made. Review the source account and recipient before creating a new transfer plan.': 'Chưa chuyển tiền. Kiểm tra tài khoản nguồn và người nhận trước khi tạo kế hoạch chuyển tiền mới.',
  'Before transfer, the system rechecks the source account, recipient, limits, safety buffer and Emergency Stop.': 'Trước khi chuyển tiền, hệ thống kiểm tra lại tài khoản nguồn, người nhận, hạn mức, đệm an toàn và Dừng khẩn cấp.',
  'Internal Sandbox transfer': 'Chuyển nội bộ trong Sandbox', 'Transfer channel': 'Kênh chuyển tiền',
  'Transfer purpose': 'Mục đích chuyển tiền', 'Recipient account': 'Tài khoản người nhận',
  'Review the source account, recipient and policy status before creating a new transfer plan.': 'Kiểm tra tài khoản nguồn, người nhận và trạng thái chính sách trước khi tạo kế hoạch chuyển tiền mới.',
  'Bill and beneficiary': 'Hóa đơn và người thụ hưởng', 'Funding and total cost': 'Nguồn tiền và tổng chi phí',
  'Payment channel': 'Kênh thanh toán', 'Total debit': 'Tổng tiền trừ',
  'Current source balance': 'Số dư tài khoản hiện tại', 'Source balance before payment': 'Số dư trước thanh toán',
  'Projected balance after payment': 'Số dư dự kiến sau thanh toán', 'Source balance after payment': 'Số dư sau thanh toán',
  'Quote source': 'Nguồn báo giá',
  'This is the quote used for the completed payment. Refreshing quotes does not change this receipt.': 'Đây là báo giá đã dùng cho khoản thanh toán hoàn tất. Làm mới báo giá không thay đổi biên nhận này.',
  'Policy Guard rechecks the bill, recipient, quote, account, safe balance and Emergency Stop before executing.': 'Policy Guard kiểm tra lại hóa đơn, người nhận, báo giá, tài khoản, số dư an toàn và Dừng khẩn cấp trước khi thực thi.',
  'Approve and pay in Sandbox': 'Phê duyệt và thanh toán trong Sandbox', 'Cancel plan': 'Hủy kế hoạch',
  'Processing in Payment Sandbox. Please wait; do not submit another payment.': 'Đang xử lý trong Payment Sandbox. Vui lòng chờ; không gửi thêm thanh toán.',
  'Payment completed': 'Thanh toán hoàn tất',
  'This receipt belongs to the displayed plan. No real money was moved.': 'Biên nhận thuộc đúng kế hoạch đang hiển thị. Không có tiền thật được chuyển.',
  'Total VND debit': 'Tổng tiền VND đã trừ',
  'Conversion included in total debit': 'Tiền quy đổi nằm trong tổng tiền trừ',
  'Fees included in total debit': 'Phí nằm trong tổng tiền trừ',
  'Technical plan details': 'Chi tiết kỹ thuật của kế hoạch', 'Plan ID': 'Mã kế hoạch',
  'Audit Log for this plan': 'Nhật ký kiểm toán của kế hoạch này', 'Payment plan history': 'Lịch sử kế hoạch thanh toán',
  'Reopen a plan or receipt without creating a new payment.': 'Mở lại kế hoạch hoặc biên nhận mà không tạo thanh toán mới.',
  'View receipt': 'Xem biên nhận', 'Demo tools and policy settings': 'Công cụ demo và thiết lập chính sách',
  'These controls test synthetic behavior. Education payments always require approval; changing the demo mode does not authorize them.': 'Các công cụ này dùng để kiểm tra dữ liệu mô phỏng. Thanh toán giáo dục luôn cần phê duyệt; đổi chế độ demo không cấp quyền thực thi các khoản này.',
  'This bill has already been paid.': 'Hóa đơn này đã được thanh toán.',
  'Open its receipt or select another active bill to compare channels. Another payment cannot be created for this bill.': 'Mở biên nhận hoặc chọn hóa đơn đang hoạt động khác để so sánh kênh. Không thể tạo thêm thanh toán cho hóa đơn này.',
  'Payment plan canceled. No funds were debited.': 'Đã hủy kế hoạch thanh toán. Chưa trừ tiền.',
  'The selected bill changed; reload comparison before creating a plan': 'Hóa đơn đã chọn đã thay đổi; hãy tải lại so sánh trước khi tạo kế hoạch',
  'The FX quote changed; refresh comparison and create a new plan': 'Báo giá đã thay đổi; hãy làm mới so sánh và tạo kế hoạch mới',
  'The source account does not belong to this payment channel': 'Tài khoản nguồn không thuộc kênh thanh toán này',
  'This bill is inactive or already paid; open its existing payment record instead': 'Hóa đơn không còn hoạt động hoặc đã thanh toán; hãy mở lại hồ sơ thanh toán hiện có',
  'Sandbox balances': 'Số dư Sandbox', 'Demo audit events': 'Nhật ký kiểm toán demo',
  'Payment Sandbox demo': 'Demo Payment Sandbox', 'Create low-risk demo plan': 'Tạo kế hoạch demo rủi ro thấp',
  'Create a synthetic low-risk example to explore demo permission settings. Education payments start from Student finance and always require approval.': 'Tạo ví dụ mô phỏng rủi ro thấp để thử thiết lập quyền hạn demo. Thanh toán giáo dục bắt đầu từ Tài chính du học và luôn cần phê duyệt.'
}).forEach(([key, value]) => translationsVi.set(key, value));
Object.entries({
  "Skip to main content": "Chuyển đến nội dung chính",
  "Personal planning sources": "Nguồn tiền lập kế hoạch cá nhân",
  "Separate records for everyday spending and manual cash. Payment account balances above are not included.": "Các bản ghi riêng cho chi tiêu hằng ngày và tiền mặt nhập thủ công. Không bao gồm số dư tài khoản thanh toán ở trên.",
  "Transaction history": "Lịch sử giao dịch",
  "All time": "Tất cả thời gian",
  "Period": "Khoảng thời gian", "Last 1 day": "1 ngày qua", "Last 7 days": "7 ngày qua",
  "Last 30 days": "30 ngày qua",
  "Filters narrow the recorded history, not the monthly spending report.": "Bộ lọc thu hẹp lịch sử đã ghi nhận, không thay đổi báo cáo chi tiêu tháng.",
  "Block new payments immediately. Completed payments are not reversed.": "Chặn thanh toán mới ngay. Không hoàn tác các khoản đã hoàn tất.",
  "Your financial workspace": "Không gian tài chính của bạn",
  "Simulation environment": "Môi trường mô phỏng",
  "Payments & history": "Thanh toán & Lịch sử",
  "PAYMENTS & HISTORY": "THANH TOÁN & LỊCH SỬ",
  "No real money is transferred.": "Không có tiền thật được chuyển.",
  "Simulation environment · No real money is transferred.": "Môi trường mô phỏng · Không có tiền thật được chuyển.",
  "Shared financial workspace": "Không gian tài chính dùng chung",
  "Financial data is shared with your team. Reset affects everyone; a server restart restores the starting data. No real bank is connected.": "Dữ liệu tài chính dùng chung cho team. Đặt lại ảnh hưởng mọi người; khởi động lại máy chủ sẽ khôi phục dữ liệu ban đầu. Không kết nối ngân hàng thật.",
  "A clearer view of your money": "Hiểu rõ hơn về tài chính của bạn",
  "Review finances, ask FinBridge, or prepare an education payment. You stay in control of every payment.": "Xem tài chính, hỏi FinBridge hoặc chuẩn bị thanh toán giáo dục. Bạn quyết định mọi khoản thanh toán.",
  "View accounts": "Xem tài khoản",
  "Ask a question": "Đặt câu hỏi",
  "Plan tuition": "Lập kế hoạch học phí",
  "Check your balances, review spending and plan your next payment.": "Xem số dư, kiểm tra chi tiêu và lập kế hoạch thanh toán tiếp theo.",
  "Overview for": "Tổng quan của",
  "YOUR FINANCES": "TÀI CHÍNH CỦA BẠN",
  "PAYMENT ACCOUNTS": "TÀI KHOẢN THANH TOÁN",
  "Current account balances": "Số dư tài khoản hiện tại",
  "Latest balances for simulated payment accounts. Currencies remain separate.": "Số dư mới nhất của các tài khoản thanh toán mô phỏng. Hiển thị riêng từng tiền tệ.",
  "Planning accounts below and payment accounts use separate records in this simulation. They are not added together.": "Tài khoản lập kế hoạch bên dưới và tài khoản thanh toán dùng các bản ghi riêng trong môi trường mô phỏng. Không cộng chung các nguồn này.",
  "Monthly spending": "Chi tiêu tháng này",
  "Reporting period": "Kỳ báo cáo",
  "Confirmed and automatically categorized expenses. Refunds are shown separately.": "Khoản chi đã xác nhận hoặc tự phân loại. Hoàn tiền được hiển thị riêng.",
  "Category budget remaining": "Ngân sách danh mục còn lại",
  "Budget limits are not money in your account.": "Hạn mức ngân sách không phải tiền trong tài khoản.",
  "Manage budgets →": "Quản lý ngân sách →",
  "View transactions →": "Xem giao dịch →",
  "Planning balance": "Số dư lập kế hoạch",
  "Planning accounts · VND": "Tài khoản lập kế hoạch · VND",
  "Unreserved planning balance": "Số dư lập kế hoạch chưa giữ trước",
  "Planning estimate after reservations and safety buffer": "Ước tính lập kế hoạch sau khoản giữ trước và đệm an toàn",
  "YOUR ASSISTANT": "TRỢ LÝ CỦA BẠN",
  "Ask about balances, spending, budgets or education payments from any screen.": "Hỏi về số dư, chi tiêu, ngân sách hoặc thanh toán giáo dục từ mọi màn hình.",
  "Recent answers": "Câu trả lời gần đây",
  "See what changed, why it matters and what you can do next.": "Xem điều gì thay đổi, tác động và bước tiếp theo.",
  "Sources and assumptions": "Nguồn dữ liệu và giả định",
  "ALL RECORDED TRANSACTIONS": "TẤT CẢ GIAO DỊCH ĐÃ GHI NHẬN",
  "Recorded money movement": "Các khoản tiền đã ghi nhận",
  "Totals for all recorded transactions. Internal transfers are excluded; refunds remain separate.": "Tổng giao dịch đã ghi nhận. Không tính chuyển khoản nội bộ; hoàn tiền được tách riêng.",
  "Expenses": "Chi tiêu",
  "Refunds": "Hoàn tiền",
  "These records contain different currencies. Ask FinBridge for separate currency totals; no combined total is shown here.": "Các bản ghi có nhiều tiền tệ. Hỏi FinBridge để xem tổng theo từng loại; không hiển thị tổng cộng gộp.",
  "A plan is a forecast. It never executes a payment. Payment approval remains in Payments & history.": "Kế hoạch là dự báo, không thực thi thanh toán. Phê duyệt thanh toán nằm trong Thanh toán & Lịch sử.",
  "Add budget": "Thêm ngân sách",
  "Monthly limit (VND)": "Hạn mức tháng (VND)",
  "Environment tools: payment examples": "Công cụ môi trường: ví dụ thanh toán",
  "These examples use synthetic data. Education payments always require your approval.": "Các ví dụ dùng dữ liệu mô phỏng. Thanh toán giáo dục luôn cần bạn phê duyệt.",
  "Create a simulated low-risk plan": "Tạo kế hoạch mô phỏng rủi ro thấp",
  "EDUCATION PAYMENTS": "THANH TOÁN GIÁO DỤC",
  "Select a school or education-provider bill, verify its beneficiary and compare payment options before preparing a plan.": "Chọn hóa đơn trường hoặc nhà cung cấp giáo dục, xác minh người thụ hưởng và so sánh kênh trước khi tạo kế hoạch.",
  "Select bill": "Chọn hóa đơn",
  "Verify beneficiary": "Xác minh người nhận",
  "Compare channels": "So sánh kênh",
  "Approve": "Phê duyệt",
  "BENEFICIARY VERIFICATION": "XÁC MINH NGƯỜI THỤ HƯỞNG",
  "Compare total cost and delivery time for the selected bill. Open details to see the quote source and full breakdown.": "So sánh tổng chi phí và thời gian nhận tiền cho hóa đơn đang chọn. Mở chi tiết để xem nguồn báo giá và từng khoản phí.",
  "Total cost": "Tổng chi phí",
  "Current balance": "Số dư hiện tại",
  "Estimated balance after payment": "Số dư dự kiến sau thanh toán",
  "Balance immediately after this payment": "Số dư ngay sau thanh toán này",
  "↻ Refresh quotes": "↻ Làm mới báo giá",
  "Refresh expired quote": "Làm mới báo giá hết hạn",
  "Refresh is manual. Review the new cost before creating a plan.": "Báo giá chỉ làm mới khi bạn bấm nút. Kiểm tra chi phí mới trước khi tạo kế hoạch.",
  "This account cannot cover the total cost and safety buffer. Choose another account.": "Tài khoản không đủ tổng chi phí và đệm an toàn. Hãy chọn tài khoản khác.",
  "Verify the bill beneficiary before creating a plan.": "Xác minh người thụ hưởng trên hóa đơn trước khi tạo kế hoạch.",
  "QUOTE DETAILS · SIMULATION": "CHI TIẾT BÁO GIÁ · MÔ PHỎNG",
  "PAYMENT PLAN": "KẾ HOẠCH THANH TOÁN",
  "PAYMENT RECEIPT": "BIÊN NHẬN THANH TOÁN",
  "This plan is no longer valid": "Kế hoạch không còn hiệu lực",
  "The bill or payment details changed. Review the updated bill and create a new plan; the previous approval cannot be reused.": "Hóa đơn hoặc thông tin thanh toán đã thay đổi. Kiểm tra hóa đơn mới và tạo kế hoạch mới; không dùng lại phê duyệt cũ.",
  "Review bill and create a new plan": "Kiểm tra hóa đơn và tạo kế hoạch mới",
  "Plan canceled": "Kế hoạch đã hủy",
  "No payment was made. You can create a new plan for an active bill.": "Chưa thanh toán. Bạn có thể tạo kế hoạch mới cho hóa đơn đang hoạt động.",
  "Review payment details": "Kiểm tra chi tiết thanh toán",
  "Payment details and quote used": "Chi tiết thanh toán và báo giá đã dùng",
  "Before payment, the system rechecks the bill, beneficiary, quote, account, limits, safety buffer and Emergency Stop.": "Trước khi thanh toán, hệ thống kiểm tra lại hóa đơn, người nhận, báo giá, tài khoản, hạn mức, đệm an toàn và Dừng khẩn cấp.",
  "This receipt belongs to the displayed plan. No real money is transferred.": "Biên nhận thuộc kế hoạch đang hiển thị. Không có tiền thật được chuyển.",
  "This is a historical receipt balance, not necessarily the latest account balance.": "Đây là số dư lịch sử trên biên nhận, không nhất thiết là số dư tài khoản mới nhất.",
  "Plan references": "Mã tham chiếu kế hoạch",
  "Receipt and Audit Log are immutable": "Biên nhận và nhật ký kiểm toán không thể chỉnh sửa",
  "Environment tools and policy settings": "Công cụ môi trường và thiết lập chính sách",
  "These controls change synthetic workspace settings. Education payments always require approval.": "Các công cụ thay đổi thiết lập của môi trường mô phỏng. Thanh toán giáo dục luôn cần phê duyệt.",
  "YOUR ACTIVITY": "HOẠT ĐỘNG CỦA BẠN",
  "Review incoming activity, confirm categories and find past payments.": "Kiểm tra giao dịch mới, xác nhận danh mục và tìm khoản thanh toán trước đó.",
  "Ask about spending": "Hỏi về chi tiêu",
  "Merchant and date": "Đơn vị và ngày giao dịch",
  "Why this category?": "Vì sao chọn danh mục này?",
  "Environment tools": "Công cụ môi trường",
  "SIMULATED BANK EVENTS": "SỰ KIỆN NGÂN HÀNG MÔ PHỎNG",
  "Generate a synthetic event to explore how transactions arrive and are categorized. No real bank is connected.": "Tạo sự kiện mô phỏng để xem giao dịch được nhận và phân loại. Không kết nối ngân hàng thật.",
  "Reset environment data": "Đặt lại dữ liệu môi trường",
  "These controls change shared synthetic data used by the team. They are not restricted administrator controls.": "Các công cụ thay đổi dữ liệu mô phỏng dùng chung của team. Đây không phải khu vực có phân quyền quản trị.",
  "↻ Refresh quote and create replacement plan": "↻ Làm mới báo giá và tạo kế hoạch thay thế",
  "Review the new amount, beneficiary, source account and expiry before approving again.": "Kiểm tra số tiền, người nhận, tài khoản nguồn và hạn báo giá mới trước khi phê duyệt.",
  "PAYMENT": "THANH TOÁN",
  "STUDENT": "SINH VIÊN",
  "Current balances": "Số dư hiện tại",
  "Screen is a hint; you can ask about any supported topic.": "Màn hình chỉ gợi ý ngữ cảnh; bạn có thể hỏi mọi chủ đề được hỗ trợ.",
  "Conversation choices apply to this chat, not the shared bill selection.": "Lựa chọn hội thoại áp dụng cho phiên chat này, không thay đổi hóa đơn chung đang chọn.",
  "Open workspace screen": "Mở màn hình làm việc",
  "Close introduction": "Đóng hướng dẫn",
  "Enter a valid value to continue.": "Nhập giá trị hợp lệ để tiếp tục.",
  "Fill in this field.": "Vui lòng điền trường này.",
  "Choose one of the available options.": "Chọn một lựa chọn trong danh sách.",
  "Enter a value within the displayed limits.": "Nhập giá trị trong giới hạn hiển thị.",
  "Use the requested format.": "Nhập đúng định dạng được yêu cầu.",
  "AI helps understand your question. Asking does not authorize a payment.": "AI giúp hiểu câu hỏi của bạn. Đặt câu hỏi không cấp quyền thanh toán.",
  "Transactions awaiting review are excluded from confirmed expense and budget figures.": "Giao dịch đang chờ xem xét chưa được tính vào chi tiêu và ngân sách đã xác nhận.",
  "Budget remaining is a spending limit, not a bank balance.": "Ngân sách còn lại là hạn mức chi tiêu, không phải số dư ngân hàng.",
  "This insight applies the safety buffer to the recorded checking balance.": "Thông tin này áp dụng đệm an toàn cho số dư tài khoản thanh toán đã ghi nhận.",
  "Verify the bill and available channels before preparing a payment.": "Xác minh hóa đơn và kênh khả dụng trước khi lập kế hoạch thanh toán.",
  "Review transactions →": "Xem xét giao dịch →",
  "Review budgets →": "Xem ngân sách →",
  "Review planning accounts →": "Xem tài khoản lập kế hoạch →",
  "Review education bill →": "Xem hóa đơn giáo dục →",
  "Transactions need your input": "Giao dịch cần bạn xem xét",
  "Budget progress": "Tiến độ ngân sách",
  "Safety buffer protected": "Đệm an toàn được giữ",
  "Tuition payment needs a controlled plan": "Học phí cần kế hoạch có kiểm soát"
}).forEach(([key,value]) => translationsVi.set(key,value));
Object.entries({"Provider, beneficiary, receiving bank, account, corridor and currency match the trusted demo registry": "Nhà cung cấp, người nhận, ngân hàng, tài khoản, hành lang và tiền tệ khớp danh bạ mô phỏng.", "One or more beneficiary fields do not match the trusted education-provider registry": "Có thông tin người thụ hưởng không khớp danh bạ nhà cung cấp giáo dục.", "Food & Drinks": "Ăn uống", "Shopping": "Mua sắm", "Housing": "Nhà ở", "Utilities": "Tiện ích", "Education": "Giáo dục", "Travel": "Du lịch", "Transport": "Đi lại", "Entertainment": "Giải trí", "PAUSED": "Đã dừng", "CONFIRMED": "Đã xác nhận", "CONFIRMATION REQUIRED": "Cần xác nhận", "PURPOSE REQUIRED": "Cần mục đích", "AUTO": "Tự động phân loại", "LINKED": "Đã liên kết", "OPPORTUNITY": "Gợi ý", "HIGH": "Ưu tiên cao", "MEDIUM": "Ưu tiên vừa", "TUITION": "Học phí", "DORMITORY": "Ký túc xá", "INSURANCE": "Bảo hiểm", "VISA": "Phí thị thực", "LIVING": "Chi phí nhà cung cấp", "OTHER": "Phí giáo dục khác", "Education payment progress": "Tiến trình thanh toán giáo dục", "Dismiss introduction": "Đóng hướng dẫn", "Main sections": "Các mục chính", "Overview views": "Các phần tổng quan", "Transaction views": "Các phần giao dịch", "Education bill pages": "Trang hóa đơn giáo dục", "Transaction pages": "Trang giao dịch", "Payment progress": "Tiến trình thanh toán", "Close": "Đóng", "Language": "Ngôn ngữ", "Confidence rules: medium and low confidence": "Quy tắc độ tin cậy: mức vừa và thấp", "Confirmed and auto-categorized expenses": "Khoản chi đã xác nhận và tự phân loại", "Demo checking balance minus configured buffer": "Số dư thanh toán mô phỏng trừ đệm an toàn đã cấu hình"}).forEach(([key,value]) => translationsVi.set(key,value));
Object.entries({"CHECKING": "Tài khoản thanh toán", "SAVINGS": "Tài khoản tiết kiệm", "WALLET": "Ví điện tử", "ACCOUNT": "Chọn tài khoản", "PAUSED": "Đã dừng", "Transaction type is incoming income": "Loại giao dịch là khoản thu vào.", "Both accounts belong to the demo profile": "Hai tài khoản cùng thuộc hồ sơ mô phỏng.", "Refund marker found in the normalized description": "Mô tả giao dịch có dấu hiệu hoàn tiền.", "Merchant rule v1 matched a known cafe": "Đơn vị bán hàng khớp quy tắc cho quán cà phê.", "Merchant rule v1 matched transport": "Đơn vị bán hàng khớp quy tắc đi lại.", "Merchant rule v1 matched a utility provider": "Đơn vị bán hàng khớp quy tắc dịch vụ tiện ích.", "Merchant can represent shopping or groceries": "Đơn vị bán hàng có thể thuộc mua sắm hoặc thực phẩm.", "No deterministic merchant rule matched": "Chưa có quy tắc xác định đơn vị bán hàng; cần bạn bổ sung mục đích.", "User reviewed bank transaction": "Bạn đã xem xét giao dịch ngân hàng.", "Category change undone by user": "Bạn đã hoàn tác thay đổi danh mục.", "Bank A supports this Vietnam to China payment": "Bank A hỗ trợ thanh toán Việt Nam → Trung Quốc.", "Vietcombank supports this Vietnam to China payment": "Vietcombank hỗ trợ thanh toán Việt Nam → Trung Quốc.", "Techcombank supports this Vietnam to China payment": "Techcombank hỗ trợ thanh toán Việt Nam → Trung Quốc.", "MoMo supports this synthetic education corridor": "MoMo hỗ trợ hành lang giáo dục mô phỏng này.", "Reference only: no connected Bank B account": "Chỉ để tham khảo: chưa kết nối tài khoản Bank B.", "Alipay Education Wallet is not configured for this demo corridor": "Ví giáo dục Alipay chưa hỗ trợ hành lang mô phỏng này.", "MoMo Wallet is not configured for this demo corridor": "Ví MoMo chưa hỗ trợ hành lang mô phỏng này.", "Bank A supports this configured student-payment corridor": "Bank A hỗ trợ hành lang du học đang chọn.", "Vietcombank supports this configured student-payment corridor": "Vietcombank hỗ trợ hành lang du học đang chọn.", "Techcombank supports this configured student-payment corridor": "Techcombank hỗ trợ hành lang du học đang chọn.", "Alipay education profile is eligible": "Hồ sơ giáo dục Alipay đủ điều kiện.", "Simulated Bank Event": "Sự kiện ngân hàng mô phỏng", "Payment Sandbox": "Thanh toán Sandbox", "Synthetic Alipay education quote feed": "Nguồn báo giá giáo dục Alipay mô phỏng", "Synthetic Bank A treasury quote": "Nguồn báo giá Bank A mô phỏng", "Synthetic Vietcombank treasury quote": "Nguồn báo giá Vietcombank mô phỏng", "Synthetic Techcombank treasury quote": "Nguồn báo giá Techcombank mô phỏng", "Synthetic MoMo education quote feed": "Nguồn báo giá giáo dục MoMo mô phỏng", "Synthetic Bank B promotional quote": "Nguồn báo giá ưu đãi Bank B mô phỏng", "Synthetic Alipay reference quote": "Báo giá tham khảo Alipay mô phỏng", "Synthetic MoMo reference quote": "Báo giá tham khảo MoMo mô phỏng", "Resume Agent": "Tiếp tục tác vụ", "Plan blocked. Review the policy reason before continuing.": "Kế hoạch bị chặn. Kiểm tra lý do an toàn trước khi tiếp tục.", "Plan invalidated. Review the changed bill or quote before creating a new draft.": "Kế hoạch đã vô hiệu. Kiểm tra hóa đơn hoặc báo giá đã thay đổi trước khi tạo bản nháp mới.", "Close assistant": "Đóng trợ lý"}).forEach(([key,value]) => translationsVi.set(key,value));
translationsVi.set('Compare payment options →','So sánh phương án thanh toán →');
translationsVi.set('Locked payment summary','Tóm tắt thanh toán đã khóa');
translationsVi.set('Quote details','Chi tiết báo giá');
translationsVi.set('Reference only. This channel cannot create or execute a payment.','Chỉ để tham khảo. Kênh này không thể tạo hoặc thực thi thanh toán.');
Object.entries({
  "Action exceeds the delegated per-transaction limit.": "Tác vụ vượt hạn mức mỗi giao dịch được ủy quyền.",
  "Action exceeds the delegated daily amount or frequency limit.": "Tác vụ vượt hạn mức tiền hoặc số lần được ủy quyền trong ngày.",
  "Recipient is not on the allowlist for this action.": "Người nhận không nằm trong danh sách được phép của tác vụ này.",
  "User approval is required before sandbox execution.": "Cần người dùng phê duyệt trước khi thực thi trong Sandbox.",
  "Approval is missing, expired, or no longer matches the action.": "Phê duyệt còn thiếu, đã hết hạn hoặc không còn khớp tác vụ.",
  "This legacy plan has no locked bill and quote snapshot; create and approve a new plan before payment.": "Kế hoạch cũ không có bản ghi hóa đơn và báo giá đã khóa; hãy tạo và phê duyệt kế hoạch mới trước khi thanh toán.",
  "This unexecuted plan was canceled; no payment will be made.": "Kế hoạch chưa thực thi này đã bị hủy; sẽ không có thanh toán.",
  "This student bill already has a completed payment; another payment is blocked.": "Hóa đơn du học này đã có thanh toán hoàn tất; thanh toán khác bị chặn.",
  "The linked student bill changed, was archived, or was cancelled; create a new plan.": "Hóa đơn du học liên quan đã thay đổi, được lưu trữ hoặc bị hủy; hãy tạo kế hoạch mới.",
  "Emergency Stop is active, so new actions are blocked.": "Dừng khẩn cấp đang bật, nên các tác vụ mới bị chặn.",
  "Action would reduce the VND balance below the safety buffer.": "Tác vụ sẽ làm số dư VND xuống dưới đệm an toàn.",
  "Untrusted input cannot modify policy, recipient, rates, fees or approval.": "Dữ liệu không đáng tin không thể sửa chính sách, người nhận, tỷ giá, phí hoặc phê duyệt.",
  "The idempotency key has already produced a receipt.": "Khóa chống thực thi trùng đã tạo biên nhận.",
  "The selected payment channel is unavailable to the user.": "Kênh thanh toán được chọn không khả dụng với người dùng.",
  "The stored FX quote is expired or no longer matches the action.": "Báo giá tỷ giá đã lưu đã hết hạn hoặc không còn khớp tác vụ.",
  "The beneficiary profile does not match the trusted education-provider registry.": "Hồ sơ người thụ hưởng không khớp danh bạ nhà cung cấp giáo dục đáng tin cậy.",
  "The selected student-payment corridor has no configured deterministic quote data.": "Hành lang thanh toán du học được chọn chưa có dữ liệu báo giá theo quy tắc đã cấu hình.",
  "The plan currencies must match the selected expense and corridor.": "Tiền tệ của kế hoạch phải khớp khoản phí và hành lang đã chọn.",
  "The source account is not connected, verified, or enabled for this corridor.": "Tài khoản nguồn chưa được kết nối, xác minh hoặc bật cho hành lang này.",
  "The selected source account does not belong to this payment channel.": "Tài khoản nguồn được chọn không thuộc kênh thanh toán này.",
  "Settlement time plus the one-day safety margin may miss the tuition due date.": "Thời gian quyết toán cộng thêm một ngày dự phòng có thể vượt hạn học phí.",
  "Processing…": "Đang xử lý…"
}).forEach(([key,value]) => translationsVi.set(key,value));
Object.entries({
  'YOUR MONEY': 'TIỀN CỦA BẠN',
  'Your money, your decisions.': 'Tiền của bạn, quyết định của bạn.',
  'FinBridge · Your money, your decisions.': 'FinBridge · Tiền của bạn, quyết định của bạn.',
  'Confirm the category or add the payment purpose.': 'Xác nhận danh mục hoặc bổ sung mục đích giao dịch.',
  'Upcoming money is reserved': 'Đã giữ tiền cho các khoản sắp tới',
  'Review the date or mark the item complete.': 'Kiểm tra ngày dự kiến hoặc đánh dấu khoản đã hoàn tất.',
  'Confirming a category keeps your spending report accurate.': 'Xác nhận danh mục giúp báo cáo chi tiêu chính xác.',
  'Check category limits before your next expense.': 'Kiểm tra hạn mức danh mục trước khoản chi tiếp theo.',
  'This insight applies the safety buffer to the recorded checking balance.': 'Thông tin này áp dụng đệm an toàn cho số dư tài khoản thanh toán đã ghi nhận.',
  'Verify the bill and available channels before preparing a payment.': 'Xác minh hóa đơn và kênh khả dụng trước khi chuẩn bị thanh toán.',
  'Review planning accounts →': 'Xem tài khoản dùng cho kế hoạch →',
  'Review education bill →': 'Xem hóa đơn giáo dục →',
  'Quote valid until': 'Báo giá còn hiệu lực đến',
  'Balances and planning': 'Số dư và kế hoạch',
  'Your account balances and upcoming commitments, in one place.': 'Số dư tài khoản và các khoản dự kiến chi, tại một nơi.',
  'Reservations do not move money': 'Giữ trước không làm chuyển tiền',
  'Kept aside in this planning estimate': 'Được giữ lại trong ước tính kế hoạch',
  'Planning estimate · VND': 'Ước tính kế hoạch · VND',
  'Balances by account': 'Số dư từng tài khoản',
  'Total VND balance minus upcoming reservations and the safety buffer. This includes savings and manually maintained cash; it is not a guarantee that every source can fund an online payment. Payment availability is checked separately for the selected account and channel. Other currencies are not added to VND.': 'Tổng số dư VND trừ khoản giữ trước và đệm an toàn. Bao gồm tiền tiết kiệm và tiền mặt nhập tay; không có nghĩa mọi nguồn tiền đều dùng được để thanh toán trực tuyến. Khả năng thanh toán được kiểm tra riêng theo tài khoản và kênh đã chọn. Không cộng tiền tệ khác vào VND.',
  'Technical reference': 'Thông tin kỹ thuật',
  'FINAL REVIEW': 'XÁC NHẬN CUỐI CÙNG',
  'Confirm education payment': 'Xác nhận thanh toán giáo dục',
  'Confirm internal transfer': 'Xác nhận chuyển nội bộ',
  'Check these locked details. Confirming approves this plan and executes it immediately.': 'Kiểm tra các thông tin đã khóa dưới đây. Xác nhận sẽ phê duyệt kế hoạch này và thực thi ngay.',
  'I checked the beneficiary and total amount.': 'Tôi đã kiểm tra người nhận và tổng số tiền.',
  'Go back': 'Quay lại',
  'Review and approve': 'Xem lại và phê duyệt',
  'Confirm and pay': 'Xác nhận và thanh toán',
  'Stop all new payments': 'Dừng mọi thanh toán mới',
  'Plan reference': 'Mã kế hoạch',
  'Bill / reference': 'Hóa đơn / mã tham chiếu',
  'Payment details': 'Chi tiết thanh toán',
  'Previous items': 'Các mục trước',
  'Next items': 'Các mục tiếp theo',
  'Account connected, but this channel does not support': 'Tài khoản đã liên kết, nhưng kênh này chưa hỗ trợ',
  'Quote expired. Go back and refresh the quote before approving.': 'Báo giá đã hết hạn. Quay lại làm mới báo giá trước khi phê duyệt.',
  'Payment is paused or this plan changed. Go back and review its current status.': 'Thanh toán đã tạm dừng hoặc kế hoạch thay đổi. Quay lại kiểm tra trạng thái mới nhất.',
  'The payment details will be checked again before execution.': 'Thông tin thanh toán sẽ được kiểm tra lại trước khi thực thi.'
}).forEach(([key,value]) => translationsVi.set(key,value));

const originalText = new WeakMap();
const originalPlaceholder = new WeakMap();
const englishDisplayLabels = new Map(Object.entries({
 NONE:'Not selected', SPENDING:'Spending', BUDGET:'Budgets', CURRENT_BALANCE:'Current account balance',
 RECEIPT_BALANCE:'Historical receipt balance', TUITION_AFFORDABILITY:'Estimated tuition impact',
 TUITION_CHANNELS:'Tuition channels', CHANNEL_UNAVAILABLE:'Channel eligibility', TUITION_PLAN:'Tuition plan',
 TUITION_STATUS:'Payment status', LIVING_EXPENSE_RUNWAY:'Living-expense estimate',
 ACCOUNT:'Choose account', BILL:'Choose bill', PLAN:'Choose plan', CHANNEL:'Choose channel',
 TOPIC:'Choose topic', MONTHLY_EXPENSE:'Confirm monthly expense',
 PAUSED:'Stopped', CHECKING:'Checking account', SAVINGS:'Savings account', WALLET:'Wallet', EWALLET:'E-wallet', ACTIVE:'Active', ARCHIVED:'Archived', CANCELLED:'Canceled', PAID:'Paid', COMPLETED:'Completed',
 TUITION:'Tuition', DORMITORY:'Dormitory', INSURANCE:'Insurance', VISA:'Visa fee', LIVING:'Provider living costs', OTHER:'Other education fee',
 ONCE:'Once', WEEKLY:'Weekly', MONTHLY:'Monthly', CHEAPER:'Cheaper', FASTER:'Faster', SAFER:'Safer',
 CONNECTED:'Connected', DISCONNECTED:'Disconnected', VERIFIED:'Verified', MANUAL:'Manual', CASH:'Cash', LINKED:'Linked',
 AUTO:'Automatically categorized', CONFIRMED:'Confirmed by you', 'CONFIRMATION REQUIRED':'Needs confirmation',
 'PURPOSE REQUIRED':'Needs purpose', 'AWAITING APPROVAL':'Awaiting approval'
}));
englishDisplayLabels.forEach((display,key) => {
  if(!translationsVi.has(key)) translationsVi.set(key,translationsVi.get(display) || display);
});
const originalAria = new WeakMap();
const originalTitles = new WeakMap();
const dialogOpeners = new WeakMap();
const tabLabels = {
  dashboard: { en: 'Overview', vi: 'Tổng quan' },
  transactions: { en: 'Transactions', vi: 'Giao dịch' },
  student: { en: 'Student finance', vi: 'Tài chính du học' },
  agent: { en: 'Payments & history', vi: 'Thanh toán & Lịch sử' }
};
const tabAnchors = {
  dashboard: '#overview',
  transactions: '#transactions',
  student: '#student-finance',
  agent: '#agent-workspace'
};
let activeTab = 'dashboard';
let activeDashboardView = 'summary';
let renderTransactionList = () => {};
let renderStudentBillList = () => {};
let renderPaymentAccounts = () => {};
let renderQuoteExpiryStatuses = () => {};
let quoteExpiryTimer;
const noticeTimers = new WeakMap();
let pendingUpdate = 0;
let updateSequence = 0;
let pendingChatController = null;
const chatControlBaseline = new WeakMap();
let validationSequence=0;
let assistantOpener = null;
let assistantScrollPosition = null;
const assistantQuestions = {
  balance: { en: 'What is my current Bank A balance?', vi: 'Số dư hiện tại của Bank A là bao nhiêu?' },
  spending: { en: 'Where did I spend the most this month?', vi: 'Tháng này tôi chi nhiều nhất vào đâu?' },
  budget: { en: 'How much budget do I have left this month?', vi: 'Ngân sách tháng này của tôi còn bao nhiêu?' },
  affordability: { en: 'After paying tuition, will I have enough money for living expenses?', vi: 'Nếu đóng học phí thì còn đủ tiền sinh hoạt không?' },
  compare: { en: 'Compare the available tuition payment channels.', vi: 'So sánh các kênh thanh toán học phí khả dụng.' },
  unavailable: { en: 'Why is Bank B unavailable?', vi: 'Vì sao Bank B không khả dụng?' }
};
const workspaceFilterSelectors = [
  '[data-testid="student-bill-search"]', '[data-testid="student-bill-status-filter"]',
  '[data-testid="student-bill-verification-filter"]', '[data-testid="student-bill-sort"]',
  '[data-testid="payment-account-filter"]', '[data-testid="transaction-search"]',
  '[data-testid="transaction-period-filter"]',
  '[data-testid="transaction-type-filter"]', '[data-testid="transaction-status-filter"]',
  '[data-testid="transaction-category-filter"]', '[data-testid="transaction-sort"]'
];
const workspaceScrollSelectors = [
  '[data-testid="student-expense-list"]', '[data-testid="payment-account-list"]',
  '.current-account-grid', '.money-source-grid', '.plan-list',
  '.budget-snapshot-list', '.transaction-review-list', '.custom-category-list', '.payment-history-list',
  '.insight-list', '.attention-list', '.budget-edit-list', '.payment-audit .table-scroll'
];

function selectedLanguage() {
  return localStorage.getItem('finbridge-language') === 'vi' ? 'vi' : 'en';
}

function updateActiveTabLabel(language = selectedLanguage()) {
  const label = document.querySelector('[data-active-tab-label]');
  if (label) label.textContent = tabLabels[activeTab][language];
}

function activateTab(tab, updateLocation = true) {
  activeTab = tabLabels[tab] ? tab : 'dashboard';
  document.querySelectorAll('[data-tab-panel]').forEach((panel) => {
    panel.hidden = panel.dataset.tabPanel !== activeTab;
  });
  document.querySelectorAll('[data-tab]').forEach((button) => {
    button.setAttribute('aria-pressed', String(button.dataset.tab === activeTab));
  });
  if (activeTab === 'transactions') {
    activateTransactionView(updateLocation ? 'history' : initialTransactionView(), false);
  }
  if (activeTab === 'dashboard') {
    activateDashboardView(updateLocation ? 'summary' : initialDashboardView(), false);
  }
  updateActiveTabLabel();
  renderAssistantContext();
  localStorage.setItem('finbridge-active-tab', activeTab);
  if (updateLocation) {
    history.replaceState(null, '', tabAnchors[activeTab]);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}

function initialDashboardView() {
  return { '#accounts': 'accounts', '#planning': 'planning' }[window.location.hash] || 'summary';
}

function renderAssistantContext() {
  const panel = document.querySelector('[data-assistant-panel]');
  if (!panel) return;
  const label = panel.querySelector('[data-assistant-screen]');
  label.textContent = tabLabels[activeTab].en;
  originalText.set(label.firstChild, tabLabels[activeTab].en);
  label.firstChild.nodeValue = tabLabels[activeTab][selectedLanguage()];
  panel.querySelectorAll('[data-assistant-for]').forEach((group) => {
    group.hidden = !group.dataset.assistantFor.split(' ').includes(activeTab);
  });
  panel.setAttribute('aria-label', selectedLanguage() === 'vi' ? 'Hỏi FinBridge' : 'Ask FinBridge');
}

function rememberAssistantPosition() {
  const panel = document.querySelector('[data-assistant-panel]');
  const body = panel?.querySelector('.assistant-body');
  if (!body || panel.hidden) return;
  const bounds = body.getBoundingClientRect();
  const reply = Array.from(body.querySelectorAll('[data-reply-id]')).find((node) => {
    const rect = node.getBoundingClientRect();
    return rect.bottom > bounds.top && rect.top < bounds.bottom;
  });
  assistantScrollPosition = {
    top: body.scrollTop,
    atEnd: body.scrollHeight - body.clientHeight - body.scrollTop <= 2,
    replyId: reply?.dataset.replyId,
    offset: reply ? reply.getBoundingClientRect().top - bounds.top : 0
  };
}

function restoreAssistantPosition() {
  const panel = document.querySelector('[data-assistant-panel]');
  const body = panel?.querySelector('.assistant-body');
  if (!body || panel.hidden) return;
  if (assistantScrollPosition?.atEnd) {
    body.scrollTop = body.scrollHeight;
    return;
  }
  const reply = assistantScrollPosition?.replyId
    ? body.querySelector('[data-reply-id="' + CSS.escape(assistantScrollPosition.replyId) + '"]')
    : !assistantScrollPosition ? body.querySelector('.message.assistant:last-child') : null;
  if (reply) {
    body.scrollTop += reply.getBoundingClientRect().top - body.getBoundingClientRect().top - (assistantScrollPosition?.offset ?? 8);
  } else {
    body.scrollTop = assistantScrollPosition?.top ?? 0;
  }
}

let assistantBackgroundInert = null;
function syncAssistantAccessibility() {
  const panel = document.querySelector('[data-assistant-panel]');
  const shell = document.querySelector('.shell');
  const modal = !!panel && !panel.hidden && window.matchMedia('(max-width:700px)').matches;
  panel?.setAttribute('aria-modal', String(modal));
  const backdrop = document.querySelector('[data-assistant-backdrop]');
  if (backdrop) backdrop.hidden = !modal;
  if (shell) {
    if (modal) {
      if (assistantBackgroundInert === null) assistantBackgroundInert = shell.inert;
      shell.inert = true;
    } else if (assistantBackgroundInert !== null) {
      shell.inert = assistantBackgroundInert;
      assistantBackgroundInert = null;
    }
  }
  // A resize can turn a nonmodal desktop panel into a mobile dialog.
  if (modal && !panel.contains(document.activeElement)) {
    const input = panel.querySelector('input[name="message"]');
    (input && !input.disabled ? input : panel.querySelector('[data-close-assistant]'))?.focus({preventScroll:true});
  }
}

function assistantFocusables(panel) {
  return Array.from(panel.querySelectorAll('button, a[href], input, select, textarea, [tabindex]'))
    .filter(element => !element.disabled && element.tabIndex >= 0 && !element.closest('[inert]') && element.getClientRects().length);
}

function setAssistantOpen(open, opener) {
  const panel = document.querySelector('[data-assistant-panel]');
  if (!panel) return;
  const wasOpen = !panel.hidden;
  if (!open) rememberAssistantPosition();
  if (open && opener) assistantOpener = opener;
  panel.hidden = !open;
  document.querySelectorAll('[data-open-assistant]').forEach((button) => {
    button.setAttribute('aria-expanded', String(open));
  });
  const launcher = document.querySelector('[data-testid="assistant-launcher"]');
  launcher.hidden = open;
  syncAssistantAccessibility();
  placeWorkspaceNotices();
  if (open) {
    updateAssistantViewport();
    renderAssistantContext();
    const input = panel.querySelector('input[name="message"]');
    (input.disabled ? panel.querySelector('[data-close-assistant]') : input).focus({ preventScroll: true });
    if (!wasOpen) restoreAssistantPosition();
  } else {
    const returnTarget = assistantOpener?.isConnected ? assistantOpener : assistantOpener?.dataset.testid ? document.querySelector('[data-testid="' + CSS.escape(assistantOpener.dataset.testid) + '"]') : null;
    (returnTarget?.getClientRects().length ? returnTarget : launcher).focus({ preventScroll: true });
  }
}

function handleAssistantClick(event) {
  const opener = event.target.closest('[data-open-assistant]');
  const question = event.target.closest('[data-assistant-question]');
  if (opener) {
    setAssistantOpen(true, opener);
  }
  if (question && !pendingUpdate) {
    const input = document.querySelector('[data-testid="assistant-conversation-input"]');
    const suggestion = assistantQuestions[question.dataset.assistantQuestion];
    // Suggestions fill the existing composer. Only an explicit Send submits a request.
    if (input && suggestion) {
      input.value = suggestion[selectedLanguage()];
      input.focus({ preventScroll: true });
    }
  }
  if (opener?.dataset.assistantContextToken) {
    const contextForm = document.querySelector('[data-assistant-context-form]');
    contextForm.elements.token.value = opener.dataset.assistantContextToken;
    contextForm.requestSubmit();
  }
  if (event.target.closest('[data-dismiss-runway]')) {
    document.querySelector('[data-assistant-runway]')?.setAttribute('hidden', '');
  }
  if (event.target.closest('[data-close-assistant]')) setAssistantOpen(false);
  if (event.target.closest('[data-assistant-review-plan]')) {
    setAssistantOpen(false);
  }
}

function syncAssistant(nextDocument, resetPosition = false) {
  // Keep the panel and composer mounted so drafts, focus and pending controls survive tab updates.
  const panel = document.querySelector('[data-assistant-panel]');
  const previousReplyId = panel?.querySelector('.message:last-child')?.dataset.replyId;
  rememberAssistantPosition();
  for (const selector of ['[data-testid="assistant-replies"]', '[data-assistant-plan]', '[data-assistant-policy]', '[data-assistant-context-state]', '[data-assistant-runway]', '[data-assistant-actions]']) {
    const current = document.querySelector(selector);
    const next = nextDocument.querySelector(selector);
    if (current && next) current.replaceChildren(...next.childNodes);
  }
  const latestReply = panel?.querySelector('.message.assistant:last-child');
  if (resetPosition) {
    assistantScrollPosition = null;
  } else if (panel?.hidden && latestReply && latestReply.dataset.replyId !== previousReplyId) {
    // A response arriving while closed must be visible on the next open.
    assistantScrollPosition = { top: 0, atEnd: false, replyId: latestReply.dataset.replyId, offset: 8 };
  }
  restoreAssistantPosition();
  document.querySelectorAll('[data-assistant-panel] .request-error').forEach((notice) => notice.remove());
  syncAssistantAccessibility();
}

function updateAssistantViewport() {
  const panel = document.querySelector('[data-assistant-panel]');
  if (!panel) return;
  const mobile = window.matchMedia('(max-width:700px)').matches;
  const viewport = window.visualViewport;
  const height = viewport?.height || window.innerHeight;
  panel.classList.toggle('assistant-compact', mobile && height < 560);
  panel.classList.toggle('assistant-short', mobile && height < 380);
  panel.style.setProperty('--assistant-visible-height', height + 'px');
  if (mobile) {
    // The visual viewport excludes the on-screen keyboard and follows browser panning.
    panel.style.top = ((viewport?.offsetTop || 0) + 8) + 'px';
    panel.style.left = ((viewport?.offsetLeft || 0) + 8) + 'px';
    panel.style.right = 'auto';
    panel.style.bottom = 'auto';
    panel.style.width = Math.max(0, (viewport?.width || window.innerWidth) - 16) + 'px';
    panel.style.height = Math.max(0, height - 16) + 'px';
  } else {
    for (const name of ['top', 'left', 'right', 'bottom', 'width', 'height']) panel.style.removeProperty(name);
  }
  syncAssistantAccessibility();
}

function renderResponseQuotes() {
  document.querySelectorAll('[data-response-quote]').forEach((badge) => {
    const vi = badge.dataset.language === 'vi';
    const remaining = Math.floor((Number(badge.dataset.expiryEpoch) - Date.now()) / 1000);
    const expired = !Number.isFinite(remaining) || remaining <= 0;
    badge.classList.toggle('quote-expired', expired);
    badge.textContent = expired
      ? (vi ? 'Báo giá đã hết hạn · cần làm mới trước khi tạo hoặc duyệt kế hoạch' : 'Quote expired · refresh before preparing or approving a plan')
      : (vi ? `Báo giá còn hiệu lực · ${Math.floor(remaining / 60)} phút ${remaining % 60} giây` : `Quote valid · ${Math.floor(remaining / 60)}m ${remaining % 60}s remaining`);
  });
}

function activateDashboardView(view, updateLocation = true) {
  activeDashboardView = document.querySelector('[data-dashboard-view="' + view + '"]') ? view : 'summary';
  document.querySelectorAll('[data-dashboard-view-panel]').forEach((panel) => {
    panel.hidden = panel.dataset.dashboardViewPanel !== activeDashboardView;
  });
  document.querySelectorAll('[data-dashboard-view]').forEach((button) => {
    button.setAttribute('aria-pressed', String(button.dataset.dashboardView === activeDashboardView));
  });
  renderIntroduction();
  if (updateLocation) {
    const hashes = { summary: '#overview', accounts: '#accounts', planning: '#planning' };
    history.replaceState(null, '', hashes[activeDashboardView]);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}

function renderIntroduction() {
  const intro=document.querySelector('[data-first-use]');
  if(intro) intro.hidden=activeTab!=='dashboard' || activeDashboardView!=='summary' || localStorage.getItem('finbridge-intro-dismissed')==='true';
}

function initialTransactionView() {
  const viewByHash = {
    '#transaction-review': 'review',
    '#transactions': 'history',
    '#transaction-categories': 'categories',
    '#transaction-tools': 'demo'
  };
  return viewByHash[window.location.hash] || 'history';
}

function activateTransactionView(view, updateLocation = true) {
  const requested = document.querySelector('[data-transaction-view="' + view + '"]') ? view : 'history';
  document.querySelectorAll('[data-transaction-view-panel]').forEach((panel) => {
    panel.hidden = panel.dataset.transactionViewPanel !== requested;
  });
  document.querySelectorAll('[data-transaction-view]').forEach((button) => {
    button.setAttribute('aria-pressed', String(button.dataset.transactionView === requested));
  });
  if (updateLocation) {
    const hashes = { review: '#transaction-review', history: '#transactions',
      categories: '#transaction-categories', demo: '#transaction-tools' };
    history.replaceState(null, '', hashes[requested]);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}

function initialTab() {
  const hashTabs = {
    '#overview': 'dashboard', '#feed': 'dashboard', '#budget': 'dashboard',
    '#accounts': 'dashboard', '#planning': 'dashboard',
    '#transaction-tools': 'transactions', '#transactions': 'transactions',
    '#transaction-review': 'transactions', '#transaction-categories': 'transactions',
    '#student-finance': 'student', '#agent-workspace': 'agent'
  };
  return hashTabs[window.location.hash]
    || localStorage.getItem('finbridge-active-tab')
    || 'dashboard';
}

function initializeTransactionList(initialPage = 1) {
  const rows = Array.from(document.querySelectorAll('[data-testid="transaction-row"]'));
  if (!rows.length) return;

  const search = document.querySelector('[data-testid="transaction-search"]');
  const period = document.querySelector('[data-testid="transaction-period-filter"]');
  const type = document.querySelector('[data-testid="transaction-type-filter"]');
  const status = document.querySelector('[data-testid="transaction-status-filter"]');
  const category = document.querySelector('[data-testid="transaction-category-filter"]');
  const sort = document.querySelector('[data-testid="transaction-sort"]');
  const clear = document.querySelector('[data-testid="transaction-clear-filters"]');
  const previous = document.querySelector('[data-testid="transaction-prev"]');
  const next = document.querySelector('[data-testid="transaction-next"]');
  const count = document.querySelector('[data-testid="transaction-result-count"]');
  const pageLabel = document.querySelector('[data-testid="transaction-page"]');
  const empty = document.querySelector('[data-testid="transaction-empty"]');
  const pageSize = 5;
  let page = initialPage;

  [...new Set(rows.map((row) => row.dataset.category).filter(Boolean))]
    .sort()
    .forEach((value) => {
      const option = document.createElement('option');
      option.value = value;
      option.textContent = value;
      category.append(option);
    });

  const render = () => {
    const query = search.value.trim().toLowerCase();
    const cutoff = period.value === 'all' ? null : Date.now() - Number(period.value) * 86400000;
    let matches = rows.filter((row) =>
      (cutoff === null || Number(row.dataset.occurredAt) >= cutoff)
      && (!query || row.dataset.search.toLowerCase().includes(query))
      && (!type.value || row.dataset.type === type.value)
      && (!status.value || row.dataset.reviewStatus === status.value)
      && (!category.value || row.dataset.category === category.value));

    if (sort.value === 'largest') {
      matches = matches.sort((left, right) => Number(right.dataset.amount) - Number(left.dataset.amount));
    } else if (sort.value === 'smallest') {
      matches = matches.sort((left, right) => Number(left.dataset.amount) - Number(right.dataset.amount));
    }

    const pages = Math.max(1, Math.ceil(matches.length / pageSize));
    page = Math.min(page, pages);
    document.querySelector('[data-testid="transaction-table-body"]').dataset.page = page;
    const start = (page - 1) * pageSize;
    const visibleRows = new Set(matches.slice(start, start + pageSize));
    rows.forEach((row) => { row.hidden = !visibleRows.has(row); });

    const language = selectedLanguage();
    const scopeLabel = document.querySelector('[data-testid="transaction-history-scope"] strong');
    if (scopeLabel) scopeLabel.textContent = period.value === 'all' ? (language === 'vi' ? 'Toàn bộ thời gian' : 'All time')
      : (language === 'vi' ? `${period.value} ngày qua` : `Last ${period.value} day${period.value === '1' ? '' : 's'}`);
    count.textContent = language === 'vi'
      ? `${matches.length} giao dịch phù hợp`
      : `${matches.length} matching transactions`;
    pageLabel.textContent = language === 'vi'
      ? `Trang ${page} / ${pages}`
      : `Page ${page} of ${pages}`;
    empty.hidden = matches.length !== 0;
    previous.disabled = page === 1;
    next.disabled = page === pages;
  };

  [search, period, type, status, category, sort].forEach((control) => {
    control.addEventListener(control === search ? 'input' : 'change', () => {
      page = 1;
      render();
    });
  });
  clear.addEventListener('click', () => {
    search.value = '';
    period.value = '30';
    type.value = '';
    status.value = '';
    category.value = '';
    sort.value = 'newest';
    page = 1;
    render();
  });
  previous.addEventListener('click', () => { page -= 1; render(); });
  next.addEventListener('click', () => { page += 1; render(); });

  renderTransactionList = render;
  render();
}

function initializeStudentBillList(initialPage = 1) {
  const list = document.querySelector('[data-testid="student-expense-list"]');
  if (!list) return;
  const rows = Array.from(list.querySelectorAll('[data-bill-row]'));
  const search = document.querySelector('[data-testid="student-bill-search"]');
  const status = document.querySelector('[data-testid="student-bill-status-filter"]');
  const verification = document.querySelector('[data-testid="student-bill-verification-filter"]');
  const sort = document.querySelector('[data-testid="student-bill-sort"]');
  const clear = document.querySelector('[data-testid="student-bill-clear"]');
  const previous = document.querySelector('[data-testid="student-bill-prev"]');
  const next = document.querySelector('[data-testid="student-bill-next"]');
  const count = document.querySelector('[data-testid="student-bill-visible-count"]');
  const pageLabel = document.querySelector('[data-testid="student-bill-page"]');
  const empty = document.querySelector('[data-testid="student-bill-empty"]');
  const pageSize = 5;
  let page = initialPage;

  const matchesStatus = (row) => {
    if (status.value === 'ALL') return true;
    if (status.value === 'PAID') return row.dataset.billStatus === 'PAID';
    if (status.value === 'ACTIVE') {
      return row.dataset.billLifecycle === 'ACTIVE' && row.dataset.billStatus !== 'PAID';
    }
    return row.dataset.billLifecycle === status.value;
  };

  const render = () => {
    const query = search.value.trim().toLowerCase();
    let matches = rows.filter((row) =>
      (!query || row.textContent.toLowerCase().includes(query))
      && matchesStatus(row)
      && (verification.value === 'ALL' || row.dataset.billVerification === verification.value));

    const byId = (left, right) => Number(left.dataset.billRow) - Number(right.dataset.billRow);
    if (sort.value === 'newest') {
      matches.sort((left, right) => right.dataset.billCreated.localeCompare(left.dataset.billCreated)
        || byId(right, left));
    } else if (sort.value === 'amount-desc') {
      matches.sort((left, right) => Number(right.dataset.billAmount) - Number(left.dataset.billAmount)
        || byId(left, right));
    } else if (sort.value === 'provider-asc') {
      matches.sort((left, right) => left.dataset.billProvider.localeCompare(right.dataset.billProvider)
        || byId(left, right));
    } else {
      matches.sort((left, right) => left.dataset.billDue.localeCompare(right.dataset.billDue)
        || byId(left, right));
    }
    matches.forEach((row) => list.append(row));

    const pages = Math.max(1, Math.ceil(matches.length / pageSize));
    page = Math.min(page, pages);
    list.dataset.page = page;
    const start = (page - 1) * pageSize;
    const visibleRows = new Set(matches.slice(start, start + pageSize));
    rows.forEach((row) => { row.hidden = !visibleRows.has(row); });
    list.scrollLeft = 0;

    const vietnamese = selectedLanguage() === 'vi';
    count.textContent = vietnamese
      ? `${matches.length} hóa đơn phù hợp`
      : `${matches.length} matching bills`;
    pageLabel.textContent = vietnamese
      ? `Trang ${page} / ${pages}`
      : `Page ${page} of ${pages}`;
    empty.hidden = matches.length !== 0;
    previous.disabled = page === 1;
    next.disabled = page === pages;
  };

  [search, status, verification, sort].forEach((control) => {
    control.addEventListener(control === search ? 'input' : 'change', () => {
      page = 1;
      render();
    });
  });
  clear.addEventListener('click', () => {
    search.value = '';
    status.value = 'ALL';
    verification.value = 'ALL';
    sort.value = 'due-asc';
    page = 1;
    render();
  });
  previous.addEventListener('click', () => { page -= 1; render(); });
  next.addEventListener('click', () => { page += 1; render(); });

  renderStudentBillList = render;
  render();
}
function initializeCategoryReviewForms() {
  document.querySelectorAll('[data-category-review]').forEach((form) => {
    const select = form.querySelector('[data-category-select]');
    const field = form.querySelector('.custom-category-field');
    const input = form.querySelector('[data-custom-category]');
    if (!select || !field || !input) return;
    const render = () => {
      const custom = select.value === '__custom__';
      field.hidden = !custom;
      input.required = custom;
      if (!custom) input.value = '';
    };
    select.addEventListener('change', render);
    render();
  });
}

Object.entries({
  'Select for comparison': 'Chọn để so sánh',
  'Reference estimate only; payment is unavailable.': 'Chỉ là ước tính tham khảo; không thể thanh toán qua kênh này.',
  'Account connected, but this demo channel does not support': 'Tài khoản đã liên kết, nhưng kênh mô phỏng này chưa hỗ trợ',
  '. Reconnecting the account will not enable this route. Choose an eligible channel.': '. Liên kết lại tài khoản không mở được tuyến này. Hãy chọn một kênh đủ điều kiện.'
}).forEach(([en, vi]) => translationsVi.set(en, vi));

function initializePaymentAccounts() {
  const grid = document.querySelector('[data-testid="payment-account-list"]');
  if (!grid) return;
  const cards = Array.from(grid.querySelectorAll('.connected-payment-card'));
  const filter = document.querySelector('[data-testid="payment-account-filter"]');
  const empty = document.querySelector('[data-testid="payment-account-empty"]');
  const selectionKey = 'finbridge-comparison-channel-' + grid.dataset.billId;
  let selectedChannel = sessionStorage.getItem(selectionKey);
  const renderSelection = () => cards.forEach(card => {
    const picker = card.querySelector('[data-select-channel]');
    const selected = picker && !picker.disabled && picker.dataset.selectChannel === selectedChannel;
    card.classList.toggle('selected-channel', Boolean(selected));
    picker?.setAttribute('aria-pressed', String(Boolean(selected)));
    if (picker) picker.querySelector('span').textContent = selectedLanguage() === 'vi'
      ? (selected ? '✓ Đang chọn để so sánh' : 'Chọn để so sánh')
      : (selected ? '✓ Selected for comparison' : 'Select for comparison');
  });
  grid.addEventListener('click', event => {
    if (event.target.closest('dialog')) return;
    const card = event.target.closest('.connected-payment-card');
    const picker = card?.querySelector('[data-select-channel]');
    if (!picker || picker.disabled || (event.target.closest('button, a, input, select, form') && !event.target.closest('[data-select-channel]'))) return;
    selectedChannel = picker.dataset.selectChannel;
    sessionStorage.setItem(selectionKey, selectedChannel);
    renderSelection();
  });
  renderSelection();

  const render = () => {
    renderSelection();
    const visible = cards.filter((card) => filter.value === 'all' || card.dataset.state === filter.value);
    cards.forEach((card) => { card.hidden = !visible.includes(card); });
    empty.hidden = visible.length !== 0;
  };
  filter.addEventListener('change', render);
  document.querySelectorAll('[data-account-action="show-payable"]').forEach((button) => {
    button.addEventListener('click', () => {
      filter.value = 'payable';
      render();
      grid.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  });
  renderPaymentAccounts = render;
  render();
}

function initializeStudentExpenseCorridor() {
  document.querySelectorAll('[data-corridor-form]').forEach((form) => {
    const select = form.querySelector('[data-corridor-select]');
    const currency = form.querySelector('[data-corridor-currency]');
    const institution = form.querySelector('[data-corridor-institution]');
    const recipientName = form.querySelector('[data-corridor-recipient-name]');
    const bankName = form.querySelector('[data-corridor-bank-name]');
    const bankCode = form.querySelector('[data-corridor-bank-code]');
    const recipient = form.querySelector('[data-corridor-recipient]');
    if (!select || !currency || !institution || !recipientName || !bankName || !bankCode || !recipient) return;
    const render = (replaceProvider) => {
      const option = select.selectedOptions[0];
      if (!option) return;
      currency.value = option.dataset.currency;
      if (replaceProvider) {
        institution.value = option.dataset.institution;
        recipientName.value = option.dataset.recipientName;
        bankName.value = option.dataset.bankName;
        bankCode.value = option.dataset.bankCode;
        recipient.value = option.dataset.recipient;
      }
    };
    select.addEventListener('change', () => render(true));
    render(form.dataset.useCorridorDefaults === 'true');
  });
}

function initializeQuoteExpiryStatuses() {
  window.clearInterval(quoteExpiryTimer);
  const cards = Array.from(document.querySelectorAll('[data-quote-expiry]'));
  if (!cards.length && !document.querySelector('[data-plan-expiry]') && !document.querySelector('[data-response-quote]')) return;

  const render = () => {
    const vietnamese = selectedLanguage() === 'vi';
    let accountFilterChanged = false;
    cards.forEach((card) => {
      const expiryEpochMillis = Number(card.dataset.quoteExpiry);
      const remainingSeconds = Math.floor((expiryEpochMillis - Date.now()) / 1000);
      const expired = !Number.isFinite(expiryEpochMillis) || remainingSeconds <= 0;
      const channelEligible = card.dataset.channelEligible !== 'false';
      const status = card.querySelector('[data-quote-live-status]');
      const planForm = card.querySelector('.planning-form');
      const eligibility = card.querySelector('.eligibility');
      card.classList.toggle('quote-is-expired', expired);
      if (planForm) planForm.hidden = expired;
      const refresh = card.querySelector('[data-channel-refresh]');
      if (refresh) refresh.hidden = !expired;
      if (expired && channelEligible && card.classList.contains('connected-payment-card')) {
        if (card.dataset.state !== 'expired') accountFilterChanged = true;
        card.dataset.state = 'expired';
        if (eligibility) {
          eligibility.classList.remove('eligible');
          eligibility.classList.add('unavailable-tag');
          eligibility.textContent = vietnamese ? 'Báo giá đã hết hạn' : 'Quote expired';
        }
      }
      if (!status) return;
      status.classList.toggle('quote-expired', expired);
      if (expired) {
        status.textContent = vietnamese
          ? 'Báo giá đã hết hạn · cần làm mới'
          : 'Quote expired · refresh required';
        return;
      }
      const minutes = Math.floor(remainingSeconds / 60);
      const seconds = String(remainingSeconds % 60).padStart(2, '0');
      status.textContent = vietnamese
        ? `Báo giá còn hiệu lực · còn ${minutes} phút ${seconds} giây`
        : `Quote valid · ${minutes}m ${seconds}s remaining`;
    });
    if (accountFilterChanged) renderPaymentAccounts();
    renderPaymentWorkflow();
    renderResponseQuotes();
  };
  renderQuoteExpiryStatuses = render;
  render();
  quoteExpiryTimer = window.setInterval(render, 1000);
}

function translateDynamic(text) {
  const countryDue=text.match(/^(.+) · Due (.+)$/);
  if(countryDue) return (translationsVi.get(countryDue[1]) || countryDue[1])+' · Hạn '+countryDue[2];
  const budgetProgress=text.match(/^(.+) has used (.+)% of its synthetic monthly budget\.$/);
  if(budgetProgress) return (translationsVi.get(budgetProgress[1]) || budgetProgress[1])+' đã dùng '+budgetProgress[2]+'% ngân sách tháng mô phỏng.';
  const planNotice = text.match(/^Student payment plan (.+)\. Approval is always required\.$/);
  if (planNotice) return `Kế hoạch thanh toán: ${paymentStatusLabels[planNotice[1]]?.[1] || planNotice[1]}. Luôn cần bạn phê duyệt.`;
  const rules = [
    [/^(\d+)% confidence$/, 'Độ tin cậy $1%'],
    [/^(\d+) transaction\(s\) need category confirmation or a purpose\.$/, '$1 giao dịch cần xác nhận danh mục hoặc bổ sung mục đích.'],
    [/^(.+) has used (.+)% of its synthetic monthly budget\.$/, '$1 đã dùng $2% ngân sách tháng mô phỏng.'],
    [/^(.+) VND remains above the 3,000,000 VND safety buffer\.$/, 'Còn $1 VND ngoài đệm an toàn 3.000.000 VND.'],
    [/^(.+) · Due (.+)$/, '$1 · Hạn $2'],
    [/^Prepared tuition-payment plan (.+) from the verified bill and current (.+) quote\. It is awaiting explicit approval; no Sandbox payment has been executed\.$/, 'Đã chuẩn bị kế hoạch học phí $1 từ hóa đơn đã xác minh và báo giá $2 hiện tại. Kế hoạch đang chờ phê duyệt rõ ràng; chưa có thanh toán Sandbox nào được thực thi.'],
    [/^Verified tuition-channel comparison: (.+)\. Availability, costs, and timing come from the deterministic backend\.$/, 'So sánh kênh học phí đã xác minh: $1. Tính khả dụng, chi phí và thời gian do backend xác định.'],
    [/^(\d+) insights$/, '$1 thông tin'],
    [/^(\d+) records$/, '$1 bản ghi'],
    [/^(\d+) recent events$/, '$1 sự kiện gần đây'],
    [/^(\d+) pending$/, '$1 mục cần xử lý'],
    [/^(\d+) item\(s\)$/, '$1 mục'],
    [/^(\d+) transaction\(s\) need review$/, '$1 giao dịch cần xem xét'],
    [/^(\d+) planned item\(s\) are overdue$/, '$1 khoản kế hoạch đã quá hạn'],
    [/^(.+) VND is protected for plans due in the next 30 days\.$/, '$1 VND được giữ cho các kế hoạch đến hạn trong 30 ngày tới.'],
    [/^(.+) reserved in 30 days$/, 'Giữ trước $1 trong 30 ngày'],
    [/^Updated (.+)$/, 'Cập nhật $1'],
    [/^(\d+) bill\(s\)$/, '$1 khoản phí'],
    [/^Due (.+)$/, 'Hạn $1'],
    [/^Reference · (.+)$/, 'Mã tham chiếu · $1'],
    [/^Account · (.+)$/, 'Tài khoản · $1'],
    [/^Attachment · (.+)$/, 'File đính kèm · $1'],
    [/^Attachment: (.+)$/, 'File đính kèm: $1'],
    [/^(\d+) transaction\(s\) · Active$/, '$1 giao dịch · Đang dùng'],
    [/^(\d+) transaction\(s\) · Archived$/, '$1 giao dịch · Đã lưu trữ'],
    [/^Purpose: (.+)$/, 'Mục đích: $1'],
    [/^(.+) VND remaining$/, 'Còn lại $1 VND'],
    [/^(.+) VND less$/, 'Ít hơn $1 VND'],
    [/^Needs (.+) VND more to pay safely\.$/, 'Cần thêm $1 VND để thanh toán an toàn.'],
    [/^Needs (.+) VND more for its linked channel\.$/, 'Cần thêm $1 VND cho kênh liên kết.'],
    [/^Enough for (.+) and the (.+) VND safety buffer\.$/, 'Đủ tiền cho $1 và vùng an toàn $2 VND.'],
    [/^(.+) VND available$/, 'Số dư khả dụng $1 VND'],
    [/^(.+)\. Choose an eligible bank account for this tuition payment\.$/, '$1. Hãy chọn tài khoản ngân hàng đủ điều kiện cho khoản học phí này.'],
    [/^Quoted (.+)$/, 'Báo giá lúc $1'],
    [/^Expires (.+)$/, 'Hết hạn lúc $1'],
    [/^(.+) day\(s\)$/, '$1 ngày'],
    [/^(.+)\/100 synthetic$/, '$1/100 mô phỏng'],
    [/^([A-Z]{3}) credit$/, 'Ghi có $1'],
    [/^Evidence: Selected plan · (.+)$/, 'Bằng chứng: Kế hoạch đã chọn · $1'],
    [/^Evidence: (.+)$/, 'Bằng chứng: $1'],
    [/^Payer: (.+)$/, 'Người trả: $1'],
    [/^Recipient: (.+)$/, 'Người nhận: $1'],
    [/^Simulated Bank Event: (.+)$/, 'Sự kiện ngân hàng mô phỏng: $1'],
    [/^Channel ranking updated to (.+)\.$/, 'Đã cập nhật xếp hạng kênh theo $1.'],
    [/^Payment source changed to (.+)\.$/, 'Đã đổi nguồn thanh toán sang $1.'],
    [/^Tuition plan (.+)\. Approval is always required\.$/, 'Kế hoạch học phí $1. Luôn yêu cầu phê duyệt.'],
    [/^Selected plan uses (.+) for bill (.+) of (.+) ([A-Z]{3}), due (.+), with latest safe date (.+)\. Approval Mode is required before payment\.$/, 'Kế hoạch đã chọn sử dụng $1 cho hóa đơn $2 trị giá $3 $4, hạn thanh toán $5, với ngày an toàn cuối cùng $6. Cần phê duyệt trước khi thanh toán.'],
    [/^Selected (.+) plan is ready for review$/, 'Kế hoạch $1 đã chọn đang chờ xem xét'],
    [/^(.+) needs a controlled plan$/, '$1 cần một kế hoạch có kiểm soát'],
    [/^Bill (.+) for (.+) ([A-Z]{3}) is verified, due (.+), with latest safe date (.+)\. Approval Mode is still required before payment\.$/, 'Hóa đơn $1 trị giá $2 $3 đã được xác minh, hạn thanh toán $4, với ngày an toàn cuối cùng $5. Vẫn cần phê duyệt trước khi thanh toán.'],
    [/^Low-risk plan status: (.+)\.$/, 'Trạng thái kế hoạch rủi ro thấp: $1.'],
    [/^Payment Sandbox completed: (.+)$/, 'Payment Sandbox đã hoàn tất: $1'],
    [/^Action blocked: (.+)$/, 'Tác vụ bị chặn: $1'],
    [/^Idempotent receipt: (.+)$/, 'Biên nhận chống trùng: $1'],
    [/^Request failed: (.+)$/, 'Yêu cầu thất bại: $1']
  ];
  for (const [pattern, replacement] of rules) {
    if (pattern.test(text)) return text.replace(pattern, replacement);
  }
  return text;
}

function translateValue(value, language) {
  const trimmed = value.trim();
  if (!trimmed) return value;
  const translated = language === 'vi'
    ? (translationsVi.get(trimmed) || translateDynamic(trimmed))
    : (englishDisplayLabels.get(trimmed) || trimmed);
  return translated === trimmed ? value : value.replace(trimmed, translated);
}

function applyLanguage(language) {
  const selected = language === 'vi' ? 'vi' : 'en';
  document.documentElement.lang = selected;
  document.title = selected === 'vi' ? 'FinBridge · Tài chính của bạn' : 'FinBridge · Your finances';
  document.querySelectorAll('[data-language]').forEach((button) => {
    button.setAttribute('aria-pressed', String((button.dataset.language || button.dataset.assistantLanguage) === selected));
  });
  const languageGroup = document.querySelector('.language-switcher');
  if (languageGroup) languageGroup.setAttribute('aria-label', selected === 'vi' ? 'Ngôn ngữ' : 'Language');
  document.querySelectorAll('[data-dismiss-notice]').forEach((button) => {
    button.setAttribute('aria-label', selected === 'vi' ? 'Đóng thông báo' : 'Dismiss notification');
  });

  const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {
    acceptNode(node) {
      return ['SCRIPT', 'STYLE'].includes(node.parentElement?.tagName)
        ? NodeFilter.FILTER_REJECT : NodeFilter.FILTER_ACCEPT;
    }
  });
  const nodes = [];
  while (walker.nextNode()) nodes.push(walker.currentNode);
  nodes.forEach((node) => {
    // A structured answer keeps the language of that turn, including labels and evidence.
    if (node.parentElement?.closest('[data-response-language], [data-user-authored]')) return;
    if (!originalText.has(node)) originalText.set(node, node.nodeValue);
    const messageBody = node.parentElement?.closest('.message p');
    const original = originalText.get(node);
    // User text is verbatim. Backend replies already have a language; only complete known
    // templates may be translated, never word fragments inside financial explanations.
    node.nodeValue = messageBody?.closest('.message.user') ? original
      : translateValue(original, selected);
  });

  document.querySelectorAll('[placeholder]').forEach((element) => {
    if (!originalPlaceholder.has(element)) originalPlaceholder.set(element, element.placeholder);
    const original = originalPlaceholder.get(element);
    if (selected === 'vi' && translationsVi.has(original)) {
      element.placeholder = translationsVi.get(original);
    } else if (selected === 'vi' && original === 'Ask to compare, check, or prepare a tuition plan') {
      element.placeholder = 'Yêu cầu so sánh, kiểm tra hoặc chuẩn bị kế hoạch học phí';
    } else if (selected === 'vi' && original === 'e.g. Groceries') {
      element.placeholder = 'ví dụ: Thực phẩm';
    } else {
      element.placeholder = original;
    }
  });
  document.querySelectorAll('[aria-label]').forEach(element => {
    if (!originalAria.has(element)) originalAria.set(element,element.getAttribute('aria-label'));
    element.setAttribute('aria-label',translateValue(originalAria.get(element),selected));
  });
  document.querySelectorAll('[title]').forEach(element => {
    if (!originalTitles.has(element)) originalTitles.set(element,element.getAttribute('title'));
    element.setAttribute('title',translateValue(originalTitles.get(element),selected));
  });
  document.querySelectorAll('#transactions tbody tr').forEach(row => {
    const labels=Array.from(document.querySelectorAll('#transactions thead th')).map(th => th.textContent.trim());
    Array.from(row.cells).forEach((cell,index) => cell.dataset.label=labels[index]);
  });
  updateActiveTabLabel(selected);
  renderTransactionList();
  renderStudentBillList();
  renderPaymentAccounts();
  renderQuoteExpiryStatuses();
  renderPaymentWorkflow();
  renderAssistantContext();
  renderResponseQuotes();
}

// Give status messages their own layout row; never float over approval or chat controls.
function placeWorkspaceNotices() {
  const panel = document.querySelector('[data-assistant-panel]');
  const workspace = document.querySelector('[data-workspace-notices]');
  const assistant = document.querySelector('[data-assistant-notices]');
  const host = panel && !panel.hidden ? assistant : workspace;
  if (!host) return;
  document.querySelectorAll('.notice.in-place-notice').forEach(notice => {
    if (notice.parentElement === host) return;
    // Content replacement previously discarded the old notification too.
    if (!notice.closest('[data-workspace-notices], [data-assistant-notices]')) {
      host.querySelectorAll('.notice').forEach(old => {
        window.clearTimeout(noticeTimers.get(old));
        old.remove();
      });
    }
    host.append(notice);
  });
}

function initializeWorkspaceNotices(root = document) {
  root.querySelectorAll('.notice').forEach((notice) => {
    if (notice.querySelector('[data-dismiss-notice]')) return;
    if (!notice.closest('dialog, [data-assistant-panel]')) notice.classList.add('in-place-notice');
    const close = document.createElement('button');
    close.type = 'button';
    close.className = 'notice-dismiss';
    close.dataset.dismissNotice = '';
    close.textContent = '×';
    close.setAttribute('aria-label', selectedLanguage() === 'vi' ? 'Đóng thông báo' : 'Dismiss notification');
    close.addEventListener('click', () => {
      window.clearTimeout(noticeTimers.get(notice));
      notice.remove();
    });
    notice.append(close);
    if (notice.getAttribute('role') === 'status' && !notice.classList.contains('request-error')) {
      // Capture this notice so an older timeout cannot dismiss a newer message.
      noticeTimers.set(notice, window.setTimeout(() => notice.remove(), 4000));
    }
  });
  placeWorkspaceNotices();
}

function captureWorkspaceState() {
  const filters = new Map();
  workspaceFilterSelectors.forEach((selector) => {
    const control = document.querySelector(selector);
    if (control) filters.set(selector, control.value);
  });
  const scrolls = new Map();
  workspaceScrollSelectors.forEach((selector) => {
    const element = document.querySelector(selector);
    if (element) scrolls.set(selector, { left: element.scrollLeft, top: element.scrollTop });
  });
  return {
    tab: activeTab,
    dashboardView: activeDashboardView,
    anchor: window.location.hash || tabAnchors[activeTab],
    top: window.scrollY,
    left: window.scrollX,
    paymentDetails: Array.from(document.querySelectorAll('.payment-workspace details[open]')).map((item) => item.dataset.testid).filter(Boolean),
    filters,
    scrolls,
    transactionPage: Number(document.querySelector('[data-testid="transaction-table-body"]')?.dataset.page || 1),
    billPage: Number(document.querySelector('[data-testid="student-expense-list"]')?.dataset.page || 1)
  };
}

const paymentStatusLabels = {
  AWAITING_APPROVAL: ['Awaiting your approval', 'Chờ bạn phê duyệt'], APPROVED: ['Approved', 'Đã phê duyệt'],
  POLICY_ALLOWED: ['Allowed by policy', 'Đủ điều kiện theo chính sách'], COMPLETED: ['Completed', 'Hoàn tất'],
  ALLOWED: ['Allowed by policy', 'Đủ điều kiện theo chính sách'], APPROVAL_REQUIRED: ['Awaiting your approval', 'Chờ bạn phê duyệt'],
  VERIFIED: ['Verified', 'Đã xác minh'], SELECTED: ['Selected', 'Đang chọn'], DEADLINE_RISK: ['Deadline risk', 'Rủi ro trễ hạn'],
  BLOCKED: ['Blocked', 'Bị chặn'], INVALIDATED: ['No longer valid', 'Không còn hiệu lực'], CANCELED: ['Canceled', 'Đã hủy']
};
function renderPaymentWorkflow() {
  const vietnamese = selectedLanguage() === 'vi';
  const index = vietnamese ? 1 : 0;
  document.querySelectorAll('[data-payment-status]').forEach((label) => {
    const text = paymentStatusLabels[label.dataset.status];
    if (text) label.textContent = text[index];
  });
  document.querySelectorAll('[data-payment-reason]').forEach((label) => {
    const original=label.dataset.backendExplanation || label.textContent;
    label.textContent=vietnamese ? translationsVi.get(original) || original : original;
  });
  document.querySelectorAll('[data-plan-expiry]').forEach((quote) => {
    const seconds = Math.floor((Number(quote.dataset.planExpiry) - Date.now()) / 1000);
    const expired = !Number.isFinite(seconds) || seconds <= 0;
    const label = quote.querySelector('[data-plan-expiry-status]');
    const refreshForm = quote.querySelector('[data-quote-refresh-form]');
    if (label) {
      label.classList.toggle('quote-expired', expired);
      label.textContent = expired
        ? (vietnamese ? 'Báo giá đã hết hạn · quay lại lấy báo giá mới; chưa chuyển tiền.' : 'Quote expired · return for a new quote; no money moved.')
        : (vietnamese ? `Báo giá còn hiệu lực · còn ${Math.floor(seconds / 60)} phút ${String(seconds % 60).padStart(2, '0')} giây` : `Quote valid · ${Math.floor(seconds / 60)}m ${String(seconds % 60).padStart(2, '0')}s remaining`);
    }
    if (refreshForm) refreshForm.hidden = !expired;
    const button = quote.closest('[data-testid="latest-action"]')?.querySelector('[data-testid="approve-action"]');
    if (button) button.disabled = expired || Boolean(pendingUpdate);
    if (expired) { const status = quote.closest('[data-testid="latest-action"]')?.querySelector('[data-payment-status]'); if (status) status.textContent = vietnamese ? 'Báo giá đã hết hạn' : 'Quote expired'; }
  });
}
function syncPaymentNavigation(nextDocument) {
  const count = Number(document.querySelector('[data-payment-count]')?.dataset.paymentCount || 0);
  document.querySelectorAll('[data-payment-entry]').forEach((entry) => { entry.hidden = false; });
  if (nextDocument) {
    const current = document.querySelector('[data-global-policy]');
    const next = nextDocument.querySelector('[data-global-policy]');
    if (current && next) current.replaceWith(next);
  }
}
async function openPaymentPlan(event) {
  const link = event.target.closest('a[data-open-payment-plan]');
  if (!link || !window.fetch || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
  const url = new URL(link.href, window.location.href);
  if (url.origin !== window.location.origin) return;
  event.preventDefault();
  if (pendingUpdate && !pendingChatController) return;
  if (pendingChatController) { pendingChatController.abort(); pendingChatController = null; }
  const requestId = ++updateSequence;
  pendingUpdate = requestId;
  const state = captureWorkspaceState();
  try {
    const response = await fetch(url, { credentials: 'same-origin', headers: { Accept: 'text/html' } });
    if (!response.ok) throw new Error('Plan unavailable');
    const nextDocument = new DOMParser().parseFromString(await response.text(), 'text/html');
    if (requestId !== updateSequence) return;
    const nextContent = nextDocument.querySelector('.content');
    if (!nextContent) throw new Error('Plan response missing');
    nextContent.querySelectorAll('[data-tab-panel]').forEach((panel) => { panel.hidden = panel.dataset.tabPanel !== 'agent'; });
    document.querySelector('.content').replaceChildren(...nextContent.childNodes);
    syncPaymentNavigation(nextDocument);
    syncAssistant(nextDocument);
    history.replaceState(null, '', url.pathname + url.search + '#agent-workspace');
    initializeWorkspaceContent({ ...state, tab: 'agent' });
    restoreAssistantPosition();
    const receipt = link.hasAttribute('data-assistant-review-receipt') ? document.querySelector('[data-testid="latest-receipt"]') : null;
    if (receipt) {
      const card = receipt.closest('.receipt-card') || receipt;
      card.scrollIntoView({ block: 'start', behavior: 'instant' });
      const headerBottom = Math.max(0, ...Array.from(document.querySelectorAll('main > header, .mobile-tabs'))
        .filter(node => node.getClientRects().length)
        .map(node => node.getBoundingClientRect().bottom));
      window.scrollBy({ top: card.getBoundingClientRect().top - headerBottom - 12, behavior: 'instant' });
    } else window.scrollTo({ top: 0, behavior: 'instant' });
  } catch (error) {
    if (requestId === updateSequence) showWorkspaceUpdateError(link);
  } finally {
    if (pendingUpdate === requestId) pendingUpdate = 0;
    renderPaymentWorkflow();
  }
}
function initializeHorizontalLists() {
  document.querySelectorAll('.student-bill-list,.channel-list').forEach((track,index) => {
    if (track.dataset.horizontalControls) return;
    track.dataset.horizontalControls = 'true';
    track.id ||= 'comparison-track-'+index;
    const controls = document.createElement('div');
    controls.className = 'scroll-track-controls';
    const buttons = [-1,1].map(direction => {
      const button = document.createElement('button');
      button.type = 'button'; button.className = 'secondary';
      button.setAttribute('aria-controls',track.id);
      const label = direction < 0 ? 'Previous items' : 'Next items';
      button.textContent = label;
      button.addEventListener('click', () => track.scrollBy({
        left:direction*Math.max(240,track.clientWidth*.75),
        behavior:window.matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth'
      }));
      controls.append(button);
      return button;
    });
    track.before(controls);
    const update = () => {
      const overflow = track.clientWidth > 0 && track.scrollWidth > track.clientWidth+2;
      controls.hidden = !overflow;
      buttons[0].disabled = track.scrollLeft <= 1;
      buttons[1].disabled = track.scrollLeft+track.clientWidth >= track.scrollWidth-2;
    };
    track.addEventListener('scroll',update,{passive:true});
    const size = new ResizeObserver(update); size.observe(track);
    const changes = new MutationObserver(update);
    changes.observe(track,{childList:true,subtree:true,attributes:true,attributeFilter:['hidden','style']});
    update();
  });
}

// Bound growing lists while keeping headers and primary actions outside their scroll area.
function initializeBoundedLists() {
  const selectors='.current-account-grid,.money-source-grid,.plan-list,.budget-snapshot-list,.transaction-review-list,.custom-category-list,.payment-history-list,.insight-list,.attention-list,.budget-edit-list,.payment-audit .table-scroll';
  document.querySelectorAll(selectors).forEach((region,index) => {
    if(region.dataset.boundedList)return;
    region.dataset.boundedList='true';
    region.classList.add('bounded-list');
    region.tabIndex=0;
    region.setAttribute('role','region');
    const heading=region.closest('section')?.querySelector('h2,h3');
    if(heading){ if(!heading.id)heading.id='list-heading-'+index; region.setAttribute('aria-labelledby',heading.id); }
    region.id ||= 'bounded-list-'+index;
    const button=document.createElement('button');
    button.type='button';button.className='text-action list-scroll-down';button.hidden=true;
    button.textContent='Scroll down';button.setAttribute('aria-controls',region.id);
    button.addEventListener('click',()=>region.scrollBy({top:Math.max(120,region.clientHeight*.75),behavior:window.matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth'}));
    region.after(button);
    const update=()=>{
      const overflow=region.clientHeight>0 && region.scrollHeight>region.clientHeight+2;
      button.hidden=!overflow;
      button.disabled=!overflow || region.scrollHeight-region.clientHeight-region.scrollTop<2;
    };
    region.addEventListener('scroll',update,{passive:true});
    const observer=new ResizeObserver(update);observer.observe(region);
    Array.from(region.children).forEach(child=>observer.observe(child));
    const contentObserver=new MutationObserver(records=>{
      records.forEach(record=>record.addedNodes.forEach(node=>{if(node.nodeType===1)observer.observe(node);}));
      update();
    });
    contentObserver.observe(region,{childList:true,subtree:true,attributes:true,attributeFilter:['hidden','style']});
    update();
  });
}

async function handleDocumentExtraction(event) {
  const button = event.target.closest('[data-ai-extract]');
  if (!button || button.disabled) return;
  const section = button.closest('[data-ai-upload]') || button.closest('[data-student-ai-upload]');
  const file = section?.querySelector('[data-ai-file]')?.files?.[0];
  const status = section?.querySelector('[data-ai-status]');
  const form = button.dataset.aiKind === 'transaction'
    ? button.closest('dialog')?.querySelector('[data-manual-transaction]')
    : document.querySelector('#add-student-expense [data-corridor-form]');
  if (!file || !status || !form) { if (status) status.textContent = 'Choose an image first.'; return; }
  if (!/^image\/(jpeg|png|webp)$/.test(file.type) || file.size > 5 * 1024 * 1024) {
    status.textContent = 'Choose a JPG, PNG or WEBP image up to 5 MB.'; return;
  }
  const data = new FormData(); data.append('file', file); data.append('kind', button.dataset.aiKind);
  button.disabled = true; status.textContent = 'Reading image…';
  try {
    const response = await fetch('/api/ai/extract-document', { method: 'POST', body: data, headers: { Accept: 'application/json' } });
    const result = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(result.message || 'AI/OCR extraction is not configured on this server yet.');
    const fields = result.fields || {};
    Object.entries(fields).forEach(([key, value]) => {
      const input = form.elements.namedItem(key);
      if (input && value != null && typeof value !== 'object') {
        input.value = value == null ? '' : String(value);
        input.dispatchEvent(new Event('input', { bubbles: true }));
        input.dispatchEvent(new Event('change', { bubbles: true }));
      }
    });
    const note = button.closest('[data-ai-upload], [data-student-ai-upload]')?.querySelector('[data-ai-review-note]');
    if (note) note.hidden = false;
    if (button.dataset.aiKind === 'transaction') form.hidden = false;
    const confidence = Number.isFinite(Number(result.confidence)) ? ` · ${Math.round(Number(result.confidence) * 100)}% confidence` : '';
    const provider = result.provider === 'local' ? 'Local AI' : 'Gemini';
    status.textContent = `${provider} recognized the available details${confidence}. Please review and edit before saving.`;
  } catch (error) {
    status.textContent = error.message;
  } finally { button.disabled = false; }
}

function initializeWorkspaceContent(state) {
  initializeBoundedLists();
  renderTransactionList = () => {};
  renderStudentBillList = () => {};
  renderPaymentAccounts = () => {};
  renderQuoteExpiryStatuses = () => {};
  initializeTransactionList(state?.transactionPage);
  initializeStudentBillList(state?.billPage);
  initializeCategoryReviewForms();
  initializePaymentAccounts();
  initializeHorizontalLists();
  initializeStudentExpenseCorridor();
  initializeQuoteExpiryStatuses();
  state?.filters.forEach((value, selector) => {
    const control = document.querySelector(selector);
    if (control && (control.tagName !== 'SELECT'
        || Array.from(control.options).some((option) => option.value === value))) {
      control.value = value;
    }
  });
  initializeWorkspaceNotices();
  syncPaymentNavigation();
  state?.paymentDetails?.forEach((testId) => { const details = document.querySelector('[data-testid="' + CSS.escape(testId) + '"]'); if (details?.tagName === 'DETAILS') details.open = true; });
  activateTab(state?.tab || initialTab(), false);
  if ((state?.tab || initialTab()) === 'dashboard') {
    activateDashboardView(state?.dashboardView || initialDashboardView(), false);
  }
  applyLanguage(selectedLanguage());

  document.querySelectorAll('[data-entry-manual]').forEach(button => button.addEventListener('click', () => {
    const dialog = button.closest('dialog');
    dialog.querySelector('[data-ai-upload]').hidden = true;
    dialog.querySelector('[data-manual-transaction]').hidden = false;
    dialog.querySelector('[data-manual-transaction] input[name="merchant"]').focus();
  }));
  document.querySelectorAll('[data-entry-ai]').forEach(button => button.addEventListener('click', () => {
    const dialog = button.closest('dialog');
    dialog.querySelector('[data-ai-upload]').hidden = false;
    dialog.querySelector('[data-manual-transaction]').hidden = true;
  }));
  document.querySelectorAll('[data-open-dialog]').forEach((button) => {
    button.addEventListener('click', () => {
      const dialog = document.getElementById(button.dataset.openDialog);
      if (dialog && typeof dialog.showModal === 'function') { dialogOpeners.set(dialog,button); dialog.showModal(); }
    });
  });
  document.querySelectorAll('dialog').forEach((modal) => {
    modal.addEventListener('close', () => {
      const opener=dialogOpeners.get(modal);
      if(opener?.isConnected) opener.focus({preventScroll:true});
    });
    modal.querySelectorAll('[data-close-dialog]').forEach((button) => {
      button.addEventListener('click', () => modal.close());
    });
    modal.addEventListener('click', (event) => {
      const bounds = modal.getBoundingClientRect();
      const inside = event.clientX >= bounds.left && event.clientX <= bounds.right
        && event.clientY >= bounds.top && event.clientY <= bounds.bottom;
      if (!inside) modal.close();
    });
  });
  document.querySelectorAll('[data-transaction-view]').forEach((button) => {
    button.addEventListener('click', () => activateTransactionView(button.dataset.transactionView));
  });
  document.querySelectorAll('[data-dashboard-view]').forEach((button) => {
    button.addEventListener('click', () => activateDashboardView(button.dataset.dashboardView));
  });
  document.querySelectorAll('[data-dashboard-view-target]').forEach((button) => {
    button.addEventListener('click', () => activateDashboardView(button.dataset.dashboardViewTarget));
  });
  const dialog = document.querySelector('dialog[data-auto-open="true"]');
  if (dialog && typeof dialog.showModal === 'function') dialog.showModal();
}

function showWorkspaceUpdateError(form) {
  const container = form.closest('[data-assistant-panel]') || form.closest('dialog[open]') || document.querySelector('.content');
  if (!container) return;
  container.querySelector('.request-error')?.remove();
  const notice = document.createElement('div');
  notice.className = 'notice request-error' + (container.matches('dialog, [data-assistant-panel]') ? '' : ' in-place-notice');
  notice.setAttribute('role', 'alert');
  const message = form.matches('[data-chat-form]')
    ? (selectedLanguage() === 'vi' ? form.dataset.chatUnavailableVi || 'Không xác nhận được kết quả. Hãy tải lại để kiểm tra dữ liệu trước khi thử lại.' : form.dataset.chatUnavailableEn || 'Unable to confirm the result. Reload to check the current data before trying again.')
    : 'Unable to confirm the result. Reload to check the current data before trying again.';
  notice.textContent = translateValue(message, selectedLanguage());
  originalText.set(notice.firstChild, message);
  container.prepend(notice);
  initializeWorkspaceNotices(container);
}

let pendingPaymentConfirmation = false;
// Confirmation displays server-rendered plan fields. It never calculates amounts or grants authority.
function confirmPaymentPlan(form) {
  const dialog = document.querySelector('[data-payment-confirm-dialog]');
  if (!dialog || typeof dialog.showModal !== 'function' || pendingPaymentConfirmation
      || dialog.dataset.actionId !== form.dataset.paymentApproval) return Promise.resolve(false);
  pendingPaymentConfirmation = true;
  const check = dialog.querySelector('[data-payment-confirm-check]');
  const confirm = dialog.querySelector('[data-payment-confirm-submit]');
  const quoteLabel = dialog.querySelector('[data-confirm-quote-status]');
  check.checked = false;
  const opener = form.querySelector('[data-testid="approve-action"]');
  return new Promise(resolve => {
    const listeners = new AbortController();
    let completed = false;
    const valid = () => {
      const expiry = dialog.dataset.quoteExpires;
      const expired = dialog.dataset.actionType === 'TUITION' && (!expiry || !Number.isFinite(Number(expiry)) || Number(expiry) <= Date.now());
      const changed = !form.isConnected || document.querySelector('[data-testid="latest-action"]')?.dataset.actionId !== form.dataset.paymentApproval;
      const paused = document.querySelector('[data-global-policy]')?.dataset.agentState !== 'ACTIVE';
      const message = expired ? 'Quote expired. Go back and refresh the quote before approving.'
        : changed || paused ? 'Payment is paused or this plan changed. Go back and review its current status.'
        : 'The payment details will be checked again before execution.';
      let statusText = translateValue(message, selectedLanguage());
      if (!expired && !changed && !paused && dialog.dataset.actionType === 'TUITION') {
        statusText = translateValue('Quote valid until', selectedLanguage())+' '+new Intl.DateTimeFormat(selectedLanguage()==='vi'?'vi-VN':'en-GB',{hour:'2-digit',minute:'2-digit',second:'2-digit',day:'2-digit',month:'2-digit',year:'numeric'}).format(new Date(Number(expiry)));
      }
      if (quoteLabel.textContent !== statusText) quoteLabel.textContent = statusText;
      quoteLabel.classList.toggle('quote-expired', expired || changed || paused);
      confirm.disabled = !check.checked || expired || changed || paused;
      return !expired && !changed && !paused;
    };
    const finish = approved => {
      if (completed) return;
      completed = true;
      listeners.abort();
      window.clearInterval(timer);
      pendingPaymentConfirmation = false;
      if (dialog.open) dialog.close();
      if (opener?.isConnected) opener.focus({preventScroll:true});
      resolve(approved);
    };
    const timer = window.setInterval(valid, 500);
    check.addEventListener('change', valid, {signal:listeners.signal});
    dialog.querySelectorAll('[data-payment-confirm-cancel]').forEach(button =>
      button.addEventListener('click', () => finish(false), {signal:listeners.signal}));
    dialog.addEventListener('keydown', event => {
      if (event.key !== 'Tab') return;
      const controls = Array.from(dialog.querySelectorAll('button:not(:disabled),input:not(:disabled),summary,a[href]'))
        .filter(control => control.getClientRects().length);
      const first = controls[0], last = controls.at(-1);
      if ((event.shiftKey && document.activeElement === first) || (!event.shiftKey && document.activeElement === last)) {
        event.preventDefault();
        (event.shiftKey ? last : first)?.focus({preventScroll:true});
      }
    }, {signal:listeners.signal});
    dialog.addEventListener('cancel', event => { event.preventDefault(); finish(false); }, {signal:listeners.signal});
    dialog.addEventListener('close', () => finish(false), {signal:listeners.signal});
    confirm.addEventListener('click', () => {
      if (!check.checked || !valid()) return;
      confirm.disabled = true;
      finish(true);
    }, {signal:listeners.signal});
    dialog.querySelector('[data-payment-confirm-stop]')?.addEventListener('click', () => {
      const stop = document.querySelector('[data-testid="emergency-stop"]')?.closest('form');
      finish(false);
      stop?.requestSubmit();
    }, {signal:listeners.signal});
    valid();
    dialog.showModal();
    dialog.scrollTop = 0;
    dialog.querySelector('.confirmation-body')?.scrollTo({top:0});
  });
}

async function submitWorkspaceForm(event) {
  const form = event.target;
  if (!(form instanceof HTMLFormElement) || form.method.toLowerCase() !== 'post') return;
  const action = new URL(form.getAttribute('action') || window.location.href, window.location.href);
  if (action.origin !== window.location.origin) return;
  if (form.dataset.paymentApproval) {
    event.preventDefault();
    if (pendingUpdate || pendingPaymentConfirmation || form.getAttribute('aria-busy') === 'true') return;
    if (!await confirmPaymentPlan(form)) return;
    if (!form.isConnected || pendingUpdate) return;
  } else if (form.dataset.confirmEn) {
    const message = selectedLanguage() === 'vi' ? form.dataset.confirmVi : form.dataset.confirmEn;
    if (!window.confirm(message)) {
      event.preventDefault();
      return;
    }
  }
  if (!window.fetch || !window.DOMParser) return;
  event.preventDefault();
  if (form.dataset.billSelected === 'true') return;
  // Emergency Stop must remain available while another request is pending.
  const changesContext = ['/reset', '/agent/emergency-stop', '/agent/context', '/agent/context/choice', '/agent/runway/monthly-expense', '/student/expenses/select', '/student/source-account', '/student/quotes/refresh'].includes(action.pathname);
  if (pendingUpdate && !changesContext) return;
  if (pendingChatController && changesContext) {
    pendingChatController.abort();
    pendingChatController = null;
  }

  const processing = form.closest('[data-testid="latest-action"]')?.querySelector('[data-payment-processing]');
  if (processing && action.pathname.endsWith('/approve')) processing.hidden = false;
  const restoreBillFocus = event.submitter?.matches('.student-bill-select:focus-visible');
  const requestId = ++updateSequence;
  pendingUpdate = requestId;
  const isContext = action.pathname === '/agent/context' || action.pathname === '/agent/context/choice' || action.pathname === '/agent/runway/monthly-expense';
  const previousReplies = new Set(Array.from(document.querySelectorAll('[data-testid="assistant-replies"] .message.assistant [data-response-raw]'), node => node.textContent));
  const isChat = action.pathname === '/agent/message' || isContext;
  const chatController = isChat ? new AbortController() : null;
  if (chatController) pendingChatController = chatController;
  const chatTimer = chatController ? setTimeout(() => chatController.abort(),
    Number(form.dataset.chatTimeout || 70000)) : null;
  const chatForms = isChat ? Array.from(document.querySelectorAll('[data-chat-form], [data-context-form]')).filter(composer => !composer.matches('[data-runway-form]') || composer === form) : [];
  const chatControls = chatForms.flatMap((composer) => Array.from(composer.querySelectorAll('button,input'))
    .map((control) => {
      if (!chatControlBaseline.has(control)) chatControlBaseline.set(control, control.disabled);
      return { control, disabled: chatControlBaseline.get(control) };
    }));
  const data = new FormData(form);
  if (isContext) data.set('token', form.elements.token.value);
  if (isChat) data.set('language', selectedLanguage());
  chatControls.forEach(({ control }) => { control.disabled = true; });
  chatForms.forEach((composer) => {
    composer.setAttribute('aria-busy', 'true');
    const label = composer.querySelector('[data-chat-processing]');
    if (label) { label.hidden = false; label.setAttribute('role', 'status'); label.textContent = translateValue(isContext ? 'Updating conversation context…' : 'Processing your question…', selectedLanguage()); }
  });
  if (event.submitter?.name) data.append(event.submitter.name, event.submitter.value);
  const body = form.enctype === 'multipart/form-data' ? data : new URLSearchParams(data);
  const buttons = Array.from(form.querySelectorAll('button[type="submit"], button:not([type])'))
    .map((button) => ({ button, disabled: isChat ? (chatControls.find(item => item.control === button)?.disabled ?? button.disabled) : button.disabled }));
  buttons.forEach(({ button }) => { button.disabled = true; });
  let requestStatus=null;
  if(!isChat && !(processing && action.pathname.endsWith('/approve'))){
    requestStatus=document.createElement('span');requestStatus.className='request-processing';
    requestStatus.setAttribute('role','status');requestStatus.textContent=translateValue('Processing…',selectedLanguage());
    form.append(requestStatus);
  }
  form.setAttribute('aria-busy', 'true');
  document.querySelector('.content')?.setAttribute('aria-busy', 'true');

  try {
    const response = await fetch(action, {
      method: 'POST', body, credentials: 'same-origin',
      ...(chatController ? { signal: chatController.signal } : {}),
      headers: { Accept: 'text/html', ...(action.pathname !== '/reset' && document.querySelector('[data-testid="latest-action"]')?.dataset.actionId ? { 'X-Workspace-Action': document.querySelector('[data-testid="latest-action"]').dataset.actionId } : {}) }
    });
    if (!response.ok) throw new Error('Update failed');
    const documentText = await response.text();
    if (requestId !== updateSequence) return;
    const nextDocument = new DOMParser().parseFromString(documentText, 'text/html');
    const nextContent = nextDocument.querySelector('.content');
    const content = document.querySelector('.content');
    if (!nextContent || !content) throw new Error('Workspace response missing');
    const resultUrl = new URL(response.url);
    if (resultUrl.origin !== window.location.origin) throw new Error('Unexpected update destination');
    // Capture at completion so a user's scrolling while waiting is preserved.
    const state = captureWorkspaceState();
    let anchor = form.dataset.nextAnchor || state.anchor;
    if (anchor === 'transaction-event') {
      anchor = resultUrl.searchParams.has('newTransaction')
        ? (resultUrl.searchParams.has('review') ? '#transaction-review' : '#transactions')
        : state.anchor;
    }
    const destinationTabs = {
      '#overview': 'dashboard', '#accounts': 'dashboard', '#planning': 'dashboard',
      '#transactions': 'transactions',
      '#transaction-review': 'transactions', '#transaction-categories': 'transactions',
      '#transaction-tools': 'transactions', '#student-finance': 'student',
      '#agent-workspace': 'agent'
    };
    const destinationTab = destinationTabs[anchor] || state.tab;
    const preservePosition = !form.dataset.nextAnchor || (destinationTab === state.tab && anchor === state.anchor);
    if (action.pathname === '/reset') {
      state.filters.clear();
      state.scrolls.clear();
      state.transactionPage = 1;
      state.billPage = 1;
    }
    const focusTestId = event.submitter?.dataset.testid;
    const selectedAction = nextContent.querySelector('[data-testid="latest-action"]')?.dataset.actionId;
    if (selectedAction) resultUrl.searchParams.set('action', selectedAction); else resultUrl.searchParams.delete('action');
    const nextLocation = resultUrl.pathname + resultUrl.search + anchor;
    if (nextLocation !== window.location.pathname + window.location.search + window.location.hash) {
      history.replaceState(null, '', nextLocation);
    }
    // Keep the intended panel visible during replacement, avoiding viewport clamping.
    nextContent.querySelectorAll('[data-tab-panel]').forEach((panel) => {
      panel.hidden = panel.dataset.tabPanel !== destinationTab;
    });
    if (destinationTab === 'transactions') {
      const view = initialTransactionView();
      nextContent.querySelectorAll('[data-transaction-view-panel]').forEach((panel) => {
        panel.hidden = panel.dataset.transactionViewPanel !== view;
      });
    }
    content.replaceChildren(...nextContent.childNodes);
    syncPaymentNavigation(nextDocument);
    syncAssistant(nextDocument, action.pathname === '/reset');
    if (action.pathname === '/agent/message' && form.closest('[data-assistant-panel]')) form.reset();
    content.querySelector('.notice')?.classList.add('in-place-notice');
    initializeWorkspaceContent({ ...state, tab: destinationTab });
    restoreAssistantPosition();

    const restorePosition = () => {
      state.scrolls.forEach((position, selector) => {
        const element = document.querySelector(selector);
        if (element) {
          element.scrollLeft = position.left;
          element.scrollTop = position.top;
        }
      });
      if (isChat) {
        document.querySelectorAll('.finance-message-list, .message-list, .assistant-message-list').forEach((list) => {
          if (!list.getClientRects().length) return;
          const latestQuestion = Array.from(list.querySelectorAll('.message.user')).at(-1);
          const reply = action.pathname === '/agent/runway/monthly-expense' && list.matches('.assistant-message-list')
            ? Array.from(list.querySelectorAll('.message.assistant')).find(node => !previousReplies.has(node.querySelector('[data-response-raw]')?.textContent)) || list.querySelector('.message.assistant:last-child')
            : latestQuestion?.nextElementSibling;
          if (reply?.classList.contains('assistant')) {
            // Reveal the beginning of the new reply, including assumptions, without moving the page.
            const scrollRegion = list.closest('.assistant-body') || list;
            scrollRegion.scrollTop += reply.getBoundingClientRect().top - scrollRegion.getBoundingClientRect().top - 8;
          }
        });
        rememberAssistantPosition();
      }
      if (focusTestId) {
        document.querySelector('[data-testid="' + CSS.escape(focusTestId) + '"]')
          ?.focus({ preventScroll: true });
      } else if (action.pathname === '/student/expenses/select' && restoreBillFocus) {
        document.querySelector('.student-bill-select[aria-pressed="true"]')?.focus({ preventScroll: true });
      }
      window.scrollTo({ left: preservePosition ? state.left : 0, top: preservePosition ? state.top : 0, behavior: 'instant' });
    };
    restorePosition();
    await new Promise(requestAnimationFrame);
    if (requestId === updateSequence) restorePosition();
  } catch (error) {
    // A failed response can follow a successful mutation. Do not resend automatically.
    if (requestId === updateSequence) showWorkspaceUpdateError(form);
  } finally {
    if (chatTimer) clearTimeout(chatTimer);
    if (pendingChatController === chatController) pendingChatController = null;
    chatControls.forEach(({ control, disabled }) => { if (control.isConnected && (pendingUpdate === requestId || !pendingUpdate)) { control.disabled = disabled; chatControlBaseline.delete(control); } });
    chatForms.forEach((composer) => {
      if (!composer.isConnected || (pendingUpdate && pendingUpdate !== requestId)) return;
      composer.removeAttribute('aria-busy');
      const label = composer.querySelector('[data-chat-processing]');
      if (label) { label.hidden = true; label.removeAttribute('role'); }
    });
    buttons.forEach(({ button, disabled }) => { if (button.isConnected && (pendingUpdate === requestId || !pendingUpdate)) button.disabled = disabled; });
    if (form.isConnected) form.removeAttribute('aria-busy');
    if (processing?.isConnected) processing.hidden = true;
    requestStatus?.remove();
    if (pendingUpdate === requestId) {
      pendingUpdate = 0;
      document.querySelector('.content')?.removeAttribute('aria-busy');
      if (action.pathname === '/reset' || action.pathname === '/agent/emergency-stop') {
        const sharedComposer = document.querySelector('[data-assistant-panel] [data-chat-form]');
        sharedComposer?.querySelectorAll('input,button').forEach((control) => { control.disabled = false; });
        sharedComposer?.removeAttribute('aria-busy');
        const label = sharedComposer?.querySelector('[data-chat-processing]');
        if (label) { label.hidden = true; label.removeAttribute('role'); }
      }
      renderPaymentWorkflow();
    }
  }
}

document.addEventListener('DOMContentLoaded', () => {
  initializeWorkspaceContent();
  renderIntroduction();
  document.addEventListener('click', event => {
    if (event.target.closest('[data-dismiss-intro]')) {
      document.querySelector('[data-first-use]').hidden = true;
      localStorage.setItem('finbridge-intro-dismissed', 'true');
      document.querySelector('[data-testid="overview-primary-action"]')?.focus({ preventScroll: true });
    }
  });
  updateAssistantViewport();
  const workspaceHeader=document.querySelector('main>header');
  const syncHeaderHeight=()=>document.documentElement.style.setProperty('--workspace-header-height',workspaceHeader.getBoundingClientRect().height+'px');
  new ResizeObserver(syncHeaderHeight).observe(workspaceHeader);syncHeaderHeight();
  window.addEventListener('resize', updateAssistantViewport);
  window.visualViewport?.addEventListener('resize', updateAssistantViewport);
  window.visualViewport?.addEventListener('scroll', updateAssistantViewport);

  document.querySelectorAll('[data-tab]').forEach((button) => {
    button.addEventListener('click', () => activateTab(button.dataset.tab));
  });

  document.querySelectorAll('[data-language]').forEach((button) => {
    button.addEventListener('click', () => {
      const language = button.dataset.language || button.dataset.assistantLanguage;
      localStorage.setItem('finbridge-language', language);
      applyLanguage(language);
    });
  });
  document.addEventListener('invalid',event => {
    const control=event.target;
    if(!control.matches('input,select,textarea') || control.type==='hidden') return;
    const label=control.closest('label') || control.parentElement;
    let feedback=label.querySelector('.field-error');
    if(!feedback) {
      feedback=document.createElement('small');feedback.className='field-error';
      feedback.id='field-error-'+(++validationSequence);label.append(feedback);
    }
    const message=control.validity.valueMissing ? 'Fill in this field.' : control.validity.rangeUnderflow || control.validity.rangeOverflow ? 'Enter a value within the displayed limits.' : control.validity.patternMismatch ? 'Use the requested format.' : 'Enter a valid value to continue.';
    feedback.textContent=translateValue(message,selectedLanguage());
    control.setAttribute('aria-invalid','true');control.setAttribute('aria-describedby',feedback.id);
  },true);
  document.addEventListener('input',event => {
    const control=event.target;
    if(control.validity?.valid) {control.removeAttribute('aria-invalid');control.closest('label')?.querySelector('.field-error')?.remove();}
  });
  document.addEventListener('submit', submitWorkspaceForm);
  document.addEventListener('click', openPaymentPlan);
  document.addEventListener('click', handleDocumentExtraction);
  document.addEventListener('click', handleAssistantClick);
  document.addEventListener('click', (event) => {
    if (event.target.closest('[data-return-to-comparison]')) { event.preventDefault(); activateTab('student'); }
  });
  document.addEventListener('click', (event) => {
    const link = event.target.closest('a[href^="#"]');
    if (!link) return;
    const hash = link.getAttribute('href');
    const destination = {
      '#overview': ['dashboard', 'summary'], '#accounts': ['dashboard', 'accounts'],
      '#planning': ['dashboard', 'planning'], '#transaction-review': ['transactions', 'review'],
      '#transactions': ['transactions', 'history'], '#student-finance': ['student', null],
      '#agent-workspace': ['agent', null]
    }[hash];
    if (!destination) return;
    event.preventDefault();
    activateTab(destination[0], false);
    if (destination[0] === 'dashboard') activateDashboardView(destination[1], false);
    if (destination[0] === 'transactions') activateTransactionView(destination[1], false);
    history.replaceState(null, '', hash);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  });

  document.addEventListener('click', (event) => {
    document.querySelectorAll('.category-row-actions details[open]').forEach((details) => {
      if (!details.contains(event.target)) details.removeAttribute('open');
    });
  });
  document.addEventListener('keydown', (event) => {
    const panel = document.querySelector('[data-assistant-panel]');
    if (event.key === 'Tab' && panel?.getAttribute('aria-modal') === 'true' && !document.querySelector('dialog[open]')) {
      const controls = assistantFocusables(panel);
      const first = controls[0], last = controls.at(-1);
      if (!controls.length) return;
      if (!panel.contains(document.activeElement) || (event.shiftKey && document.activeElement === first) || (!event.shiftKey && document.activeElement === last)) {
        event.preventDefault();
        (event.shiftKey ? last : first).focus({preventScroll:true});
      }
    }
    if (event.key === 'Escape') {
      if (!document.querySelector('dialog[open]') && !document.querySelector('[data-assistant-panel]').hidden) setAssistantOpen(false);
      document.querySelectorAll('.category-row-actions details[open]').forEach((details) => {
        details.removeAttribute('open');
      });
    }
  });
});
