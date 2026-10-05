package com.example.finance;

import java.math.BigDecimal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** One current-balance ledger. Payment IDs are stable aliases, never a second balance. */
@Service
public class AccountLedgerService {
    private final JdbcTemplate db;
    public AccountLedgerService(JdbcTemplate db) { this.db=db; }

    public String canonicalId(String id) {
        return "PAYER_VND".equals(id) ? "CHECKING" : "EMERGENCY_VND".equals(id) ? "SAVINGS" : id;
    }

    /** Called once at startup, before reading/seeding payment data. No receipt is rewritten. */
    public void migrateLegacyBalances() {
        db.update("UPDATE payment_source_accounts SET financial_account_id=CASE WHEN account_id='PAYER_VND' THEN 'CHECKING' ELSE account_id END WHERE financial_account_id IS NULL");
        String type=db.queryForObject("SELECT table_type FROM information_schema.tables WHERE LOWER(table_name)='sandbox_accounts'",String.class);
        if (!"BASE TABLE".equals(type)) return;
        for (var row:db.queryForList("SELECT * FROM sandbox_accounts")) {
            String id=(String)row.get("id"), canonical=canonicalId(id);
            BigDecimal balance=(BigDecimal)row.get("balance");
            var metadata=db.queryForList("SELECT * FROM payment_source_accounts WHERE account_id=?",id);
            boolean personal=!metadata.isEmpty();
            var existing=db.queryForList("SELECT balance,currency FROM financial_accounts WHERE id=?",canonical);
            if (!existing.isEmpty() && !row.get("currency").equals(existing.getFirst().get("currency")))
                throw new IllegalStateException("Legacy account currency mismatch: "+id);
            if ("PAYER_VND".equals(id) && !existing.isEmpty()) {
                // Legacy CHECKING held the seeded 100m plus personal bank-event changes.
                // Sandbox held the same seeded 100m minus payments. Combine changes, not seeds.
                balance=balance.add(((BigDecimal)existing.getFirst().get("balance"))
                        .subtract(new BigDecimal("100000000.00")));
            } else if ("EMERGENCY_VND".equals(id) && !existing.isEmpty()) {
                db.update("UPDATE financial_accounts SET balance=balance+? WHERE id='SAVINGS'",balance);
                continue;
            } else if (!existing.isEmpty()) {
                throw new IllegalStateException("Conflicting legacy account ID: "+id+"; reconcile before startup");
            }
            if (personal) {
                var m=metadata.getFirst();
                seed(canonical,(String)row.get("display_name"),(String)row.get("currency"),balance,
                        (String)m.get("institution"),accountType((String)m.get("account_type")),
                        (String)m.get("masked_number"),"PERSONAL");
                db.update("UPDATE financial_accounts SET connection_status=? WHERE id=?",m.get("connection_status"),canonical);
            } else seed(canonical,(String)row.get("display_name"),(String)row.get("currency"),balance,
                    "Sandbox recipient","RECIPIENT","—","SYSTEM");
        }
        db.execute("DROP TABLE sandbox_accounts");
        // Read-only compatibility projection for historic IDs and existing inspection tools.
        // All writes go through financial_accounts; this view owns no money or mutable data.
        db.execute("""
                CREATE VIEW sandbox_accounts AS
                SELECT CASE WHEN a.id='SAVINGS' THEN 'EMERGENCY_VND' ELSE COALESCE(p.account_id,a.id) END AS id,a.account_name AS display_name,a.currency,a.balance
                FROM financial_accounts a LEFT JOIN payment_source_accounts p ON p.financial_account_id=a.id
                """);
    }

    static String accountType(String type) {
        if (type.toLowerCase().contains("wallet")) return "EWALLET";
        if (type.toLowerCase().contains("saving")) return "SAVINGS";
        return "CHECKING";
    }

    public void seed(String id,String name,String currency,BigDecimal balance,String institution,
                     String type,String masked,String scope) {
        db.update("""
                MERGE INTO financial_accounts
                (id,owner_profile_id,account_name,currency,balance,institution,account_type,masked_number,
                 source_type,connection_status,balance_updated_at,archived,account_scope)
                KEY(id) VALUES (?,1,?,?,?,?,?,?,'CONNECTED','CONNECTED',CURRENT_TIMESTAMP,FALSE,?)
                """,id,name,currency,balance,institution,type,masked,scope);
    }

    public BigDecimal balance(String id) {
        return db.queryForObject("SELECT balance FROM financial_accounts WHERE id=? AND archived=FALSE",
                BigDecimal.class,canonicalId(id));
    }

    public void lock(String... ids) {
        java.util.Arrays.stream(ids).map(this::canonicalId).distinct().sorted().forEach(id -> {
            var rows=db.queryForList("SELECT id FROM financial_accounts WHERE id=? AND archived=FALSE FOR UPDATE",id);
            if(rows.isEmpty()) throw new IllegalArgumentException("Account is unavailable: "+id);
        });
    }

    public void setBalance(String id,BigDecimal amount) {
        int count=db.update("UPDATE financial_accounts SET balance=?,balance_updated_at=CURRENT_TIMESTAMP WHERE id=? AND archived=FALSE",amount,canonicalId(id));
        if(count!=1)throw new IllegalStateException("Account is unavailable: "+id);
    }

    public boolean personalVnd(String id) {
        return db.queryForObject("SELECT COUNT(*) FROM financial_accounts WHERE id=? AND owner_profile_id=1 AND account_scope='PERSONAL' AND archived=FALSE AND currency='VND'",Integer.class,canonicalId(id))==1;
    }
}
