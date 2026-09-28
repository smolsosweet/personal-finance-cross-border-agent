package com.example.finance;

import org.springframework.stereotype.Service;

@Service
public class CategorizationService {
    public static final int AUTO_THRESHOLD = 90;
    public static final int CONFIRM_THRESHOLD = 60;

    public record Suggestion(String category, int confidence, String reviewStatus, String evidence) {}

    public Suggestion categorize(TransactionService.Normalized transaction) {
        if ("Income".equals(transaction.type())) {
            return suggestion("Income", 99, "Transaction type is incoming income");
        }
        if ("Internal Transfer".equals(transaction.type())) {
            return suggestion("Transfer", 99, "Both accounts belong to the demo profile");
        }
        if ("Refund".equals(transaction.type())) {
            return suggestion("Refund", 95, "Refund marker found in the normalized description");
        }

        String merchant = transaction.merchant().toLowerCase();
        if (merchant.contains("highlands") || merchant.contains("coffee")) {
            return suggestion("Food & Drinks", 97, "Merchant rule v1 matched a known cafe");
        }
        if (merchant.contains("grab") || merchant.contains("metro")) {
            return suggestion("Transport", 94, "Merchant rule v1 matched transport");
        }
        if (merchant.contains("electricity") || merchant.contains("power")) {
            return suggestion("Utilities", 96, "Merchant rule v1 matched a utility provider");
        }
        if (merchant.contains("campus store") || merchant.contains("minimart")) {
            return suggestion("Shopping", 72, "Merchant can represent shopping or groceries");
        }
        return suggestion(null, 35, "No deterministic merchant rule matched");
    }

    public static String statusFor(int confidence) {
        if (confidence >= AUTO_THRESHOLD) return "AUTO";
        if (confidence >= CONFIRM_THRESHOLD) return "CONFIRMATION_REQUIRED";
        return "PURPOSE_REQUIRED";
    }

    private static Suggestion suggestion(String category, int confidence, String evidence) {
        return new Suggestion(category, confidence, statusFor(confidence), evidence);
    }
}
