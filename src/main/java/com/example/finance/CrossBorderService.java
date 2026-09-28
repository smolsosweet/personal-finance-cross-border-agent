package com.example.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CrossBorderService {
    public static final BigDecimal TUITION_AMOUNT = new BigDecimal("20000.00");
    public static final String SCHOOL_NAME = "Shenzhen Demo University";
    public static final String SCHOOL_RECIPIENT = "SZDU-TUITION-2026";
    public static final int QUOTE_VALIDITY_MINUTES = 5;
    public static final int SETTLEMENT_SAFETY_MARGIN_DAYS = 1;

    private final JdbcTemplate db;

    public CrossBorderService(JdbcTemplate db) {
        this.db = db;
    }

    public record StudentProfile(String name, String sourceCountry, String destinationCountry,
                                 String sourceCurrency, String destinationCurrency, String preference) {}

    public record TuitionBill(int id, String institution, BigDecimal amount, String currency,
                              String recipientAccount, String paymentReference, LocalDate dueDate,
                              String evidenceLabel) {}

    public record RecipientVerification(boolean verified, String status, String reason,
                                        String registryInstitution, String registryAccount) {}

    public record ChannelQuote(
            String channelId,
            String displayName,
            boolean eligible,
            String eligibilityReason,
            Integer rank,
            BigDecimal rateVndPerCny,
            String quoteId,
            String quoteSource,
            LocalDateTime quotedAt,
            LocalDateTime expiresAt,
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
            return new ChannelQuote(channelId, displayName, eligible, eligibilityReason, value,
                    rateVndPerCny, quoteId, quoteSource, quotedAt, expiresAt, expired,
                    destinationAmount, sourceAmount, transferFee, markupRate, fxMarkup,
                    landedCost, expectedReceived, settlementMinDays, settlementMaxDays,
                    estimatedArrival, latestSafeDate, safetyScore);
        }
    }

    @Transactional
    public void reset() {
        db.update("DELETE FROM fx_quotes");
        db.update("DELETE FROM payment_channels");
        db.update("DELETE FROM international_bills");
        db.update("DELETE FROM school_registry");
        db.update("DELETE FROM student_corridor_profile");

        db.update("""
                INSERT INTO student_corridor_profile
                (id,demo_profile_id,source_country,destination_country,source_currency,destination_currency,preference)
                VALUES (1,1,'Vietnam','China','VND','CNY','CHEAPER')
                """);
        db.update("""
                INSERT INTO school_registry
                (id,institution,recipient_account,destination_country,destination_currency,verification_status)
                VALUES (1,?,?, 'China','CNY','VERIFIED')
                """, SCHOOL_NAME, SCHOOL_RECIPIENT);
        db.update("""
                INSERT INTO international_bills
                (id,institution,amount,currency,recipient_account,payment_reference,due_date,evidence_label)
                VALUES (1,?,?,?,?,?,?,'Synthetic tuition bill')
                """, SCHOOL_NAME, TUITION_AMOUNT, "CNY", SCHOOL_RECIPIENT,
                "SZDU-2026-MINH", LocalDate.now().plusDays(14));

        insertChannel("ALIPAY", "Alipay Student Payment", true,
                "Student profile includes an eligible Alipay education account",
                "3535.0000", "120000.00", "0.003000", 85, 1, 1,
                "Synthetic Alipay education quote feed");
        insertChannel("BANK_A", "Bank A International Transfer", true,
                "Student profile includes an active Bank A account",
                "3520.0000", "220000.00", "0.002000", 95, 2, 3,
                "Synthetic Bank A treasury quote");
        insertChannel("BANK_B", "Bank B Promotional Rate", false,
                "Unavailable: the student does not have a Bank B account",
                "3480.0000", "100000.00", "0.001000", 90, 1, 2,
                "Synthetic Bank B promotional quote");

        refreshQuotes();
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
        if (activeQuoteCount == null || activeQuoteCount == 0) refreshQuotes();
    }

    private void insertChannel(String id, String name, boolean eligible, String reason,
                               String rate, String fee, String markup, int safety,
                               int minDays, int maxDays, String source) {
        db.update("""
                INSERT INTO payment_channels
                (id,display_name,eligible,eligibility_reason,seed_rate_vnd_per_cny,transfer_fee_vnd,
                 fx_markup_rate,safety_score,settlement_min_days,settlement_max_days,quote_source)
                VALUES (?,?,?,?,?,?,?,?,?,?,?)
                """, id, name, eligible, reason, new BigDecimal(rate), new BigDecimal(fee),
                new BigDecimal(markup), safety, minDays, maxDays, source);
    }

    @Transactional
    public void refreshQuotes() {
        db.update("DELETE FROM fx_quotes");
        LocalDateTime now = LocalDateTime.now().withNano(0);
        for (var channel : db.queryForList("SELECT id,seed_rate_vnd_per_cny,quote_source FROM payment_channels")) {
            String channelId = (String) channel.get("id");
            db.update("""
                    INSERT INTO fx_quotes
                    (id,channel_id,rate_vnd_per_cny,source_label,quoted_at,expires_at,quote_status)
                    VALUES (?,?,?,?,?,?,'ESTIMATED')
                    """, "Q-" + channelId + "-" + UUID.randomUUID().toString().substring(0,8).toUpperCase(),
                    channelId, channel.get("seed_rate_vnd_per_cny"), channel.get("quote_source"),
                    now, now.plusMinutes(QUOTE_VALIDITY_MINUTES));
        }
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

    public TuitionBill bill() {
        return db.queryForObject("""
                SELECT id,institution,amount,currency,recipient_account,payment_reference,due_date,evidence_label
                FROM international_bills WHERE id=1
                """, (rs,n) -> new TuitionBill(rs.getInt(1), rs.getString(2), rs.getBigDecimal(3),
                rs.getString(4), rs.getString(5), rs.getString(6), rs.getObject(7, LocalDate.class),
                rs.getString(8)));
    }

    public RecipientVerification verifyRecipient() {
        TuitionBill bill = bill();
        List<RecipientVerification> matches = db.query("""
                SELECT institution,recipient_account,verification_status
                FROM school_registry
                WHERE institution=? AND recipient_account=? AND destination_country='China' AND destination_currency='CNY'
                """, (rs,n) -> new RecipientVerification(
                "VERIFIED".equals(rs.getString(3)), rs.getString(3),
                "Bill institution, recipient account, corridor and currency match the School Registry",
                rs.getString(1), rs.getString(2)), bill.institution(), bill.recipientAccount());
        if (matches.isEmpty()) {
            return new RecipientVerification(false, "MISMATCH",
                    "Bill recipient does not match the School Registry",
                    null, null);
        }
        return matches.getFirst();
    }

    public List<ChannelQuote> rankedQuotes() {
        return rankedQuotes(profile().preference());
    }

    public List<ChannelQuote> rankedQuotes(String preference) {
        String normalizedPreference = preference == null ? "CHEAPER" : preference.toUpperCase();
        TuitionBill bill = bill();
        LocalDateTime now = LocalDateTime.now();
        List<ChannelQuote> options = db.query("""
                SELECT c.id,c.display_name,c.eligible,c.eligibility_reason,c.transfer_fee_vnd,c.fx_markup_rate,
                       c.safety_score,c.settlement_min_days,c.settlement_max_days,
                       q.id,q.rate_vnd_per_cny,q.source_label,q.quoted_at,q.expires_at
                FROM payment_channels c JOIN fx_quotes q ON q.channel_id=c.id
                """, (rs,n) -> {
            BigDecimal rate = rs.getBigDecimal(11);
            BigDecimal sourceAmount = bill.amount().multiply(rate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal markupRate = rs.getBigDecimal(6);
            BigDecimal fxMarkup = sourceAmount.multiply(markupRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal transferFee = rs.getBigDecimal(5);
            BigDecimal landedCost = sourceAmount.add(transferFee).add(fxMarkup).setScale(2, RoundingMode.HALF_UP);
            int maxDays = rs.getInt(9);
            LocalDateTime quotedAt = rs.getTimestamp(13).toLocalDateTime();
            LocalDateTime expiresAt = rs.getTimestamp(14).toLocalDateTime();
            return new ChannelQuote(
                    rs.getString(1), rs.getString(2), rs.getBoolean(3), rs.getString(4), null,
                    rate, rs.getString(10), rs.getString(12), quotedAt, expiresAt,
                    !now.isBefore(expiresAt), bill.amount(), sourceAmount, transferFee,
                    markupRate, fxMarkup, landedCost, bill.amount(),
                    rs.getInt(8), maxDays, LocalDate.now().plusDays(maxDays),
                    bill.dueDate().minusDays(maxDays + SETTLEMENT_SAFETY_MARGIN_DAYS),
                    rs.getInt(7));
        });

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
