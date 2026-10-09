package com.example.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceWorkspaceService {
    private static final BigDecimal SAFETY_BUFFER = new BigDecimal("3000000.00");
    private static final BigDecimal WEEKS_PER_MONTH = new BigDecimal("4.345");
    private final JdbcTemplate db;

    public FinanceWorkspaceService(JdbcTemplate db) {
        this.db = db;
    }

    public record FinanceSummary(BigDecimal totalBalance, BigDecimal reservedNext30Days,
                                 BigDecimal availableToAllocate, BigDecimal safetyBuffer,
                                 BigDecimal monthlyBudget, BigDecimal monthlySpent,
                                 BigDecimal weeklyGuide, int activePlanCount) {}

    @Transactional
    public void seedIfEmpty() {
        Integer count = db.queryForObject("SELECT COUNT(*) FROM finance_plans", Integer.class);
        if (count != null && count == 0) seedPlans();
    }

    @Transactional
    public void clear() {
        db.update("DELETE FROM finance_plans");
    }

    private void seedPlans() {
        LocalDate today = LocalDate.now();
        insertPlan("PLAN-RENT", "Monthly rent", "RECURRING_BILL", "Housing",
                new BigDecimal("8000000"), "MONTHLY", today.plusDays(5), "CHECKING", true,
                "Reserved for the next rent payment");
        insertPlan("PLAN-PHONE", "Mobile plan", "RECURRING_BILL", "Utilities",
                new BigDecimal("300000"), "MONTHLY", today.plusDays(10), "CHECKING", true,
                "Expected monthly bill");
        insertPlan("GOAL-EMERGENCY", "Emergency fund target", "SAVINGS_GOAL", "Savings",
                new BigDecimal("15000000"), "ONCE", today.plusMonths(6), "SAVINGS", false,
                "Target only; not deducted from available money");
    }

    private void insertPlan(String id, String title, String type, String category, BigDecimal amount,
                            String cadence, LocalDate dueDate, String accountId, boolean reserve, String notes) {
        db.update("""
                INSERT INTO finance_plans
                (id,title,plan_type,category,amount,currency,cadence,next_due_date,funding_account_id,
                 reserve_funds,status,notes,created_at,updated_at)
                VALUES (?,?,?,?,?,'VND',?,?,?,?, 'ACTIVE',?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                """, id, title, type, category, amount, cadence, dueDate, accountId, reserve, notes);
    }

    public List<Map<String,Object>> accounts() {
        return db.queryForList("""
                SELECT * FROM financial_accounts
                WHERE archived=FALSE AND account_scope='PERSONAL' AND owner_profile_id=1
                ORDER BY CASE source_type WHEN 'CONNECTED' THEN 0 WHEN 'MANUAL' THEN 1 ELSE 2 END,
                         balance DESC,account_name
                """);
    }

    public List<Map<String,Object>> plans() {
        List<Map<String,Object>> rows = db.queryForList("""
                SELECT p.*,a.account_name AS funding_account_name
                FROM finance_plans p
                LEFT JOIN financial_accounts a ON a.id=p.funding_account_id
                ORDER BY CASE p.status WHEN 'ACTIVE' THEN 0 WHEN 'COMPLETED' THEN 1 ELSE 2 END,
                         p.next_due_date,p.created_at DESC
                """);
        LocalDate today = LocalDate.now();
        for (Map<String,Object> row : rows) {
            LocalDate due = ((java.sql.Date) row.get("next_due_date")).toLocalDate();
            String cadence = (String) row.get("cadence");
            BigDecimal amount = (BigDecimal) row.get("amount");
            int occurrences = projectedOccurrences(due, cadence, today, today.plusDays(30));
            row.put("projected_occurrences", occurrences);
            row.put("projected_30_day_amount", amount.multiply(BigDecimal.valueOf(occurrences)));
            row.put("overdue", "ACTIVE".equals(row.get("status")) && due.isBefore(today));
        }
        return rows;
    }

    public FinanceSummary summary() {
        BigDecimal total = scalarMoney("""
                SELECT COALESCE(SUM(balance),0) FROM financial_accounts
                WHERE archived=FALSE AND account_scope='PERSONAL' AND owner_profile_id=1 AND currency='VND'
                """);
        BigDecimal reserved = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();
        for (Map<String,Object> plan : plans()) {
            if (!"ACTIVE".equals(plan.get("status")) || !Boolean.TRUE.equals(plan.get("reserve_funds"))
                    || "SAVINGS_GOAL".equals(plan.get("plan_type"))) continue;
            reserved = reserved.add((BigDecimal) plan.get("projected_30_day_amount"));
        }
        BigDecimal monthlyBudget = scalarMoney("SELECT COALESCE(SUM(monthly_limit),0) FROM budgets");
        LocalDate monthStart = today.withDayOfMonth(1);
        BigDecimal monthlySpent = scalarMoney("""
                SELECT COALESCE(SUM(amount),0) FROM transactions
                WHERE currency='VND' AND type='Expense' AND review_status IN ('AUTO','CONFIRMED')
                  AND occurred_at>=? AND occurred_at<?
                """, monthStart.atStartOfDay(), monthStart.plusMonths(1).atStartOfDay());
        BigDecimal available = total.subtract(reserved).subtract(SAFETY_BUFFER);
        BigDecimal weeklyGuide = monthlyBudget.divide(WEEKS_PER_MONTH, 0, RoundingMode.HALF_UP);
        Integer active = db.queryForObject("SELECT COUNT(*) FROM finance_plans WHERE status='ACTIVE'", Integer.class);
        return new FinanceSummary(total, reserved, available, SAFETY_BUFFER, monthlyBudget,
                monthlySpent, weeklyGuide, active == null ? 0 : active);
    }

    public List<Map<String,Object>> attentionItems(int pendingTransactions) {
        List<Map<String,Object>> items = new ArrayList<>();
        if (pendingTransactions > 0) {
            items.add(attention("MEDIUM", pendingTransactions + " transaction(s) need review",
                    "Confirm the category or add the payment purpose.", "#transaction-review", "Review transactions"));
        }
        LocalDate today = LocalDate.now();
        long overdue = plans().stream().filter(p -> Boolean.TRUE.equals(p.get("overdue"))).count();
        if (overdue > 0) {
            items.add(attention("HIGH", overdue + " planned item(s) are overdue",
                    "Review the date or mark the item complete.", "#planning", "Review plans"));
        }
        BigDecimal reserved = summary().reservedNext30Days();
        if (reserved.signum() > 0) {
            items.add(attention("INFO", "Upcoming money is reserved",
                    money(reserved) + " VND is protected for plans due in the next 30 days.",
                    "#planning", "Open planning"));
        }
        return items;
    }

    private Map<String,Object> attention(String priority, String title, String message, String href, String action) {
        Map<String,Object> item = new LinkedHashMap<>();
        item.put("priority", priority);
        item.put("title", title);
        item.put("message", message);
        item.put("href", href);
        item.put("action", action);
        return NotificationMetadata.attach(item,
                "Upcoming money is reserved".equals(title) ? "UPDATE" : "ACTION",
                "Upcoming money is reserved".equals(title) ? "Information only" : "Needs review",
                db.queryForObject("SELECT MAX(updated_at) FROM finance_plans", LocalDateTime.class));
    }

    @Transactional
    public String addManualAccount(String name, String institution, String accountType,
                                   String maskedNumber, BigDecimal balance) {
        name = required(name, "Account name", 80);
        institution = required(institution, "Institution", 120);
        accountType = choice(accountType, List.of("CHECKING","SAVINGS","CASH","EWALLET"), "Account type");
        positiveOrZero(balance, "Balance");
        String id = "MAN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String sourceType = "CASH".equals(accountType) ? "CASH" : "MANUAL";
        String mask = maskedNumber == null || maskedNumber.isBlank() ? "Manual entry" : maskedNumber.trim();
        if (mask.length() > 20) throw new IllegalArgumentException("Reference must be 20 characters or fewer");
        db.update("""
                INSERT INTO financial_accounts
                (id,owner_profile_id,account_name,currency,balance,institution,account_type,masked_number,
                 source_type,connection_status,balance_updated_at,archived)
                VALUES (?,1,?,'VND',?,?,?,?,?,'MANUAL',CURRENT_TIMESTAMP,FALSE)
                """, id, name, balance.setScale(2), institution, accountType, mask, sourceType);
        return id;
    }

    @Transactional
    public void updateManualAccount(String id, String name, String institution, String accountType,
                                    String maskedNumber, BigDecimal balance) {
        db.queryForList("SELECT id FROM financial_accounts WHERE id=? FOR UPDATE",id);
        Map<String,Object> account = account(id);
        if ("CONNECTED".equals(account.get("source_type"))) {
            throw new IllegalStateException("Connected account balances can only be changed by bank events");
        }
        name = required(name, "Account name", 80);
        institution = required(institution, "Institution", 120);
        accountType = choice(accountType, List.of("CHECKING","SAVINGS","CASH","EWALLET"), "Account type");
        positiveOrZero(balance, "Balance");
        String sourceType = "CASH".equals(accountType) ? "CASH" : "MANUAL";
        String mask = maskedNumber == null || maskedNumber.isBlank() ? "Manual entry" : maskedNumber.trim();
        if (mask.length() > 20) throw new IllegalArgumentException("Reference must be 20 characters or fewer");
        db.update("""
                UPDATE financial_accounts SET account_name=?,institution=?,account_type=?,masked_number=?,
                balance=?,source_type=?,balance_updated_at=CURRENT_TIMESTAMP WHERE id=?
                """, name, institution, accountType, mask, balance.setScale(2), sourceType, id);
    }

    @Transactional
    public void archiveManualAccount(String id) {
        db.queryForList("SELECT id FROM financial_accounts WHERE id=? FOR UPDATE",id);
        Map<String,Object> account = account(id);
        if ("CONNECTED".equals(account.get("source_type"))) {
            throw new IllegalStateException("Connected accounts cannot be archived from this workspace");
        }
        Integer activePlans = db.queryForObject("""
                SELECT COUNT(*) FROM finance_plans
                WHERE funding_account_id=? AND status='ACTIVE'
                """, Integer.class, id);
        if (activePlans != null && activePlans > 0) {
            throw new IllegalStateException("Move or archive active plans before archiving this money source");
        }
        db.update("UPDATE financial_accounts SET archived=TRUE WHERE id=?", id);
    }

    private Map<String,Object> account(String id) {
        List<Map<String,Object>> rows = db.queryForList("SELECT * FROM financial_accounts WHERE id=? AND archived=FALSE AND account_scope='PERSONAL' AND owner_profile_id=1", id);
        if (rows.isEmpty()) throw new IllegalArgumentException("Money source not found");
        return rows.getFirst();
    }

    @Transactional
    public String addPlan(String title, String planType, String category, BigDecimal amount,
                          String cadence, LocalDate nextDueDate, String fundingAccountId,
                          boolean reserveFunds, String notes) {
        String id = "FP-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase(Locale.ROOT);
        savePlan(id, false, title, planType, category, amount, cadence, nextDueDate,
                fundingAccountId, reserveFunds, notes);
        return id;
    }

    @Transactional
    public void updatePlan(String id, String title, String planType, String category, BigDecimal amount,
                           String cadence, LocalDate nextDueDate, String fundingAccountId,
                           boolean reserveFunds, String notes) {
        savePlan(id, true, title, planType, category, amount, cadence, nextDueDate,
                fundingAccountId, reserveFunds, notes);
    }

    private void savePlan(String id, boolean update, String title, String planType, String category,
                          BigDecimal amount, String cadence, LocalDate nextDueDate,
                          String fundingAccountId, boolean reserveFunds, String notes) {
        title = required(title, "Plan name", 120);
        category = required(category, "Category", 80);
        planType = choice(planType, List.of("EXPENSE","RECURRING_BILL","SAVINGS_GOAL"), "Plan type");
        cadence = choice(cadence, List.of("ONCE","WEEKLY","MONTHLY"), "Cadence");
        if (nextDueDate == null) throw new IllegalArgumentException("Next due date is required");
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Amount must be positive");
        String accountId = fundingAccountId == null || fundingAccountId.isBlank() ? null : fundingAccountId;
        if (accountId != null && !"VND".equals(account(accountId).get("currency")))
            throw new IllegalArgumentException("VND plans require a VND funding account");
        String safeNotes = notes == null ? null : notes.trim();
        if (safeNotes != null && safeNotes.length() > 255) throw new IllegalArgumentException("Notes must be 255 characters or fewer");
        if (update) {
            Integer count = db.queryForObject("SELECT COUNT(*) FROM finance_plans WHERE id=? AND status='ACTIVE'", Integer.class, id);
            if (count == null || count == 0) throw new IllegalStateException("Only active plans can be edited");
            db.update("""
                    UPDATE finance_plans SET title=?,plan_type=?,category=?,amount=?,cadence=?,
                    next_due_date=?,funding_account_id=?,reserve_funds=?,notes=?,updated_at=CURRENT_TIMESTAMP
                    WHERE id=?
                    """, title, planType, category, amount.setScale(2), cadence, nextDueDate,
                    accountId, reserveFunds, safeNotes, id);
        } else {
            db.update("""
                    INSERT INTO finance_plans
                    (id,title,plan_type,category,amount,currency,cadence,next_due_date,funding_account_id,
                     reserve_funds,status,notes,created_at,updated_at)
                    VALUES (?,?,?,?,?,'VND',?,?,?,?, 'ACTIVE',?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                    """, id, title, planType, category, amount.setScale(2), cadence, nextDueDate,
                    accountId, reserveFunds, safeNotes);
        }
    }

    @Transactional
    public void completePlan(String id) {
        int updated = db.update("""
                UPDATE finance_plans SET status='COMPLETED',updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND status='ACTIVE'
                """, id);
        if (updated == 0) throw new IllegalStateException("Only active plans can be completed");
    }

    @Transactional
    public void archivePlan(String id) {
        int updated = db.update("""
                UPDATE finance_plans SET status='ARCHIVED',updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND status IN ('ACTIVE','COMPLETED')
                """, id);
        if (updated == 0) throw new IllegalStateException("Plan is already archived or does not exist");
    }

    @Transactional
    public void reopenPlan(String id) {
        int updated = db.update("""
                UPDATE finance_plans SET status='ACTIVE',updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND status='COMPLETED'
                """, id);
        if (updated == 0) throw new IllegalStateException("Only completed plans can be reopened");
    }

    @Transactional
    public void updateBudget(String category, BigDecimal monthlyLimit) {
        category = required(category, "Category", 80);
        if (monthlyLimit == null || monthlyLimit.signum() <= 0) {
            throw new IllegalArgumentException("Monthly limit must be positive");
        }
        int updated = db.update("UPDATE budgets SET monthly_limit=? WHERE category=?",
                monthlyLimit.setScale(2), category);
        if (updated == 0) throw new IllegalArgumentException("Budget category not found");
    }

    public List<String> budgetCategories() {
        return db.queryForList("""
                SELECT c.name
                FROM transaction_categories c
                WHERE c.active=TRUE
                  AND LOWER(c.name) NOT IN ('income','transfer','refund')
                  AND NOT EXISTS (
                    SELECT 1 FROM budgets b WHERE LOWER(b.category)=LOWER(c.name)
                  )
                ORDER BY c.name
                """, String.class);
    }

    @Transactional
    public void addBudget(String category, BigDecimal monthlyLimit) {
        category = required(category, "Category", 80);
        if (monthlyLimit == null || monthlyLimit.signum() <= 0) {
            throw new IllegalArgumentException("Monthly limit must be positive");
        }
        Integer eligible = db.queryForObject("""
                SELECT COUNT(*) FROM transaction_categories
                WHERE active=TRUE AND LOWER(name)=LOWER(?)
                  AND LOWER(name) NOT IN ('income','transfer','refund')
                """, Integer.class, category);
        if (eligible == null || eligible == 0) {
            throw new IllegalArgumentException("Choose an active spending category");
        }
        Integer existing = db.queryForObject(
                "SELECT COUNT(*) FROM budgets WHERE LOWER(category)=LOWER(?)", Integer.class, category);
        if (existing != null && existing > 0) {
            throw new IllegalStateException("This category already has a monthly budget");
        }
        String canonicalCategory = db.queryForObject(
                "SELECT name FROM transaction_categories WHERE LOWER(name)=LOWER(?)", String.class, category);
        db.update("INSERT INTO budgets(category,monthly_limit) VALUES (?,?)",
                canonicalCategory, monthlyLimit.setScale(2));
    }

    private int projectedOccurrences(LocalDate due, String cadence, LocalDate start, LocalDate end) {
        if (due.isAfter(end)) return 0;
        if ("ONCE".equals(cadence)) return 1;
        LocalDate cursor = due;
        while (cursor.isBefore(start)) cursor = "WEEKLY".equals(cadence) ? cursor.plusWeeks(1) : cursor.plusMonths(1);
        int count = 0;
        while (!cursor.isAfter(end)) {
            count++;
            cursor = "WEEKLY".equals(cadence) ? cursor.plusWeeks(1) : cursor.plusMonths(1);
        }
        return count;
    }

    private BigDecimal scalarMoney(String sql, Object... args) {
        BigDecimal value = db.queryForObject(sql, BigDecimal.class, args);
        return value == null ? BigDecimal.ZERO : value.setScale(2);
    }

    private String required(String value, String label, int max) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(label + " is required");
        String clean = value.trim().replaceAll("\\s+", " ");
        if (clean.length() > max) throw new IllegalArgumentException(label + " is too long");
        return clean;
    }

    private String choice(String value, List<String> allowed, String label) {
        String clean = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(clean)) throw new IllegalArgumentException(label + " is invalid");
        return clean;
    }

    private void positiveOrZero(BigDecimal value, String label) {
        if (value == null || value.signum() < 0) throw new IllegalArgumentException(label + " cannot be negative");
    }

    private String money(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
