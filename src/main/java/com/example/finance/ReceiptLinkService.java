package com.example.finance;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Deterministic receipt evidence. Capabilities prove session origin, not ownership. */
@Service
public class ReceiptLinkService {
    public static final int DEFAULT_DATE_WINDOW_DAYS = 3;
    private static final String OWNER = "receipt.origin.owner";
    private static final String CURRENT = "receipt.current";
    private final JdbcTemplate db;
    private final TransactionService transactions;
    private final AccountLedgerService ledger;
    private final int window;

    public ReceiptLinkService(JdbcTemplate db, TransactionService transactions, AccountLedgerService ledger,
                              @Value("${finbridge.receipt.date-window-days:}") String configuredWindow) {
        int window=configuredWindow.isBlank()?DEFAULT_DATE_WINDOW_DAYS:Integer.parseInt(configuredWindow);
        if (window < 0 || window > 31) throw new IllegalArgumentException("Receipt date window must be 0–31 days");
        this.db=db; this.transactions=transactions; this.ledger=ledger; this.window=window;
    }
    public record Draft(String merchant, BigDecimal amount, String currency, LocalDate date,
                        String paymentMethod, String category, String reference, String description) {}
    public record Candidate(String id, String merchant, LocalDate date, BigDecimal amount, String currency) {}
    public record Outcome(String state, String capability, String transactionId, List<Candidate> candidates) {
        public String message() {
            return switch (state) {
                case "LINKED" -> "Receipt linked to the existing expense. No new expense was created.";
                case "ASK" -> "Similar expenses found. Choose the matching expense or confirm a different purchase. Nothing was added.";
                case "OVERRIDDEN" -> "A different purchase was recorded once and linked to this receipt.";
                case "CREATED" -> "Receipt saved and linked to the recorded expense.";
                default -> "Receipt request rejected. Reopen and review the receipt. No financial data changed.";
            };
        }
        public boolean actionable() { return "LINKED".equals(state) || "ASK".equals(state); }
    }

    public DocumentExtractionService.Result sample() {
        var rows=db.queryForList("""
                SELECT t.merchant,t.amount,t.currency,t.occurred_at,t.category FROM transactions t
                JOIN bank_events e ON e.id=t.event_id
                WHERE e.raw_reference='SEED-15' AND t.source_label='Synthetic Data' AND t.type='Expense'
                """);
        if (rows.size()!=1) throw new IllegalStateException("Synthetic sample unavailable. Use a local seeded workspace.");
        var row=rows.getFirst();
        if (!"Highlands Coffee".equals(row.get("merchant")) || !"VND".equals(row.get("currency"))
                || ((BigDecimal)row.get("amount")).compareTo(new BigDecimal("130000"))!=0)
            throw new IllegalStateException("Synthetic sample unavailable. Use a local seeded workspace.");
        return new DocumentExtractionService.Result(Map.of("merchant","Highlands Coffee","amount","130000","currency","VND",
                "occurredAt",((java.sql.Timestamp)row.get("occurred_at")).toLocalDateTime().toLocalDate().toString(),
                "paymentMethod","Card","category","Food & Drinks","reference",""),null,"synthetic");
    }
    private String owner(HttpSession session) {
        synchronized (session) {
            Object value=session.getAttribute(OWNER);
            if (value==null) { value=UUID.randomUUID().toString(); session.setAttribute(OWNER,value); }
            return value.toString();
        }
    }

    /** Called only after successful image extraction (or an explicitly enabled fixed demo sample). */
    @Transactional
    public String issue(HttpSession session, byte[] image) {
        String capability=UUID.randomUUID().toString();
        db.update("INSERT INTO receipt_submissions(capability,session_owner,origin_hash,expires_at,state,override_operation) VALUES (?,?,?,?,?,?)",
                capability,owner(session),hash(image),LocalDateTime.now().plusMinutes(20),"ISSUED",UUID.randomUUID().toString());
        session.setAttribute(CURRENT,capability);
        return capability;
    }

