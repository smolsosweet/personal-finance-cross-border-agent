package com.example.finance;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Read-only catalog for this synthetic profile. Planner accounts are a separate, unmapped source. */
@Service
public class GlobalAssistantQueries {
    private final PhaseFourService payments;
    private final JdbcTemplate db;
    public GlobalAssistantQueries(PhaseFourService payments, JdbcTemplate db) {
        this.payments=payments; this.db=db;
    }
    public List<PhaseFourService.PaymentSourceAccount> accounts() {
        return payments.paymentSourceAccounts().stream()
                .filter(a->"CONNECTED".equals(a.connectionStatus())&&"VERIFIED".equals(a.verificationStatus())).toList();
    }
    static String normalized(String text) {
        return Normalizer.normalize(text==null?"":text,Normalizer.Form.NFD)
                .replaceAll("\\p{M}","").replace('đ','d').replace('Đ','D')
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();
    }
    static boolean contains(String message,String reference) {
        String needle=normalized(reference);
        return !needle.isEmpty() && (" "+normalized(message)+" ").contains(" "+needle+" ");
    }
    public List<PhaseFourService.PaymentSourceAccount> namedAccounts(String message) {
        var exact=accounts().stream().filter(a->contains(message,a.displayName())||contains(message,a.accountId())).toList();
        if(!exact.isEmpty())return exact;
        return accounts().stream().filter(a->contains(message,a.institution())
                || "VCB_VND".equals(a.accountId())&&contains(message,"VCB")
                || "TCB_VND".equals(a.accountId())&&contains(message,"TCB")).toList();
    }
    public List<PhaseFourService.ActionPlan> plans() {
        return db.query("SELECT id FROM action_plans WHERE action_type='TUITION' ORDER BY created_at DESC,id",
                (rs,n)->rs.getString(1)).stream().map(payments::action).toList();
    }
    public String balances(List<PhaseFourService.PaymentSourceAccount> accounts,boolean vi) {
        if(accounts.isEmpty())return vi?"Chưa có tài khoản đã kết nối và xác minh có số dư đáng tin cậy.":"No connected, verified account with an authoritative balance is available.";
        StringBuilder out=new StringBuilder(vi?"SỐ DƯ TÀI KHOẢN HIỆN TẠI\n":"CURRENT ACCOUNT BALANCES\n");
        var totals=new TreeMap<String,BigDecimal>();
        for(var a:accounts) {
            out.append(a.displayName()).append(" · ").append(a.maskedNumber()).append(": ")
                    .append(a.balance().setScale(2).toPlainString()).append(' ').append(a.currency()).append(".\n");
            totals.merge(a.currency(),a.balance(),BigDecimal::add);
        }
        totals.forEach((currency,total)->out.append(vi?"Tổng ":"Total ").append(currency).append(": ")
                .append(total.setScale(2).toPlainString()).append(".\n"));
        return out.append(vi?"Quan sát lúc: ":"Observed at: ").append(LocalDateTime.now()).append(".\n")
                .append(vi
                    ?"Nguồn: tài khoản đã kết nối/xác minh trong payment_source_accounts, số dư mới nhất từ sandbox_accounts. Không cộng tài khoản planner, tài khoản người nhận hoặc tài khoản phí. Không cộng các tiền tệ; không trừ đệm an toàn. Dữ liệu mô phỏng, không phải số dư ngân hàng thật."
                    :"Source: connected/verified payment_source_accounts with latest sandbox_accounts balances. Planner, recipient and fee accounts are excluded. Currencies are not added; safety buffer is not deducted. Synthetic data, not a live bank balance.")
                .toString();
    }
}
