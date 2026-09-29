package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:phase3_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class PhaseThreeIntegrationTest {
    @Autowired CrossBorderService crossBorder;
    @Autowired PhaseFourService phaseFour;
    @Autowired DemoDataService demoData;
    @Autowired JdbcTemplate db;
    @Autowired MockMvc mvc;

    @BeforeEach void reset() { demoData.resetAll(); }

    @Test void fixedStudentCorridorAndTuitionBillAreLoaded() {
        var profile = crossBorder.profile();
        assertEquals("Vietnam", profile.sourceCountry());
        assertEquals("China", profile.destinationCountry());
        assertEquals("VND", profile.sourceCurrency());
        assertEquals("CNY", profile.destinationCurrency());

        var bill = crossBorder.bill();
        assertEquals(0, CrossBorderService.TUITION_AMOUNT.compareTo(bill.amount()));
        assertEquals("CNY", bill.currency());
        assertEquals(CrossBorderService.SCHOOL_NAME, bill.institution());
        assertEquals(CrossBorderService.SCHOOL_RECIPIENT, bill.recipientAccount());
        assertNotNull(bill.paymentReference());
        assertNotNull(bill.dueDate());
    }

    @Test void recipientMustMatchSchoolRegistry() {
        var verified = crossBorder.verifyRecipient();
        assertTrue(verified.verified());
        assertEquals("VERIFIED", verified.status());

        db.update("UPDATE international_bills SET recipient_account='UNKNOWN-ACCOUNT' WHERE id=1");
        var mismatch = crossBorder.verifyRecipient();
        assertFalse(mismatch.verified());
        assertEquals("MISMATCH", mismatch.status());
    }

    @Test void eligibilityIsAppliedBeforePrice() {
        List<CrossBorderService.ChannelQuote> quotes = crossBorder.rankedQuotes("CHEAPER");
        assertEquals(List.of("BANK_A", "ALIPAY", "BANK_B"), quotes.stream().map(CrossBorderService.ChannelQuote::channelId).toList());
        assertEquals(2, quotes.stream().filter(CrossBorderService.ChannelQuote::eligible).count());

        var bankB = quote("BANK_B", quotes);
        var bankA = quote("BANK_A", quotes);
        assertFalse(bankB.eligible());
        assertNull(bankB.rank());
        assertTrue(bankB.rateVndPerCny().compareTo(bankA.rateVndPerCny()) < 0);
        assertTrue(bankB.landedCost().compareTo(bankA.landedCost()) < 0);
        assertFalse(crossBorder.eligibleQuotes().stream().anyMatch(q -> "BANK_B".equals(q.channelId())));
    }

    @Test void bigDecimalCostBreakdownIsExact() {
        var bankA = quote("BANK_A", crossBorder.rankedQuotes("CHEAPER"));
        assertMoney("70400000.00", bankA.sourceAmount());
        assertMoney("220000.00", bankA.transferFee());
        assertMoney("140800.00", bankA.fxMarkup());
        assertMoney("70760800.00", bankA.landedCost());
        assertMoney("20000.00", bankA.expectedReceived());

        var alipay = quote("ALIPAY", crossBorder.rankedQuotes("CHEAPER"));
        assertMoney("70700000.00", alipay.sourceAmount());
        assertMoney("120000.00", alipay.transferFee());
        assertMoney("212100.00", alipay.fxMarkup());
        assertMoney("71032100.00", alipay.landedCost());
    }

    @Test void paymentBalanceSupportsExactRemainingBalanceForEveryQuote() {
        BigDecimal balance = phaseFour.payerBalance();
        assertMoney("100000000.00", balance);

        var bankA = quote("BANK_A", crossBorder.rankedQuotes("CHEAPER"));
        var alipay = quote("ALIPAY", crossBorder.rankedQuotes("CHEAPER"));
        var bankB = quote("BANK_B", crossBorder.rankedQuotes("CHEAPER"));
        assertMoney("29239200.00", balance.subtract(bankA.landedCost()));
        assertMoney("28967900.00", balance.subtract(alipay.landedCost()));
        assertMoney("30230400.00", balance.subtract(bankB.landedCost()));
    }

    @Test void comparisonExplainsWhenThePaymentBalanceIsInsufficient() throws Exception {
        db.update("UPDATE sandbox_accounts SET balance=29239200.00 WHERE id='PAYER_VND'");

        String html = mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(html.contains("29,239,200 VND"));
        assertTrue(html.contains("-41,521,600 VND"));
        assertTrue(html.contains("Insufficient balance"));
    }

    @Test void quotesHaveSourceTimestampExpiryAndDeadline() {
        var bill = crossBorder.bill();
        for (var quote : crossBorder.rankedQuotes()) {
            assertNotNull(quote.quoteId());
            assertFalse(quote.quoteSource().isBlank());
            assertEquals(5, Duration.between(quote.quotedAt(), quote.expiresAt()).toMinutes());
            assertFalse(quote.expired());
            assertEquals(bill.dueDate().minusDays(quote.settlementMaxDays() + 1L), quote.latestSafeDate());
            assertEquals(java.time.LocalDate.now().plusDays(quote.settlementMaxDays()), quote.estimatedArrival());
        }
    }

    @Test void preferencesRankOnlyEligibleChannels() {
        assertEquals("BANK_A", crossBorder.rankedQuotes("CHEAPER").getFirst().channelId());
        assertEquals("ALIPAY", crossBorder.rankedQuotes("FASTER").getFirst().channelId());
        assertEquals("BANK_A", crossBorder.rankedQuotes("SAFER").getFirst().channelId());

        for (String preference : List.of("CHEAPER", "FASTER", "SAFER")) {
            List<CrossBorderService.ChannelQuote> quotes = crossBorder.rankedQuotes(preference);
            assertTrue(quotes.get(0).eligible());
            assertTrue(quotes.get(1).eligible());
            assertEquals("BANK_B", quotes.get(2).channelId());
            assertNull(quotes.get(2).rank());
        }
    }

    @Test void preferenceAndQuoteRefreshAreRepeatable() {
        String oldQuoteId = crossBorder.rankedQuotes().getFirst().quoteId();
        crossBorder.setPreference("FASTER");
        assertEquals("FASTER", crossBorder.profile().preference());
        assertEquals("ALIPAY", crossBorder.rankedQuotes().getFirst().channelId());

        crossBorder.refreshQuotes();
        assertNotEquals(oldQuoteId, crossBorder.rankedQuotes().getFirst().quoteId());

        demoData.resetAll();
        assertEquals("CHEAPER", crossBorder.profile().preference());
        assertEquals(3, crossBorder.rankedQuotes().size());
    }

    @Test void resetSurfacesTuitionInsightInMainFeedConsistentWithWorkspace() throws Exception {
        var insight = crossBorder.tuitionInsight();
        var bill = crossBorder.bill();
        var preferred = crossBorder.rankedQuotes("CHEAPER").stream()
                .filter(CrossBorderService.ChannelQuote::eligible).findFirst().orElseThrow();

        String html = mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int feedStart = html.indexOf("id=\"feed\"");
        int feedEnd = html.indexOf("id=\"budget\"", feedStart);
        assertTrue(feedStart >= 0 && feedEnd > feedStart);
        String feed = html.substring(feedStart, feedEnd);

        assertTrue(feed.contains((String) insight.get("title")));
        assertTrue(feed.contains((String) insight.get("message")));
        assertTrue(feed.contains((String) insight.get("evidence")));
        assertTrue(feed.contains(bill.paymentReference()));
        assertTrue(feed.contains(bill.dueDate().toString()));
        assertTrue(feed.contains(preferred.latestSafeDate().toString()));
        assertTrue(feed.contains("Approval Mode is still required before payment"));
        assertFalse(feed.contains("payment has been executed"));
    }

    private static CrossBorderService.ChannelQuote quote(String id, List<CrossBorderService.ChannelQuote> quotes) {
        return quotes.stream().filter(q -> id.equals(q.channelId())).findFirst().orElseThrow();
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
