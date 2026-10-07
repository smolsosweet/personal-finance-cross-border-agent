package com.example.finance;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Locale;
import java.time.DateTimeException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** All figures come from existing aggregation, quote and account services; no model-generated facts. */
@Service
public class PersonalFinanceInsights {
    private final TransactionService transactions;
    private final FinanceWorkspaceService workspace;
    private final CrossBorderService crossBorder;
    private static final Pattern ISO_DATE = Pattern.compile("(?iu)(20\\d{2})-(\\d{1,2})-(\\d{1,2})");
    private static final Pattern DAY_MONTH_DATE = Pattern.compile("(?iu)(?:ngày\\s*)?(\\d{1,2})[/-](\\d{1,2})(?:[/-](20\\d{2}))?");
    private static final Pattern EXPENSE_WORD = Pattern.compile("(?iu)\\b(chi|spend|spent|spending|expense|expenses|paid|mua)\\b|đã chi|chi tiêu|tiêu bao nhiêu");
    private static final Pattern SPENDING_DATES = Pattern.compile("(?iu)ngày nào|những ngày nào|hôm nào|what dates|which dates|what days|which days|on what date");

    public PersonalFinanceInsights(TransactionService transactions, FinanceWorkspaceService workspace,
                                   CrossBorderService crossBorder) {
        this.transactions = transactions;
        this.workspace = workspace;
        this.crossBorder = crossBorder;
    }

    public String render(LlmIntent.Intent intent, PhaseFourService payments, boolean vi) {
        return render(intent, payments, vi, "");
    }

    public String render(LlmIntent.Intent intent, PhaseFourService payments, boolean vi, String question) {
        return switch (intent) {
            case EXPLAIN_SPENDING_SUMMARY -> spending(vi, question);
            case EXPLAIN_BUDGET_STATUS -> budgets(vi);
            case EXPLAIN_TUITION_AFFORDABILITY -> affordability(payments, vi);
            default -> throw new IllegalArgumentException("Not a read-only Personal Finance intent");
        };
    }

    /** Recognize a narrow, safe read-only query so a local LLM call is not needed for it. */
    public boolean isCategorySpendingQuestion(String question) {
        return !requestedCategories(question).isEmpty();
    }

    public boolean isSupportedSpendingScope(String question) {
        return isCategorySpendingQuestion(question) || isSpendingDatesQuestion(question) || requestedDate(question) != null && isExpenseQuestion(question);
    }

    public boolean isSpendingDatesQuestion(String question) {
        return isCategorySpendingQuestion(question) && SPENDING_DATES.matcher(question).find();
    }

    public boolean isDailySpendingQuestion(String question) {
        return requestedDate(question) != null && isExpenseQuestion(question);
    }

    private boolean isExpenseQuestion(String question) {
        return question != null && EXPENSE_WORD.matcher(question).find();
    }

    private LocalDate requestedDate(String question) {
        if (question == null) return null;
        LocalDate reportingDate = transactions.reportingDate();
        Matcher iso = ISO_DATE.matcher(question);
        Matcher localized = DAY_MONTH_DATE.matcher(question);
        try {
            LocalDate date;
            if (iso.find()) {
                date = LocalDate.of(Integer.parseInt(iso.group(1)), Integer.parseInt(iso.group(2)), Integer.parseInt(iso.group(3)));
            } else if (localized.find()) {
                int year = localized.group(3) == null ? reportingDate.getYear() : Integer.parseInt(localized.group(3));
                date = LocalDate.of(year, Integer.parseInt(localized.group(2)), Integer.parseInt(localized.group(1)));
            } else return null;
            return date.getYear() == reportingDate.getYear() && date.getMonth() == reportingDate.getMonth() ? date : null;
        } catch (DateTimeException | NumberFormatException ex) {
            return null;
        }
    }

    private List<String> requestedCategories(String question) {
        if (question == null || question.isBlank() || !isExpenseQuestion(question)) return List.of();
        String normalized = normalize(question);
        var requested = new LinkedHashSet<String>();
        Map<String, List<String>> aliases = Map.of(
                "Education", List.of("education", "giao duc", "hoc phi", "tuition", "school"),
                "Food & Drinks", List.of("food", "food drinks", "an uong", "coffee"),
                "Shopping", List.of("shopping", "mua sam", "groceries", "thuc pham"),
                "Transport", List.of("transport", "di lai", "xe buyt"),
                "Utilities", List.of("utilities", "tien ich", "dien nuoc"));
        for (var entry : aliases.entrySet()) {
            if (entry.getValue().stream().anyMatch(alias -> containsPhrase(normalized, normalize(alias))))
                requested.add(entry.getKey());
        }
        for (var row : transactions.monthlySpending()) {
            String category = (String) row.get("category");
            if (containsPhrase(normalized, normalize(category))) requested.add(category);
        }
        return List.copyOf(requested);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ").trim().replaceAll(" +", " ");
    }

