package com.example.finance;

import java.util.List;
import static com.example.finance.LlmIntent.Intent.*;

/** Fixed acceptance corpus: interpretation mocked in regression; real model evaluated separately. */
final class GlobalAssistantCorpus {
    record Case(String text,String language,LlmIntent.Intent intent,String object,String source,String evidence) {}
    static List<Case> cases(){return List.of(
        new Case("What is my current Vietcombank balance?","en",EXPLAIN_CURRENT_BALANCE,"VCB_VND","sandbox_accounts","82000000.00"),
        new Case("Hiện Vietcombank còn bao nhiêu tiền?","vi",EXPLAIN_CURRENT_BALANCE,"VCB_VND","sandbox_accounts","82000000.00"),
        new Case("Show all my account balances","en",EXPLAIN_CURRENT_BALANCE,"ALL_PROFILE_ACCOUNTS","sandbox_accounts","314000000.00"),
        new Case("Tổng số tiền tôi còn lại trong tất cả tài khoản là bao nhiêu?","vi",EXPLAIN_CURRENT_BALANCE,"ALL_PROFILE_ACCOUNTS","sandbox_accounts","314000000.00"),
        new Case("Show my remaining budgets this month","en",EXPLAIN_BUDGET_STATUS,"CURRENT_MONTH","budgets/transactions","Budget remaining is not an account balance"),
        new Case("Ngân sách tháng này còn bao nhiêu?","vi",EXPLAIN_BUDGET_STATUS,"CURRENT_MONTH","budgets/transactions","Ngân sách không phải số dư tài khoản"),
        new Case("Where did I spend most this month?","en",EXPLAIN_SPENDING_SUMMARY,"CURRENT_MONTH","transactions","Expense total"),
        new Case("Tháng này tôi chi nhiều nhất vào đâu?","vi",EXPLAIN_SPENDING_SUMMARY,"CURRENT_MONTH","transactions","Tổng chi tiêu"),
        new Case("Compare tuition channels","en",COMPARE_TUITION_CHANNELS,"SZDU-2026-MINH","fx_quotes","Verified tuition-channel comparison"),
        new Case("So sánh các kênh học phí","vi",COMPARE_TUITION_CHANNELS,"SZDU-2026-MINH","fx_quotes","So sánh kênh học phí"),
        new Case("Why is Bank B unavailable?","en",EXPLAIN_CHANNEL_UNAVAILABLE,"BANK_B","payment_channel_corridors","cannot be executed"),
        new Case("Vì sao không dùng được Bank B?","vi",EXPLAIN_CHANNEL_UNAVAILABLE,"BANK_B","payment_channel_corridors","không thể thực thi"),
        new Case("Can I afford tuition using Vietcombank?","en",EXPLAIN_TUITION_AFFORDABILITY,"VCB_VND/SZDU-2026-MINH","sandbox_accounts/fx_quotes","11153028.00"),
        new Case("Nếu đóng học phí bằng Vietcombank thì còn bao nhiêu?","vi",EXPLAIN_TUITION_AFFORDABILITY,"VCB_VND/SZDU-2026-MINH","sandbox_accounts/fx_quotes","11153028.00"),
        new Case("How many months of living costs after tuition?","en",EXPLAIN_LIVING_EXPENSE_RUNWAY,"PAYER_VND/SZDU-2026-MINH","confirmed monthly form","each month"),
        new Case("Sau khi đóng học phí đủ sinh hoạt mấy tháng?","vi",EXPLAIN_LIVING_EXPENSE_RUNWAY,"PAYER_VND/SZDU-2026-MINH","confirmed monthly form","mỗi tháng")
    );}
}
