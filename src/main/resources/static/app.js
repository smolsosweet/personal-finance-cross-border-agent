const translationsVi = new Map(Object.entries({
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
  'Manage several student bills, verify the selected recipient and compare simulated payment channels. No provider API or real payment is connected.': 'Quản lý nhiều khoản phí du học, xác minh người nhận của khoản đang chọn và so sánh các kênh thanh toán mô phỏng. Không kết nối API nhà cung cấp hoặc tiền thật.',
  '+ Add expense': '+ Thêm khoản phí',
  'STUDENT EXPENSES': 'CÁC KHOẢN PHÍ DU HỌC',
  'Select an expense to compare': 'Chọn khoản phí để so sánh',
  'The selected bill controls recipient verification, quote amounts and the action plan.': 'Khoản đang chọn quyết định việc xác minh người nhận, số tiền báo giá và kế hoạch hành động.',
  'Selected': 'Đang chọn',
  'Compare this expense': 'So sánh khoản này',
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
  'Add a bill or fee': 'Thêm hóa đơn hoặc khoản phí',
  'Enter the verified fields from your bill. An attachment is stored as demo evidence only; this version does not run OCR.': 'Nhập các trường đã kiểm tra từ hóa đơn. File đính kèm chỉ được lưu làm bằng chứng demo; phiên bản này chưa chạy OCR.',
  'Expense type': 'Loại chi phí',
  'Expense name': 'Tên khoản phí',
  'Institution or provider': 'Trường hoặc đơn vị cung cấp',
  'Currency': 'Tiền tệ',
  'China': 'Trung Quốc',
  'United States': 'Hoa Kỳ',
  'Australia': 'Úc',
  'Recipient account': 'Tài khoản người nhận',
  'Payment reference': 'Mã tham chiếu thanh toán',
  'Attachment (optional)': 'File đính kèm (không bắt buộc)',
  'PDF, JPG or PNG · maximum 5 MB · no OCR in this version': 'PDF, JPG hoặc PNG · tối đa 5 MB · phiên bản này chưa có OCR',
  'Add and compare expense': 'Thêm và so sánh khoản phí',
  'Tuition': 'Học phí',
  'Dormitory': 'Ký túc xá',
  'Insurance': 'Bảo hiểm',
  'Visa': 'Thị thực',
  'Living expense': 'Sinh hoạt phí',
  'Other': 'Khác',
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
  'Recipient verified': 'Đã xác minh người nhận',
  'Recipient mismatch': 'Người nhận không khớp',
  'No matching account': 'Không có tài khoản khớp',
  'Bill institution, recipient account, corridor and currency match the School Registry': 'Trường, tài khoản nhận, hành lang và tiền tệ trên hóa đơn khớp Danh bạ Nhà trường',
  'Bill recipient does not match the School Registry': 'Người nhận trên hóa đơn không khớp Danh bạ Nhà trường',
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

const originalText = new WeakMap();
const originalPlaceholder = new WeakMap();
const translationFragments = [...translationsVi.entries()]
  .sort(([left], [right]) => right.length - left.length);
const tabLabels = {
  dashboard: { en: 'Dashboard', vi: 'Tổng quan' },
  transactions: { en: 'Transactions', vi: 'Giao dịch' },
  student: { en: 'Student finance', vi: 'Tài chính du học' },
  agent: { en: 'Agent & Payments', vi: 'Tác vụ & Thanh toán' }
};
const tabAnchors = {
  dashboard: '#overview',
  transactions: '#transactions',
  student: '#student-finance',
  agent: '#agent-workspace'
};
let activeTab = 'dashboard';
let renderTransactionList = () => {};
let renderPaymentAccounts = () => {};
let renderQuoteExpiryStatuses = () => {};

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
    button.setAttribute('aria-selected', String(button.dataset.tab === activeTab));
  });
  if (activeTab === 'transactions') {
    activateTransactionView(updateLocation ? 'history' : initialTransactionView(), false);
  }
  updateActiveTabLabel();
  localStorage.setItem('finbridge-active-tab', activeTab);
  if (updateLocation) {
    history.replaceState(null, '', tabAnchors[activeTab]);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
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
    button.setAttribute('aria-selected', String(button.dataset.transactionView === requested));
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
    '#transaction-tools': 'transactions', '#transactions': 'transactions',
    '#transaction-review': 'transactions', '#transaction-categories': 'transactions',
    '#student-finance': 'student', '#agent-workspace': 'agent'
  };
  return hashTabs[window.location.hash]
    || localStorage.getItem('finbridge-active-tab')
    || 'dashboard';
}