    private static boolean containsPhrase(String text, String phrase) {
        return !phrase.isBlank() && (" " + text + " ").contains(" " + phrase + " ");
    }

    private String period(boolean vi) {
        LocalDate start = transactions.reportingDate().withDayOfMonth(1);
        return (vi ? "Kỳ báo cáo: " : "Reporting period: ") + start + " – " + start.plusMonths(1).minusDays(1) + ".\n";
    }

    private String spending(boolean vi, String question) {
        StringBuilder out = new StringBuilder(period(vi));
        var rows = transactions.monthlySpending();
        LocalDate requestedDate = requestedDate(question);
        List<String> requestedCategories = requestedCategories(question);
        if (requestedDate != null && isExpenseQuestion(question)) {
            var dayRows = transactions.spendingOn(requestedDate).stream()
                    .filter(row -> requestedCategories.isEmpty() || requestedCategories.stream()
                            .anyMatch(category -> category.equalsIgnoreCase((String) row.get("category"))))
                    .toList();
            out.append(vi ? "Giao dịch chi tiêu ngày " : "Expenses on ").append(requestedDate).append(":\n");
            if (dayRows.isEmpty()) return out.append(vi ? "Không có giao dịch chi tiêu đã xác nhận trong phạm vi này." : "No confirmed expenses found for this scope.").toString();
            for (var row : dayRows) {
                out.append(row.get("merchant")).append(" · ").append(categoryLabel((String) row.get("category"), vi))
                        .append(" · ").append(money((BigDecimal) row.get("amount"))).append(' ')
                        .append(row.get("currency")).append('\n');
            }
            var currencies = dayRows.stream().map(row -> (String) row.get("currency")).distinct().toList();
            for (String currency : currencies) {
                BigDecimal total = dayRows.stream().filter(row -> currency.equals(row.get("currency")))
                        .map(row -> (BigDecimal) row.get("amount")).reduce(BigDecimal.ZERO, BigDecimal::add);
                out.append(vi ? "Tổng " : "Total ").append(currency).append(": ").append(money(total)).append(".\n");
            }
            return out.append(vi ? "Nguồn: giao dịch chi tiêu tự động phân loại hoặc đã xác nhận; giao dịch chờ xem xét không được tính." : "Source: automatically categorized or confirmed expenses; pending reviews are excluded.").toString();
        }
        if (isSpendingDatesQuestion(question)) {
            var matching = requestedCategories.stream()
                    .flatMap(category -> transactions.monthlySpendingTransactions(category).stream())
                    .toList();
            out.append(vi ? "Các giao dịch " : "Transactions ")
                    .append(requestedCategories.stream().map(category -> categoryLabel(category, vi)).reduce((a,b) -> a + (vi ? " và " : " and ") + b).orElse(""))
                    .append(vi ? " trong kỳ báo cáo:\n" : " during this reporting period:\n");
            if (matching.isEmpty()) return out.append(vi ? "Chưa có giao dịch chi tiêu đã xác nhận trong danh mục này." : "No confirmed expenses recorded in this category.").toString();
            for (var row : matching) {
                out.append(row.get("spent_on")).append(" · ").append(row.get("merchant")).append(" · ")
                        .append(money((BigDecimal) row.get("amount"))).append(' ').append(row.get("currency")).append('\n');
            }
            return out.append(vi ? "Nguồn: giao dịch chi tiêu AUTO/CONFIRMED trong tháng báo cáo; giao dịch chờ xem xét không được tính." : "Source: AUTO/CONFIRMED expenses in the reporting month; pending reviews excluded.").toString();
        }
        if (!requestedCategories.isEmpty()) {
            var matching = rows.stream()
                    .filter(row -> requestedCategories.stream().anyMatch(category -> category.equalsIgnoreCase((String) row.get("category")))).toList();
            if (matching.isEmpty()) {
                return out.append(vi ? "Chưa ghi nhận khoản chi cho " : "No recorded spending for ")
                        .append(requestedCategories.stream().map(category -> categoryLabel(category, vi)).reduce((a,b) -> a + (vi ? " và " : " and ") + b).orElse(""))
                        .append(vi ? " trong kỳ này." : " in this period.").toString();
            }
            for (String currency : matching.stream().map(row -> (String) row.get("currency")).distinct().toList()) {
                BigDecimal combined = BigDecimal.ZERO;
                for (String category : requestedCategories) {
                    BigDecimal total = matching.stream().filter(row -> currency.equals(row.get("currency")) && category.equalsIgnoreCase((String) row.get("category")))
                            .map(row -> (BigDecimal) row.get("spent")).reduce(BigDecimal.ZERO, BigDecimal::add);
                    if (total.signum() == 0) continue;
                    combined = combined.add(total);
                    out.append(categoryLabel(category, vi)).append(": ").append(money(total)).append(' ').append(currency).append(".\n");
                }
                out.append(vi ? "Tổng các danh mục được hỏi: " : "Total for requested categories: ")
                        .append(money(combined)).append(' ').append(currency).append(".\n");
            }
            return out.append(vi
                    ? "Nguồn: giao dịch chi tiêu đã ghi nhận, trạng thái tự động hoặc đã xác nhận; không gồm giao dịch đang chờ xem xét."
                    : "Source: recorded expenses that are automatic or confirmed; pending-review transactions are excluded.").toString();
        }
        var refunds = transactions.monthlyRefunds();
        var currencies = new LinkedHashSet<String>();
        rows.forEach(row -> currencies.add((String) row.get("currency")));
        refunds.forEach(row -> currencies.add((String) row.get("currency")));
        if (currencies.isEmpty()) out.append(vi ? "Chưa có giao dịch chi tiêu/hoàn tiền trong kỳ.\n" : "No recorded expenses/refunds in this period.\n");
        for (String currency : currencies) {
            var items = rows.stream().filter(row -> currency.equals(row.get("currency"))).toList();
            BigDecimal total = items.stream().map(row -> (BigDecimal) row.get("spent")).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal refunded = refunds.stream().filter(row -> currency.equals(row.get("currency")))
                    .map(row -> (BigDecimal) row.get("refunded")).reduce(BigDecimal.ZERO, BigDecimal::add);
            out.append(vi ? "Tổng chi tiêu " : "Expense total ").append(currency).append(": ").append(money(total)).append(".\n");
            for (var item : items) out.append(item.get("category")).append(": ").append(money((BigDecimal) item.get("spent"))).append(' ').append(currency).append(".\n");
            if (!items.isEmpty()) out.append(vi ? "Danh mục lớn nhất: " : "Largest category: ").append(items.getFirst().get("category")).append(".\n");
            out.append(vi ? "Hoàn tiền ghi nhận riêng: " : "Refunds recorded separately: ").append(money(refunded)).append(' ').append(currency).append(".\n");
        }
        out.append(vi
                ? "Nguồn: giao dịch đã ghi nhận; chi tiêu AUTO/CONFIRMED. Chuyển nội bộ và thu nhập không tính vào chi; hoàn tiền không giảm chi tiêu/ngân sách. Không cộng các tiền tệ.\n"
                : "Evidence: recorded transactions; expenses AUTO/CONFIRMED. Internal transfers and income excluded; refunds do not reduce gross expense/budget spending. Currencies are not added together.\n");
        out.append(vi ? "Giao dịch đang chờ xem xét trong kỳ: " : "Pending reviews in period: ").append(transactions.monthlyPendingReviews())
                .append(vi ? ". Đây chỉ là dữ liệu hiện có; chưa biết chi tiêu tương lai." : ". Recorded data only; future spending is unknown.");
        return out.toString();
    }

