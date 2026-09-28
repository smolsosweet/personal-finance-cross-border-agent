package com.example.finance;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {
    private final JdbcTemplate db;
    public TransactionService(JdbcTemplate db) { this.db = db; }

    public record BankEvent(String reference, LocalDateTime occurredAt, String merchant,
                            String description, BigDecimal amount, String direction,
                            String sourceAccount, String destinationAccount, String sourceLabel) {}
    public record Normalized(String merchant, String description, BigDecimal amount,
                             String currency, String direction, String type, LocalDateTime occurredAt) {}

    public static Normalized normalize(BankEvent event) {
        if (event.amount() == null || event.amount().signum() <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (event.occurredAt() == null) throw new IllegalArgumentException("Event timestamp is required");
        String direction = event.direction() == null ? "" : event.direction().trim().toUpperCase();
        if (!List.of("IN", "OUT").contains(direction)) throw new IllegalArgumentException("Direction must be IN or OUT");
        String merchant = event.merchant() == null || event.merchant().isBlank() ? "Unknown merchant" : event.merchant().trim().replaceAll("\\s+", " ");
        String description = event.description() == null ? "" : event.description().trim();
        String lower = description.toLowerCase();
        String type;
        if (event.sourceAccount() != null && event.destinationAccount() != null &&
                List.of("CHECKING", "SAVINGS").contains(event.sourceAccount()) &&
                List.of("CHECKING", "SAVINGS").contains(event.destinationAccount()) &&
                !event.sourceAccount().equals(event.destinationAccount())) type = "Internal Transfer";
        else if (lower.contains("refund") || lower.contains("hoàn tiền")) type = "Refund";
        else type = direction.equals("IN") ? "Income" : "Expense";
        return new Normalized(merchant, description, event.amount().setScale(2), "VND", direction, type, event.occurredAt());
    }

    @Transactional
    public void reset() {
        db.update("DELETE FROM transactions");
        db.update("DELETE FROM bank_events");
        db.update("DELETE FROM financial_accounts");
        db.update("DELETE FROM demo_profile");
        db.update("INSERT INTO demo_profile VALUES (1,'Minh Nguyen','Vietnam','China','VND','CNY')");
        db.update("INSERT INTO financial_accounts VALUES ('CHECKING',1,'Demo Checking','VND',100000000.00)");
        db.update("INSERT INTO financial_accounts VALUES ('SAVINGS',1,'Emergency Fund','VND',1000000.00)");
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 20; i++) {
            String merchant = switch(i % 5) { case 0 -> "Highlands Coffee"; case 1 -> "Grab"; case 2 -> "Electricity Provider"; case 3 -> "Campus Store"; default -> "Demo Employer"; };
            String direction = i % 5 == 4 ? "IN" : "OUT";
            BigDecimal amount = i % 5 == 4 ? new BigDecimal("5000000") : new BigDecimal(70000 + (i % 4) * 20000);
            ingest(new BankEvent("SEED-" + i, today.minusDays(20 - i).atTime(12,i), merchant,
                    "Synthetic seed transaction", amount, direction, "CHECKING", null, "Synthetic Data"));
        }
        ingest(new BankEvent("SEED-TRANSFER", today.minusDays(2).atTime(9,0), "Own Account", "Move to savings", new BigDecimal("300000"), "OUT", "CHECKING", "SAVINGS", "Synthetic Data"));
        ingest(new BankEvent("SEED-REFUND", today.minusDays(1).atTime(10,0), "Highlands Coffee", "Refund for purchase", new BigDecimal("85000"), "IN", "CHECKING", null, "Synthetic Data"));
    }

    @Transactional
    public void seedIfEmpty() {
        if (db.queryForObject("SELECT COUNT(*) FROM demo_profile", Integer.class) == 0) reset();
    }

    @Transactional
    public String ingest(BankEvent event) {
        Normalized tx = normalize(event);
        if (event.reference() == null || event.reference().isBlank()) throw new IllegalArgumentException("Event reference is required");
        List<String> byReference = db.query("SELECT id FROM bank_events WHERE raw_reference=?", (rs,n) -> rs.getString(1), event.reference());
        if (!byReference.isEmpty()) return db.queryForObject("SELECT id FROM transactions WHERE event_id=?", String.class, byReference.getFirst());
        String fingerprint = sha256("CHECKING|" + tx.direction() + "|" + tx.amount() + "|" + tx.merchant().toLowerCase() + "|" + tx.occurredAt().truncatedTo(ChronoUnit.MINUTES));
        List<String> duplicate = db.query("SELECT id FROM transactions WHERE fingerprint=?", (rs,n) -> rs.getString(1), fingerprint);
        if (!duplicate.isEmpty()) return duplicate.getFirst();
        String eventId = UUID.randomUUID().toString(); String transactionId = UUID.randomUUID().toString();
        String source = event.sourceLabel() == null || event.sourceLabel().isBlank() ? "Simulated Bank Event" : event.sourceLabel();
        db.update("INSERT INTO bank_events VALUES (?,?,?,?,?)", eventId, source, event.reference(), LocalDateTime.now(), "NORMALIZED");
        db.update("INSERT INTO transactions VALUES (?,?,?,?,?,?,?,?,?,?,?,?)", transactionId, eventId, fingerprint, "CHECKING", tx.occurredAt(), tx.merchant(), tx.description(), tx.amount(), tx.currency(), tx.direction(), tx.type(), source);
        if ("Simulated Bank Event".equals(source)) {
            BigDecimal delta = tx.direction().equals("IN") ? tx.amount() : tx.amount().negate();
            db.update("UPDATE financial_accounts SET balance=balance+? WHERE id='CHECKING'", delta);
            if (tx.type().equals("Internal Transfer")) db.update("UPDATE financial_accounts SET balance=balance+? WHERE id='SAVINGS'", tx.amount());
        }
        return transactionId;
    }

    @Transactional
    public String simulate(String scenario) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        String reference = "SIM-" + UUID.randomUUID();
        BankEvent event = switch(scenario) {
            case "income" -> new BankEvent(reference,now,"Demo Employer","Part-time salary",new BigDecimal("900000"),"IN","CHECKING",null,"Simulated Bank Event");
            case "transfer" -> new BankEvent(reference,now,"Own Account","Transfer to emergency fund",new BigDecimal("200000"),"OUT","CHECKING","SAVINGS","Simulated Bank Event");
            case "refund" -> new BankEvent(reference,now,"Highlands Coffee","Refund for purchase",new BigDecimal("85000"),"IN","CHECKING",null,"Simulated Bank Event");
            default -> new BankEvent(reference,now,"Highlands Coffee","Card purchase",new BigDecimal("85000"),"OUT","CHECKING",null,"Simulated Bank Event");
        };
        return ingest(event);
    }

    public Map<String,Object> profile() { return db.queryForMap("SELECT * FROM demo_profile WHERE id=1"); }
    public List<Map<String,Object>> accounts() { return db.queryForList("SELECT * FROM financial_accounts ORDER BY id"); }
    public List<Map<String,Object>> transactions() { return db.queryForList("SELECT * FROM transactions ORDER BY occurred_at DESC"); }
    public int eventCount() { return db.queryForObject("SELECT COUNT(*) FROM bank_events", Integer.class); }

    private static String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
