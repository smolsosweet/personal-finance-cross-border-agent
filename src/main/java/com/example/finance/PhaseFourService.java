package com.example.finance;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
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
    public record ActionPlan(String id, String actionType, String purpose,
            BigDecimal debitAmount, BigDecimal conversionAmount, BigDecimal transferFee,
            BigDecimal fxMarkup, String sourceCurrency, BigDecimal destinationAmount,
            String destinationCurrency, String recipient, String channelId, String quoteId,
            String requiredPermission, String impact, String risk, String status,
            String actionHash, String idempotencyKey, LocalDateTime createdAt) {
        public BigDecimal feeTotal() { return transferFee.add(fxMarkup); }
    }
    public record PolicyDecision(String decision, String reasonCode, String explanation) {}
    public record Receipt(String transactionId, String actionId, String idempotencyKey,
            String channelId, String quoteId, String recipient, BigDecimal vndBalanceBefore,
            BigDecimal vndDebit, BigDecimal conversionVnd, BigDecimal feeDeductionVnd,
            BigDecimal vndBalanceAfter, BigDecimal cnyBalanceBefore, BigDecimal cnyCredit,
            BigDecimal cnyBalanceAfter, BigDecimal rateVndPerCny, String status,
            LocalDateTime createdAt) {}
    public record AuditEvent(String id, LocalDateTime occurredAt, String actor,
            String eventType, String referenceId, String status, String reasonCode, String details) {}
    public record ConversationMessage(String id, String role, String message, LocalDateTime createdAt) {}

    @Transactional
    public void reset() {
        db.update("DELETE FROM audit_log");
        db.update("DELETE FROM sandbox_ledger_entries");
        db.update("DELETE FROM sandbox_transactions");
        db.update("DELETE FROM approvals");
        db.update("DELETE FROM action_plans");
        db.update("DELETE FROM conversation_messages");
        db.update("DELETE FROM recipient_allowlist");
        db.update("DELETE FROM agent_policy");
        db.update("DELETE FROM sandbox_accounts");
        db.update("""
                INSERT INTO agent_policy
                (id,mode,agent_state,runtime_mode,per_transaction_limit,daily_limit,frequency_limit,safety_buffer)
                VALUES (1,'APPROVAL','ACTIVE','OFFLINE',?,?,?,?)
                """, PER_TX_LIMIT, DAILY_LIMIT, FREQUENCY_LIMIT, SAFETY_BUFFER);
        db.update("INSERT INTO recipient_allowlist VALUES (?,?,?)",
                CrossBorderService.SCHOOL_RECIPIENT, "Shenzhen Demo University", "TUITION");
        db.update("INSERT INTO recipient_allowlist VALUES (?,?,?)",
                EMERGENCY, "Emergency Fund", "LOW_RISK");
        db.update("INSERT INTO sandbox_accounts VALUES ('PAYER_VND','Minh Nguyen sandbox payer','VND',100000000.00)");
        db.update("INSERT INTO sandbox_accounts VALUES ('SCHOOL_CNY','Shenzhen Demo University','CNY',0.00)");
        db.update("INSERT INTO sandbox_accounts VALUES ('EMERGENCY_VND','Emergency Fund sandbox recipient','VND',0.00)");
        addMessage("ASSISTANT", "I can explain the tuition bill, create a structured payment plan, or prepare a low-risk Emergency Fund transfer. Every amount comes from deterministic demo data.");
        audit("SYSTEM", "DEMO_RESET", null, "COMPLETED", null,
                "Phase 4 policy, Payment Sandbox, conversation and audit data reset");
    }

    @Transactional
    public void seedIfEmpty() {
        Integer count = db.queryForObject("SELECT COUNT(*) FROM agent_policy", Integer.class);
        if (count == null || count == 0) reset();
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
        String mode = requested == null ? "" : requested.trim().toUpperCase(Locale.ROOT);
        if (!List.of("APPROVAL","DELEGATED").contains(mode))
            throw new IllegalArgumentException("Mode must be APPROVAL or DELEGATED");
        db.update("UPDATE agent_policy SET mode=? WHERE id=1", mode);
        audit("USER","POLICY_MODE_CHANGED","POLICY-1","COMPLETED",null,"Mode changed to "+mode);
    }

    @Transactional
    public void emergencyStop() {
        db.update("UPDATE agent_policy SET agent_state='PAUSED' WHERE id=1");
        audit("USER","EMERGENCY_STOP","POLICY-1","COMPLETED","AGENT PAUSED",explanation("AGENT PAUSED"));
    }

    @Transactional
    public void resumeAgent() {
        db.update("UPDATE agent_policy SET agent_state='ACTIVE' WHERE id=1");
        audit("USER","AGENT_RESUMED","POLICY-1","COMPLETED",null,"Agent resumed by demo user");
    }

    @Transactional
    public void enableOfflineFallback() {
        db.update("UPDATE agent_policy SET runtime_mode='OFFLINE' WHERE id=1");
        audit("SYSTEM","OFFLINE_FALLBACK_ENABLED","POLICY-1","COMPLETED",null,
                "Deterministic local responses are active; no LLM or network dependency is required");
    }

    @Transactional
    public String sendMessage(String raw) {
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
            String response="Created tuition plan "+plan.id()+" using Bank A. The 20,000 CNY payment remains in Approval Mode.";
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
        var quote=crossBorder.rankedQuotes().stream().filter(q->q.channelId().equals(channelId))
                .findFirst().orElseThrow(()->new IllegalArgumentException("Unknown payment channel"));
        var bill=crossBorder.bill();
        String id=newId("ACT");
        String impact="Debit "+quote.landedCost().toPlainString()+" VND; convert "
                +quote.sourceAmount().toPlainString()+" VND; credit "
                +quote.expectedReceived().toPlainString()+" CNY";
        ActionPlan plan=insertPlan(id,"TUITION","Pay tuition bill "+bill.paymentReference(),
                quote.landedCost(),quote.sourceAmount(),quote.transferFee(),quote.fxMarkup(),"VND",
                quote.expectedReceived(),"CNY",bill.recipientAccount(),quote.channelId(),quote.quoteId(),
                "APPROVAL",impact,"Quote, recipient and approval are rechecked before execution");
        return applyInitialPolicy(plan);
    }

    @Transactional
    public ActionPlan createLowRiskPlan(BigDecimal amount) {
        if(amount==null||amount.signum()<=0) throw new IllegalArgumentException("Amount must be positive");
        BigDecimal normalized=amount.setScale(2);
        String id=newId("ACT");
        ActionPlan plan=insertPlan(id,"LOW_RISK","Move funds to Emergency Fund",normalized,normalized,
                BigDecimal.ZERO.setScale(2),BigDecimal.ZERO.setScale(2),"VND",normalized,"VND",
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

    private ActionPlan insertPlan(String id,String type,String purpose,BigDecimal debit,
            BigDecimal conversion,BigDecimal fee,BigDecimal markup,String sourceCurrency,
            BigDecimal destination,String destinationCurrency,String recipient,String channelId,
            String quoteId,String permission,String impact,String risk) {
        LocalDateTime now=LocalDateTime.now();
        String idem="IDEMP-"+id;
        String hash=hash(id,type,purpose,debit,destination,recipient,channelId,quoteId);
        db.update("""
                INSERT INTO action_plans
                (id,action_type,purpose,debit_amount,conversion_amount,transfer_fee,fx_markup,
                 source_currency,destination_amount,destination_currency,recipient,channel_id,quote_id,
                 required_permission,impact,risk,status,action_hash,idempotency_key,created_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,id,type,purpose,debit,conversion,fee,markup,sourceCurrency,destination,
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
        if ("DEADLINE RISK".equals(d.reasonCode())) {
            db.update("UPDATE action_plans SET risk='DEADLINE RISK: ' || risk WHERE id=?", plan.id());
        }
        audit("POLICY_GUARD","POLICY_CHECKED",plan.id(),d.decision(),d.reasonCode(),d.explanation());
        return action(plan.id());
    }

    public PolicyDecision evaluate(ActionPlan plan,boolean execution) {
        Policy p=policy();
        if("PAUSED".equals(p.state())) return blocked("AGENT PAUSED");
        if(!recipientAllowed(plan.recipient(),plan.actionType())) return blocked("RECIPIENT NOT ALLOWED");
        if(accountBalance(PAYER).subtract(plan.debitAmount()).compareTo(p.safetyBuffer())<0)
            return blocked("INSUFFICIENT SAFE BALANCE");
        if("TUITION".equals(plan.actionType())) {
            var verified=crossBorder.verifyRecipient();
            if(!verified.verified()||!crossBorder.bill().recipientAccount().equals(plan.recipient()))
                return blocked("RECIPIENT MISMATCH");
            var quote=crossBorder.rankedQuotes().stream().filter(q->q.channelId().equals(plan.channelId()))
                    .findFirst().orElse(null);
            if(quote==null||!quote.eligible()) return blocked("CHANNEL NOT AVAILABLE");
            if(!quote.quoteId().equals(plan.quoteId())||quote.expired()) return blocked("FX QUOTE EXPIRED");
            if(quote.landedCost().compareTo(plan.debitAmount())!=0
                    ||quote.sourceAmount().compareTo(plan.conversionAmount())!=0
                    ||quote.transferFee().compareTo(plan.transferFee())!=0
                    ||quote.fxMarkup().compareTo(plan.fxMarkup())!=0
                    ||quote.expectedReceived().compareTo(plan.destinationAmount())!=0)
                return blocked("APPROVAL EXPIRED");
            boolean deadlineRisk = LocalDate.now()
                    .plusDays(quote.settlementMaxDays() + CrossBorderService.SETTLEMENT_SAFETY_MARGIN_DAYS)
                    .isAfter(crossBorder.bill().dueDate());
            if (deadlineRisk) {
                if (!execution) return decision("DEADLINE_RISK","DEADLINE RISK");
                return validApproval(plan) ? decision("ALLOWED",null) : blocked("APPROVAL EXPIRED");
            }
            if(!execution) return decision("APPROVAL_REQUIRED","APPROVAL REQUIRED");
            return validApproval(plan)?decision("ALLOWED",null):blocked("APPROVAL EXPIRED");
        }
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
        ActionPlan plan=action(actionId);
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
        return "BLOCKED".equals(approved.status())?null:execute(actionId);
    }

    @Transactional
    public Receipt execute(String actionId) {
        ActionPlan plan=action(actionId);
        List<Receipt> prior=receiptsByIdempotency(plan.idempotencyKey());
        if(!prior.isEmpty()) {
            audit("PAYMENT_SANDBOX","IDEMPOTENT_RETRY",plan.id(),"COMPLETED","DUPLICATE ACTION",
                    "Existing receipt returned; balances were not changed");
            return prior.getFirst();
        }
        PolicyDecision check=evaluate(plan,true);
        if("BLOCKED".equals(check.decision())) { block(plan,check); return null; }

        BigDecimal payerBefore=accountBalance(PAYER);
        String target="TUITION".equals(plan.actionType())?SCHOOL:"EMERGENCY_VND";
        BigDecimal targetBefore=accountBalance(target);
        BigDecimal payerAfter=payerBefore.subtract(plan.debitAmount());
        BigDecimal targetAfter=targetBefore.add(plan.destinationAmount());
        BigDecimal rate="TUITION".equals(plan.actionType())
                ?plan.conversionAmount().divide(plan.destinationAmount()):BigDecimal.ONE.setScale(4);
        String txId=newId("SBOX");
        LocalDateTime now=LocalDateTime.now();
        db.update("UPDATE sandbox_accounts SET balance=? WHERE id=?",payerAfter,PAYER);
        db.update("UPDATE sandbox_accounts SET balance=? WHERE id=?",targetAfter,target);
        db.update("""
                INSERT INTO sandbox_transactions
                (id,action_id,idempotency_key,channel_id,quote_id,recipient,vnd_balance_before,
                 vnd_debit,conversion_vnd,fee_deduction_vnd,vnd_balance_after,cny_balance_before,
                 cny_credit,cny_balance_after,rate_vnd_per_cny,status,created_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,txId,plan.id(),plan.idempotencyKey(),plan.channelId(),plan.quoteId(),plan.recipient(),
                payerBefore,plan.debitAmount(),plan.conversionAmount(),plan.feeTotal(),payerAfter,
                targetBefore,plan.destinationAmount(),targetAfter,rate,"COMPLETED",now);
        ledger(txId,"VND_DEBIT",PAYER,plan.debitAmount().negate(),"VND");
        ledger(txId,"CONVERSION",PAYER,plan.conversionAmount(),plan.sourceCurrency());
        ledger(txId,"FEE_DEDUCTION",PAYER,plan.feeTotal().negate(),"VND");
        ledger(txId,"CNY_CREDIT",target,plan.destinationAmount(),plan.destinationCurrency());
        db.update("UPDATE action_plans SET status='COMPLETED' WHERE id=?",plan.id());
        db.update("UPDATE approvals SET status='USED' WHERE action_id=? AND status='VALID'",plan.id());
        audit("PAYMENT_SANDBOX","SANDBOX_EXECUTED",plan.id(),"COMPLETED",null,
                "Created transaction "+txId+" with VND debit and "+plan.destinationCurrency()+" credit");
        return receipt(txId);
    }

    private void ledger(String tx,String type,String account,BigDecimal amount,String currency) {
        db.update("INSERT INTO sandbox_ledger_entries VALUES (?,?,?,?,?,?,?)",
                newId("LEDGER"),tx,type,account,amount,currency,LocalDateTime.now());
    }

    private ActionPlan block(ActionPlan plan,PolicyDecision d) {
        db.update("UPDATE action_plans SET status='BLOCKED' WHERE id=?",plan.id());
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
        return hash(p.id(),p.actionType(),p.purpose(),p.debitAmount(),p.destinationAmount(),
                p.recipient(),p.channelId(),p.quoteId());
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
                SELECT id,action_type,purpose,debit_amount,conversion_amount,transfer_fee,fx_markup,
                       source_currency,destination_amount,destination_currency,recipient,channel_id,quote_id,
                       required_permission,impact,risk,status,action_hash,idempotency_key,created_at
                FROM action_plans WHERE id=?
                """,(rs,n)->new ActionPlan(rs.getString(1),rs.getString(2),rs.getString(3),
                rs.getBigDecimal(4),rs.getBigDecimal(5),rs.getBigDecimal(6),rs.getBigDecimal(7),
                rs.getString(8),rs.getBigDecimal(9),rs.getString(10),rs.getString(11),
                rs.getString(12),rs.getString(13),rs.getString(14),rs.getString(15),
                rs.getString(16),rs.getString(17),rs.getString(18),rs.getString(19),
                rs.getTimestamp(20).toLocalDateTime()),id);
    }

    public ActionPlan latestAction() {
        List<String> ids=db.query("SELECT id FROM action_plans ORDER BY created_at DESC FETCH FIRST 1 ROWS ONLY",
                (rs,n)->rs.getString(1));
        return ids.isEmpty()?null:action(ids.getFirst());
    }

    public Receipt latestReceipt() {
        List<String> ids=db.query("SELECT id FROM sandbox_transactions ORDER BY created_at DESC FETCH FIRST 1 ROWS ONLY",
                (rs,n)->rs.getString(1));
        return ids.isEmpty()?null:receipt(ids.getFirst());
    }

    public Receipt receipt(String id) {
        return db.queryForObject("""
                SELECT id,action_id,idempotency_key,channel_id,quote_id,recipient,
                       vnd_balance_before,vnd_debit,conversion_vnd,fee_deduction_vnd,vnd_balance_after,
                       cny_balance_before,cny_credit,cny_balance_after,rate_vnd_per_cny,status,created_at
                FROM sandbox_transactions WHERE id=?
                """,this::mapReceipt,id);
    }

    private List<Receipt> receiptsByIdempotency(String key) {
        return db.query("""
                SELECT id,action_id,idempotency_key,channel_id,quote_id,recipient,
                       vnd_balance_before,vnd_debit,conversion_vnd,fee_deduction_vnd,vnd_balance_after,
                       cny_balance_before,cny_credit,cny_balance_after,rate_vnd_per_cny,status,created_at
                FROM sandbox_transactions WHERE idempotency_key=?
                """,this::mapReceipt,key);
    }

    private Receipt mapReceipt(java.sql.ResultSet rs,int n) throws java.sql.SQLException {
        return new Receipt(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),
                rs.getString(5),rs.getString(6),rs.getBigDecimal(7),rs.getBigDecimal(8),
                rs.getBigDecimal(9),rs.getBigDecimal(10),rs.getBigDecimal(11),rs.getBigDecimal(12),
                rs.getBigDecimal(13),rs.getBigDecimal(14),rs.getBigDecimal(15),rs.getString(16),
                rs.getTimestamp(17).toLocalDateTime());
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

    public List<Map<String,Object>> sandboxAccounts() {
        return db.queryForList("SELECT * FROM sandbox_accounts ORDER BY id");
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
            case "AGENT PAUSED"->"Emergency Stop is active, so new actions are blocked.";
            case "INSUFFICIENT SAFE BALANCE"->"Action would reduce the VND balance below the safety buffer.";
            case "UNTRUSTED INSTRUCTION"->"Untrusted input cannot modify policy, recipient, rates, fees or approval.";
            case "DUPLICATE ACTION"->"The idempotency key has already produced a receipt.";
            case "CHANNEL NOT AVAILABLE"->"The selected payment channel is unavailable to the user.";
            case "FX QUOTE EXPIRED"->"The stored FX quote is expired or no longer matches the action.";
            case "RECIPIENT MISMATCH"->"The recipient does not match the tuition bill and School Registry.";
            case "DEADLINE RISK"->"Settlement time plus the one-day safety margin may miss the tuition due date.";
            default->code;
        };
    }

    private static String hash(String id,String type,String purpose,BigDecimal debit,
            BigDecimal destination,String recipient,String channel,String quote) {
        return sha256(String.join("|",id,type,purpose,debit.toPlainString(),                destination.toPlainString(),recipient,value(channel),value(quote)));
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
