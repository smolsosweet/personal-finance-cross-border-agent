package com.example.finance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class PhaseOneController {
    private final TransactionService transactions;
    private final CrossBorderService crossBorder;
    private final PhaseFourService phaseFour;
    private final DemoDataService demoData;
    private final boolean demoToolsEnabled;

    public PhaseOneController(TransactionService transactions, CrossBorderService crossBorder,
                              PhaseFourService phaseFour, DemoDataService demoData,
                              @Value("${app.demo-tools-enabled:true}") boolean demoToolsEnabled) {
        this.transactions = transactions;
        this.crossBorder = crossBorder;
        this.phaseFour = phaseFour;
        this.demoData = demoData;
        this.demoToolsEnabled = demoToolsEnabled;
    }

    @Bean
    ApplicationRunner seedDemo(DemoDataService demoData) {
        return args -> demoData.initialize();
    }

    @GetMapping("/")
    public String home(@RequestParam(required=false) String review,
                       @RequestParam(required=false) String newTransaction,
                       @RequestParam(required=false) String action, HttpServletRequest request, Model model) {
        model.addAttribute("profile", transactions.profile());
        model.addAttribute("accounts", transactions.accounts());
        model.addAttribute("transactions", transactions.transactions());
        model.addAttribute("pendingTransactions", transactions.pendingTransactions());
        model.addAttribute("transactionCategories", transactions.categories());
        model.addAttribute("eventCount", transactions.eventCount());
        model.addAttribute("dashboard", transactions.dashboard());
        model.addAttribute("budgets", transactions.budgetSummary());
        Map<String,Object> defaultTuitionInsight = crossBorder.tuitionInsight();
        var insights = new ArrayList<>(transactions.proactiveFeed());
        insights.add(defaultTuitionInsight);
        model.addAttribute("insights", insights);
        model.addAttribute("studentProfile", crossBorder.profile());
        model.addAttribute("tuitionBill", crossBorder.bill());
        model.addAttribute("studentExpenses", crossBorder.expenses());
        model.addAttribute("selectedExpense", crossBorder.selectedExpense());
        model.addAttribute("recipientVerification", crossBorder.verifyRecipient());
        var channelQuotes = crossBorder.rankedQuotes();
        var eligibleQuotes = channelQuotes.stream().filter(CrossBorderService.ChannelQuote::eligible).toList();
        var connectedQuotes = channelQuotes.stream().filter(quote -> quote.sourceAccountId() != null).toList();
        var suggestedQuotes = channelQuotes.stream().filter(quote -> quote.sourceAccountId() == null).toList();
        BigDecimal cheapestEligibleCost = eligibleQuotes.stream()
                .map(CrossBorderService.ChannelQuote::landedCost).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        model.addAttribute("channelQuotes", channelQuotes);
        model.addAttribute("eligibleChannelQuotes", eligibleQuotes);
        model.addAttribute("connectedChannelQuotes", connectedQuotes);
        model.addAttribute("suggestedChannelQuotes", suggestedQuotes);
        model.addAttribute("cheapestEligibleCost", cheapestEligibleCost);
        model.addAttribute("agentPolicy", phaseFour.policy());
        var paymentAccounts = phaseFour.paymentSourceAccounts();
        model.addAttribute("paymentAccounts", paymentAccounts);
        model.addAttribute("paymentAccountById", paymentAccounts.stream().collect(Collectors.toMap(
                PhaseFourService.PaymentSourceAccount::accountId, account -> account)));
        model.addAttribute("channelBySourceAccountId", eligibleQuotes.stream().collect(Collectors.toMap(
                CrossBorderService.ChannelQuote::sourceAccountId, quote -> quote)));
        model.addAttribute("conversation", phaseFour.messages());
        var latestAction = phaseFour.latestAction();
        if (action == null || action.isBlank()) action=request.getHeader("X-Workspace-Action");
        if (action != null && !action.isBlank()) {
            try { latestAction=phaseFour.action(action); }
            catch (org.springframework.dao.EmptyResultDataAccessException ex) {
                latestAction=null;
                model.addAttribute("message", "Request failed: This payment plan no longer exists; choose a bill and create a new plan.");
            }
        }
        var paymentReview=phaseFour.paymentReview(latestAction);
        var paymentDecision=latestAction==null?null:"COMPLETED".equals(latestAction.status())
                ? new PhaseFourService.PolicyDecision("COMPLETED",null,"This plan already has a completed Sandbox receipt")
                : phaseFour.evaluate(latestAction,false);
        model.addAttribute("latestAction", latestAction);
        model.addAttribute("paymentReview", paymentReview);
        model.addAttribute("paymentDecision", paymentDecision);
        model.addAttribute("paymentPlans", phaseFour.recentPlans());
        model.addAttribute("completedPlanByExpense", phaseFour.completedPlansByExpense());
        Map<String,Object> agentTuitionInsight=latestAction!=null && "TUITION".equals(latestAction.actionType())
                ? Map.of("priority", "BLOCKED".equals(paymentDecision.decision())?"BLOCKED":"HIGH",
                        "title", "Student payment plan needs controlled execution",
                        "message", "Bill "+paymentReview.paymentReference()+" for "+latestAction.destinationAmount().toPlainString()
                                +" "+latestAction.destinationCurrency()+"; due "+paymentReview.dueDate()
                                +"; latest safe date "+paymentReview.latestSafeDate()+". Approval Mode is required before payment.",
                        "evidence", "Locked payment plan · "+paymentReview.channelName()+" · quote "+latestAction.quoteId()
                                +" · landed cost "+latestAction.debitAmount().toPlainString()+" VND")
                : defaultTuitionInsight;
        model.addAttribute("tuitionInsight", agentTuitionInsight);
        model.addAttribute("latestReceipt", latestAction==null?null:phaseFour.receiptForAction(latestAction.id()));
        model.addAttribute("sandboxAccounts", phaseFour.sandboxAccounts());
        model.addAttribute("auditEvents", phaseFour.auditEvents(latestAction==null?null:latestAction.id()));
        model.addAttribute("demoAuditEvents", phaseFour.auditEvents());
        model.addAttribute("demoToolsEnabled", demoToolsEnabled);
        model.addAttribute("newTransaction", newTransaction);
        if (review != null && !review.isBlank()) model.addAttribute("reviewTransactionId", review);
        return "home";
    }

    @PostMapping("/events/simulate")
    public String simulate(@RequestParam(defaultValue="high") String scenario, RedirectAttributes flash) {
        String id = transactions.simulate(scenario);
        Map<String,Object> transaction = transactions.transaction(id);
        flash.addFlashAttribute("message", "Simulated Bank Event: " + transaction.get("category") +
                " · confidence " + transaction.get("confidence") + "% · " + transaction.get("review_status"));
        String status = (String) transaction.get("review_status");
        if ("CONFIRMATION_REQUIRED".equals(status) || "PURPOSE_REQUIRED".equals(status)) {
            return "redirect:/?review=" + id + "&newTransaction=" + id + "#transaction-review";
        }
        return "redirect:/?newTransaction=" + id + "#transactions";
    }

    @PostMapping("/transactions/{id}/category")
    public String confirmCategory(@PathVariable String id, @RequestParam String category, RedirectAttributes flash) {
        transactions.confirmCategory(id, category);
        flash.addFlashAttribute("message", "Category confirmed and dashboard updated.");
        return "redirect:/#transactions";
    }

    @PostMapping("/transactions/{id}/review")
    public String reviewTransaction(@PathVariable String id,
                                    @RequestParam(required=false) String category,
                                    @RequestParam(required=false) String customCategory,
                                    @RequestParam(required=false) String purpose,
                                    RedirectAttributes flash) {
        transactions.reviewTransaction(id, category, customCategory, purpose);
        flash.addFlashAttribute("message", "Transaction reviewed. Dashboard and budget are now updated.");
        return "redirect:/#transaction-review";
    }

    @PostMapping("/transactions/categories")
    public String addCategory(@RequestParam String name, RedirectAttributes flash) {
        transactions.addCustomCategory(name);
        flash.addFlashAttribute("message", "Custom category is ready to use.");
        return "redirect:/#transaction-categories";
    }

    @PostMapping("/transactions/categories/archive")
    public String archiveCategory(@RequestParam String name, RedirectAttributes flash) {
        transactions.archiveCustomCategory(name);
        flash.addFlashAttribute("message", "Custom category archived. Existing transactions keep their category.");
        return "redirect:/#transaction-categories";
    }

    @PostMapping("/transactions/categories/restore")
    public String restoreCategory(@RequestParam String name, RedirectAttributes flash) {
        transactions.restoreCustomCategory(name);
        flash.addFlashAttribute("message", "Custom category restored.");
        return "redirect:/#transaction-categories";
    }

    @PostMapping("/transactions/categories/rename")
    public String renameCategory(@RequestParam String name, @RequestParam String newName, RedirectAttributes flash) {
        transactions.renameCustomCategory(name, newName);
        flash.addFlashAttribute("message", "Category renamed across transactions and reports.");
        return "redirect:/#transaction-categories";
    }

    @PostMapping("/transactions/{id}/undo")
    public String undoCategory(@PathVariable String id, RedirectAttributes flash) {
        transactions.undoCategory(id);
        flash.addFlashAttribute("message", "Category change undone.");
        return "redirect:/?review=" + id + "#transaction-review";
    }

    @PostMapping("/student/preference")
    public String setPreference(@RequestParam String preference, RedirectAttributes flash) {
        crossBorder.setPreference(preference);
        flash.addFlashAttribute("message", "Channel ranking updated to " + preference.toLowerCase() + ".");
        return "redirect:/#student-finance";
    }

    @PostMapping("/student/quotes/refresh")
    public String refreshQuotes(RedirectAttributes flash) {
        crossBorder.refreshQuotes();
        flash.addFlashAttribute("message", "Synthetic FX quotes refreshed for five minutes.");
        return "redirect:/#student-finance";
    }

    @PostMapping("/student/source-account")
    public String selectSourceAccount(@RequestParam String account, RedirectAttributes flash) {
        PhaseFourService.PaymentSourceAccount selected = phaseFour.selectPaymentSource(account);
        flash.addFlashAttribute("message", "Payment source changed to " + selected.displayName() + ".");
        return "redirect:/#student-finance";
    }

    @PostMapping("/student/expenses")
    public String addStudentExpense(@RequestParam String expenseType,
                                    @RequestParam String title,
                                    @RequestParam String institution,
                                    @RequestParam BigDecimal amount,
                                    @RequestParam String destinationCountry,
                                    @RequestParam String currency,
                                    @RequestParam String recipientName,
                                    @RequestParam String recipientBankName,
                                    @RequestParam String recipientBankCode,
                                    @RequestParam String recipientAccount,
                                    @RequestParam String paymentReference,
                                    @RequestParam LocalDate dueDate,
                                    @RequestParam(required=false) MultipartFile document,
                                    RedirectAttributes flash) {
        String fileName = null;
        String contentType = null;
        Long size = null;
        if (document != null && !document.isEmpty()) {
            fileName = safeFileName(document.getOriginalFilename());
            contentType = document.getContentType();
            size = document.getSize();
        }
        int id = crossBorder.addExpense(expenseType, title, institution, amount, destinationCountry,
                currency, recipientName, recipientBankName, recipientBankCode, recipientAccount,
                paymentReference, dueDate, fileName, contentType, size);
        flash.addFlashAttribute("message", "Student bill added: #" + id + ". Only a verified beneficiary can be selected for comparison.");
        return "redirect:/#student-finance";
    }

    @PostMapping("/student/expenses/select")
    public String selectStudentExpense(@RequestParam int id, RedirectAttributes flash) {
        crossBorder.selectExpense(id);
        flash.addFlashAttribute("message", "Selected student expense updated. Channel costs were recalculated.");
        return "redirect:/#student-finance";
    }

    @PostMapping("/student/expenses/{id}/edit")
    public String editStudentExpense(@PathVariable int id,
                                     @RequestParam String expenseType,
                                     @RequestParam String title,
                                     @RequestParam String institution,
                                     @RequestParam BigDecimal amount,
                                     @RequestParam String destinationCountry,
                                     @RequestParam String currency,
                                     @RequestParam String recipientName,
                                     @RequestParam String recipientBankName,
                                     @RequestParam String recipientBankCode,
                                     @RequestParam String recipientAccount,
                                     @RequestParam String paymentReference,
                                     @RequestParam LocalDate dueDate,
                                     RedirectAttributes flash) {
        crossBorder.updateExpense(id, expenseType, title, institution, amount, destinationCountry,
                currency, recipientName, recipientBankName, recipientBankCode, recipientAccount,
                paymentReference, dueDate);
        flash.addFlashAttribute("message", "Student bill updated. Pending plans and approvals were invalidated.");
        return "redirect:/#student-finance";
    }

    @PostMapping("/student/expenses/{id}/archive")
    public String archiveStudentExpense(@PathVariable int id, RedirectAttributes flash) {
        crossBorder.archiveExpense(id);
        flash.addFlashAttribute("message", "Student bill archived. Receipts and Audit Log were preserved.");
        return "redirect:/#student-finance";
    }

    @PostMapping("/student/expenses/{id}/cancel")
    public String cancelStudentExpense(@PathVariable int id, RedirectAttributes flash) {
        crossBorder.cancelExpense(id);
        flash.addFlashAttribute("message", "Student bill cancelled. It can no longer be compared or paid.");
        return "redirect:/#student-finance";
    }

    @PostMapping("/student/expenses/{id}/restore")
    public String restoreStudentExpense(@PathVariable int id, RedirectAttributes flash) {
        crossBorder.restoreExpense(id);
        flash.addFlashAttribute("message", "Student bill restored as active.");
        return "redirect:/#student-finance";
    }

    private String safeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) return "attachment";
        String normalized = originalName.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        return fileName.isBlank() ? "attachment" : fileName.substring(0, Math.min(fileName.length(), 255));
    }

    @PostMapping("/agent/message")
    public String message(@RequestParam String message, RedirectAttributes flash) {
        phaseFour.sendMessage(message);
        flash.addFlashAttribute("message", "Conversation updated from deterministic demo data.");
        return "redirect:/#agent-workspace";
    }

    @PostMapping("/agent/mode")
    public String mode(@RequestParam String mode, @RequestParam(required=false) String action, RedirectAttributes flash) {
        phaseFour.setMode(mode);
        flash.addFlashAttribute("message", "Agent mode changed to " + mode + ".");
        return paymentRedirect(action);
    }

    @PostMapping("/agent/plans/tuition")
    public String tuitionPlan(@RequestParam String channel, @RequestParam String sourceAccountId,
                              @RequestParam int expenseId, @RequestParam String billVersion,
                              @RequestParam String quoteId, RedirectAttributes flash) {
        PhaseFourService.ActionPlan plan=phaseFour.createTuitionPlan(channel,sourceAccountId,expenseId,billVersion,quoteId);
        flash.addFlashAttribute("message", "Student payment plan "+plan.status()+". Approval is always required.");
        return paymentRedirect(plan.id());
    }

    @PostMapping("/agent/plans/low-risk")
    public String lowRiskPlan(@RequestParam(defaultValue="250000") BigDecimal amount, RedirectAttributes flash) {
        PhaseFourService.ActionPlan plan = phaseFour.createLowRiskPlan(amount);
        flash.addFlashAttribute("message", "Low-risk plan status: " + plan.status() + ".");
        return paymentRedirect(plan.id());
    }

    @PostMapping("/agent/actions/{id}/approve")
    public String approve(@PathVariable String id, RedirectAttributes flash) {
        PhaseFourService.Receipt receipt = phaseFour.approveAndExecute(id);
        PhaseFourService.ActionPlan plan = phaseFour.action(id);
        flash.addFlashAttribute("message", receipt == null
                ? "Action blocked: " + plan.status()
                : "Payment Sandbox completed: " + receipt.transactionId());
        return paymentRedirect(id);
    }

    @PostMapping("/agent/actions/{id}/cancel")
    public String cancelPlan(@PathVariable String id, RedirectAttributes flash) {
        phaseFour.cancel(id);
        flash.addFlashAttribute("message", "Payment plan canceled. No funds were debited.");
        return paymentRedirect(id);
    }

    @PostMapping("/agent/actions/{id}/retry")
    public String retry(@PathVariable String id, RedirectAttributes flash) {
        PhaseFourService.Receipt receipt = phaseFour.execute(id);
        flash.addFlashAttribute("message", receipt == null
                ? "Retry blocked by Policy Guard."
                : "Idempotent receipt: " + receipt.transactionId());
        return paymentRedirect(id);
    }

    @PostMapping("/agent/offline")
    public String offlineFallback(@RequestParam(required=false) String action, RedirectAttributes flash) {
        phaseFour.enableOfflineFallback();
        flash.addFlashAttribute("message", "Offline fallback active: deterministic local responses are available without the LLM.");
        return paymentRedirect(action);
    }

    @PostMapping("/agent/emergency-stop")
    public String emergencyStop(@RequestParam(required=false) String action, RedirectAttributes flash) {
        phaseFour.emergencyStop();
        flash.addFlashAttribute("message", "Emergency Stop active. New actions receive AGENT PAUSED.");
        return paymentRedirect(action);
    }

    @PostMapping("/agent/resume")
    public String resume(@RequestParam(required=false) String action, RedirectAttributes flash) {
        phaseFour.resumeAgent();
        flash.addFlashAttribute("message", "Agent resumed.");
        return paymentRedirect(action);
    }

    private String paymentRedirect(String actionId) {
        return actionId==null || actionId.isBlank()?"redirect:/#agent-workspace"
                : "redirect:/?action="+java.net.URLEncoder.encode(actionId,java.nio.charset.StandardCharsets.UTF_8)+"#agent-workspace";
    }

    @PostMapping("/reset")
    public String reset(RedirectAttributes flash) {
        demoData.resetAll();
        flash.addFlashAttribute("message", "Synthetic Phase 4 data reset.");
        return "redirect:/";
    }

    @ExceptionHandler(Exception.class)
    public String error(Exception ex, HttpServletRequest request, RedirectAttributes flash) {
        flash.addFlashAttribute("message", "Request failed: " + ex.getMessage());
        String uri=request.getRequestURI();
        if (uri.startsWith("/agent/actions/")) {
            String[] segments=uri.split("/");
            if (segments.length>3) return paymentRedirect(segments[3]);
        }
        if (uri.equals("/agent/plans/tuition")) return "redirect:/#student-finance";
        if (uri.startsWith("/agent/")) return paymentRedirect(request.getParameter("action"));
        return uri.startsWith("/student/")?"redirect:/#student-finance":"redirect:/";
    }
}
