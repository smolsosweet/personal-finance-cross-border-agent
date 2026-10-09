package com.example.finance;

import com.microsoft.playwright.Page;

/** User interactions with the final review dialog; does not call approval endpoints directly. */
final class PaymentApprovalControls {
    private PaymentApprovalControls() {}
    static void approve(Page page) {
        page.getByTestId("approve-action").click();
        page.getByTestId("payment-confirm-check").check();
        page.getByTestId("payment-confirm-submit").click();
    }
    static void cancel(Page page) {
        page.getByTestId("approve-action").click();
        page.getByTestId("payment-confirm-cancel").click();
    }
}
