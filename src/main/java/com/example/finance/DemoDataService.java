package com.example.finance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoDataService {
    private final TransactionService transactions;
    private final CrossBorderService crossBorder;
    private final PhaseFourService phaseFour;

    public DemoDataService(TransactionService transactions, CrossBorderService crossBorder,
                           PhaseFourService phaseFour) {
        this.transactions = transactions;
        this.crossBorder = crossBorder;
        this.phaseFour = phaseFour;
    }

    @Transactional
    public void initialize() {
        transactions.seedIfEmpty();
        crossBorder.seedIfEmpty();
        phaseFour.seedIfEmpty();
    }

    @Transactional
    public void resetAll() {
        phaseFour.reset();
        crossBorder.reset();
        transactions.reset();
    }
}
