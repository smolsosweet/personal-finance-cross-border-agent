package com.example.finance;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PhaseFourService {
    public static final BigDecimal PER_TX_LIMIT = new BigDecimal("500000.00");
    public static final BigDecimal DAILY_LIMIT = new BigDecimal("1000000.00");
    public static final BigDecimal SAFETY_BUFFER = new BigDecimal("3000000.00");
    public static final int FREQUENCY_LIMIT = 3;
    public static final String PAYER = "PAYER_VND";
    public static final String SCHOOL = "SCHOOL_CNY";
    public static final String SCHOOL_USD = "SCHOOL_USD";
    public static final String SCHOOL_AUD = "SCHOOL_AUD";
    public static final String EMERGENCY = "EMERGENCY-FUND";

    private final JdbcTemplate db;
    private final CrossBorderService crossBorder;
    private final TransactionService transactions;

    public PhaseFourService(JdbcTemplate db, CrossBorderService crossBorder, TransactionService transactions) {
        this.db = db;
        this.crossBorder = crossBorder;
        this.transactions = transactions;
    }

    public record Policy(String mode, String state, String runtimeMode, BigDecimal perTxLimit,
                         BigDecimal dailyLimit, int frequencyLimit, BigDecimal safetyBuffer) {}
    public record ActionPlan(String id, String actionType, Integer expenseId, String purpose,
            BigDecimal debitAmount, BigDecimal conversionAmount, BigDecimal transferFee,
            BigDecimal fxMarkup, String sourceCurrency, String sourceAccountId, BigDecimal destinationAmount,
            String destinationCurrency, String recipient, String channelId, String quoteId,
            String requiredPermission, String impact, String risk, String status,
            String actionHash, String idempotencyKey, LocalDateTime createdAt) {
        public BigDecimal feeTotal() { return transferFee.add(fxMarkup); }
    }
    public record PaymentReview(String billTitle, String institution, String paymentReference,
            LocalDate dueDate, String recipientName, String recipientBankName, String recipientBankCode,
            String recipientAccount, String sourceDisplayName, String sourceInstitution,
            String sourceMaskedNumber, String channelName, BigDecimal rate, String quoteSource,
            LocalDateTime quotedAt, LocalDateTime expiresAt, int settlementMinDays, int settlementMaxDays,
            LocalDate latestSafeDate, BigDecimal sourceBalance, BigDecimal balanceAfter,
            BigDecimal safetyBuffer) {
        public long expiresAtEpochMillis() {
            return expiresAt == null ? 0 : expiresAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
    }
    private record PaymentSnapshot(LocalDateTime billUpdatedAt, String destinationCountry, PaymentReview review) {}
    public record PolicyDecision(String decision, String reasonCode, String explanation) {}
    public record Receipt(String transactionId, String actionId, String idempotencyKey,
            String channelId, String quoteId, String recipient, String sourceAccountId, String destinationCurrency,
            BigDecimal vndBalanceBefore,
            BigDecimal vndDebit, BigDecimal conversionVnd, BigDecimal feeDeductionVnd,
            BigDecimal vndBalanceAfter, BigDecimal cnyBalanceBefore, BigDecimal cnyCredit,
            BigDecimal cnyBalanceAfter, BigDecimal rateVndPerCny, String status,
            LocalDateTime createdAt) {}
    public record PaymentSourceAccount(String accountId, String displayName, String institution,
            String accountType, String maskedNumber, String currency, BigDecimal balance,
            String connectionStatus, String verificationStatus, boolean crossBorderEnabled,
            boolean selected, String eligibilityReason) {
        public boolean ready() {
            return "CONNECTED".equals(connectionStatus) && "VERIFIED".equals(verificationStatus)
                    && crossBorderEnabled && "VND".equals(currency);
        }
        public boolean canFund(BigDecimal amount, BigDecimal safetyBuffer) {
            return ready() && balance.subtract(amount).compareTo(safetyBuffer) >= 0;
        }
    }
    public record AuditEvent(String id, LocalDateTime occurredAt, String actor,
            String eventType, String referenceId, String status, String reasonCode, String details) {}
    public record ConversationMessage(String id, String role, String message, LocalDateTime createdAt) {}

    @Transactional
    public void reset() {
        lockPaymentWorkflow();
        db.update("DELETE FROM audit_log");
        db.update("DELETE FROM sandbox_ledger_entries");
        db.update("DELETE FROM sandbox_transactions");
        db.update("DELETE FROM approvals");
        db.update("DELETE FROM action_payment_snapshots");
        db.update("DELETE FROM action_plans");
        db.update("DELETE FROM conversation_messages");
        db.update("DELETE FROM recipient_allowlist");
        db.update("DELETE FROM agent_policy");
        db.update("DELETE FROM payment_source_accounts");
        db.update("DELETE FROM sandbox_accounts");
        db.update("""
                INSERT INTO agent_policy
                (id,mode,agent_state,runtime_mode,per_transaction_limit,daily_limit,frequency_limit,safety_buffer)
                VALUES (1,'APPROVAL','ACTIVE','OFFLINE',?,?,?,?)
                """, PER_TX_LIMIT, DAILY_LIMIT, FREQUENCY_LIMIT, SAFETY_BUFFER);
        db.update("INSERT INTO recipient_allowlist VALUES (?,?,?)",
                CrossBorderService.SCHOOL_RECIPIENT, "Shenzhen Demo University", "TUITION");
        db.update("INSERT INTO recipient_allowlist VALUES (?,?,?)",
                CrossBorderService.US_SCHOOL_RECIPIENT, CrossBorderService.US_SCHOOL_NAME, "TUITION");
        db.update("INSERT INTO recipient_allowlist VALUES (?,?,?)",
                CrossBorderService.AU_SCHOOL_RECIPIENT, CrossBorderService.AU_SCHOOL_NAME, "TUITION");
        db.update("INSERT INTO recipient_allowlist VALUES (?,?,?)",
                EMERGENCY, "Emergency Fund", "LOW_RISK");
        insertPaymentAccount(PAYER,"Bank A Everyday","Bank A","Everyday account","•••• 2048",
                new BigDecimal("100000000.00"),"CONNECTED","VERIFIED",true,true,1);
        insertPaymentAccount("VCB_VND","Vietcombank Everyday","Vietcombank","Everyday account","•••• 6682",
                new BigDecimal("82000000.00"),"CONNECTED","VERIFIED",true,false,2);
        insertPaymentAccount("TCB_VND","Techcombank Everyday","Techcombank","Everyday account","•••• 1106",
                new BigDecimal("45000000.00"),"CONNECTED","VERIFIED",true,false,3);
        insertPaymentAccount("MOMO_VND","MoMo Wallet","MoMo","E-wallet","•••• 0921",
                new BigDecimal("12000000.00"),"CONNECTED","VERIFIED",true,false,4);
        insertPaymentAccount("ALIPAY_VND","Alipay Education Wallet","Alipay","Education wallet","•••• 8890",
                new BigDecimal("75000000.00"),"CONNECTED","VERIFIED",true,false,5);
        db.update("INSERT INTO sandbox_accounts VALUES ('SCHOOL_CNY','Shenzhen Demo University','CNY',0.00)");
        db.update("INSERT INTO sandbox_accounts VALUES ('SCHOOL_USD','Pacific Demo College','USD',0.00)");
        db.update("INSERT INTO sandbox_accounts VALUES ('SCHOOL_AUD','Sydney Demo Institute','AUD',0.00)");
        db.update("INSERT INTO sandbox_accounts VALUES ('EMERGENCY_VND','Emergency Fund sandbox recipient','VND',0.00)");
        addMessage("ASSISTANT", "I can explain the tuition bill, create a structured payment plan, or prepare a low-risk Emergency Fund transfer. Every amount comes from deterministic demo data.");
        audit("SYSTEM", "DEMO_RESET", null, "COMPLETED", null,
                "Phase 4 policy, Payment Sandbox, conversation and audit data reset");
    }

    @Transactional
    public void seedIfEmpty() {
        Integer count = db.queryForObject("SELECT COUNT(*) FROM agent_policy", Integer.class);
        if (count == null || count == 0) {
            reset();
            return;
        }
        Integer sources = db.queryForObject("""
                SELECT COUNT(*) FROM payment_source_accounts
                WHERE account_id IN ('PAYER_VND','VCB_VND','TCB_VND','MOMO_VND','ALIPAY_VND')
                """, Integer.class);
        Integer destinationAccounts = db.queryForObject("""
                SELECT COUNT(*) FROM sandbox_accounts WHERE id IN ('SCHOOL_CNY','SCHOOL_USD','SCHOOL_AUD')
                """, Integer.class);
        if (sources == null || sources != 5 || destinationAccounts == null || destinationAccounts != 3) {
            reset();
        }
    }

    private void insertPaymentAccount(String id, String displayName, String institution,
            String accountType, String maskedNumber, BigDecimal balance, String connectionStatus,
            String verificationStatus, boolean crossBorderEnabled, boolean selected, int order) {
        db.update("INSERT INTO sandbox_accounts VALUES (?,?,?,?)",id,displayName,"VND",balance);
        db.update("""
                INSERT INTO payment_source_accounts
                (account_id,institution,account_type,masked_number,connection_status,
                 verification_status,cross_border_enabled,selected,display_order)
                VALUES (?,?,?,?,?,?,?,?,?)
                """,id,institution,accountType,maskedNumber,connectionStatus,verificationStatus,
                crossBorderEnabled,selected,order);
    }

    public Policy policy() {
        return db.queryForObject("""
                SELECT mode,agent_state,runtime_mode,per_transaction_limit,daily_limit,frequency_limit,safety_buffer
                FROM agent_policy WHERE id=1
                """, (rs,n) -> new Policy(rs.getString(1),rs.getString(2),rs.getString(3),rs.getBigDecimal(4),
                rs.getBigDecimal(5),rs.getInt(6),rs.getBigDecimal(7)));
    }

    @Transactional
    public void setMode(String requested) {
        lockPaymentWorkflow();
        String mode = requested == null ? "" : requested.trim().toUpperCase(Locale.ROOT);
        if (!List.of("APPROVAL","DELEGATED").contains(mode))
            throw new IllegalArgumentException("Mode must be APPROVAL or DELEGATED");
        db.update("UPDATE agent_policy SET mode=? WHERE id=1", mode);
        audit("USER","POLICY_MODE_CHANGED","POLICY-1","COMPLETED",null,"Mode changed to "+mode);
    }

    @Transactional
    public void emergencyStop() {
        lockPaymentWorkflow();
        db.update("UPDATE agent_policy SET agent_state='PAUSED' WHERE id=1");
        audit("USER","EMERGENCY_STOP","POLICY-1","COMPLETED","AGENT PAUSED",explanation("AGENT PAUSED"));
    }

    @Transactional
    public void resumeAgent() {
        lockPaymentWorkflow();
        db.update("UPDATE agent_policy SET agent_state='ACTIVE' WHERE id=1");
        audit("USER","AGENT_RESUMED","POLICY-1","COMPLETED",null,"Agent resumed by demo user");
    }

    @Transactional
    public void enableOfflineFallback() {
        lockPaymentWorkflow();
        db.update("UPDATE agent_policy SET runtime_mode='OFFLINE' WHERE id=1");
        audit("SYSTEM","OFFLINE_FALLBACK_ENABLED","POLICY-1","COMPLETED",null,
                "Deterministic local responses are active; no LLM or network dependency is required");
    }

    @Transactional
    public String sendMessage(String raw) {
        lockPaymentWorkflow();
        String message = raw == null ? "" : raw.trim();
        if (message.isBlank()) throw new IllegalArgumentException("Message is required");
        if (message.length()>500) throw new IllegalArgumentException("Message must be 500 characters or fewer");
        addMessage("USER",message);
        audit("USER","CONVERSATION_INPUT",null,"RECEIVED",null,"Untrusted user message recorded");
        if (isInjection(message)) {
            String response="I ignored that instruction. Policy, recipient, quote and fees only come from trusted application data.";
            addMessage("ASSISTANT",response);            audit("POLICY_GUARD","INPUT_BLOCKED",null,"BLOCKED","UNTRUSTED INSTRUCTION",
                    explanation("UNTRUSTED INSTRUCTION"));
            return response;
        }
        String lower=message.toLowerCase(Locale.ROOT);
        if (lower.contains("tuition")||lower.contains("học phí")||lower.contains("payment plan")) {
            ActionPlan plan=createTuitionPlan("BANK_A");
            var bill=crossBorder.bill();
            String response="Created student-payment plan "+plan.id()+" using Bank A. The "
                    +bill.amount().toPlainString()+" "+bill.currency()+" payment remains in Approval Mode.";
            addMessage("ASSISTANT",response);
            return response;
        }
        if (lower.contains("emergency")||lower.contains("low-risk")||lower.contains("low risk")) {
            ActionPlan plan=createLowRiskPlan(new BigDecimal("250000.00"));
            String response="Created low-risk action "+plan.id()+" for 250,000 VND. Status: "+plan.status()+".";
            addMessage("ASSISTANT",response);
            return response;
        }
        if (lower.contains("surplus")||lower.contains("balance")) {
            BigDecimal surplus=(BigDecimal)transactions.dashboard().get("surplus");
            String response="The deterministic surplus is "+surplus.toPlainString()+" VND after the 3,000,000 VND safety buffer.";
            addMessage("ASSISTANT",response);
            return response;
        }
        var bill=crossBorder.bill();
        String response="The synthetic tuition bill is "+bill.amount().toPlainString()+" "+bill.currency()
                +" for "+bill.institution()+", due "+bill.dueDate()+". I only use stored bill and quote data.";
        addMessage("ASSISTANT",response);
        return response;
    }

    private static boolean isInjection(String value) {
        String s=value.toLowerCase(Locale.ROOT);
        return s.contains("ignore policy")||s.contains("bypass policy")||s.contains("change recipient")
                ||s.contains("override recipient")||s.contains("invent rate")
                ||s.contains("ignore approval")||s.contains("system prompt");
    }

    @Transactional
    public ActionPlan createTuitionPlan(String channelId) {
        lockPaymentWorkflow();
        var quote=crossBorder.rankedQuotes().stream().filter(q->q.channelId().equals(channelId))
                .findFirst().orElseThrow(()->new IllegalArgumentException("Unknown payment channel"));
        return createTuitionPlan(channelId, quote.sourceAccountId()==null?PAYER:quote.sourceAccountId());
    }

    @Transactional
    public ActionPlan createTuitionPlan(String channelId, String sourceAccountId) {
        lockPaymentWorkflow();
        var expense=crossBorder.selectedExpense();
        var quote=crossBorder.rankedQuotesForExpense(expense.id()).stream()
                .filter(q->q.channelId().equals(channelId))
                .findFirst().orElseThrow(()->new IllegalArgumentException("Unknown payment channel"));
        return createTuitionPlan(channelId, sourceAccountId, expense.id(), expense.updatedAt().toString(), quote.quoteId());
    }

    @Transactional
    public ActionPlan createTuitionPlan(String channelId, String sourceAccountId, int expenseId,
                                        String billVersion, String quoteId) {
        lockPaymentWorkflow();
        var expense=crossBorder.expense(expenseId);
        if (!expense.active() || expense.executed())
            throw new IllegalArgumentException("This bill is inactive or already paid; open its existing payment record instead");
        LocalDateTime requestedVersion;
        try { requestedVersion=LocalDateTime.parse(billVersion); }
        catch (Exception ex) { throw new IllegalArgumentException("Bill version is missing or invalid; reload comparison before creating a plan"); }
        if (!expense.selected() || !expense.updatedAt().equals(requestedVersion))
            throw new IllegalArgumentException("The selected bill changed; reload comparison before creating a plan");
        PaymentSourceAccount source=paymentSource(sourceAccountId);
        var quote=crossBorder.rankedQuotesForExpense(expenseId).stream()
                .filter(q->q.channelId().equals(channelId) && q.quoteId().equals(quoteId))
                .findFirst().orElseThrow(()->new IllegalArgumentException("The FX quote changed; refresh comparison and create a new plan"));
        if (quote.sourceAccountId()!=null && !source.accountId().equals(quote.sourceAccountId()))
            throw new IllegalArgumentException("The source account does not belong to this payment channel");
        var bill=crossBorder.bill(expenseId);
        for (ActionPlan pending : plansForExpense(expenseId)) {
            if (terminal(pending.status())) continue;
            PaymentSnapshot stored=paymentSnapshot(pending);
            if (source.accountId().equals(pending.sourceAccountId())
                    && channelId.equals(pending.channelId()) && quoteId.equals(pending.quoteId())
                    && stored!=null && stored.billUpdatedAt().equals(expense.updatedAt())
                    && pending.debitAmount().compareTo(quote.landedCost())==0
                    && pending.destinationAmount().compareTo(bill.amount())==0
                    && bill.recipientAccount().equals(pending.recipient())) {
                audit("USER","ACTION_REOPENED",pending.id(),"COMPLETED",null,
                        "Existing matching pending plan reopened; no new payment was created");
                return applyInitialPolicy(pending);
            }
        }
        invalidateOtherPlans(expenseId);
        String id=newId("ACT");
        String impact="Debit "+quote.landedCost().toPlainString()+" VND; convert "
                +quote.sourceAmount().toPlainString()+" VND; credit "
                +quote.expectedReceived().toPlainString()+" "+bill.currency();
        ActionPlan plan=insertPlan(id,"TUITION",bill.id(),"Pay "+expense.title()+" · "+bill.paymentReference(),
                quote.landedCost(),quote.sourceAmount(),quote.transferFee(),quote.fxMarkup(),"VND",source.accountId(),
                quote.expectedReceived(),bill.currency(),bill.recipientAccount(),quote.channelId(),quote.quoteId(),
                "APPROVAL",impact,"Quote, beneficiary profile and approval are rechecked before execution");
        db.update("""
                INSERT INTO action_payment_snapshots
                (action_id,bill_updated_at,bill_title,institution,payment_reference,due_date,destination_country,
                 recipient_name,recipient_bank_name,recipient_bank_code,recipient_account,source_display_name,
                 source_institution,source_masked_number,channel_name,rate,quote_source,quoted_at,expires_at,
                 settlement_min_days,settlement_max_days,latest_safe_date)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,id,expense.updatedAt(),expense.title(),bill.institution(),bill.paymentReference(),bill.dueDate(),
                bill.destinationCountry(),bill.recipientName(),bill.recipientBankName(),bill.recipientBankCode(),
                bill.recipientAccount(),source.displayName(),source.institution(),source.maskedNumber(),quote.displayName(),
                quote.rateVndPerCny(),quote.quoteSource(),quote.quotedAt(),quote.expiresAt(),quote.settlementMinDays(),
                quote.settlementMaxDays(),quote.latestSafeDate());
        db.update("UPDATE action_plans SET action_hash=? WHERE id=?",currentHash(plan),plan.id());
        auditTuitionEvidence(plan,bill,quote);
        audit("PAYMENT_SOURCE","SOURCE_ACCOUNT_SELECTED",plan.id(),"SELECTED",null,
                source.institution()+" "+source.maskedNumber()+"; balance "+source.balance().toPlainString()+" VND");
        return applyInitialPolicy(plan);
    }

    private boolean terminal(String status) {
        return List.of("COMPLETED","INVALIDATED","CANCELED").contains(status);
    }

    private List<ActionPlan> plansForExpense(int expenseId) {
        return db.query("SELECT id FROM action_plans WHERE expense_id=? ORDER BY created_at DESC,id",(rs,n)->rs.getString(1),expenseId)
                .stream().map(this::action).toList();
    }

    private void invalidateOtherPlans(int expenseId) {
        for (ActionPlan pending : plansForExpense(expenseId)) {
            if (terminal(pending.status())) continue;
            db.update("UPDATE action_plans SET status='INVALIDATED',risk=? WHERE id=?",
                    "A replacement channel or quote was selected; this plan can no longer execute",pending.id());
            db.update("UPDATE approvals SET status='REVOKED' WHERE action_id=? AND status='VALID'",pending.id());
            audit("POLICY_GUARD","ACTION_INVALIDATED",pending.id(),"INVALIDATED","PLAN REPLACED",
                    "A replacement payment plan was created for the same bill; approval must be granted again");
        }
    }

    private void auditTuitionEvidence(ActionPlan plan, CrossBorderService.TuitionBill bill,
            CrossBorderService.ChannelQuote selectedQuote) {
        audit("CROSS_BORDER_SERVICE","TUITION_BILL_SELECTED",plan.id(),"SELECTED",null,
                "Bill "+bill.id()+"; reference "+bill.paymentReference()+"; institution "
                        +bill.institution()+"; amount "+bill.amount().toPlainString()+" "
                        +bill.currency()+"; recipient "+bill.recipientAccount()+"; due "+bill.dueDate());

        var verification=crossBorder.verifyRecipient(bill.id());
        audit("EDUCATION_PROVIDER_REGISTRY","RECIPIENT_VERIFICATION",plan.id(),
                verification.verified()?"VERIFIED":"BLOCKED",
                verification.verified()?null:"RECIPIENT MISMATCH",
                verification.reason()+"; registry institution "+value(verification.registryInstitution())
                        +"; registry bank "+value(verification.registryBankName())
                        +"; bank code "+value(verification.registryBankCode())
                        +"; registry account "+value(verification.registryAccount()));

        String comparison=crossBorder.rankedQuotesForExpense(bill.id()).stream()
                .map(option->option.channelId()+"="+(option.eligible()?"ELIGIBLE":"UNAVAILABLE")
                        +", landed cost "+option.landedCost().toPlainString()+" VND")
                .collect(Collectors.joining("; "));
        audit("CROSS_BORDER_SERVICE","CHANNEL_COMPARISON",plan.id(),"COMPLETED",null,comparison);

        audit("FX_QUOTE_SERVICE","FX_QUOTE_SELECTED",plan.id(),
                selectedQuote.eligible()?"ELIGIBLE":"BLOCKED",
                selectedQuote.eligible()?null:"CHANNEL NOT AVAILABLE",
                "Quote "+selectedQuote.quoteId()+" from "+selectedQuote.quoteSource()
                        +"; quoted at "+selectedQuote.quotedAt()+"; expires at "+selectedQuote.expiresAt()
                        +"; rate "+selectedQuote.rateVndPerCny().toPlainString()+" VND/"+bill.currency()
                        +"; transfer fee "+selectedQuote.transferFee().toPlainString()+" VND"
                        +"; FX markup "+selectedQuote.fxMarkup().toPlainString()+" VND"
                        +"; landed cost "+selectedQuote.landedCost().toPlainString()+" VND"
                        +"; expected received "+selectedQuote.expectedReceived().toPlainString()+" "+bill.currency());
    }

    @Transactional
    public ActionPlan createLowRiskPlan(BigDecimal amount) {
        lockPaymentWorkflow();
        if(amount==null||amount.signum()<=0) throw new IllegalArgumentException("Amount must be positive");
        BigDecimal normalized=amount.setScale(2);
        String id=newId("ACT");
        ActionPlan plan=insertPlan(id,"LOW_RISK",null,"Move funds to Emergency Fund",normalized,normalized,
                BigDecimal.ZERO.setScale(2),BigDecimal.ZERO.setScale(2),"VND",PAYER,normalized,"VND",
                EMERGENCY,null,null,policy().mode().equals("DELEGATED")?"DELEGATED":"APPROVAL",
                "Debit "+normalized.toPlainString()+" VND and credit the allowlisted Emergency Fund",
                "Subject to limit, frequency and safety buffer");
        plan=applyInitialPolicy(plan);
        if("DELEGATED".equals(plan.requiredPermission())&&"POLICY_ALLOWED".equals(plan.status())) {
            execute(plan.id());
            return action(plan.id());
        }
        return plan;
    }

    private ActionPlan insertPlan(String id,String type,Integer expenseId,String purpose,BigDecimal debit,
            BigDecimal conversion,BigDecimal fee,BigDecimal markup,String sourceCurrency,String sourceAccountId,
            BigDecimal destination,String destinationCurrency,String recipient,String channelId,
            String quoteId,String permission,String impact,String risk) {
        LocalDateTime now=LocalDateTime.now();
        String idem="IDEMP-"+id;
        String hash=hash(id,type,purpose,debit,destination,recipient,channelId,quoteId,sourceAccountId);
        db.update("""
                INSERT INTO action_plans
                (id,action_type,expense_id,purpose,debit_amount,conversion_amount,transfer_fee,fx_markup,
                 source_currency,source_account_id,destination_amount,destination_currency,recipient,channel_id,quote_id,
                 required_permission,impact,risk,status,action_hash,idempotency_key,created_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,id,type,expenseId,purpose,debit,conversion,fee,markup,sourceCurrency,sourceAccountId,destination,
                destinationCurrency,recipient,channelId,quoteId,permission,impact,risk,
                "PLANNED",hash,idem,now);
        audit("AGENT_ORCHESTRATOR","ACTION_PLANNED",id,"COMPLETED",null,
                "Structured action plan created from deterministic application data");
        return action(id);
    }

    private ActionPlan applyInitialPolicy(ActionPlan plan) {
        PolicyDecision d=evaluate(plan,false);
        String status=switch(d.decision()) {
            case "BLOCKED"->"BLOCKED";
            case "APPROVAL_REQUIRED", "DEADLINE_RISK"->"AWAITING_APPROVAL";
            default->"POLICY_ALLOWED";
        };
        db.update("UPDATE action_plans SET status=? WHERE id=?",status,plan.id());
        if ("DEADLINE RISK".equals(d.reasonCode()) && !plan.risk().contains("DEADLINE RISK")) {
            db.update("UPDATE action_plans SET risk='DEADLINE RISK: ' || risk WHERE id=?", plan.id());
        }
        audit("POLICY_GUARD","POLICY_CHECKED",plan.id(),d.decision(),d.reasonCode(),d.explanation());
        return action(plan.id());
    }

    public PolicyDecision evaluate(ActionPlan plan,boolean execution) {
        Policy p=policy();
        if("INVALIDATED".equals(plan.status())) return blocked("ACTION INVALIDATED");
        if("CANCELED".equals(plan.status())) return blocked("ACTION CANCELED");
        if("PAUSED".equals(p.state())) return blocked("AGENT PAUSED");
        if(!recipientAllowed(plan.recipient(),plan.actionType())) return blocked("RECIPIENT NOT ALLOWED");
        if(!sourceAccountEligible(plan.sourceAccountId())) return blocked("SOURCE ACCOUNT NOT ELIGIBLE");
        if(accountBalance(plan.sourceAccountId()).subtract(plan.debitAmount()).compareTo(p.safetyBuffer())<0)
            return blocked("INSUFFICIENT SAFE BALANCE");
        if("TUITION".equals(plan.actionType())) {
            if (plan.expenseId()==null) return blocked("ACTION INVALIDATED");
            CrossBorderService.StudentExpense expense;
            try { expense=crossBorder.expense(plan.expenseId()); }
            catch (IllegalArgumentException ex) { return blocked("ACTION INVALIDATED"); }
            if (expense.executed()) return blocked("BILL ALREADY PAID");
            if (!expense.active()) return blocked("ACTION INVALIDATED");
            var bill=crossBorder.bill(plan.expenseId());
            PaymentSnapshot snapshot=paymentSnapshot(plan);
            if (snapshot==null) return blocked("PLAN SNAPSHOT MISSING");
            var profile=crossBorder.profile();
            if(!"Vietnam".equals(profile.sourceCountry())
                    ||!crossBorder.corridorSupported(bill.destinationCountry(),bill.currency()))
                return blocked("CORRIDOR NOT ALLOWED");
            if(!"VND".equals(profile.sourceCurrency()) ||!"VND".equals(plan.sourceCurrency())
                    ||!bill.currency().equals(plan.destinationCurrency()))
                return blocked("CURRENCY NOT ALLOWED");
            var verified=crossBorder.verifyRecipient(plan.expenseId());
            if(!verified.verified()||!bill.recipientAccount().equals(plan.recipient()))
                return blocked("RECIPIENT MISMATCH");
            if (snapshot!=null && (!snapshot.billUpdatedAt().equals(expense.updatedAt())
                    || !snapshotMatchesBill(snapshot,bill))) return blocked("ACTION INVALIDATED");
            var quote=crossBorder.rankedQuotesForExpense(plan.expenseId()).stream()
                    .filter(q->q.channelId().equals(plan.channelId())).findFirst().orElse(null);
            if(quote==null) return blocked("FX QUOTE EXPIRED");
            if(!quote.eligible()) return blocked("CHANNEL NOT AVAILABLE");
            if(!plan.sourceAccountId().equals(quote.sourceAccountId()))
                return blocked("SOURCE ACCOUNT CHANNEL MISMATCH");
            if(!quote.quoteId().equals(plan.quoteId())||quote.expired()) return blocked("FX QUOTE EXPIRED");
            if(quote.landedCost().compareTo(plan.debitAmount())!=0
                    ||quote.sourceAmount().compareTo(plan.conversionAmount())!=0
                    ||quote.transferFee().compareTo(plan.transferFee())!=0
                    ||quote.fxMarkup().compareTo(plan.fxMarkup())!=0
                    ||quote.expectedReceived().compareTo(plan.destinationAmount())!=0)
                return blocked("APPROVAL EXPIRED");
            if (snapshot!=null && (snapshot.review().rate().compareTo(quote.rateVndPerCny())!=0
                    || !snapshot.review().quotedAt().equals(quote.quotedAt())
                    || !snapshot.review().expiresAt().equals(quote.expiresAt())
                    || snapshot.review().settlementMaxDays()!=quote.settlementMaxDays()))
                return blocked("FX QUOTE EXPIRED");
            boolean deadlineRisk=LocalDate.now()
                    .plusDays(quote.settlementMaxDays()+CrossBorderService.SETTLEMENT_SAFETY_MARGIN_DAYS)
                    .isAfter(bill.dueDate());
            if(deadlineRisk) {
                if(!execution) return decision("DEADLINE_RISK","DEADLINE RISK");
                return validApproval(plan)?decision("ALLOWED",null):blocked("APPROVAL EXPIRED");
            }
            if(!execution) return decision("APPROVAL_REQUIRED","APPROVAL REQUIRED");
            return validApproval(plan)?decision("ALLOWED",null):blocked("APPROVAL EXPIRED");
        }
        if(!"VND".equals(plan.sourceCurrency()) || !"VND".equals(plan.destinationCurrency()))
            return blocked("CURRENCY NOT ALLOWED");
        if (!"LOW_RISK".equals(plan.actionType()) || !EMERGENCY.equals(plan.recipient())
                || plan.debitAmount().signum()<=0 || plan.debitAmount().compareTo(plan.destinationAmount())!=0
                || plan.debitAmount().compareTo(plan.conversionAmount())!=0 || plan.feeTotal().signum()!=0)
            return blocked("APPROVAL EXPIRED");
        if(plan.debitAmount().compareTo(p.perTxLimit())>0) return blocked("LIMIT PER TX");
        BigDecimal total=db.queryForObject("""
                SELECT COALESCE(SUM(s.vnd_debit),0) FROM sandbox_transactions s
                JOIN action_plans a ON a.id=s.action_id
                WHERE a.action_type='LOW_RISK' AND s.created_at>=?
                """,BigDecimal.class,LocalDate.now().atStartOfDay());
        if(total.add(plan.debitAmount()).compareTo(p.dailyLimit())>0) return blocked("LIMIT DAILY");
        Integer count=db.queryForObject("""
                SELECT COUNT(*) FROM sandbox_transactions s JOIN action_plans a ON a.id=s.action_id
                WHERE a.action_type='LOW_RISK' AND s.created_at>=?
                """,Integer.class,LocalDate.now().atStartOfDay());
        if(count!=null&&count>=p.frequencyLimit()) return blocked("LIMIT DAILY");
        if("DELEGATED".equals(p.mode())&&"DELEGATED".equals(plan.requiredPermission()))
            return decision("ALLOWED",null);
        if(!execution) return decision("APPROVAL_REQUIRED","APPROVAL REQUIRED");
        return validApproval(plan)?decision("ALLOWED",null):blocked("APPROVAL EXPIRED");
    }

    @Transactional
    public ActionPlan approve(String actionId) {
        lockPaymentWorkflow();
        ActionPlan plan=action(actionId);
        if (receiptForAction(actionId)!=null) return plan;
        PolicyDecision check=evaluate(plan,false);
        if("BLOCKED".equals(check.decision())) return block(plan,check);
        LocalDateTime now=LocalDateTime.now();
        db.update("DELETE FROM approvals WHERE action_id=?",plan.id());
        db.update("""
                INSERT INTO approvals
                (action_id,approved_hash,approved_by,approved_at,expires_at,status)
                VALUES (?,?,?,?,?,'VALID')
                """,plan.id(),currentHash(plan),"DEMO_USER",now,now.plusMinutes(5));
        db.update("UPDATE action_plans SET status='APPROVED' WHERE id=?",plan.id());
        audit("USER","ACTION_APPROVED",plan.id(),"COMPLETED",null,
                "Approval bound to action ID, amount, recipient, quote and five-minute expiry");
        return action(plan.id());
    }

    @Transactional
    public Receipt approveAndExecute(String actionId) {
        ActionPlan approved=approve(actionId);
        return List.of("BLOCKED","INVALIDATED","CANCELED").contains(approved.status())?null:execute(actionId);
    }

    @Transactional
    public Receipt execute(String actionId) {
        lockPaymentWorkflow();
        ActionPlan plan=action(actionId);
        List<Receipt> prior=receiptsByIdempotency(plan.idempotencyKey());
        if(!prior.isEmpty()) {
            audit("PAYMENT_SANDBOX","IDEMPOTENT_RETRY",plan.id(),"COMPLETED","DUPLICATE ACTION",
                    "Existing receipt returned; balances were not changed");
            return prior.getFirst();
        }
        PolicyDecision check=evaluate(plan,true);
        audit("POLICY_GUARD","EXECUTION_POLICY_CHECKED",plan.id(),check.decision(),
                check.reasonCode(),check.explanation());
        if("BLOCKED".equals(check.decision())) { block(plan,check); return null; }

        BigDecimal payerBefore=accountBalance(plan.sourceAccountId());
        String target="TUITION".equals(plan.actionType())
                ?destinationSandboxAccount(plan.destinationCurrency()):"EMERGENCY_VND";
        BigDecimal targetBefore=accountBalance(target);
        BigDecimal payerAfter=payerBefore.subtract(plan.debitAmount());
        BigDecimal targetAfter=targetBefore.add(plan.destinationAmount());
        PaymentSnapshot snapshot=paymentSnapshot(plan);
        BigDecimal rate="TUITION".equals(plan.actionType())
                ? snapshot!=null?snapshot.review().rate():plan.conversionAmount()
                        .divide(plan.destinationAmount(),4,java.math.RoundingMode.HALF_UP)
                : BigDecimal.ONE.setScale(4);
        String txId=newId("SBOX");
        LocalDateTime now=LocalDateTime.now();
        db.update("UPDATE sandbox_accounts SET balance=? WHERE id=?",payerAfter,plan.sourceAccountId());
        db.update("UPDATE sandbox_accounts SET balance=? WHERE id=?",targetAfter,target);
        db.update("""
                INSERT INTO sandbox_transactions
                (id,action_id,idempotency_key,channel_id,quote_id,recipient,source_account_id,vnd_balance_before,
                 vnd_debit,conversion_vnd,fee_deduction_vnd,vnd_balance_after,cny_balance_before,
                 cny_credit,cny_balance_after,rate_vnd_per_cny,status,created_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,txId,plan.id(),plan.idempotencyKey(),plan.channelId(),plan.quoteId(),plan.recipient(),
                plan.sourceAccountId(),payerBefore,plan.debitAmount(),plan.conversionAmount(),plan.feeTotal(),payerAfter,
                targetBefore,plan.destinationAmount(),targetAfter,rate,"COMPLETED",now);
        ledger(txId,"VND_DEBIT",plan.sourceAccountId(),plan.debitAmount().negate(),"VND");
        ledger(txId,"CONVERSION",plan.sourceAccountId(),plan.conversionAmount(),plan.sourceCurrency());
        ledger(txId,"FEE_DEDUCTION",plan.sourceAccountId(),plan.feeTotal().negate(),"VND");
        ledger(txId,plan.destinationCurrency()+"_CREDIT",target,plan.destinationAmount(),plan.destinationCurrency());
        db.update("UPDATE action_plans SET status='COMPLETED' WHERE id=?",plan.id());
        db.update("UPDATE approvals SET status='USED' WHERE action_id=? AND status='VALID'",plan.id());
        audit("PAYMENT_SANDBOX","SANDBOX_EXECUTED",plan.id(),"COMPLETED",null,
                "Created transaction "+txId+" with VND debit and "+plan.destinationCurrency()+" credit");
        audit("PAYMENT_SANDBOX","PAYMENT_RECEIPT_CREATED",plan.id(),"COMPLETED",null,
                "Receipt transaction ID "+txId+"; idempotency key "+plan.idempotencyKey()
                +"; source account "+plan.sourceAccountId()+"; channel "+value(plan.channelId())+"; quote "+value(plan.quoteId()));
        return receipt(txId);
    }

    private void ledger(String tx,String type,String account,BigDecimal amount,String currency) {
        db.update("INSERT INTO sandbox_ledger_entries VALUES (?,?,?,?,?,?,?)",
                newId("LEDGER"),tx,type,account,amount,currency,LocalDateTime.now());
    }

    private String destinationSandboxAccount(String currency) {
        return switch (currency) {
            case "CNY" -> SCHOOL;
            case "USD" -> SCHOOL_USD;
            case "AUD" -> SCHOOL_AUD;
            default -> throw new IllegalArgumentException("No sandbox recipient account for " + currency);
        };
    }

    private ActionPlan block(ActionPlan plan,PolicyDecision d) {
        String status=switch (d.reasonCode()) {
            case "ACTION INVALIDATED" -> "INVALIDATED";
            case "ACTION CANCELED" -> "CANCELED";
            default -> "BLOCKED";
        };
        db.update("UPDATE action_plans SET status=? WHERE id=?",status,plan.id());
        db.update("UPDATE approvals SET status='REVOKED' WHERE action_id=? AND status='VALID'",plan.id());
        audit("POLICY_GUARD","ACTION_BLOCKED",plan.id(),"BLOCKED",d.reasonCode(),d.explanation());
        return action(plan.id());
    }

    private boolean validApproval(ActionPlan plan) {
        List<Map<String,Object>> rows=db.queryForList(
                "SELECT approved_hash,expires_at,status FROM approvals WHERE action_id=?",plan.id());
        if(rows.isEmpty()) return false;
        Map<String,Object> row=rows.getFirst();
        LocalDateTime expiry=((java.sql.Timestamp)row.get("expires_at")).toLocalDateTime();
        return "VALID".equals(row.get("status"))&&LocalDateTime.now().isBefore(expiry)
                &&currentHash(plan).equals(row.get("approved_hash"));
    }

    private String currentHash(ActionPlan p) {
        String identity=sha256(hash(p.id(),p.actionType(),p.purpose(),p.debitAmount(),p.destinationAmount(),
                p.recipient(),p.channelId(),p.quoteId(),p.sourceAccountId())+"|"+p.sourceCurrency()
                +"|"+p.destinationCurrency()+"|"+p.conversionAmount()+"|"+p.transferFee()+"|"+p.fxMarkup()
                +"|"+p.expenseId()+"|"+p.requiredPermission());
        PaymentSnapshot snapshot=paymentSnapshot(p);
        return snapshot==null?identity:sha256(identity+"|"+snapshot.billUpdatedAt()+"|"+snapshot.destinationCountry()
                +"|"+snapshot.review().institution()+"|"+snapshot.review().recipientName()+"|"+snapshot.review().recipientBankName()
                +"|"+snapshot.review().recipientBankCode()+"|"+snapshot.review().recipientAccount()
                +"|"+snapshot.review().paymentReference()+"|"+snapshot.review().dueDate()
                +"|"+snapshot.review().rate()+"|"+snapshot.review().quotedAt()+"|"+snapshot.review().expiresAt());
    }

    private boolean recipientAllowed(String recipient,String type) {        Integer count=db.queryForObject(
                "SELECT COUNT(*) FROM recipient_allowlist WHERE recipient_id=? AND purpose=?",
                Integer.class,recipient,type);
        return count!=null&&count>0;
    }

    private BigDecimal accountBalance(String id) {
        return db.queryForObject("SELECT balance FROM sandbox_accounts WHERE id=?",BigDecimal.class,id);
    }

    public ActionPlan action(String id) {
        return db.queryForObject("""
                SELECT id,action_type,expense_id,purpose,debit_amount,conversion_amount,transfer_fee,fx_markup,
                       source_currency,COALESCE(source_account_id,'PAYER_VND'),destination_amount,destination_currency,recipient,channel_id,quote_id,
                       required_permission,impact,risk,status,action_hash,idempotency_key,created_at
                FROM action_plans WHERE id=?
                """,(rs,n)->new ActionPlan(rs.getString(1),rs.getString(2),rs.getObject(3,Integer.class),rs.getString(4),
                rs.getBigDecimal(5),rs.getBigDecimal(6),rs.getBigDecimal(7),rs.getBigDecimal(8),
                rs.getString(9),rs.getString(10),rs.getBigDecimal(11),rs.getString(12),rs.getString(13),
                rs.getString(14),rs.getString(15),rs.getString(16),rs.getString(17),
                rs.getString(18),rs.getString(19),rs.getString(20),rs.getString(21),
                rs.getTimestamp(22).toLocalDateTime()),id);
    }

    private void lockPaymentWorkflow() {
        db.query("SELECT id FROM agent_policy WHERE id=1 FOR UPDATE",(rs,n)->rs.getInt(1));
    }

    @Transactional
    public ActionPlan cancel(String actionId) {
        lockPaymentWorkflow();
        ActionPlan plan=action(actionId);
        if ("COMPLETED".equals(plan.status()) || receiptForAction(actionId)!=null)
            throw new IllegalArgumentException("A completed payment cannot be canceled; its receipt and audit history are immutable");
        if ("CANCELED".equals(plan.status())) return plan;
        if ("INVALIDATED".equals(plan.status())) return plan;
        db.update("UPDATE action_plans SET status='CANCELED' WHERE id=?",actionId);
        db.update("UPDATE approvals SET status='REVOKED' WHERE action_id=? AND status='VALID'",actionId);
        audit("USER","ACTION_CANCELED",actionId,"CANCELED","ACTION CANCELED",
                "User canceled the unexecuted payment plan; no funds were debited");
        return action(actionId);
    }

    public List<ActionPlan> recentPlans() {
        return db.query("SELECT id FROM action_plans ORDER BY created_at DESC,id FETCH FIRST 30 ROWS ONLY",(rs,n)->rs.getString(1))
                .stream().map(this::action).toList();
    }

    public Map<Integer,ActionPlan> completedPlansByExpense() {
        return db.query("""
                SELECT a.id FROM action_plans a
                WHERE a.action_type='TUITION' AND a.expense_id IS NOT NULL AND a.status='COMPLETED'
                  AND EXISTS (SELECT 1 FROM sandbox_transactions s WHERE s.action_id=a.id AND s.status='COMPLETED')
                ORDER BY a.created_at DESC,a.id
                """,(rs,n)->rs.getString(1)).stream().map(this::action)
                .collect(Collectors.toMap(ActionPlan::expenseId,plan->plan,(latest,older)->latest));
    }

    public Receipt receiptForAction(String actionId) {
        if (actionId==null) return null;
        List<String> ids=db.query("SELECT id FROM sandbox_transactions WHERE action_id=?",(rs,n)->rs.getString(1),actionId);
        return ids.isEmpty()?null:receipt(ids.getFirst());
    }

    private boolean snapshotMatchesBill(PaymentSnapshot snapshot, CrossBorderService.TuitionBill bill) {
        PaymentReview review=snapshot.review();
        return snapshot.destinationCountry().equals(bill.destinationCountry())
                && review.institution().equals(bill.institution())
                && review.paymentReference().equals(bill.paymentReference()) && review.dueDate().equals(bill.dueDate())
                && review.recipientName().equals(bill.recipientName())
                && review.recipientBankName().equals(bill.recipientBankName())
                && review.recipientBankCode().equals(bill.recipientBankCode())
                && review.recipientAccount().equals(bill.recipientAccount());
    }

    private PaymentSnapshot paymentSnapshot(ActionPlan plan) {
        List<PaymentSnapshot> snapshots=db.query("""
                SELECT bill_updated_at,destination_country,bill_title,institution,payment_reference,due_date,
                       recipient_name,recipient_bank_name,recipient_bank_code,recipient_account,
                       source_display_name,source_institution,source_masked_number,channel_name,rate,quote_source,
                       quoted_at,expires_at,settlement_min_days,settlement_max_days,latest_safe_date
                FROM action_payment_snapshots WHERE action_id=?
                """,(rs,n)->new PaymentSnapshot(rs.getTimestamp(1).toLocalDateTime(),rs.getString(2),
                new PaymentReview(rs.getString(3),rs.getString(4),rs.getString(5),rs.getObject(6,LocalDate.class),
                rs.getString(7),rs.getString(8),rs.getString(9),rs.getString(10),rs.getString(11),rs.getString(12),
                rs.getString(13),rs.getString(14),rs.getBigDecimal(15),rs.getString(16),
                rs.getTimestamp(17).toLocalDateTime(),rs.getTimestamp(18).toLocalDateTime(),rs.getInt(19),rs.getInt(20),
                rs.getObject(21,LocalDate.class),null,null,null)),plan.id());
        return snapshots.isEmpty()?null:snapshots.getFirst();
    }

    public PaymentReview paymentReview(ActionPlan plan) {
        if (plan==null) return null;
        Receipt receipt=receiptForAction(plan.id());
        BigDecimal balance=receipt==null?accountBalance(plan.sourceAccountId()):receipt.vndBalanceBefore();
        BigDecimal after=receipt==null?balance.subtract(plan.debitAmount()):receipt.vndBalanceAfter();
        PaymentSnapshot snapshot=paymentSnapshot(plan);
        if (snapshot!=null) {
            PaymentReview r=snapshot.review();
            return new PaymentReview(r.billTitle(),r.institution(),r.paymentReference(),r.dueDate(),r.recipientName(),
                    r.recipientBankName(),r.recipientBankCode(),r.recipientAccount(),r.sourceDisplayName(),
                    r.sourceInstitution(),r.sourceMaskedNumber(),r.channelName(),r.rate(),r.quoteSource(),
                    r.quotedAt(),r.expiresAt(),r.settlementMinDays(),r.settlementMaxDays(),r.latestSafeDate(),
                    balance,after,policy().safetyBuffer());
        }
        PaymentSourceAccount source=paymentSource(plan.sourceAccountId());
        CrossBorderService.TuitionBill bill=plan.expenseId()==null?null:crossBorder.bill(plan.expenseId());
        String channelName=plan.channelId()==null?"Internal Sandbox transfer":db.queryForObject(
                "SELECT display_name FROM payment_channels WHERE id=?",String.class,plan.channelId());
        BigDecimal rate=plan.destinationAmount().signum()==0?BigDecimal.ZERO:
                plan.conversionAmount().divide(plan.destinationAmount(),4,java.math.RoundingMode.HALF_UP);
        List<String> recipientNames=db.query("SELECT display_name FROM recipient_allowlist WHERE recipient_id=? AND purpose=?",
                (rs,n)->rs.getString(1),plan.recipient(),plan.actionType());
        String storedRecipient=recipientNames.isEmpty()?plan.recipient():recipientNames.getFirst();
        return new PaymentReview(plan.purpose(),bill==null?storedRecipient:bill.institution(),
                bill==null?null:bill.paymentReference(),bill==null?null:bill.dueDate(),
                bill==null?storedRecipient:bill.recipientName(),bill==null?null:bill.recipientBankName(),
                bill==null?null:bill.recipientBankCode(),plan.recipient(),source.displayName(),source.institution(),
                source.maskedNumber(),channelName,rate,"Legacy stored plan / deterministic Sandbox",null,null,0,0,null,
                balance,after,policy().safetyBuffer());
    }

    public ActionPlan latestAction() {
        List<String> ids=db.query("SELECT id FROM action_plans ORDER BY created_at DESC,id FETCH FIRST 1 ROWS ONLY",
                (rs,n)->rs.getString(1));
        return ids.isEmpty()?null:action(ids.getFirst());
    }

    public boolean matchesCurrentStudentSelection(ActionPlan plan) {
        if (plan == null || !"TUITION".equals(plan.actionType())) return true;
        var bill = crossBorder.bill();
        if (plan.expenseId() == null || plan.expenseId() != bill.id()
                || !bill.recipientAccount().equals(plan.recipient())
                || !bill.currency().equals(plan.destinationCurrency())
                || bill.amount().compareTo(plan.destinationAmount()) != 0) return false;
        return crossBorder.rankedQuotes().stream().anyMatch(quote ->
                quote.channelId().equals(plan.channelId())
                        && quote.quoteId().equals(plan.quoteId())
                        && quote.landedCost().compareTo(plan.debitAmount()) == 0);
    }

    public Receipt latestReceipt() {
        List<String> ids=db.query("SELECT id FROM sandbox_transactions ORDER BY created_at DESC FETCH FIRST 1 ROWS ONLY",
                (rs,n)->rs.getString(1));
        return ids.isEmpty()?null:receipt(ids.getFirst());
    }

    public Receipt receipt(String id) {
        return db.queryForObject("""
                SELECT s.id,s.action_id,s.idempotency_key,s.channel_id,s.quote_id,s.recipient,
                       COALESCE(s.source_account_id,'PAYER_VND'),a.destination_currency,
                       s.vnd_balance_before,s.vnd_debit,s.conversion_vnd,s.fee_deduction_vnd,s.vnd_balance_after,
                       s.cny_balance_before,s.cny_credit,s.cny_balance_after,s.rate_vnd_per_cny,s.status,s.created_at
                FROM sandbox_transactions s JOIN action_plans a ON a.id=s.action_id WHERE s.id=?
                """,this::mapReceipt,id);
    }

    private List<Receipt> receiptsByIdempotency(String key) {
        return db.query("""
                SELECT s.id,s.action_id,s.idempotency_key,s.channel_id,s.quote_id,s.recipient,
                       COALESCE(s.source_account_id,'PAYER_VND'),a.destination_currency,
                       s.vnd_balance_before,s.vnd_debit,s.conversion_vnd,s.fee_deduction_vnd,s.vnd_balance_after,
                       s.cny_balance_before,s.cny_credit,s.cny_balance_after,s.rate_vnd_per_cny,s.status,s.created_at
                FROM sandbox_transactions s JOIN action_plans a ON a.id=s.action_id WHERE s.idempotency_key=?
                """,this::mapReceipt,key);
    }

    private Receipt mapReceipt(java.sql.ResultSet rs,int n) throws java.sql.SQLException {
        return new Receipt(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),
                rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getBigDecimal(9),
                rs.getBigDecimal(10),rs.getBigDecimal(11),rs.getBigDecimal(12),rs.getBigDecimal(13),
                rs.getBigDecimal(14),rs.getBigDecimal(15),rs.getBigDecimal(16),rs.getBigDecimal(17),
                rs.getString(18),rs.getTimestamp(19).toLocalDateTime());
    }

    public List<ConversationMessage> messages() {
        return db.query("""
                SELECT id,role,message,created_at FROM (
                  SELECT id,role,message,created_at FROM conversation_messages
                  ORDER BY created_at DESC FETCH FIRST 12 ROWS ONLY
                ) recent ORDER BY created_at
                """,(rs,n)->new ConversationMessage(rs.getString(1),rs.getString(2),
                rs.getString(3),rs.getTimestamp(4).toLocalDateTime()));
    }

    public List<AuditEvent> auditEvents() {
        return db.query("""
                SELECT id,occurred_at,actor,event_type,reference_id,status,reason_code,details
                FROM audit_log ORDER BY occurred_at DESC FETCH FIRST 30 ROWS ONLY
                """,(rs,n)->new AuditEvent(rs.getString(1),rs.getTimestamp(2).toLocalDateTime(),
                rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),
                rs.getString(7),rs.getString(8)));
    }

    public List<AuditEvent> auditEvents(String actionId) {
        if (actionId==null) return List.of();
        return db.query("""
                SELECT id,occurred_at,actor,event_type,reference_id,status,reason_code,details
                FROM audit_log WHERE reference_id=? ORDER BY occurred_at DESC,id FETCH FIRST 50 ROWS ONLY
                """,(rs,n)->new AuditEvent(rs.getString(1),rs.getTimestamp(2).toLocalDateTime(),rs.getString(3),
                rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8)),actionId);
    }

    public List<Map<String,Object>> sandboxAccounts() {
        return db.queryForList("SELECT * FROM sandbox_accounts ORDER BY id");
    }

    public BigDecimal payerBalance() {
        return selectedPaymentSource().balance();
    }

    public List<PaymentSourceAccount> paymentSourceAccounts() {
        return db.query("""
                SELECT p.account_id,a.display_name,p.institution,p.account_type,p.masked_number,
                       a.currency,a.balance,p.connection_status,p.verification_status,
                       p.cross_border_enabled,p.selected
                FROM payment_source_accounts p JOIN sandbox_accounts a ON a.id=p.account_id
                ORDER BY p.display_order
                """,(rs,n)->mapPaymentSource(rs));
    }

    public PaymentSourceAccount selectedPaymentSource() {
        return paymentSourceAccounts().stream().filter(PaymentSourceAccount::selected)
                .findFirst().orElseGet(()->paymentSource(PAYER));
    }

    public PaymentSourceAccount paymentSource(String id) {
        return db.queryForObject("""
                SELECT p.account_id,a.display_name,p.institution,p.account_type,p.masked_number,
                       a.currency,a.balance,p.connection_status,p.verification_status,
                       p.cross_border_enabled,p.selected
                FROM payment_source_accounts p JOIN sandbox_accounts a ON a.id=p.account_id
                WHERE p.account_id=?
                """,(rs,n)->mapPaymentSource(rs),id);
    }

    private PaymentSourceAccount mapPaymentSource(java.sql.ResultSet rs) throws java.sql.SQLException {
        boolean enabled=rs.getBoolean(10);
        String reason=enabled ? "Ready for eligible cross-border channels"
                : ("E-wallet".equals(rs.getString(4)) ? "This corridor is not supported by the wallet"
                : "Savings account excluded from cross-border payments");
        return new PaymentSourceAccount(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),
                rs.getString(5),rs.getString(6),rs.getBigDecimal(7),rs.getString(8),rs.getString(9),
                enabled,rs.getBoolean(11),reason);
    }

    @Transactional
    public PaymentSourceAccount selectPaymentSource(String id) {
        lockPaymentWorkflow();
        PaymentSourceAccount source=paymentSource(id);
        if(!source.ready()) throw new IllegalArgumentException("Source account is not eligible for this corridor");
        if(!source.canFund(cheapestEligibleTuitionCost(), policy().safetyBuffer()))
            throw new IllegalArgumentException("Source account cannot safely cover this tuition bill");
        db.update("UPDATE payment_source_accounts SET selected=FALSE");
        db.update("UPDATE payment_source_accounts SET selected=TRUE WHERE account_id=?",id);
        audit("USER","PAYMENT_SOURCE_CHANGED",id,"COMPLETED",null,
                source.institution()+" "+source.maskedNumber()+" selected for comparison");
        return paymentSource(id);
    }

    private boolean sourceAccountEligible(String id) {
        try { return paymentSource(id).ready(); }
        catch (Exception ignored) { return false; }
    }

    private BigDecimal cheapestEligibleTuitionCost() {
        return crossBorder.rankedQuotes().stream().filter(CrossBorderService.ChannelQuote::eligible)
                .map(CrossBorderService.ChannelQuote::landedCost).min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    public int sandboxTransactionCount() {
        return db.queryForObject("SELECT COUNT(*) FROM sandbox_transactions",Integer.class);
    }

    private void addMessage(String role,String message) {
        db.update("INSERT INTO conversation_messages VALUES (?,?,?,?)",
                newId("MSG"),role,message,LocalDateTime.now());
    }

    private void audit(String actor,String event,String ref,String status,String code,String details) {
        db.update("INSERT INTO audit_log VALUES (?,?,?,?,?,?,?,?)",
                newId("AUD"),LocalDateTime.now(),actor,event,ref,status,code,details);
    }

    private static PolicyDecision blocked(String code) { return decision("BLOCKED",code); }
    private static PolicyDecision decision(String result,String code) {
        return new PolicyDecision(result,code,code==null?"All deterministic policy checks passed":explanation(code));
    }

    public static String explanation(String code) {
        return switch(code) {
            case "LIMIT PER TX"->"Action exceeds the delegated per-transaction limit.";
            case "LIMIT DAILY"->"Action exceeds the delegated daily amount or frequency limit.";
            case "RECIPIENT NOT ALLOWED"->"Recipient is not on the allowlist for this action.";
            case "APPROVAL REQUIRED"->"User approval is required before sandbox execution.";
            case "APPROVAL EXPIRED"->"Approval is missing, expired, or no longer matches the action.";
            case "PLAN SNAPSHOT MISSING"->"This legacy plan has no locked bill and quote snapshot; create and approve a new plan before payment.";
            case "ACTION CANCELED"->"This unexecuted plan was canceled; no payment will be made.";
            case "BILL ALREADY PAID"->"This student bill already has a completed payment; another payment is blocked.";
            case "ACTION INVALIDATED"->"The linked student bill changed, was archived, or was cancelled; create a new plan.";
            case "AGENT PAUSED"->"Emergency Stop is active, so new actions are blocked.";
            case "INSUFFICIENT SAFE BALANCE"->"Action would reduce the VND balance below the safety buffer.";
            case "UNTRUSTED INSTRUCTION"->"Untrusted input cannot modify policy, recipient, rates, fees or approval.";
            case "DUPLICATE ACTION"->"The idempotency key has already produced a receipt.";
            case "CHANNEL NOT AVAILABLE"->"The selected payment channel is unavailable to the user.";
            case "FX QUOTE EXPIRED"->"The stored FX quote is expired or no longer matches the action.";
            case "RECIPIENT MISMATCH"->"The beneficiary profile does not match the trusted education-provider registry.";
            case "CORRIDOR NOT ALLOWED"->"The selected student-payment corridor has no configured deterministic quote data.";
            case "CURRENCY NOT ALLOWED"->"The plan currencies must match the selected expense and corridor.";
            case "SOURCE ACCOUNT NOT ELIGIBLE"->"The source account is not connected, verified, or enabled for this corridor.";
            case "SOURCE ACCOUNT CHANNEL MISMATCH"->"The selected source account does not belong to this payment channel.";
            case "DEADLINE RISK"->"Settlement time plus the one-day safety margin may miss the tuition due date.";
            default->code;
        };
    }

    private static String hash(String id,String type,String purpose,BigDecimal debit,
            BigDecimal destination,String recipient,String channel,String quote,String sourceAccount) {
        return sha256(String.join("|",id,type,purpose,debit.toPlainString(),
                destination.toPlainString(),recipient,value(channel),value(quote),value(sourceAccount)));
    }
    private static String value(String s) { return s==null?"":s; }
    private static String newId(String prefix) {
        return prefix+"-"+UUID.randomUUID().toString().substring(0,12).toUpperCase(Locale.ROOT);
    }
    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch(Exception ex) { throw new IllegalStateException(ex); }
    }
}
