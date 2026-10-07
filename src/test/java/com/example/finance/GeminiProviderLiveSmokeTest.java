package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mock.web.MockMultipartFile;

/** Explicit live smoke only; sends one generic question and a synthetic receipt, never personal bills. */
@EnabledIfEnvironmentVariable(named = "FINBRIDGE_GEMINI_LIVE_SMOKE", matches = "true")
class GeminiProviderLiveSmokeTest {
    @Test void chatbotAndBillExtractionReachGemini() throws Exception {
        String apiKey = System.getenv("GEMINI_API_KEY");
        assertNotNull(apiKey);
        assertFalse(apiKey.isBlank());

        var mapper = new ObjectMapper();
        var parser = new StrictLlmIntentParser(mapper);
        var chat = new GeminiIntentClient(mapper, parser, true, apiKey,
                "gemini-3.5-flash-lite", Duration.ofSeconds(10), Duration.ofSeconds(45));
        assertEquals(LlmIntent.Intent.EXPLAIN_BUDGET_STATUS,
                chat.classify("Show my configured budgets for this month.").intent());

        var bill = new GeminiDocumentExtractionService(mapper, apiKey,
                "gemini-3.5-flash-lite", Duration.ofSeconds(10),
                java.net.URI.create("https://generativelanguage.googleapis.com/v1beta/models/"));
        var result = bill.extract(new MockMultipartFile("file", "synthetic-receipt.png", "image/png", syntheticReceipt()), "transaction");
        assertEquals("gemini", result.provider());
        assertTrue(result.fields().get("merchant").toLowerCase().contains("coffee"), result.fields().toString());
        assertEquals("75000", result.fields().get("amount"), result.fields().toString());
        assertEquals("VND", result.fields().get("currency"), result.fields().toString());
    }

    private byte[] syntheticReceipt() throws Exception {
        BufferedImage image = new BufferedImage(960, 540, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 58));
            graphics.drawString("COFFEE HOUSE", 55, 105);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 42));
            graphics.drawString("Date: 2026-10-07", 55, 205);
            graphics.drawString("TOTAL: 75,000 VND", 55, 305);
            graphics.drawString("Payment: Cash", 55, 405);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "png", output));
        return output.toByteArray();
    }
}