    private static String categoryLabel(String category, boolean vi) {
        if (!vi) return category;
        return switch (category) {
            case "Education" -> "Giáo dục";
            case "Food & Drinks" -> "Ăn uống";
            case "Shopping" -> "Mua sắm";
            case "Transport" -> "Di chuyển";
            case "Utilities" -> "Tiện ích";
            default -> category;
        };
    }

    private String budgets(boolean vi) {
        StringBuilder out = new StringBuilder(period(vi)).append(vi ? "Ngân sách danh mục: VND.\n" : "Category budgets: VND.\n");
        var rows = transactions.budgetSummary();
        if (rows.isEmpty()) return out.append(vi
                ? "Chưa thiết lập ngân sách; không thể suy ra số tiền còn lại. Nguồn: cấu hình ngân sách hiện có."
                : "No budgets configured; remaining budget cannot be inferred. Evidence: current budget configuration.").toString();
        BigDecimal limitTotal = BigDecimal.ZERO, spentTotal = BigDecimal.ZERO, remainingTotal = BigDecimal.ZERO;
        for (var row : rows) {
            BigDecimal limit = (BigDecimal) row.get("monthly_limit"), spent = (BigDecimal) row.get("spent"),
                    remaining = (BigDecimal) row.get("remaining"), overspent = spent.subtract(limit).max(BigDecimal.ZERO);
            limitTotal = limitTotal.add(limit); spentTotal = spentTotal.add(spent); remainingTotal = remainingTotal.add(remaining);
            out.append(row.get("category")).append(vi ? ": hạn mức " : ": limit ").append(money(limit))
                    .append(vi ? ", đã chi " : ", spent ").append(money(spent))
                    .append(vi ? ", còn lại " : ", remaining ").append(money(remaining))
                    .append(vi ? ", vượt " : ", overspent ").append(money(overspent)).append(" VND.\n");
        }
        out.append(vi ? "Tổng ngân sách " : "Total configured budget ").append(money(limitTotal))
                .append(vi ? "; đã chi trong danh mục có ngân sách " : "; spending in budgeted categories ").append(money(spentTotal))
                .append(vi ? "; còn lại theo từng danh mục " : "; remaining across categories ").append(money(remainingTotal)).append(" VND.\n");
        var uncovered = transactions.monthlySpending().stream()
                .filter(item -> "VND".equals(item.get("currency")) && rows.stream().noneMatch(b -> b.get("category").equals(item.get("category"))))
                .map(item -> (BigDecimal) item.get("spent")).reduce(BigDecimal.ZERO, BigDecimal::add);
        out.append(vi ? "Chi ngoài danh mục có ngân sách: " : "Expenses outside budgeted categories: ").append(money(uncovered)).append(" VND.\n")
                .append(vi
                        ? "Nguồn: ngân sách cấu hình và giao dịch AUTO/CONFIRMED tháng hiện tại. Còn lại tối thiểu 0 cho từng danh mục; phần vượt hiển thị riêng. Hoàn tiền không giảm chi; giao dịch chờ xem xét chưa tính. Ngân sách không phải số dư tài khoản."
                        : "Evidence: configured budgets and current-month AUTO/CONFIRMED transactions. Remaining is floored at zero per category; overspending shown separately. Refunds do not reduce spending; pending reviews excluded. Budget remaining is not an account balance.");
        return out.toString();
    }

