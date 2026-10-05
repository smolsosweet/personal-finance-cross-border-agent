package com.example.finance;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoDataService {
    private final TransactionService transactions;
    private final AccountLedgerService ledger;
    private final CrossBorderService crossBorder;
    private final PhaseFourService phaseFour;
    private final FinanceWorkspaceService financeWorkspace;

    public DemoDataService(TransactionService transactions, CrossBorderService crossBorder,
                           PhaseFourService phaseFour, FinanceWorkspaceService financeWorkspace, AccountLedgerService ledger) {
        this.transactions = transactions;
        this.ledger=ledger;
        this.crossBorder = crossBorder;
        this.phaseFour = phaseFour;
        this.financeWorkspace = financeWorkspace;
    }

    @Transactional
    public void initialize() {
        transactions.seedIfEmpty();
        ledger.migrateLegacyBalances();
        financeWorkspace.seedIfEmpty();
        crossBorder.seedIfEmpty();
        phaseFour.seedIfEmpty();
    }

    @Transactional
    public void resetAll() {
        phaseFour.lockAccountReset();
        financeWorkspace.clear();
        transactions.reset();
        crossBorder.reset();
        phaseFour.reset();
        financeWorkspace.seedIfEmpty();
    }
}