    @Transactional
    public Outcome save(HttpSession session, String capability, Draft input) {
        ledger.lock("CHECKING");
        Map<String,Object> row=authorized(session,capability,true);
        if (row==null) return reject(capability);
        if (!"ISSUED".equals(row.get("state"))) return outcome(row);
        Draft d;
        try { d=validate(input); }
        catch (IllegalArgumentException invalid) { return reject(capability); }
        String evidence=hash((normalize(d.merchant())+"|"+d.amount().toPlainString()+"|"+d.currency()+"|"+d.date()).getBytes(StandardCharsets.UTF_8));
        db.update("""
                UPDATE receipt_submissions SET evidence_hash=?,merchant=?,amount=?,currency=?,receipt_date=?,
                payment_method=?,category=?,reference=?,description=? WHERE capability=?
                """,evidence,d.merchant(),d.amount(),d.currency(),d.date(),d.paymentMethod(),d.category(),d.reference(),d.description(),capability);
        row=authorized(session,capability,true);
        var candidates=candidates(d);
        if (candidates.size()==1 && normalize(candidates.getFirst().merchant()).equals(normalize(d.merchant()))
                && !differentEvidence(candidates.getFirst().id(),evidence)) {
            return link(row,candidates.getFirst().id());
        }
        if (!candidates.isEmpty()) {
            db.update("UPDATE receipt_submissions SET state='ASK' WHERE capability=?",capability);
            audit("RECEIPT_ASK",capability,"AWAITING_APPROVAL","User choice required; no expense created.");
            return outcome(authorized(session,capability,true));
        }
        String id=transactions.recordManualTransaction(d.merchant(),d.amount(),d.currency(),d.date(),
                d.paymentMethod(),d.category(),d.reference(),d.description());
        // Protected reference dedupe may return a different historic record. Never mislabel that as this receipt.
        var saved=transactions.transaction(id);
        if (!"Expense".equals(saved.get("type")) || !d.currency().equals(saved.get("currency"))
                || d.amount().compareTo((BigDecimal)saved.get("amount"))!=0
                || !normalize(d.merchant()).equals(normalize((String)saved.get("merchant")))
                || !d.date().equals(((java.sql.Timestamp)saved.get("occurred_at")).toLocalDateTime().toLocalDate()))
            return reject(capability);
        addEvidence(evidence,evidence,id);
        db.update("UPDATE receipt_submissions SET state='CREATED',transaction_id=? WHERE capability=?",id,capability);
        audit("RECEIPT_LINK",capability,"COMPLETED","Receipt linked to recorded expense "+id);
        return outcome(authorized(session,capability,true));
    }

    /** The operation ID never crosses the client/model boundary. Only an explicit choice invokes this. */
    @Transactional
    public Outcome differentPurchase(HttpSession session, String capability) {
        ledger.lock("CHECKING");
        var row=authorized(session,capability,true);
        if (row==null) return reject(capability);
        if ("OVERRIDDEN".equals(row.get("state"))) return outcome(row);
        if (!List.of("LINKED","ASK").contains(row.get("state"))) return reject(capability);
        Draft d=draft(row);
        String operation=(String)row.get("override_operation");
        String id=transactions.recordReceiptOverride(d.merchant(),d.amount(),d.currency(),d.date(),
                d.paymentMethod(),d.category(),d.reference(),d.description(),operation);
        String evidence=(String)row.get("evidence_hash");
        addEvidence(evidence+"-"+operation,evidence,id);
        db.update("UPDATE receipt_submissions SET state='OVERRIDDEN',transaction_id=? WHERE capability=?",id,capability);
        audit("RECEIPT_OVERRIDE",capability,"COMPLETED","Explicit different purchase "+id);
        return outcome(authorized(session,capability,true));
    }

    @Transactional
    public Outcome chooseExisting(HttpSession session, String capability, String transaction) {
        ledger.lock("CHECKING");
        var row=authorized(session,capability,true);
        if (row==null) return reject(capability);
        if ("LINKED".equals(row.get("state")) && Objects.equals(row.get("transaction_id"),transaction)) return outcome(row);
        if (!"ASK".equals(row.get("state")) || candidates(draft(row)).stream().noneMatch(c->c.id().equals(transaction)))
            return reject(capability);
        return link(row,transaction);
    }

    public Outcome view(HttpSession session) {
        Object cap=session.getAttribute(CURRENT);
        if (cap==null) return null;
        var row=authorized(session,cap.toString(),false);
        return row==null || "ISSUED".equals(row.get("state")) ? null : outcome(row);
    }

