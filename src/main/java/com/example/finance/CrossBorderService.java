package com.example.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrossBorderService {
    public static final BigDecimal TUITION_AMOUNT = new BigDecimal("20000.00");
    public static final String SCHOOL_NAME = "Shenzhen Demo University";
    public static final String SCHOOL_RECIPIENT_NAME = "Shenzhen Demo University Tuition Office";
    public static final String SCHOOL_RECIPIENT_BANK = "Shenzhen Demo Education Bank";
    public static final String SCHOOL_RECIPIENT_BANK_CODE = "SZDUCNBSXXX";
    public static final String SCHOOL_RECIPIENT = "SZDU-TUITION-2026";
    public static final String US_SCHOOL_NAME = "Pacific Demo College";
    public static final String US_SCHOOL_RECIPIENT_NAME = "Pacific Demo College Bursar";
    public static final String US_SCHOOL_RECIPIENT_BANK = "Pacific Demo Bank";
    public static final String US_SCHOOL_RECIPIENT_BANK_CODE = "PDCMUS33XXX";
    public static final String US_SCHOOL_RECIPIENT = "PDC-TUITION-USD";
    public static final String AU_SCHOOL_NAME = "Sydney Demo Institute";
    public static final String AU_SCHOOL_RECIPIENT_NAME = "Sydney Demo Institute Fees Office";
    public static final String AU_SCHOOL_RECIPIENT_BANK = "Sydney Demo Bank";
    public static final String AU_SCHOOL_RECIPIENT_BANK_CODE = "SDIIAU2SXXX";
    public static final String AU_SCHOOL_RECIPIENT = "SDI-TUITION-AUD";
    public static final int QUOTE_VALIDITY_MINUTES = 5;
    public static final int SETTLEMENT_SAFETY_MARGIN_DAYS = 1;

    private final JdbcTemplate db;

    public CrossBorderService(JdbcTemplate db) {
        this.db = db;
    }

    public record StudentProfile(String name, String sourceCountry, String destinationCountry,
                                 String sourceCurrency, String destinationCurrency, String preference) {}

    public record TuitionBill(int id, String institution, BigDecimal amount, String currency,
                              String destinationCountry, String recipientName,
                              String recipientBankName, String recipientBankCode,
                              String recipientAccount, String paymentReference, LocalDate dueDate,
                              String evidenceLabel) {}

    public record StudentExpense(int id, String expenseType, String title, String institution,
                                 BigDecimal amount, String currency, String destinationCountry,
                                 String recipientName, String recipientBankName, String recipientBankCode,
                                 String recipientAccount, String verificationStatus,
                                 String paymentReference, LocalDate dueDate, String evidenceLabel,
                                 String documentName, String documentContentType, Long documentSize,
                                 boolean selected, String lifecycleStatus, boolean executed,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        public boolean active() { return "ACTIVE".equals(lifecycleStatus); }
        public boolean editable() { return active() && !executed; }
    }

    public record RecipientVerification(boolean verified, String status, String reason,
                                        String registryInstitution, String registryAccount,
                                        String registryBankName, String registryBankCode) {}

    public record ChannelQuote(
            String channelId,
            String displayName,
            String sourceAccountId,
            boolean eligible,
            String eligibilityReason,
            Integer rank,
            BigDecimal rateVndPerCny,
            String quoteId,
            String quoteSource,
            LocalDateTime quotedAt,
            LocalDateTime expiresAt,
            long expiresAtEpochMillis,
            boolean expired,
            BigDecimal destinationAmount,
            BigDecimal sourceAmount,
            BigDecimal transferFee,
            BigDecimal markupRate,
            BigDecimal fxMarkup,
            BigDecimal landedCost,
            BigDecimal expectedReceived,
            int settlementMinDays,
            int settlementMaxDays,
            LocalDate estimatedArrival,
            LocalDate latestSafeDate,
            int safetyScore) {
        ChannelQuote withRank(Integer value) {
            return new ChannelQuote(channelId, displayName, sourceAccountId, eligible, eligibilityReason, value,
                    rateVndPerCny, quoteId, quoteSource, quotedAt, expiresAt, expiresAtEpochMillis, expired,
                    destinationAmount, sourceAmount, transferFee, markupRate, fxMarkup,
                    landedCost, expectedReceived, settlementMinDays, settlementMaxDays,
                    estimatedArrival, latestSafeDate, safetyScore);
        }
    }

    @Transactional
    public void reset() {
        lockPaymentWorkflow();
        db.update("DELETE FROM fx_quotes");
        db.update("DELETE FROM payment_channel_corridors");
        db.update("DELETE FROM payment_channels");
        db.update("DELETE FROM international_bills");
        db.update("DELETE FROM school_registry");
        db.update("DELETE FROM student_corridor_profile");

        db.update("""
                INSERT INTO student_corridor_profile
                (id,demo_profile_id,source_country,destination_country,source_currency,destination_currency,preference)
                VALUES (1,1,'Vietnam','China','VND','CNY','CHEAPER')
                """);
        insertSchool(1, SCHOOL_NAME, SCHOOL_RECIPIENT_NAME, SCHOOL_RECIPIENT_BANK,
                SCHOOL_RECIPIENT_BANK_CODE, SCHOOL_RECIPIENT, "China", "CNY");
        insertSchool(2, US_SCHOOL_NAME, US_SCHOOL_RECIPIENT_NAME, US_SCHOOL_RECIPIENT_BANK,
                US_SCHOOL_RECIPIENT_BANK_CODE, US_SCHOOL_RECIPIENT, "United States", "USD");
        insertSchool(3, AU_SCHOOL_NAME, AU_SCHOOL_RECIPIENT_NAME, AU_SCHOOL_RECIPIENT_BANK,
                AU_SCHOOL_RECIPIENT_BANK_CODE, AU_SCHOOL_RECIPIENT, "Australia", "AUD");
        db.update("""
                INSERT INTO international_bills
                (id,expense_type,title,institution,amount,currency,destination_country,recipient_name,recipient_bank_name,
                 recipient_bank_code,recipient_account,payment_reference,due_date,evidence_label,document_name,
                 document_content_type,document_size,selected,lifecycle_status,created_at,updated_at)
                VALUES (1,'TUITION','Tuition fee',?,?,?,?,?,?,?,?,?,?,'Synthetic tuition bill',NULL,NULL,NULL,TRUE,'ACTIVE',?,?)
                """, SCHOOL_NAME, TUITION_AMOUNT, "CNY", "China", SCHOOL_RECIPIENT_NAME,
                SCHOOL_RECIPIENT_BANK, SCHOOL_RECIPIENT_BANK_CODE, SCHOOL_RECIPIENT,
                "SZDU-2026-MINH", LocalDate.now().plusDays(14), LocalDateTime.now(), LocalDateTime.now());

        insertChannel("ALIPAY", "Alipay Student Payment", "ALIPAY_VND", true,
                "Student profile includes an eligible Alipay education account",
                "3535.0000", "120000.00", "0.003000", 85, 1, 1,
                "Synthetic Alipay education quote feed");
        insertChannel("BANK_A", "Bank A International Transfer", "PAYER_VND", true,
                "Student profile includes an active Bank A account",
                "3520.0000", "220000.00", "0.002000", 95, 2, 3,
                "Synthetic Bank A treasury quote");
        insertChannel("VCB", "Vietcombank International Transfer", "VCB_VND", true,
                "Connected Vietcombank account supports the Vietnam to China corridor",
                "3527.0000", "180000.00", "0.001800", 93, 2, 3,
                "Synthetic Vietcombank treasury quote");
        insertChannel("TCB", "Techcombank International Transfer", "TCB_VND", true,
                "Connected Techcombank account supports the Vietnam to China corridor",
                "3518.0000", "250000.00", "0.002200", 91, 2, 3,
                "Synthetic Techcombank treasury quote");
        insertChannel("MOMO", "MoMo Education Payment", "MOMO_VND", true,
                "Connected MoMo wallet supports this synthetic education corridor",
                "3542.0000", "90000.00", "0.003500", 82, 1, 2,
                "Synthetic MoMo education quote feed");
        insertChannel("BANK_B", "Bank B Promotional Rate", null, false,
                "Unavailable: the student does not have a Bank B account",
                "3480.0000", "100000.00", "0.001000", 90, 1, 2,
                "Synthetic Bank B promotional quote");

        insertCorridor("ALIPAY", "China", "CNY", true,
                "Eligible Alipay education account for Vietnam to China", "3535.0000", "120000.00", "0.003000", 85, 1, 1,
                "Synthetic Alipay education quote feed");
        insertCorridor("BANK_A", "China", "CNY", true,
                "Bank A supports this Vietnam to China payment", "3520.0000", "220000.00", "0.002000", 95, 2, 3,
                "Synthetic Bank A treasury quote");
        insertCorridor("VCB", "China", "CNY", true,
                "Vietcombank supports this Vietnam to China payment", "3527.0000", "180000.00", "0.001800", 93, 2, 3,
                "Synthetic Vietcombank treasury quote");
        insertCorridor("TCB", "China", "CNY", true,
                "Techcombank supports this Vietnam to China payment", "3518.0000", "250000.00", "0.002200", 91, 2, 3,
                "Synthetic Techcombank treasury quote");
        insertCorridor("MOMO", "China", "CNY", true,
                "MoMo supports this synthetic education corridor", "3542.0000", "90000.00", "0.003500", 82, 1, 2,
                "Synthetic MoMo education quote feed");
        insertCorridor("BANK_B", "China", "CNY", false,
                "Reference only: no connected Bank B account", "3480.0000", "100000.00", "0.001000", 90, 1, 2,
                "Synthetic Bank B promotional quote");

        insertBankCorridor("United States", "USD", "26100.0000", "26080.0000", "26050.0000", "25950.0000");
        insertBankCorridor("Australia", "AUD", "17200.0000", "17180.0000", "17150.0000", "17080.0000");

        refreshQuotes();
    }

    private void insertSchool(int id, String institution, String recipientName, String bankName,
                              String bankCode, String recipient, String country, String currency) {
        db.update("""
                INSERT INTO school_registry
                (id,institution,recipient_name,recipient_bank_name,recipient_bank_code,recipient_account,
                 destination_country,destination_currency,verification_status)
                VALUES (?,?,?,?,?,?,?,?,'VERIFIED')
                """, id, institution, recipientName, bankName, bankCode, recipient, country, currency);
    }

    private void insertBankCorridor(String country, String currency, String bankARate,
                                    String vcbRate, String tcbRate, String bankBRate) {
        insertCorridor("ALIPAY", country, currency, false,
                "Alipay Education Wallet is not configured for this demo corridor", bankARate, "120000.00", "0.003000", 85, 1, 2,
                "Synthetic Alipay reference quote");
        insertCorridor("BANK_A", country, currency, true,
                "Bank A supports this configured student-payment corridor", bankARate, "260000.00", "0.002000", 95, 2, 3,
                "Synthetic Bank A treasury quote");
        insertCorridor("VCB", country, currency, true,
                "Vietcombank supports this configured student-payment corridor", vcbRate, "220000.00", "0.001800", 93, 2, 3,
                "Synthetic Vietcombank treasury quote");
        insertCorridor("TCB", country, currency, true,
                "Techcombank supports this configured student-payment corridor", tcbRate, "280000.00", "0.002200", 91, 2, 3,
                "Synthetic Techcombank treasury quote");
        insertCorridor("MOMO", country, currency, false,
                "MoMo Wallet is not configured for this demo corridor", bankARate, "90000.00", "0.003500", 82, 1, 2,
                "Synthetic MoMo reference quote");
        insertCorridor("BANK_B", country, currency, false,
                "Reference only: no connected Bank B account", bankBRate, "150000.00", "0.001000", 90, 1, 2,
                "Synthetic Bank B promotional quote");
    }

    @Transactional
    public void seedIfEmpty() {
        Integer count = db.queryForObject("SELECT COUNT(*) FROM student_corridor_profile", Integer.class);
        if (count == null || count == 0) {
            reset();
            return;
        }
        Integer activeQuoteCount = db.queryForObject("SELECT COUNT(*) FROM fx_quotes WHERE expires_at>?",
                Integer.class, LocalDateTime.now());
        Integer linkedChannelCount = db.queryForObject(
                "SELECT COUNT(*) FROM payment_channels WHERE source_account_id IS NOT NULL", Integer.class);
        Integer corridorCount = db.queryForObject("SELECT COUNT(*) FROM payment_channel_corridors", Integer.class);
        if (linkedChannelCount == null || linkedChannelCount != 5 || corridorCount == null || corridorCount != 18) {
            reset();
        } else if (activeQuoteCount == null || activeQuoteCount == 0) {
            refreshQuotes();
        }
    }

    private void insertChannel(String id, String name, String sourceAccountId, boolean eligible, String reason,
                               String rate, String fee, String markup, int safety,
                               int minDays, int maxDays, String source) {
        db.update("""
                INSERT INTO payment_channels
                (id,display_name,source_account_id,eligible,eligibility_reason,seed_rate_vnd_per_cny,transfer_fee_vnd,
                 fx_markup_rate,safety_score,settlement_min_days,settlement_max_days,quote_source)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                """, id, name, sourceAccountId, eligible, reason, new BigDecimal(rate), new BigDecimal(fee),
                new BigDecimal(markup), safety, minDays, maxDays, source);
    }

    private void insertCorridor(String channelId, String country, String currency, boolean eligible, String reason,
                                String rate, String fee, String markup, int safety,
                                int minDays, int maxDays, String source) {
        db.update("""
                INSERT INTO payment_channel_corridors
                (channel_id,destination_country,destination_currency,eligible,eligibility_reason,rate_vnd_per_unit,
                 transfer_fee_vnd,fx_markup_rate,safety_score,settlement_min_days,settlement_max_days,quote_source)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                """, channelId, country, currency, eligible, reason, new BigDecimal(rate), new BigDecimal(fee),
                new BigDecimal(markup), safety, minDays, maxDays, source);
    }

    @Transactional
    public void refreshQuotes() {
        lockPaymentWorkflow();
        db.update("DELETE FROM fx_quotes");
        StudentProfile profile = profile();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        for (var channel : db.queryForList("""
                SELECT channel_id,rate_vnd_per_unit,quote_source
                FROM payment_channel_corridors
                WHERE destination_country=? AND destination_currency=?
                """, profile.destinationCountry(), profile.destinationCurrency())) {
            String channelId = (String) channel.get("channel_id");
            db.update("""
                    INSERT INTO fx_quotes
                    (id,channel_id,rate_vnd_per_cny,source_label,quoted_at,expires_at,quote_status,
                     destination_country,destination_currency)
                    VALUES (?,?,?,?,?,?,'ESTIMATED',?,?)
                    """, "Q-" + channelId + "-" + UUID.randomUUID().toString().substring(0,8).toUpperCase(),
                    channelId, channel.get("rate_vnd_per_unit"), channel.get("quote_source"),
                    now, now.plusMinutes(QUOTE_VALIDITY_MINUTES), profile.destinationCountry(),
                    profile.destinationCurrency());
        }
    }

    public boolean corridorSupported(String destinationCountry, String destinationCurrency) {
        Integer count = db.queryForObject("""
                SELECT COUNT(*) FROM payment_channel_corridors
                WHERE destination_country=? AND destination_currency=?
                """, Integer.class, destinationCountry, destinationCurrency);
        return count != null && count > 0;
    }

    @Transactional
    public void setPreference(String preference) {
        String normalized = preference == null ? "" : preference.trim().toUpperCase();
        if (!List.of("CHEAPER", "FASTER", "SAFER").contains(normalized)) {
            throw new IllegalArgumentException("Preference must be CHEAPER, FASTER or SAFER");
        }
        db.update("UPDATE student_corridor_profile SET preference=? WHERE id=1", normalized);
    }

    public StudentProfile profile() {
        return db.queryForObject("""
                SELECT d.display_name,c.source_country,c.destination_country,c.source_currency,c.destination_currency,c.preference
                FROM student_corridor_profile c JOIN demo_profile d ON d.id=c.demo_profile_id
                WHERE c.id=1
                """, (rs,n) -> new StudentProfile(rs.getString(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getString(6)));
    }

    public Map<String,Object> tuitionInsight() {
        TuitionBill tuition = bill();
        StudentExpense expense = selectedExpense();
        RecipientVerification verification = verifyRecipient();
        ChannelQuote option = rankedQuotes("CHEAPER").stream()
                .filter(ChannelQuote::eligible).findFirst().orElse(null);
        String title = "TUITION".equals(expense.expenseType())
                ? "Tuition payment needs a controlled plan"
                : expense.title() + " needs a controlled plan";
        String message = verification.verified() && option != null
                ? "Bill " + tuition.paymentReference() + " for " + tuition.amount().toPlainString()
                        + " " + tuition.currency() + " is verified, due " + tuition.dueDate() + ", with latest safe date "
                        + option.latestSafeDate() + ". Approval Mode is still required before payment."
                : verification.verified()
                ? "Bill " + tuition.paymentReference() + " is verified, but no simulated channel is configured for "
                        + tuition.destinationCountry() + " and " + tuition.currency() + "."
                : "Bill " + tuition.paymentReference()
                        + " has a recipient mismatch and is blocked until verification succeeds.";
        String evidence = option == null ? "Education Provider Registry verification" :
                "Verified Education Provider Registry · " + option.displayName() + " · landed cost " + option.landedCost().toPlainString() + " VND";
        return Map.of("priority", verification.verified() ? "HIGH" : "BLOCKED",
                "title", title, "message", message, "evidence", evidence);
    }

    public Map<String,Object> tuitionInsightForPlan(String channelId, String quoteId, BigDecimal landedCost) {
        TuitionBill tuition = bill();
        StudentExpense expense = selectedExpense();
        RecipientVerification verification = verifyRecipient();
        ChannelQuote option = rankedQuotes().stream()
                .filter(quote -> quote.channelId().equals(channelId))
                .findFirst().orElse(null);
        if (option == null) return tuitionInsight();

        String title = "TUITION".equals(expense.expenseType())
                ? "Selected tuition plan is ready for review"
                : "Selected " + expense.title() + " plan is ready for review";
        String message = verification.verified()
                ? "Selected plan uses " + option.displayName() + " for bill " + tuition.paymentReference()
                        + " of " + tuition.amount().toPlainString() + " " + tuition.currency() + ", due " + tuition.dueDate()
                        + ", with latest safe date " + option.latestSafeDate()
                        + ". Approval Mode is required before payment."
                : "Bill " + tuition.paymentReference()
                        + " has a recipient mismatch and is blocked until verification succeeds.";
        String evidence = verification.verified()
                ? "Selected plan · Verified Education Provider Registry · " + option.displayName()
                        + " · quote " + quoteId + " · landed cost " + landedCost.toPlainString() + " VND"
                : "Selected plan · Education Provider Registry verification failed";
        return Map.of("priority", verification.verified() ? "HIGH" : "BLOCKED",
                "title", title, "message", message, "evidence", evidence);
    }

    public TuitionBill bill() {
        Integer id = db.queryForObject("SELECT id FROM international_bills WHERE selected=TRUE AND lifecycle_status='ACTIVE' ORDER BY id LIMIT 1", Integer.class);
        return bill(id);
    }

    public TuitionBill bill(int id) {
        return db.queryForObject("""
                SELECT id,institution,amount,currency,destination_country,recipient_name,recipient_bank_name,
                       recipient_bank_code,recipient_account,payment_reference,due_date,evidence_label
                FROM international_bills WHERE id=?
                """, (rs,n) -> new TuitionBill(rs.getInt(1), rs.getString(2), rs.getBigDecimal(3),
                rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8),
                rs.getString(9), rs.getString(10), rs.getObject(11, LocalDate.class), rs.getString(12)), id);
    }

    public List<StudentExpense> expenses() {
        return db.query("""
                SELECT b.id,b.expense_type,b.title,b.institution,b.amount,b.currency,b.destination_country,
                       b.recipient_name,b.recipient_bank_name,b.recipient_bank_code,b.recipient_account,
                       CASE WHEN EXISTS (
                         SELECT 1 FROM school_registry r
                         WHERE r.institution=b.institution AND r.recipient_name=b.recipient_name
                           AND r.recipient_bank_name=b.recipient_bank_name
                           AND r.recipient_bank_code=b.recipient_bank_code
                           AND r.recipient_account=b.recipient_account
                           AND r.destination_country=b.destination_country
                           AND r.destination_currency=b.currency AND r.verification_status='VERIFIED'
                       ) THEN 'VERIFIED' ELSE 'MISMATCH' END AS verification_status,
                       b.payment_reference,b.due_date,b.evidence_label,b.document_name,
                       b.document_content_type,b.document_size,b.selected,b.lifecycle_status,b.created_at,b.updated_at,
                       CASE WHEN EXISTS (
                         SELECT 1 FROM action_plans a JOIN sandbox_transactions s ON s.action_id=a.id
                         WHERE a.expense_id=b.id AND s.status='COMPLETED'
                       ) THEN TRUE ELSE FALSE END AS executed
                FROM international_bills b
                ORDER BY CASE b.lifecycle_status WHEN 'ACTIVE' THEN 0 WHEN 'ARCHIVED' THEN 1 ELSE 2 END,
                         b.due_date,b.id
                """, (rs,n) -> new StudentExpense(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getBigDecimal(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9),
                rs.getString(10), rs.getString(11), rs.getString(12), rs.getString(13),
                rs.getObject(14, LocalDate.class), rs.getString(15), rs.getString(16), rs.getString(17),
                rs.getObject(18, Long.class), rs.getBoolean(19), rs.getString(20), rs.getBoolean(23),
                rs.getTimestamp(21).toLocalDateTime(), rs.getTimestamp(22).toLocalDateTime()));
    }

    public StudentExpense selectedExpense() {
        return expenses().stream().filter(StudentExpense::selected).findFirst()
                .orElseThrow(() -> new IllegalStateException("No student expense is selected"));
    }

    @Transactional
    public int addExpense(String expenseType, String title, String institution, BigDecimal amount,
                          String destinationCountry, String currency, String recipientName,
                          String recipientBankName, String recipientBankCode, String recipientAccount,
                          String paymentReference, LocalDate dueDate,
                          String documentName, String documentContentType, Long documentSize) {
        lockPaymentWorkflow();
        String normalizedType = expenseType == null ? "" : expenseType.trim().toUpperCase();
        if (!List.of("TUITION", "DORMITORY", "INSURANCE", "VISA", "LIVING", "OTHER").contains(normalizedType))
            throw new IllegalArgumentException("Choose a supported student expense type");
        String cleanedTitle = requiredText(title, "Expense title", 120);
        String cleanedInstitution = requiredText(institution, "Institution", 120);
        String cleanedCountry = requiredText(destinationCountry, "Destination country", 40);
        String cleanedCurrency = requiredText(currency, "Destination currency", 3).toUpperCase();
        if (!corridorSupported(cleanedCountry, cleanedCurrency))
            throw new IllegalArgumentException("This demo has no configured quote data for the selected corridor");
        String cleanedRecipientName = requiredText(recipientName, "Recipient legal name", 160);
        String cleanedRecipientBank = requiredText(recipientBankName, "Recipient bank", 160);
        String cleanedRecipientBankCode = requiredText(recipientBankCode, "SWIFT/BIC or bank routing code", 34).toUpperCase();
        String cleanedRecipient = requiredText(recipientAccount, "Recipient account", 120);
        String cleanedReference = requiredText(paymentReference, "Payment reference", 80);
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2)
            throw new IllegalArgumentException("Amount must be positive with at most two decimal places");
        if (dueDate == null) throw new IllegalArgumentException("Due date is required");
        if (documentSize != null && documentSize > 5L * 1024 * 1024)
            throw new IllegalArgumentException("Document must be 5 MB or smaller");
        if (documentContentType != null && !List.of("application/pdf", "image/jpeg", "image/png")
                .contains(documentContentType.toLowerCase()))
            throw new IllegalArgumentException("Document must be PDF, JPG or PNG");

        boolean verified = recipientProfileVerified(cleanedInstitution, cleanedRecipientName, cleanedRecipientBank,
                cleanedRecipientBankCode, cleanedRecipient, cleanedCountry, cleanedCurrency);
        Integer id = db.queryForObject("SELECT COALESCE(MAX(id),0)+1 FROM international_bills", Integer.class);
        if (verified) db.update("UPDATE international_bills SET selected=FALSE");
        db.update("""
                INSERT INTO international_bills
                (id,expense_type,title,institution,amount,currency,destination_country,recipient_name,recipient_bank_name,
                 recipient_bank_code,recipient_account,payment_reference,due_date,evidence_label,document_name,
                 document_content_type,document_size,selected,lifecycle_status,created_at,updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,'User-provided expense',?,?,?,?,'ACTIVE',?,?)
                """, id, normalizedType, cleanedTitle, cleanedInstitution, amount.setScale(2), cleanedCurrency,
                cleanedCountry, cleanedRecipientName, cleanedRecipientBank, cleanedRecipientBankCode, cleanedRecipient,
                cleanedReference, dueDate, documentName, documentContentType, documentSize, verified,
                LocalDateTime.now(), LocalDateTime.now());
        if (verified) applySelectedCorridor(cleanedCountry, cleanedCurrency);
        return id;
    }

    @Transactional
    public void selectExpense(int id) {
        lockPaymentWorkflow();
        Integer count = db.queryForObject("""
                SELECT COUNT(*) FROM international_bills b
                WHERE b.id=? AND b.lifecycle_status='ACTIVE' AND EXISTS (
                  SELECT 1 FROM school_registry r
                  WHERE r.institution=b.institution AND r.recipient_name=b.recipient_name
                    AND r.recipient_bank_name=b.recipient_bank_name
                    AND r.recipient_bank_code=b.recipient_bank_code
                    AND r.recipient_account=b.recipient_account
                    AND r.destination_country=b.destination_country
                    AND r.destination_currency=b.currency AND r.verification_status='VERIFIED'
                )
                """, Integer.class, id);
        if (count == null || count == 0)
            throw new IllegalArgumentException("Only an active bill with a verified beneficiary can be selected");
        db.update("UPDATE international_bills SET selected=FALSE");
        db.update("UPDATE international_bills SET selected=TRUE WHERE id=?", id);
        StudentExpense selected = selectedExpense();
        applySelectedCorridor(selected.destinationCountry(), selected.currency());
    }

    @Transactional
    public void updateExpense(int id, String expenseType, String title, String institution, BigDecimal amount,
                              String destinationCountry, String currency, String recipientName,
                              String recipientBankName, String recipientBankCode, String recipientAccount,
                              String paymentReference, LocalDate dueDate) {
        lockPaymentWorkflow();
        StudentExpense current = expense(id);
        if (!current.active()) throw new IllegalArgumentException("Only active student bills can be edited");
        if (current.executed())
            throw new IllegalArgumentException("An executed student bill cannot be edited; archive it to preserve the receipt and Audit Log");

        String normalizedType = expenseType == null ? "" : expenseType.trim().toUpperCase();
        if (!List.of("TUITION", "DORMITORY", "INSURANCE", "VISA", "LIVING", "OTHER").contains(normalizedType))
            throw new IllegalArgumentException("Choose a supported student expense type");
        String cleanedTitle = requiredText(title, "Expense title", 120);
        String cleanedInstitution = requiredText(institution, "Institution or education provider", 120);
        String cleanedCountry = requiredText(destinationCountry, "Destination country", 40);
        String cleanedCurrency = requiredText(currency, "Destination currency", 3).toUpperCase();
        if (!corridorSupported(cleanedCountry, cleanedCurrency))
            throw new IllegalArgumentException("This demo has no configured quote data for the selected corridor");
        String cleanedRecipientName = requiredText(recipientName, "Recipient legal name", 160);
        String cleanedRecipientBank = requiredText(recipientBankName, "Recipient bank", 160);
        String cleanedRecipientBankCode = requiredText(recipientBankCode, "SWIFT/BIC or bank routing code", 34).toUpperCase();
        String cleanedRecipient = requiredText(recipientAccount, "Recipient account", 120);
        String cleanedReference = requiredText(paymentReference, "Payment reference", 80);
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2)
            throw new IllegalArgumentException("Amount must be positive with at most two decimal places");
        if (dueDate == null) throw new IllegalArgumentException("Due date is required");

        boolean verified = recipientProfileVerified(cleanedInstitution, cleanedRecipientName, cleanedRecipientBank,
                cleanedRecipientBankCode, cleanedRecipient, cleanedCountry, cleanedCurrency);
        Integer verifiedFallbackId = current.selected() && !verified ? fallbackExpenseId(current) : null;
        invalidatePendingPlans(id, "Student bill details changed");
        db.update("""
                UPDATE international_bills
                SET expense_type=?,title=?,institution=?,amount=?,currency=?,destination_country=?,
                    recipient_name=?,recipient_bank_name=?,recipient_bank_code=?,recipient_account=?,
                    payment_reference=?,due_date=?,updated_at=?
                WHERE id=?
                """, normalizedType, cleanedTitle, cleanedInstitution, amount.setScale(2), cleanedCurrency,
                cleanedCountry, cleanedRecipientName, cleanedRecipientBank, cleanedRecipientBankCode,
                cleanedRecipient, cleanedReference, dueDate, LocalDateTime.now(), id);
        auditExpense("EXPENSE_UPDATED", id, "Student bill updated; all pending plans and approvals were invalidated");
        if (current.selected() && verified) {
            applySelectedCorridor(cleanedCountry, cleanedCurrency);
        } else if (current.selected()) {
            db.update("UPDATE international_bills SET selected=FALSE WHERE id=?", id);
            selectExpense(verifiedFallbackId);
        }
    }

    @Transactional
    public void archiveExpense(int id) {
        lockPaymentWorkflow();
        StudentExpense expense = expense(id);
        if (!expense.active()) throw new IllegalArgumentException("Only active student bills can be archived");
        Integer fallbackId = fallbackExpenseId(expense);
        invalidatePendingPlans(id, "Student bill archived");
        db.update("UPDATE international_bills SET lifecycle_status='ARCHIVED',selected=FALSE,updated_at=? WHERE id=?",
                LocalDateTime.now(), id);
        auditExpense("EXPENSE_ARCHIVED", id, expense.executed()
                ? "Executed student bill archived; receipt and Audit Log preserved"
                : "Student bill archived; pending plans invalidated");
        if (fallbackId != null) selectExpense(fallbackId);
    }

    @Transactional
    public void cancelExpense(int id) {
        lockPaymentWorkflow();
        StudentExpense expense = expense(id);
        if (!expense.active()) throw new IllegalArgumentException("Only active student bills can be cancelled");
        if (expense.executed())
            throw new IllegalArgumentException("An executed student bill cannot be cancelled or deleted; archive it instead");
        Integer fallbackId = fallbackExpenseId(expense);
        invalidatePendingPlans(id, "Student bill cancelled");
        db.update("UPDATE international_bills SET lifecycle_status='CANCELLED',selected=FALSE,updated_at=? WHERE id=?",
                LocalDateTime.now(), id);
        auditExpense("EXPENSE_CANCELLED", id, "Student bill cancelled; pending plans and approvals invalidated");
        if (fallbackId != null) selectExpense(fallbackId);
    }

    @Transactional
    public void restoreExpense(int id) {
        lockPaymentWorkflow();
        StudentExpense expense = expense(id);
        if (!"ARCHIVED".equals(expense.lifecycleStatus()))
            throw new IllegalArgumentException("Only archived student bills can be restored");
        db.update("UPDATE international_bills SET lifecycle_status='ACTIVE',updated_at=? WHERE id=?",
                LocalDateTime.now(), id);
        auditExpense("EXPENSE_RESTORED", id, "Archived student bill restored as active");
    }

    public StudentExpense expense(int id) {
        return expenses().stream().filter(item -> item.id() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Student bill does not exist"));
    }

    private Integer fallbackExpenseId(StudentExpense expense) {
        if (!expense.selected()) return null;
        List<Integer> candidates = db.query("""
                SELECT b.id FROM international_bills b
                WHERE b.id<>? AND b.lifecycle_status='ACTIVE' AND EXISTS (
                  SELECT 1 FROM school_registry r
                  WHERE r.institution=b.institution AND r.recipient_name=b.recipient_name
                    AND r.recipient_bank_name=b.recipient_bank_name
                    AND r.recipient_bank_code=b.recipient_bank_code
                    AND r.recipient_account=b.recipient_account
                    AND r.destination_country=b.destination_country
                    AND r.destination_currency=b.currency AND r.verification_status='VERIFIED'
                )
                ORDER BY b.due_date,b.id FETCH FIRST 1 ROWS ONLY
                """, (rs,n) -> rs.getInt(1), expense.id());
        if (candidates.isEmpty())
            throw new IllegalArgumentException("Create or restore another verified active student bill before removing the selected bill");
        return candidates.getFirst();
    }

    private void lockPaymentWorkflow() {
        db.query("SELECT id FROM agent_policy WHERE id=1 FOR UPDATE", (rs,n) -> rs.getInt(1));
    }

    private void invalidatePendingPlans(int expenseId, String reason) {
        List<String> planIds = db.query("""
                SELECT id FROM action_plans
                WHERE expense_id=? AND status NOT IN ('COMPLETED','INVALIDATED','CANCELED')
                """, (rs,n) -> rs.getString(1), expenseId);
        for (String planId : planIds) {
            db.update("UPDATE action_plans SET status='INVALIDATED',risk=? WHERE id=?", reason, planId);
            db.update("UPDATE approvals SET status='REVOKED' WHERE action_id=? AND status='VALID'", planId);
            db.update("INSERT INTO audit_log VALUES (?,?,?,?,?,?,?,?)",
                    "AUD-" + UUID.randomUUID().toString().substring(0,12).toUpperCase(), LocalDateTime.now(),
                    "POLICY_GUARD", "ACTION_INVALIDATED", planId, "INVALIDATED", "EXPENSE CHANGED", reason);
        }
    }

    private void auditExpense(String event, int expenseId, String details) {
        db.update("INSERT INTO audit_log VALUES (?,?,?,?,?,?,?,?)",
                "AUD-" + UUID.randomUUID().toString().substring(0,12).toUpperCase(), LocalDateTime.now(),
                "USER", event, Integer.toString(expenseId), "COMPLETED", null, details);
    }

    private boolean recipientProfileVerified(String institution, String recipientName, String bankName,
                                             String bankCode, String account, String country, String currency) {
        Integer count = db.queryForObject("""
                SELECT COUNT(*) FROM school_registry
                WHERE institution=? AND recipient_name=? AND recipient_bank_name=? AND recipient_bank_code=?
                  AND recipient_account=? AND destination_country=? AND destination_currency=?
                  AND verification_status='VERIFIED'
                """, Integer.class, institution, recipientName, bankName, bankCode, account, country, currency);
        return count != null && count > 0;
    }

    private void applySelectedCorridor(String country, String currency) {
        db.update("""
                UPDATE student_corridor_profile
                SET destination_country=?,destination_currency=? WHERE id=1
                """, country, currency);
        refreshQuotes();
    }

    private String requiredText(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        String cleaned = value.trim().replaceAll("\\s+", " ");
        if (cleaned.length() > maxLength) throw new IllegalArgumentException(label + " is too long");
        return cleaned;
    }

    public RecipientVerification verifyRecipient() {
        return verifyRecipient(bill().id());
    }

    public RecipientVerification verifyRecipient(int expenseId) {
        TuitionBill bill = bill(expenseId);
        List<RecipientVerification> matches = db.query("""
                SELECT institution,recipient_account,recipient_bank_name,recipient_bank_code,verification_status
                FROM school_registry
                WHERE institution=? AND recipient_name=? AND recipient_bank_name=? AND recipient_bank_code=?
                  AND recipient_account=? AND destination_country=? AND destination_currency=?
                """, (rs,n) -> new RecipientVerification(
                "VERIFIED".equals(rs.getString(5)), rs.getString(5),
                "Provider, beneficiary, receiving bank, account, corridor and currency match the trusted demo registry",
                rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)),
                bill.institution(), bill.recipientName(), bill.recipientBankName(), bill.recipientBankCode(),
                bill.recipientAccount(), bill.destinationCountry(), bill.currency());
        if (matches.isEmpty()) {
            return new RecipientVerification(false, "MISMATCH",
                    "One or more beneficiary fields do not match the trusted education-provider registry",
                    null, null, null, null);
        }
        return matches.getFirst();
    }

    public List<ChannelQuote> rankedQuotes() {
        return rankedQuotes(profile().preference());
    }

    public List<ChannelQuote> rankedQuotes(String preference) {
        String normalizedPreference = preference == null ? "CHEAPER" : preference.toUpperCase();
        return rankedQuotesForBill(bill(), normalizedPreference);
    }

    public List<ChannelQuote> rankedQuotesForExpense(int expenseId) {
        return rankedQuotesForBill(bill(expenseId), profile().preference());
    }

    private List<ChannelQuote> rankedQuotesForBill(TuitionBill bill, String normalizedPreference) {
        LocalDateTime now = LocalDateTime.now();
        List<ChannelQuote> options = db.query("""
                SELECT c.id,c.display_name,c.source_account_id,cc.eligible,cc.eligibility_reason,cc.transfer_fee_vnd,cc.fx_markup_rate,
                       cc.safety_score,cc.settlement_min_days,cc.settlement_max_days,
                       q.id,q.rate_vnd_per_cny,q.source_label,q.quoted_at,q.expires_at
                FROM payment_channels c
                JOIN payment_channel_corridors cc ON cc.channel_id=c.id
                JOIN fx_quotes q ON q.channel_id=c.id
                  AND q.destination_country=cc.destination_country
                  AND q.destination_currency=cc.destination_currency
                WHERE cc.destination_country=? AND cc.destination_currency=?
                """, (rs,n) -> {
            BigDecimal rate = rs.getBigDecimal(12);
            BigDecimal sourceAmount = bill.amount().multiply(rate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal markupRate = rs.getBigDecimal(7);
            BigDecimal fxMarkup = sourceAmount.multiply(markupRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal transferFee = rs.getBigDecimal(6);
            BigDecimal landedCost = sourceAmount.add(transferFee).add(fxMarkup).setScale(2, RoundingMode.HALF_UP);
            int maxDays = rs.getInt(10);
            LocalDateTime quotedAt = rs.getTimestamp(14).toLocalDateTime();
            LocalDateTime expiresAt = rs.getTimestamp(15).toLocalDateTime();
            return new ChannelQuote(
                    rs.getString(1), rs.getString(2), rs.getString(3), rs.getBoolean(4), rs.getString(5), null,
                    rate, rs.getString(11), rs.getString(13), quotedAt, expiresAt,
                    expiresAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    !now.isBefore(expiresAt), bill.amount(), sourceAmount, transferFee,
                    markupRate, fxMarkup, landedCost, bill.amount(),
                    rs.getInt(9), maxDays, LocalDate.now().plusDays(maxDays),
                    bill.dueDate().minusDays(maxDays + SETTLEMENT_SAFETY_MARGIN_DAYS),
                    rs.getInt(8));
        }, bill.destinationCountry(), bill.currency());

        Comparator<ChannelQuote> preferenceComparator = switch(normalizedPreference) {
            case "FASTER" -> Comparator.comparingInt(ChannelQuote::settlementMaxDays)
                    .thenComparing(ChannelQuote::landedCost);
            case "SAFER" -> Comparator.comparingInt(ChannelQuote::safetyScore).reversed()
                    .thenComparing(ChannelQuote::landedCost);
            default -> Comparator.comparing(ChannelQuote::landedCost);
        };
        options.sort(Comparator.comparing((ChannelQuote q) -> !q.eligible())
                .thenComparing(preferenceComparator)
                .thenComparing(ChannelQuote::channelId));

        List<ChannelQuote> ranked = new ArrayList<>();
        int eligibleRank = 1;
        for (ChannelQuote option : options) {
            ranked.add(option.withRank(option.eligible() ? eligibleRank++ : null));
        }
        return ranked;
    }

    public List<ChannelQuote> eligibleQuotes() {
        return rankedQuotes().stream().filter(ChannelQuote::eligible).toList();
    }
}
