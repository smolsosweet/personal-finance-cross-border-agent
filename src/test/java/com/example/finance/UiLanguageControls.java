package com.example.finance;

import com.microsoft.playwright.Page;

/** Use the one global language control, as a user would after closing the assistant. */
final class UiLanguageControls {
    private UiLanguageControls() {}

    static void select(Page page, String language) {
        boolean reopen = page.getByTestId("assistant-panel").isVisible();
        if (reopen) page.getByTestId("assistant-close").click();
        page.getByTestId("language-" + language).click();
        if (reopen) page.getByTestId("assistant-launcher").click();
    }
}
