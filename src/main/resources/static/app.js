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
  'Synthetic Phase 4 data reset.': 'Đã đặt lại dữ liệu mô phỏng Giai đoạn 4.'
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
  transactions: '#transaction-tools',
  student: '#student-finance',
  agent: '#agent-workspace'
};
let activeTab = 'dashboard';
let renderTransactionList = () => {};

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
  updateActiveTabLabel();
  localStorage.setItem('finbridge-active-tab', activeTab);
  if (updateLocation) {
    history.replaceState(null, '', tabAnchors[activeTab]);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}

function initialTab() {
  const hashTabs = {
    '#overview': 'dashboard', '#feed': 'dashboard', '#budget': 'dashboard',
    '#transaction-tools': 'transactions', '#transactions': 'transactions',
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

function translateDynamic(text) {
  const rules = [
    [/^(\d+) insights$/, '$1 thông tin'],
    [/^(\d+) records$/, '$1 bản ghi'],
    [/^(\d+) recent events$/, '$1 sự kiện gần đây'],
    [/^(.+) VND remaining$/, 'Còn lại $1 VND'],
    [/^Quoted (.+)$/, 'Báo giá lúc $1'],
    [/^Expires (.+)$/, 'Hết hạn lúc $1'],
    [/^(.+) day\(s\)$/, '$1 ngày'],
    [/^(.+)\/100 synthetic$/, '$1/100 mô phỏng'],
    [/^Evidence: (.+)$/, 'Bằng chứng: $1'],
    [/^Payer: (.+)$/, 'Người trả: $1'],
    [/^Recipient: (.+)$/, 'Người nhận: $1'],
    [/^Simulated Bank Event: (.+)$/, 'Sự kiện ngân hàng mô phỏng: $1'],
    [/^Channel ranking updated to (.+)\.$/, 'Đã cập nhật xếp hạng kênh theo $1.'],
    [/^Tuition plan (.+)\. Approval is always required\.$/, 'Kế hoạch học phí $1. Luôn yêu cầu phê duyệt.'],
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
    if (selected === 'vi' && original === 'Ask about tuition, surplus, or create a payment plan') {
      element.placeholder = 'Hỏi về học phí, số dư khả dụng hoặc tạo kế hoạch thanh toán';
    } else if (selected === 'vi' && original === 'e.g. Groceries') {
      element.placeholder = 'ví dụ: Thực phẩm';
    } else {
      element.placeholder = original;
    }
  });
  updateActiveTabLabel(selected);
  renderTransactionList();
}

document.addEventListener('DOMContentLoaded', () => {
  const dialog = document.querySelector('dialog[data-auto-open="true"]');
  if (dialog && typeof dialog.showModal === 'function') dialog.showModal();

  activateTab(initialTab(), false);
  requestAnimationFrame(() => window.scrollTo({ top: 0 }));
  initializeTransactionList();
  const savedLanguage = localStorage.getItem('finbridge-language') || 'en';
  applyLanguage(savedLanguage);

  document.querySelectorAll('[data-tab]').forEach((button) => {
    button.addEventListener('click', () => activateTab(button.dataset.tab));
  });

  document.querySelectorAll('[data-language]').forEach((button) => {
    button.addEventListener('click', () => {
      localStorage.setItem('finbridge-language', button.dataset.language);
      applyLanguage(button.dataset.language);
    });
  });

  document.querySelectorAll('form[data-confirm-en]').forEach((form) => {
    form.addEventListener('submit', (event) => {
      const language = localStorage.getItem('finbridge-language') || 'en';
      const message = language === 'vi' ? form.dataset.confirmVi : form.dataset.confirmEn;
      if (!window.confirm(message)) event.preventDefault();
    });
  });
});
