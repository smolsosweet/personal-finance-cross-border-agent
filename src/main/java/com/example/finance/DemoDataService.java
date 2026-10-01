package com.example.finance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoDataService {
    private final TransactionService transactions;
    private final CrossBorderService crossBorder;
    private final PhaseFourService phaseFour;
    private final FinanceWorkspaceService financeWorkspace;

    public DemoDataService(TransactionService transactions, CrossBorderService crossBorder,
                           PhaseFourService phaseFour, FinanceWorkspaceService financeWorkspace) {
        this.transactions = transactions;
        this.crossBorder = crossBorder;
        this.phaseFour = phaseFour;
        this.financeWorkspace = financeWorkspace;
    }

    @Transactional
    public void initialize() {
        transactions.seedIfEmpty();
        financeWorkspace.seedIfEmpty();
        crossBorder.seedIfEmpty();
        phaseFour.seedIfEmpty();
    }

    @Transactional
    public void resetAll() {
        phaseFour.reset();
        crossBorder.reset();
        financeWorkspace.clear();
        transactions.reset();
        financeWorkspace.seedIfEmpty();
    }
}