function initializeTransactionList() {
  const rows = Array.from(document.querySelectorAll('[data-testid="transaction-row"]'));
  if (!rows.length) return;

  const search = document.querySelector('[data-testid="transaction-search"]');
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
  let page = 1;

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
    let matches = rows.filter((row) =>
      (!query || row.dataset.search.toLowerCase().includes(query))
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
    const start = (page - 1) * pageSize;
    const visibleRows = new Set(matches.slice(start, start + pageSize));
    rows.forEach((row) => { row.hidden = !visibleRows.has(row); });

    const language = selectedLanguage();
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

  [search, type, status, category, sort].forEach((control) => {
    control.addEventListener(control === search ? 'input' : 'change', () => {
      page = 1;
      render();
    });
  });
  clear.addEventListener('click', () => {
    search.value = '';
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

function initializePaymentAccounts() {
  const grid = document.querySelector('[data-testid="payment-account-list"]');
  if (!grid) return;
  const cards = Array.from(grid.querySelectorAll('.connected-payment-card'));
  const filter = document.querySelector('[data-testid="payment-account-filter"]');
  const empty = document.querySelector('[data-testid="payment-account-empty"]');

  const render = () => {
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
  const select = document.querySelector('[data-corridor-select]');
  if (!select) return;
  const currency = document.querySelector('[data-corridor-currency]');
  const institution = document.querySelector('[data-corridor-institution]');
  const recipient = document.querySelector('[data-corridor-recipient]');
  const render = () => {
    const option = select.selectedOptions[0];
    if (!option) return;
    currency.value = option.dataset.currency;
    institution.value = option.dataset.institution;
    recipient.value = option.dataset.recipient;
  };
  select.addEventListener('change', render);
  render();
}

function initializeQuoteExpiryStatuses() {
  const cards = Array.from(document.querySelectorAll('[data-quote-expiry]'));
  if (!cards.length) return;

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
  };
  renderQuoteExpiryStatuses = render;
  render();
  window.setInterval(render, 1000);
}

function translateDynamic(text) {
  const rules = [
    [/^(\d+) insights$/, '$1 thông tin'],
    [/^(\d+) records$/, '$1 bản ghi'],
    [/^(\d+) recent events$/, '$1 sự kiện gần đây'],
    [/^(\d+) pending$/, '$1 mục cần xử lý'],
    [/^(\d+) bill\(s\)$/, '$1 khoản phí'],
    [/^Due (.+)$/, 'Hạn $1'],
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
  if (language !== 'vi') return value;
  const trimmed = value.trim();
  if (!trimmed) return value;
  let translated = translationsVi.get(trimmed) || translateDynamic(trimmed);
  if (translated === trimmed) {
    for (const [english, vietnamese] of translationFragments) {
      if (translated.includes(english)) translated = translated.split(english).join(vietnamese);
    }
  }
  return translated === trimmed ? value : value.replace(trimmed, translated);
}

function applyLanguage(language) {
  const selected = language === 'vi' ? 'vi' : 'en';
  document.documentElement.lang = selected;
  document.title = selected === 'vi' ? 'Atlas · Giai đoạn 5' : 'Atlas · Phase 5';
  document.querySelectorAll('[data-language]').forEach((button) => {
    button.setAttribute('aria-pressed', String(button.dataset.language === selected));
  });
  const languageGroup = document.querySelector('.language-switcher');
  if (languageGroup) languageGroup.setAttribute('aria-label', selected === 'vi' ? 'Ngôn ngữ' : 'Language');

  const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {
    acceptNode(node) {
      return ['SCRIPT', 'STYLE'].includes(node.parentElement?.tagName)
        ? NodeFilter.FILTER_REJECT : NodeFilter.FILTER_ACCEPT;
    }
  });
  const nodes = [];
  while (walker.nextNode()) nodes.push(walker.currentNode);
  nodes.forEach((node) => {
    if (!originalText.has(node)) originalText.set(node, node.nodeValue);
    node.nodeValue = translateValue(originalText.get(node), selected);
  });

  document.querySelectorAll('[placeholder]').forEach((element) => {
    if (!originalPlaceholder.has(element)) originalPlaceholder.set(element, element.placeholder);
    const original = originalPlaceholder.get(element);
    if (selected === 'vi' && translationsVi.has(original)) {
      element.placeholder = translationsVi.get(original);
    } else if (selected === 'vi' && original === 'Ask about tuition, surplus, or create a payment plan') {
      element.placeholder = 'Hỏi về học phí, số dư khả dụng hoặc tạo kế hoạch thanh toán';
    } else if (selected === 'vi' && original === 'e.g. Groceries') {
      element.placeholder = 'ví dụ: Thực phẩm';
    } else {
      element.placeholder = original;
    }
  });
  updateActiveTabLabel(selected);
  renderTransactionList();
  renderPaymentAccounts();
  renderQuoteExpiryStatuses();
}

document.addEventListener('DOMContentLoaded', () => {
  const dialog = document.querySelector('dialog[data-auto-open="true"]');
  if (dialog && typeof dialog.showModal === 'function') dialog.showModal();

  activateTab(initialTab(), false);
  requestAnimationFrame(() => window.scrollTo({ top: 0 }));
  initializeTransactionList();
  initializeCategoryReviewForms();
  initializePaymentAccounts();
  initializeStudentExpenseCorridor();
  initializeQuoteExpiryStatuses();
  const savedLanguage = localStorage.getItem('finbridge-language') || 'en';
  applyLanguage(savedLanguage);

  document.querySelectorAll('[data-tab]').forEach((button) => {
    button.addEventListener('click', () => activateTab(button.dataset.tab));
  });

  document.querySelectorAll('[data-transaction-view]').forEach((button) => {
    button.addEventListener('click', () => activateTransactionView(button.dataset.transactionView));
  });

  document.querySelectorAll('[data-language]').forEach((button) => {
    button.addEventListener('click', () => {
      localStorage.setItem('finbridge-language', button.dataset.language);
      applyLanguage(button.dataset.language);
    });
  });

  document.querySelectorAll('[data-open-dialog]').forEach((button) => {
    button.addEventListener('click', () => {
      const dialog = document.getElementById(button.dataset.openDialog);
      if (dialog && typeof dialog.showModal === 'function') dialog.showModal();
    });
  });

  document.querySelectorAll('dialog').forEach((modal) => {
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

  document.addEventListener('click', (event) => {
    document.querySelectorAll('.category-row-actions details[open]').forEach((details) => {
      if (!details.contains(event.target)) details.removeAttribute('open');
    });
  });

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') {
      document.querySelectorAll('.category-row-actions details[open]').forEach((details) => {
        details.removeAttribute('open');
      });
    }
  });

  document.querySelectorAll('form[data-confirm-en]').forEach((form) => {
    form.addEventListener('submit', (event) => {
      const language = localStorage.getItem('finbridge-language') || 'en';
      const message = language === 'vi' ? form.dataset.confirmVi : form.dataset.confirmEn;
      if (!window.confirm(message)) event.preventDefault();
    });
  });
});