    private String affordability(PhaseFourService payments, boolean vi) {
        return tuitionProjection(payments,null,null,ModelConversationContext.Channel.NONE,vi);
    }
    String tuitionProjection(PhaseFourService payments,Integer billId,String accountId,ModelConversationContext.Channel channel,boolean vi) {
        String heading = period(vi) + (vi ? "ƯỚC TÍNH SAU HỌC PHÍ · VND (không phải thanh toán).\n" : "TUITION PROJECTION · VND (estimate, not a payment).\n")
                + (vi ? "Ước tính từ số dư tài khoản chung và các khoản giữ trước gắn với tài khoản nguồn. Chưa tính thu nhập tương lai hoặc chi phí phát sinh.\n"
                      : "Estimate from the unified account balance and reservations assigned to this source. Future income and unexpected costs are excluded.\n");
        CrossBorderService.StudentExpense expense;
        try { expense = billId==null?crossBorder.selectedExpense():crossBorder.expense(billId); }
        catch (IllegalStateException ex) { return heading + limitation(vi, "Không có hóa đơn đang chọn.", "No selected bill."); }
        if (expense.executed()) return completedAffordability(payments,expense,vi);
        if (!expense.active() || expense.amount().signum() <= 0 || !"TUITION".equals(expense.expenseType()))
            return heading + limitation(vi, "Cần hóa đơn học phí đang hoạt động, chưa thanh toán.", "An active unpaid tuition bill is required.");
        if (!crossBorder.verifyRecipient(expense.id()).verified())
            return heading + limitation(vi, "Người thụ hưởng chưa khớp registry.", "Recipient does not match the registry.");
        var source = payments.paymentSourceAccounts().stream().filter(a->accountId==null?a.selected():a.accountId().equals(accountId)).findFirst().orElse(null);
        if (source == null || !source.ready())
            return heading + limitation(vi, "Chưa có tài khoản nguồn được kết nối, xác minh và cho phép.", "No connected, verified, eligible selected source account.");
        var quote = crossBorder.rankedQuotesForExpense(expense.id()).stream()
                .filter(q -> source.accountId().equals(q.sourceAccountId())&&(channel==ModelConversationContext.Channel.NONE||channel.name().equals(q.channelId()))).findFirst().orElse(null);
        if (quote == null || !quote.eligible())
            return heading + limitation(vi, "Kênh của tài khoản đang chọn không khả dụng hoặc thiếu báo giá.", "Selected source channel is unavailable or has no quote.");
        if (quote.expired())
            return heading + limitation(vi, "Báo giá đã hết hạn; cần làm mới trước khi tính dự kiến.", "Quote expired; refresh before calculating a projection.");
        if (quote.quotedAt().isAfter(java.time.LocalDateTime.now()) || quote.landedCost().signum() <= 0
                || quote.rateVndPerCny().signum() <= 0 || quote.transferFee().signum() < 0 || quote.fxMarkup().signum() < 0)
            return heading + limitation(vi, "Dữ liệu báo giá không hợp lệ.", "Quote data is invalid.");
        BigDecimal after = source.balance().subtract(quote.landedCost());
        BigDecimal buffer = payments.policy().safetyBuffer();
        var commitments = workspace.plans().stream()
                .filter(p -> "ACTIVE".equals(p.get("status")) && Boolean.TRUE.equals(p.get("reserve_funds"))
                        && !"SAVINGS_GOAL".equals(p.get("plan_type")) && "VND".equals(p.get("currency"))
                        && ("PAYER_VND".equals(source.accountId())?"CHECKING":source.accountId()).equals(p.get("funding_account_id")))
                .toList();
        BigDecimal known = commitments.stream().map(p -> (BigDecimal) p.get("projected_30_day_amount"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal afterBuffer = after.subtract(buffer), conservative = afterBuffer.subtract(known);
        StringBuilder out = new StringBuilder(heading)
                .append(vi ? "Tài khoản nguồn " : "Selected source ").append(source.displayName())
                .append(vi ? "; số dư hiện tại " : "; current balance ").append(money(source.balance())).append(" VND.\n")
                .append(vi ? "Hóa đơn đã xác minh " : "Verified bill ").append(expense.paymentReference()).append(": ")
                .append(money(expense.amount())).append(' ').append(expense.currency()).append(".\n")
                .append(vi ? "Kênh " : "Channel ").append(quote.displayName())
                .append(vi ? "; tiền nguồn " : "; source amount ").append(money(quote.sourceAmount()))
                .append(vi ? "; phí " : "; fee ").append(money(quote.transferFee()))
                .append(vi ? "; phụ phí FX " : "; FX markup ").append(money(quote.fxMarkup()))
                .append(vi ? "; tổng chi phí " : "; total cost ").append(money(quote.landedCost())).append(" VND.\n")
                .append(vi ? "Số dư dự kiến sau học phí: " : "Projected balance after tuition: ").append(money(after)).append(" VND.\n")
                .append(vi ? "Đệm an toàn cấu hình: " : "Configured safety buffer: ").append(money(buffer))
                .append(vi ? "; còn sau đệm " : "; after buffer ").append(money(afterBuffer)).append(" VND.\n")
                .append(after.compareTo(buffer) >= 0
                        ? (vi ? "Dự kiến đáp ứng đệm an toàn.\n" : "Projected safety buffer is covered.\n")
                        : (vi ? "Dự kiến không đủ đệm an toàn.\n" : "Projected safety buffer is not covered.\n"))
                .append(vi ? "Khoản sinh hoạt đã giữ trước trong planner 30 ngày: " : "Known reserved planner commitments for 30 days: ")
                .append(money(known)).append(" VND.\n");
        commitments.forEach(p -> out.append(p.get("title")).append(": ").append(money((BigDecimal) p.get("projected_30_day_amount"))).append(" VND.\n"));
        out.append(vi ? "Nếu giữ thêm toàn bộ khoản planner trên: " : "If all these planner commitments are also earmarked: ")
                .append(money(conservative)).append(" VND.\n")
                .append(vi
                        ? "Giả định bảo thủ: chỉ giữ trước các khoản gắn với tài khoản nguồn đã chọn; không cộng số dư. Khoản chưa giữ trước và chi tiêu tương lai chưa biết; không thể bảo đảm đủ sinh hoạt.\n"
                        : "Conservative assumption: only reservations assigned to the selected source are included; each balance is counted once. Unreserved commitments and future spending are unknown; living-cost sufficiency is not guaranteed.\n")
                .append(vi ? "Nguồn: registry hóa đơn; sổ tài khoản chung đang chọn; policy safety buffer; planner; báo giá " : "Evidence: bill registry; selected unified account; policy safety buffer; planner; quote ")
                .append(quote.quoteId()).append(" · ").append(quote.quoteSource()).append(" · ").append(quote.quotedAt())
                .append(vi ? " · hết hạn " : " · expires ").append(quote.expiresAt()).append(".\n")
                .append(vi ? "Chỉ là mô phỏng; chưa tạo kế hoạch, phê duyệt hay biên nhận." : "Synthetic projection only; no plan, approval or receipt created.");
        return out.toString();
    }

    private String completedAffordability(PhaseFourService payments, CrossBorderService.StudentExpense expense, boolean vi) {
        var plan=payments.completedPlansByExpense().get(expense.id());
        return completedTuitionBalance(payments,plan==null?null:plan.id(),vi);
    }

    String completedTuitionBalance(PhaseFourService payments, String planId, boolean vi) {
        var plan=planId==null?null:payments.action(planId);
        var receipt=plan==null||!"COMPLETED".equals(plan.status())?null:payments.receiptForAction(plan.id());
        if(receipt==null) return vi
                ?"Không tìm thấy biên nhận Payment Sandbox cho hóa đơn đã thanh toán này. Không suy đoán số dư và không tạo thanh toán mới."
                :"No Payment Sandbox receipt was found for this paid bill. No balance is inferred and no new payment is created.";
        return new StringBuilder(vi
                ?"THANH TOÁN HỌC PHÍ ĐÃ HOÀN TẤT · VND (Payment Sandbox).\n"
                :"COMPLETED TUITION PAYMENT · VND (Payment Sandbox).\n")
                .append(vi?"Số dư thực tế sau thanh toán: ":"Actual balance immediately after payment: ")
                .append(money(receipt.vndBalanceAfter())).append(" VND.\n")
                .append(vi?"Biên nhận: ":"Receipt: ").append(receipt.transactionId())
                .append(vi?" · Kế hoạch: ":" · Plan: ").append(plan.id()).append(".\n")
                .append(vi?"Thời điểm thanh toán: ":"Payment timestamp: ").append(receipt.createdAt()).append(".\n")
                .append(vi?"Tài khoản nguồn: ":"Source account: ").append(receipt.sourceAccountId()).append(".\n")
                .append(vi?"Tổng tiền đã trừ: ":"Total debited: ").append(money(receipt.vndDebit()))
                .append(vi?" VND; tiền quy đổi: ":" VND; conversion: ").append(money(receipt.conversionVnd()))
                .append(vi?" VND; phí: ":" VND; fees: ").append(money(receipt.feeDeductionVnd())).append(" VND.\n")
                .append(vi?"Người nhận đã ghi có: ":"Recipient credited: ").append(money(receipt.cnyCredit()))
                .append(' ').append(receipt.destinationCurrency()).append(".\n")
                .append(vi
                        ?"Đây là số dư được biên nhận ghi lại ngay sau giao dịch này. Sự kiện ngân hàng hoặc giao dịch phát sinh sau đó có thể làm số dư tài khoản hiện tại khác đi. Không cần báo giá đang hiệu lực và không tạo kế hoạch, phê duyệt hoặc thanh toán mới."
                        :"This is the balance recorded by the receipt immediately after this payment. Later bank events or transactions can make the current account balance different. No current quote is required and no plan, approval, or payment is created.")
                .toString();
    }

    private String limitation(boolean vi, String vietnamese, String english) {
        return (vi ? vietnamese + " Không tính số dư dự kiến, chưa tạo kế hoạch hay thanh toán. Nguồn: dữ liệu hiện có và registry/báo giá mô phỏng."
                : english + " No projected balance calculated and no plan or payment created. Evidence: current data and synthetic registry/quotes.");
    }

    private static String money(BigDecimal amount) { return amount.setScale(2).toPlainString(); }
}
