package com.example.finance;

import java.math.BigDecimal;

/** Existing tuition type and verified synthetic registry; workspace A remains selected. */
final class GlobalAssistantFixtures {
    static int secondTuition(CrossBorderService border) {
        var a=border.selectedExpense();
        int b=border.addExpense("TUITION","Second tuition fixture",a.institution(),new BigDecimal("1000"),
                a.destinationCountry(),a.currency(),a.recipientName(),a.recipientBankName(),
                a.recipientBankCode(),a.recipientAccount(),"TUITION-B-DEMO",a.dueDate(),null,null,null);
        border.selectExpense(a.id());
        return b;
    }
}
