package com.example.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

/** Read-only scenario. Existing services own balances, quotes, fees and verification. */
@Service
public class LivingExpenseRunwayService {
    private final PhaseFourService payments;
    private final CrossBorderService crossBorder;
    public LivingExpenseRunwayService(PhaseFourService payments, CrossBorderService crossBorder) {
        this.payments=payments; this.crossBorder=crossBorder;
    }
    public record Inputs(PhaseFourService.PaymentSourceAccount source, CrossBorderService.StudentExpense bill,
                         CrossBorderService.ChannelQuote quote, BigDecimal buffer, LocalDateTime observedAt) {}
    public record Calculation(BigDecimal projectedBalance, BigDecimal rawFunds, BigDecimal funds,
                              BigDecimal months, BigDecimal tuitionShortfall, BigDecimal safeShortfall) {}
    public static BigDecimal parseMonthly(String amount, String currency) {
        if (!"VND".equals(currency)) throw new IllegalArgumentException("VND_ONLY");
        if (amount==null || amount.length()>15 || !amount.matches("[0-9]{1,12}(\\.[0-9]{1,2})?"))
            throw new IllegalArgumentException("INVALID_MONTHLY_AMOUNT");
        BigDecimal value=new BigDecimal(amount);
        if(value.signum()<=0)throw new IllegalArgumentException("INVALID_MONTHLY_AMOUNT");
        return value.setScale(2);
    }
    public Inputs inspect(Integer billId, String accountId, ModelConversationContext.Channel channel, String planId) {
        if(billId==null)throw new IllegalArgumentException("SELECT_UNPAID_BILL");
        var bill=crossBorder.expense(billId);
        if(bill.executed() || planId!=null && "COMPLETED".equals(payments.action(planId).status()))
            throw new IllegalArgumentException("TUITION_ALREADY_PAID");
        if(!bill.active() || !"TUITION".equals(bill.expenseType()))throw new IllegalArgumentException("SELECT_UNPAID_BILL");
        if(!crossBorder.verifyRecipient(billId).verified())throw new IllegalArgumentException("RECIPIENT_MISMATCH");
        if(accountId==null)throw new IllegalArgumentException("SELECT_SOURCE_ACCOUNT");
        var sources=payments.paymentSourceAccounts().stream().filter(a->accountId.equals(a.accountId())).toList();
        if(sources.size()!=1 || !sources.getFirst().ready())throw new IllegalArgumentException("SELECT_SOURCE_ACCOUNT");
        var source=sources.getFirst();
        var options=crossBorder.rankedQuotesForExpense(billId).stream()
                .filter(q->channel!=ModelConversationContext.Channel.NONE ? q.channelId().equals(channel.name())
                        : accountId.equals(q.sourceAccountId())).toList();
        if(options.size()!=1)throw new IllegalArgumentException("SELECT_ELIGIBLE_CHANNEL");
        var quote=options.getFirst();
        if(!quote.eligible())throw new IllegalArgumentException("CHANNEL_UNAVAILABLE");
        if(!accountId.equals(quote.sourceAccountId()))throw new IllegalArgumentException("CHANNEL_SOURCE_MISMATCH");
        if(planId!=null && !payments.action(planId).quoteId().equals(quote.quoteId()))
            throw new IllegalArgumentException("QUOTE_CHANGED");
        LocalDateTime observed=LocalDateTime.now();
        if(quote.expired() || !quote.expiresAt().isAfter(observed))throw new IllegalArgumentException("FX_QUOTE_EXPIRED");
        BigDecimal buffer=payments.policy().safetyBuffer();
        if(source.balance().signum()<0 || buffer.signum()<0 || quote.landedCost().signum()<=0
                || quote.rateVndPerCny().signum()<=0 || quote.transferFee().signum()<0 || quote.fxMarkup().signum()<0
                || quote.quotedAt().isAfter(observed) || quote.expectedReceived().compareTo(bill.amount())!=0)
            throw new IllegalArgumentException("INVALID_RUNWAY_DATA");
        return new Inputs(source,bill,quote,buffer,observed);
    }
    public static Calculation calculate(BigDecimal balance, BigDecimal tuition, BigDecimal buffer, BigDecimal monthly) {
        if(balance.signum()<0 || tuition.signum()<=0 || buffer.signum()<0 || monthly.signum()<=0)
            throw new IllegalArgumentException("INVALID_RUNWAY_DATA");
        BigDecimal projected=balance.subtract(tuition),raw=projected.subtract(buffer),funds=raw.max(BigDecimal.ZERO);
        return new Calculation(projected,raw,funds,funds.divide(monthly,2,RoundingMode.DOWN),
                tuition.subtract(balance).max(BigDecimal.ZERO),raw.negate().max(BigDecimal.ZERO));
    }
    public String render(Inputs inputs, BigDecimal monthly, boolean vi) {
        var q=inputs.quote(); var c=calculate(inputs.source().balance(),q.landedCost(),inputs.buffer(),monthly);
        return (vi?"ƯỚC TÍNH THỜI GIAN SINH HOẠT · VND · ":"LIVING-EXPENSE RUNWAY ESTIMATE · VND · ")+inputs.observedAt()+"\n"
            +(vi?"Tài khoản nguồn: ":"Source account: ")+inputs.source().displayName()
            +(vi?"; số dư quan sát lúc ":"; balance observed at ")+inputs.observedAt()+": "+money(inputs.source().balance())+" VND.\n"
            +(vi?"Hóa đơn chưa thanh toán: ":"Unpaid bill: ")+inputs.bill().paymentReference()+" · "+money(inputs.bill().amount())+" "+inputs.bill().currency()+".\n"
            +(vi?"Kênh: ":"Channel: ")+q.displayName()+" · "+q.quoteId()+" · "+q.quoteSource()+" · "+q.quotedAt()
            +(vi?" · hết hạn ":" · expires ")+q.expiresAt()+".\n"
            +(vi?"Tổng học phí gồm phí và phụ phí FX: ":"Total tuition debit including fees and FX markup: ")+money(q.landedCost())+" VND.\n"
            +(vi?"Số dư dự kiến sau học phí: ":"Projected balance after tuition: ")+money(c.projectedBalance())+" VND.\n"
            +(vi?"Đệm an toàn: ":"Safety buffer: ")+money(inputs.buffer())+" VND.\n"
            +(vi?"Tiền dành cho sinh hoạt: ":"Available runway funds: ")+money(c.funds())+" VND.\n"
            +(vi?"Chi sinh hoạt mỗi tháng: ":"Monthly living expense: ")+money(monthly)+" VND · "
            +(vi?"giả định kịch bản do người dùng xác nhận.":"user-confirmed scenario assumption.")+"\n"
            +(vi?"Khoảng ":"Approximately ")+c.months().toPlainString()+(vi?" tháng (làm tròn xuống 2 chữ số).":" months (rounded DOWN to 2 decimals).")+"\n"
            +(vi?"Công thức: ":"Formula: ")+"max(0, "+money(inputs.source().balance())+" − "+money(q.landedCost())+" − "+money(inputs.buffer())+") ÷ "+money(monthly)+" = "+c.months()+".\n"
            +(c.safeShortfall().signum()>0 ? (vi?"THIẾU TIỀN: thiếu cho học phí ":"SHORTFALL: tuition shortfall ")+money(c.tuitionShortfall())
                +(vi?" VND; thiếu để đủ học phí và đệm ":" VND; tuition-plus-buffer shortfall ")+money(c.safeShortfall())
                +(vi?" VND. Không đủ tiền an toàn cho kịch bản học phí.":" VND. This scenario cannot safely cover tuition.")+"\n" : "")
            +(vi?"Giả định: chi hàng tháng ổn định, chỉ một tài khoản nguồn; không tính thu nhập tương lai, chi phí phát sinh, tài khoản khác hoặc chuyển đổi tiền tệ. Không trừ planner lần nữa: mức chi đã gồm tiền thuê nhà, ăn uống, đi lại và tiện ích; planner và Sandbox chưa ánh xạ.\n"
                :"Assumptions: stable monthly spending, one source account; future income, unexpected costs, other accounts and currency conversion excluded. Planner is not subtracted again: the baseline includes rent, food, transport and utilities; planner and Sandbox accounts are not mapped.\n")
            +(vi?"Số dư lấy từ Sandbox tại thời điểm quan sát, không phải timestamp đồng bộ ngân hàng. Kết quả là kịch bản trước thanh toán, không phải quyết định quyền thanh toán. Chưa tạo kế hoạch, phê duyệt, thanh toán hay biên nhận."
                :"Balance observed from Sandbox, not a bank synchronization timestamp. This is a before-payment scenario, not a payment-permission decision. No plan, approval, payment or receipt was created.");
    }
    private static String money(BigDecimal amount){return amount.setScale(2).toPlainString();}
    public static String error(String reason, boolean vi) {
        String detail=switch(reason) {
            case "VND_ONLY" -> vi?"Chỉ hỗ trợ chi sinh hoạt VND. Không đổi CNY sang VND bằng báo giá học phí.":"Living expenses support VND only. CNY expenses are not converted using a tuition quote.";
            case "INVALID_MONTHLY_AMOUNT" -> vi?"Nhập số tiền dương bằng chữ số, tối đa 12 chữ số phần nguyên và 2 chữ số thập phân; không dùng dấu phân cách hoặc số mũ.":"Enter a positive decimal amount with up to 12 integer digits and 2 decimal places; no separators or exponents.";
            case "TUITION_ALREADY_PAID" -> vi?"Học phí đã thanh toán. Kịch bản trước thanh toán không còn áp dụng; chọn hóa đơn chưa thanh toán khác. Không trừ học phí lần nữa.":"Tuition is already paid. This before-payment scenario no longer applies; select an unpaid bill. Tuition is not subtracted again.";
            case "RECIPIENT_MISMATCH" -> vi?"Người nhận chưa được xác minh hoặc không khớp registry. Hãy sửa và xác minh hóa đơn.":"Recipient is unverified or mismatches the registry. Correct and verify the bill.";
            case "SELECT_SOURCE_ACCOUNT" -> vi?"Chọn rõ một tài khoản nguồn VND đã kết nối và xác minh trong Tài chính du học.":"Select one connected, verified VND source account in Student finance.";
            case "CHANNEL_UNAVAILABLE","SELECT_ELIGIBLE_CHANNEL","CHANNEL_SOURCE_MISMATCH" -> vi?"Kênh không khả dụng hoặc không khớp tài khoản nguồn. Chọn một kênh đủ điều kiện gắn với tài khoản đang chọn.":"Channel is unavailable or mismatches the source. Select an eligible channel linked to the selected account.";
            case "FX_QUOTE_EXPIRED","QUOTE_CHANGED" -> vi?"Báo giá đã hết hạn hoặc thay đổi. Làm mới/chọn lại bằng luồng có hướng dẫn; không tự làm mới.":"Quote expired or changed. Refresh/reselect through the guided flow; no automatic refresh.";
            case "SELECT_UNPAID_BILL" -> vi?"Chọn rõ hóa đơn học phí đang hoạt động, chưa thanh toán và đã xác minh.":"Select an active, unpaid, verified tuition bill.";
            default -> vi?"Dữ liệu kịch bản không hợp lệ hoặc đã thay đổi. Hãy chọn lại hóa đơn, tài khoản và báo giá.":"Scenario data is invalid or changed. Reselect the bill, account and quote.";
        };
        return detail+(vi?" Chưa tính số tháng, tạo kế hoạch hay thanh toán.":" No months, plan or payment created.");
    }
}