    private Map<String,Object> authorized(HttpSession session,String capability,boolean lock) {
        if (capability==null || !capability.matches("[0-9a-f-]{36}")) return null;
        var rows=db.queryForList("SELECT * FROM receipt_submissions WHERE capability=? AND session_owner=?"+(lock?" FOR UPDATE":""),
                capability,owner(session));
        if (rows.isEmpty() || !((java.sql.Timestamp)rows.getFirst().get("expires_at")).toLocalDateTime().isAfter(LocalDateTime.now())) return null;
        return rows.getFirst();
    }
    private Draft validate(Draft d) {
        if (d==null || d.merchant()==null || d.merchant().isBlank() || d.merchant().trim().length()>120
                || d.amount()==null || d.amount().signum()<=0 || d.amount().precision()>20 || d.amount().scale()>2
                || d.date()==null || d.currency()==null || !d.currency().trim().matches("[a-zA-Z]{3}")
                || d.category()==null || db.queryForObject("SELECT COUNT(*) FROM transaction_categories WHERE LOWER(name)=LOWER(?) AND active=TRUE",Integer.class,d.category())!=1
                || length(d.paymentMethod())>80 || length(d.reference())>120 || length(d.description())>160)
            throw new IllegalArgumentException("Invalid receipt fields");
        String description=Objects.toString(d.description(),"")+" "+Objects.toString(d.paymentMethod(),"")+" "+Objects.toString(d.reference(),"");
        var normalized=TransactionService.normalize(new TransactionService.BankEvent("VALIDATE",d.date().atStartOfDay(),d.merchant(),description,d.amount(),"OUT","CHECKING",null,"Manual Entry"));
        if (!"Expense".equals(normalized.type())) throw new IllegalArgumentException("Only purchase receipts can be linked");
        return new Draft(d.merchant().trim().replaceAll("\\s+"," "),d.amount().setScale(2),d.currency().trim().toUpperCase(Locale.ROOT),d.date(),
                trim(d.paymentMethod()),d.category(),trim(d.reference()),trim(d.description()));
    }
    private static int length(String s) { return s==null?0:s.trim().length(); }
    private static String trim(String s) { return s==null?"":s.trim(); }
    private static String normalize(String merchant) { return merchant.trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT); }
    private Draft draft(Map<String,Object> row) {
        return new Draft((String)row.get("merchant"),(BigDecimal)row.get("amount"),(String)row.get("currency"),
                ((java.sql.Date)row.get("receipt_date")).toLocalDate(),(String)row.get("payment_method"),
                (String)row.get("category"),(String)row.get("reference"),(String)row.get("description"));
    }
    private List<Candidate> candidates(Draft d) {
        return db.query("""
                SELECT id,merchant,occurred_at,amount,currency FROM transactions
                WHERE type='Expense' AND currency=? AND amount=? AND occurred_at>=? AND occurred_at<?
                ORDER BY occurred_at,id
                """,(rs,n)->new Candidate(rs.getString("id"),rs.getString("merchant"),rs.getTimestamp("occurred_at").toLocalDateTime().toLocalDate(),rs.getBigDecimal("amount"),rs.getString("currency")),
                d.currency(),d.amount(),d.date().minusDays(window).atStartOfDay(),d.date().plusDays(window+1L).atStartOfDay());
    }
    private boolean differentEvidence(String transaction,String hash) {
        return db.queryForObject("SELECT COUNT(*) FROM receipt_evidence WHERE transaction_id=? AND evidence_hash<>?",Integer.class,transaction,hash)>0;
    }
    private void addEvidence(String identity,String hash,String transaction) {
        if (db.queryForObject("SELECT COUNT(*) FROM receipt_evidence WHERE identity_key=?",Integer.class,identity)==0)
            db.update("INSERT INTO receipt_evidence VALUES (?,?,?,?)",identity,hash,transaction,LocalDateTime.now());
    }
    private Outcome link(Map<String,Object> row,String id) {
        String hash=(String)row.get("evidence_hash");
        var existing=db.queryForList("SELECT transaction_id FROM receipt_evidence WHERE identity_key=?",hash);
        String key=existing.isEmpty() || id.equals(existing.getFirst().get("transaction_id")) ? hash : hash+"-"+row.get("capability");
        addEvidence(key,hash,id);
        db.update("UPDATE receipt_submissions SET state='LINKED',transaction_id=? WHERE capability=?",id,row.get("capability"));
        audit("RECEIPT_LINK",(String)row.get("capability"),"COMPLETED","Receipt linked to existing expense "+id+"; no financial write.");
        return new Outcome("LINKED",(String)row.get("capability"),id,List.of());
    }
    private Outcome outcome(Map<String,Object> row) {
        String state=(String)row.get("state");
        return new Outcome(state,(String)row.get("capability"),(String)row.get("transaction_id"),"ASK".equals(state)?candidates(draft(row)):List.of());
    }
    private Outcome reject(String capability) {
        audit("RECEIPT_REJECT",null,"BLOCKED","Invalid, stale or foreign receipt request; zero financial writes.");
        return new Outcome("REJECTED",null,null,List.of());
    }
    private void audit(String event,String reference,String status,String details) {
        db.update("INSERT INTO audit_log VALUES (?,?,?,?,?,?,?,?)",UUID.randomUUID().toString(),LocalDateTime.now(),"USER",event,reference,status,null,details);
    }
    private static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}