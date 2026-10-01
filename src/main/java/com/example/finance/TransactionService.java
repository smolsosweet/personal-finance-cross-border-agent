package com.example.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {
    private static final BigDecimal SAFETY_BUFFER = new BigDecimal("3000000.00");
    private final JdbcTemplate db;
    private final CategorizationService categorization;

    public TransactionService(JdbcTemplate db, CategorizationService categorization) {
        this.db = db;
        this.categorization = categorization;
    }

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
        db.update("DELETE FROM transaction_categories");
        db.update("DELETE FROM budgets");
        db.update("DELETE FROM financial_accounts");
        db.update("DELETE FROM demo_profile");
        db.update("INSERT INTO demo_profile VALUES (1,'Minh Nguyen','Vietnam','China','VND','CNY')");
        db.update("""
                INSERT INTO financial_accounts
                (id,owner_profile_id,account_name,currency,balance,institution,account_type,masked_number,source_type,connection_status,balance_updated_at,archived)
                VALUES ('CHECKING',1,'Everyday account','VND',100000000.00,'Demo Bank','CHECKING','•••• 1106','CONNECTED','CONNECTED',CURRENT_TIMESTAMP,FALSE)
                """);
        db.update("""
                INSERT INTO financial_accounts
                (id,owner_profile_id,account_name,currency,balance,institution,account_type,masked_number,source_type,connection_status,balance_updated_at,archived)
                VALUES ('SAVINGS',1,'Emergency fund','VND',1000000.00,'Demo Bank','SAVINGS','•••• 7715','CONNECTED','CONNECTED',CURRENT_TIMESTAMP,FALSE)
                """);
        seedCategories();
        seedBudgets();

        LocalDate today = LocalDate.now();
        for (int i = 0; i < 20; i++) {
            String merchant = switch(i % 5) {
                case 0 -> "Highlands Coffee";
                case 1 -> "Grab";
                case 2 -> "Electricity Provider";
                case 3 -> "Campus Store";
                default -> "Demo Employer";
            };
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
        if (db.queryForObject("SELECT COUNT(*) FROM demo_profile", Integer.class) == 0) {
            reset();
            return;
        }
        seedCategories();
        seedBudgets();
        for (Map<String,Object> row : db.queryForList("SELECT id,merchant,description,amount,currency,direction,type,occurred_at FROM transactions WHERE confidence=0")) {
            Normalized normalized = new Normalized(
                    (String) row.get("merchant"),
                    (String) row.get("description"),
                    (BigDecimal) row.get("amount"),
                    (String) row.get("currency"),
                    (String) row.get("direction"),
                    (String) row.get("type"),
                    ((java.sql.Timestamp) row.get("occurred_at")).toLocalDateTime());
            CategorizationService.Suggestion suggestion = categorization.categorize(normalized);
            db.update("UPDATE transactions SET category=?,confidence=?,review_status=?,categorization_evidence=? WHERE id=?",
                    suggestion.category(), suggestion.confidence(), suggestion.reviewStatus(), suggestion.evidence(), row.get("id"));
        }
    }

    private void seedBudgets() {
        insertBudget("Food & Drinks", "2000000");
        insertBudget("Transport", "1500000");
        insertBudget("Utilities", "1800000");
        insertBudget("Shopping", "1000000");
    }

    private void seedCategories() {
        for (String name : List.of("Food & Drinks", "Transport", "Utilities", "Shopping",
                "Groceries", "Education", "Health", "Housing", "Entertainment",
                "Income", "Transfer", "Refund")) {
            Integer count = db.queryForObject(
                    "SELECT COUNT(*) FROM transaction_categories WHERE LOWER(name)=LOWER(?)",
                    Integer.class, name);
            if (count == null || count == 0) {
                db.update("INSERT INTO transaction_categories VALUES (?, 'SYSTEM', TRUE, ?)",
                        name, LocalDateTime.now());
            }
        }
    }

    private void insertBudget(String category, String limit) {
        BigDecimal amount = new BigDecimal(limit);
        int updated = db.update("UPDATE budgets SET monthly_limit=? WHERE category=?", amount, category);
        if (updated == 0) db.update("INSERT INTO budgets (category,monthly_limit) VALUES (?,?)", category, amount);
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

        CategorizationService.Suggestion suggestion = categorization.categorize(tx);
        String eventId = UUID.randomUUID().toString();
        String transactionId = UUID.randomUUID().toString();
        String source = event.sourceLabel() == null || event.sourceLabel().isBlank() ? "Simulated Bank Event" : event.sourceLabel();

        db.update("INSERT INTO bank_events VALUES (?,?,?,?,?)", eventId, source, event.reference(), LocalDateTime.now(), "CATEGORIZED");
        db.update("""
                INSERT INTO transactions
                (id,event_id,fingerprint,account_id,occurred_at,merchant,description,amount,currency,direction,type,source_label,
                 category,previous_category,confidence,review_status,categorization_evidence)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,NULL,?,?,?)
                """,
                transactionId, eventId, fingerprint, "CHECKING", tx.occurredAt(), tx.merchant(), tx.description(),
                tx.amount(), tx.currency(), tx.direction(), tx.type(), source, suggestion.category(),
                suggestion.confidence(), suggestion.reviewStatus(), suggestion.evidence());

        if ("Simulated Bank Event".equals(source)) {
            BigDecimal delta = tx.direction().equals("IN") ? tx.amount() : tx.amount().negate();
            db.update("UPDATE financial_accounts SET balance=balance+? WHERE id='CHECKING'", delta);
            if (tx.type().equals("Internal Transfer")) {
                db.update("UPDATE financial_accounts SET balance=balance+? WHERE id='SAVINGS'", tx.amount());
            }
        }
        return transactionId;
    }

    /**
     * Records a completed Payment Sandbox debit in the personal transaction history.
     * This is intentionally separate from bank-event ingestion: sandbox balances are
     * maintained by PhaseFourService, while this immutable history row is only a
     * receipt link for the user's ledger.
     */
    @Transactional
    public String recordSandboxPayment(String receiptId, String actionId, String quoteId,
                                       String channelId, String merchant, String description,
                                       BigDecimal amount, String sourceAccountId,
                                       LocalDateTime occurredAt) {
        if (receiptId == null || receiptId.isBlank()) throw new IllegalArgumentException("Receipt ID is required");
        if (actionId == null || actionId.isBlank()) throw new IllegalArgumentException("Action ID is required");
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Payment amount must be positive");
        String existing = db.query("SELECT id FROM transactions WHERE payment_receipt_id=?",
                (rs, n) -> rs.getString(1), receiptId).stream().findFirst().orElse(null);
        if (existing != null) return existing;

        String transactionId = UUID.randomUUID().toString();
        String eventId = "PAY-EVT-" + receiptId.substring(0, Math.min(31, receiptId.length()));
        String rawReference = "SANDBOX-" + receiptId;
        String fingerprint = sha256("PAYMENT_SANDBOX|" + receiptId);
        String cleanMerchant = merchant == null || merchant.isBlank() ? "Cross-border payment" : merchant.trim();
        String cleanDescription = description == null || description.isBlank()
                ? "Payment Sandbox completed tuition payment" : description.trim();
        String account = sourceAccountId == null || sourceAccountId.isBlank() ? "PAYER_VND" : sourceAccountId;
        LocalDateTime timestamp = occurredAt == null ? LocalDateTime.now() : occurredAt;

        db.update("INSERT INTO bank_events VALUES (?,?,?,?,?)", eventId, "Payment Sandbox", rawReference,
                LocalDateTime.now(), "COMPLETED");
        db.update("""
                INSERT INTO transactions
                (id,event_id,fingerprint,account_id,occurred_at,merchant,description,amount,currency,direction,type,source_label,
                 category,previous_category,confidence,review_status,categorization_evidence,category_source,reviewed_at,
                 payment_action_id,payment_receipt_id,payment_quote_id,payment_channel_id)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,NULL,?,'AUTO',?,?,?,?,?,?,?)
                """,
                transactionId, eventId, fingerprint, account, timestamp, cleanMerchant, cleanDescription,
                amount.setScale(2), "VND", "OUT", "Expense", "Payment Sandbox", "Education", 99,
                "Payment Sandbox receipt " + receiptId + " · action " + actionId,
                "RULE", timestamp, actionId, receiptId, quoteId, channelId);
        return transactionId;
    }

    @Transactional
    public String simulate(String scenario) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime latest = db.queryForObject("SELECT MAX(occurred_at) FROM transactions", LocalDateTime.class);
        if (latest != null && !now.isAfter(latest)) now = latest.plusNanos(1_000);
        String reference = "SIM-" + UUID.randomUUID();
        BankEvent event = switch(scenario) {
            case "medium" -> new BankEvent(reference,now,"Campus Store","Card purchase",new BigDecimal("120000"),"OUT","CHECKING",null,"Simulated Bank Event");
            case "low" -> new BankEvent(reference,now,"Unknown QR Merchant","QR payment",new BigDecimal("64000"),"OUT","CHECKING",null,"Simulated Bank Event");
            case "income" -> new BankEvent(reference,now,"Demo Employer","Part-time salary",new BigDecimal("900000"),"IN","CHECKING",null,"Simulated Bank Event");
            case "transfer" -> new BankEvent(reference,now,"Own Account","Transfer to emergency fund",new BigDecimal("200000"),"OUT","CHECKING","SAVINGS","Simulated Bank Event");
            case "refund" -> new BankEvent(reference,now,"Highlands Coffee","Refund for purchase",new BigDecimal("85000"),"IN","CHECKING",null,"Simulated Bank Event");
            default -> new BankEvent(reference,now,"Highlands Coffee","Card purchase",new BigDecimal("85000"),"OUT","CHECKING",null,"Simulated Bank Event");
        };
        return ingest(event);
    }

    @Transactional
    public void confirmCategory(String id, String category) {
        String status = (String) transaction(id).get("review_status");
        if (!categoryAvailable(category)) category = addCustomCategory(category);
        reviewTransaction(id, category, "PURPOSE_REQUIRED".equals(status) ? category : null);
    }

    @Transactional
    public void reviewTransaction(String id, String category, String customCategory, String purpose) {
        String selected = category;
        if (customCategory != null && !customCategory.isBlank()) {
            selected = addCustomCategory(customCategory);
        }
        reviewTransaction(id, selected, purpose);
    }

    @Transactional
    public void reviewTransaction(String id, String category, String purpose) {
        Map<String,Object> transaction = transaction(id);
        String status = (String) transaction.get("review_status");
        if (!List.of("CONFIRMATION_REQUIRED", "PURPOSE_REQUIRED").contains(status))
            throw new IllegalArgumentException("Transaction does not need review");
        String cleaned = canonicalActiveCategory(category);
        String cleanedPurpose = purpose == null ? null : purpose.trim().replaceAll("\\s+", " ");
        if ("PURPOSE_REQUIRED".equals(status) && (cleanedPurpose == null || cleanedPurpose.isBlank()))
            throw new IllegalArgumentException("Transaction purpose is required");
        if (cleanedPurpose != null && cleanedPurpose.length() > 160)
            throw new IllegalArgumentException("Purpose must be 160 characters or fewer");
        db.update("""
                UPDATE transactions
                SET previous_category=category, category=?, review_status='CONFIRMED',
                    purpose=?, category_source='USER', reviewed_at=?,
                    categorization_evidence='User reviewed bank transaction'
                WHERE id=?
                """, cleaned, cleanedPurpose, LocalDateTime.now(), id);
    }

    @Transactional
    public String addCustomCategory(String name) {
        String cleaned = cleanCategory(name);
        List<Map<String,Object>> existing = db.queryForList(
                "SELECT name,category_type,active FROM transaction_categories WHERE LOWER(name)=LOWER(?)",
                cleaned);
        if (!existing.isEmpty()) {
            Map<String,Object> category = existing.getFirst();
            if ("SYSTEM".equals(category.get("category_type")))
                throw new IllegalArgumentException("A system category already uses this name");
            db.update("UPDATE transaction_categories SET active=TRUE WHERE name=?", category.get("name"));
            return (String) category.get("name");
        }
        db.update("INSERT INTO transaction_categories VALUES (?, 'CUSTOM', TRUE, ?)",
                cleaned, LocalDateTime.now());
        return cleaned;
    }

    @Transactional
    public void archiveCustomCategory(String name) {
        Map<String,Object> category = findCategory(name);
        if (!"CUSTOM".equals(category.get("category_type")))
            throw new IllegalArgumentException("System categories cannot be archived");
        db.update("UPDATE transaction_categories SET active=FALSE WHERE name=?", category.get("name"));
    }

    @Transactional
    public void restoreCustomCategory(String name) {
        Map<String,Object> category = findCategory(name);
        if (!"CUSTOM".equals(category.get("category_type")))
            throw new IllegalArgumentException("Only custom categories can be restored");
        db.update("UPDATE transaction_categories SET active=TRUE WHERE name=?", category.get("name"));
    }

    @Transactional
    public void renameCustomCategory(String name, String newName) {
        Map<String,Object> category = findCategory(name);
        if (!"CUSTOM".equals(category.get("category_type")))
            throw new IllegalArgumentException("System categories cannot be renamed");

        String currentName = (String) category.get("name");
        String cleanedNewName = cleanCategory(newName);
        List<Map<String,Object>> matching = db.queryForList(
                "SELECT name FROM transaction_categories WHERE LOWER(name)=LOWER(?)", cleanedNewName);
        if (!matching.isEmpty() && !currentName.equals(matching.getFirst().get("name")))
            throw new IllegalArgumentException("Another category already uses this name");

        db.update("UPDATE transactions SET category=? WHERE LOWER(category)=LOWER(?)", cleanedNewName, currentName);
        db.update("UPDATE transactions SET previous_category=? WHERE LOWER(previous_category)=LOWER(?)",
                cleanedNewName, currentName);
        db.update("UPDATE budgets SET category=? WHERE LOWER(category)=LOWER(?)", cleanedNewName, currentName);
        db.update("UPDATE transaction_categories SET name=? WHERE name=?", cleanedNewName, currentName);
    }

    private Map<String,Object> findCategory(String name) {
        String cleaned = cleanCategory(name);
        List<Map<String,Object>> categories = db.queryForList(
                "SELECT * FROM transaction_categories WHERE LOWER(name)=LOWER(?)", cleaned);
        if (categories.isEmpty()) throw new IllegalArgumentException("Category does not exist");
        return categories.getFirst();
    }

    private boolean categoryAvailable(String name) {
        Integer count = db.queryForObject(
                "SELECT COUNT(*) FROM transaction_categories WHERE LOWER(name)=LOWER(?) AND active=TRUE",
                Integer.class, name);
        return count != null && count > 0;
    }

    private String canonicalActiveCategory(String name) {
        String cleaned = cleanCategory(name);
        List<String> categories = db.query(
                "SELECT name FROM transaction_categories WHERE LOWER(name)=LOWER(?) AND active=TRUE",
                (rs,n) -> rs.getString(1), cleaned);
        if (categories.isEmpty()) throw new IllegalArgumentException("Choose an active category");
        return categories.getFirst();
    }

    private String cleanCategory(String category) {
        if (category == null || category.isBlank())
            throw new IllegalArgumentException("Category is required");
        String cleaned = category.trim().replaceAll("\\s+", " ");
        if (cleaned.length() < 2 || cleaned.length() > 80)
            throw new IllegalArgumentException("Category must be between 2 and 80 characters");
        return cleaned;
    }

    @Transactional
    public void undoCategory(String id) {
        Map<String,Object> transaction = transaction(id);
        String current = (String) transaction.get("category");
        if (current == null) throw new IllegalArgumentException("There is no category to undo");
        String previous = (String) transaction.get("previous_category");
        String status = previous == null ? "PURPOSE_REQUIRED" : "CONFIRMATION_REQUIRED";
        db.update("""
                UPDATE transactions
                SET category=?, previous_category=NULL, review_status=?,
                    purpose=NULL, category_source='RULE', reviewed_at=NULL,
                    categorization_evidence='Category change undone by user'
                WHERE id=?
                """, previous, status, id);
    }

    public Map<String,Object> transaction(String id) {
        return db.queryForMap("SELECT * FROM transactions WHERE id=?", id);
    }

    public Map<String,Object> profile() { return db.queryForMap("SELECT * FROM demo_profile WHERE id=1"); }
    public List<Map<String,Object>> accounts() { return db.queryForList("SELECT * FROM financial_accounts WHERE archived=FALSE ORDER BY source_type,account_name"); }
    public List<Map<String,Object>> transactions() {
        return db.queryForList("""
                SELECT t.*
                FROM transactions t
                JOIN bank_events e ON e.id=t.event_id
                ORDER BY t.occurred_at DESC, e.received_at DESC
                """);
    }
    public List<Map<String,Object>> pendingTransactions() {
        return db.queryForList("""
                SELECT t.*
                FROM transactions t
                JOIN bank_events e ON e.id=t.event_id
                WHERE t.review_status IN ('CONFIRMATION_REQUIRED','PURPOSE_REQUIRED')
                ORDER BY t.occurred_at DESC, e.received_at DESC
                """);
    }
    public List<Map<String,Object>> categories() {
        return db.queryForList("""
                SELECT c.name,c.category_type,c.active,c.created_at,COUNT(t.id) AS transaction_count
                FROM transaction_categories c
                LEFT JOIN transactions t ON LOWER(t.category)=LOWER(c.name)
                GROUP BY c.name,c.category_type,c.active,c.created_at
                ORDER BY CASE c.category_type WHEN 'SYSTEM' THEN 0 ELSE 1 END,c.active DESC,c.name
                """);
    }
    public int eventCount() { return db.queryForObject("SELECT COUNT(*) FROM bank_events", Integer.class); }

    public Map<String,Object> dashboard() {
        Map<String,Object> result = new LinkedHashMap<>();
        BigDecimal balance = db.queryForObject("SELECT balance FROM financial_accounts WHERE id='CHECKING'", BigDecimal.class);
        BigDecimal income = totalForType("Income");
        BigDecimal expenses = totalForType("Expense");
        BigDecimal refunds = totalForType("Refund");
        result.put("balance", balance);
        result.put("income", income);
        result.put("expenses", expenses);
        result.put("refunds", refunds);
        result.put("netCashFlow", income.add(refunds).subtract(expenses));
        result.put("pendingReview", db.queryForObject("SELECT COUNT(*) FROM transactions WHERE review_status IN ('CONFIRMATION_REQUIRED','PURPOSE_REQUIRED')", Integer.class));
        result.put("safetyBuffer", SAFETY_BUFFER);
        result.put("surplus", balance.subtract(SAFETY_BUFFER).max(BigDecimal.ZERO));
        return result;
    }

    private BigDecimal totalForType(String type) {
        BigDecimal total = db.queryForObject("SELECT COALESCE(SUM(amount),0) FROM transactions WHERE type=?", BigDecimal.class, type);
        return total == null ? BigDecimal.ZERO : total;
    }

    public List<Map<String,Object>> budgetSummary() {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate nextMonth = monthStart.plusMonths(1);
        List<Map<String,Object>> rows = db.queryForList("""
                SELECT b.category, b.monthly_limit,
                       COALESCE(SUM(CASE WHEN t.type='Expense' AND t.review_status IN ('AUTO','CONFIRMED') THEN t.amount ELSE 0 END),0) AS spent
                FROM budgets b
                LEFT JOIN transactions t ON t.category=b.category
                  AND t.occurred_at>=? AND t.occurred_at<?
                GROUP BY b.category,b.monthly_limit
                ORDER BY b.category
                """, monthStart.atStartOfDay(), nextMonth.atStartOfDay());
        List<Map<String,Object>> result = new ArrayList<>();
        for (Map<String,Object> row : rows) {
            BigDecimal limit = (BigDecimal) row.get("monthly_limit");
            BigDecimal spent = (BigDecimal) row.get("spent");
            Map<String,Object> item = new LinkedHashMap<>(row);
            item.put("remaining", limit.subtract(spent).max(BigDecimal.ZERO));
            item.put("percent", spent.multiply(new BigDecimal("100")).divide(limit, 0, RoundingMode.HALF_UP).min(new BigDecimal("100")));
            result.add(item);
        }
        return result;
    }

    public List<Map<String,Object>> proactiveFeed() {
        List<Map<String,Object>> insights = new ArrayList<>();
        Map<String,Object> dashboard = dashboard();
        int pending = (Integer) dashboard.get("pendingReview");
        if (pending > 0) {
            insights.add(insight("HIGH", "Transactions need your input",
                    pending + " transaction(s) need category confirmation or a purpose.",
                    "Confidence rules: medium and low confidence"));
        }

        Map<String,Object> highest = budgetSummary().stream()
                .max((a,b) -> ((BigDecimal)a.get("percent")).compareTo((BigDecimal)b.get("percent")))
                .orElse(null);
        if (highest != null) {
            insights.add(insight("MEDIUM", "Budget progress",
                    highest.get("category") + " has used " + highest.get("percent") + "% of its synthetic monthly budget.",
                    "Confirmed and auto-categorized expenses"));
        }

        BigDecimal surplus = (BigDecimal) dashboard.get("surplus");
        insights.add(insight("OPPORTUNITY", "Safety buffer protected",
                surplus.setScale(0, RoundingMode.HALF_UP) + " VND remains above the 3,000,000 VND safety buffer.",
                "Demo checking balance minus configured buffer"));
        return insights;
    }

    private static Map<String,Object> insight(String priority, String title, String message, String evidence) {
        Map<String,Object> item = new LinkedHashMap<>();
        item.put("priority", priority);
        item.put("title", title);
        item.put("message", message);
        item.put("evidence", evidence);
        return item;
    }

    private static String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
