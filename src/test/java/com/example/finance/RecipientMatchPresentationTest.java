package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecipientMatchPresentationTest {
    @Test void mapsPositiveAndNegativeCopyWithoutCreatingAClockOrChangingEvidenceValues() {
        String raw = "Bill SZDU-2026-MINH for 20000.00 CNY is verified, due 2026-10-23.";
        assertEquals("Bill SZDU-2026-MINH for 20000.00 CNY matches the simulated School Registry, due 2026-10-23.", RecipientMatchPresentation.display(raw));
        assertEquals("One or more beneficiary fields do not match the Simulated School Registry", RecipientMatchPresentation.display("One or more beneficiary fields do not match the trusted education-provider registry"));
        assertEquals("Hóa đơn khớp Danh bạ Nhà trường mô phỏng SZDU-2026-MINH: 20000.00 CNY", RecipientMatchPresentation.display("Hóa đơn đã xác minh SZDU-2026-MINH: 20000.00 CNY"));
        assertEquals("VERIFIED", RecipientMatchPresentation.display("VERIFIED"));
        assertEquals("RECIPIENT MISMATCH", RecipientMatchPresentation.display("RECIPIENT MISMATCH"));
        assertNull(RecipientMatchPresentation.display(null));
    }
    @Test void userMessagesRemainVerbatimAndChatComparisonStillUsesStructuredRendering() {
        String raw = "Verified tuition-channel comparison: SZDU-2026-MINH\nShowing 1 eligible channels by cost\nBank A: 70760800.00 VND; 2–3 days; BANK_A; SYNTHETIC; quote; 2026-10-10T10:00:00";
        var messages = List.of(new PhaseFourService.ConversationMessage("1", "USER", "Verified tuition bill", LocalDateTime.now()), new PhaseFourService.ConversationMessage("2", "ASSISTANT", raw, LocalDateTime.now()));
        var rendered = new ConversationPresentation().present(messages);
        assertEquals("Verified tuition bill",rendered.get(0).message());
        assertTrue(rendered.get(1).rich());
        assertTrue(rendered.get(1).message().startsWith("Tuition-channel comparison · Matched against simulated School Registry:"));
        assertEquals("70,760,800 VND",rendered.get(1).channels().getFirst().cost());
        assertEquals(raw,messages.get(1).message());
    }
}
