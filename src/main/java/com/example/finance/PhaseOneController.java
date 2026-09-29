package com.example.finance;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PhaseOneController {
    private final TransactionService transactions;
    private final CrossBorderService crossBorder;
    private final PhaseFourService phaseFour;
    private final DemoDataService demoData;

    public PhaseOneController(TransactionService transactions, CrossBorderService crossBorder,
                              PhaseFourService phaseFour, DemoDataService demoData) {
        this.transactions = transactions;
        this.crossBorder = crossBorder;
        this.phaseFour = phaseFour;
        this.demoData = demoData;
    }

    @Bean
    ApplicationRunner seedDemo(DemoDataService demoData) {
        return args -> demoData.initialize();
    }

    @GetMapping("/")
    public String home(@RequestParam(required=false) String review, Model model) {
        model.addAttribute("profile", transactions.profile());
        model.addAttribute("accounts", transactions.accounts());
        model.addAttribute("transactions", transactions.transactions());
        model.addAttribute("eventCount", transactions.eventCount());
        model.addAttribute("dashboard", transactions.dashboard());
        model.addAttribute("budgets", transactions.budgetSummary());
        model.addAttribute("insights", transactions.proactiveFeed());
        model.addAttribute("tuitionInsight", crossBorder.tuitionInsight());
        model.addAttribute("studentProfile", crossBorder.profile());
        model.addAttribute("tuitionBill", crossBorder.bill());
        model.addAttribute("recipientVerification", crossBorder.verifyRecipient());
        model.addAttribute("channelQuotes", crossBorder.rankedQuotes());
        model.addAttribute("agentPolicy", phaseFour.policy());
        model.addAttribute("conversation", phaseFour.messages());
        model.addAttribute("latestAction", phaseFour.latestAction());
        model.addAttribute("latestReceipt", phaseFour.latestReceipt());
        model.addAttribute("sandboxAccounts", phaseFour.sandboxAccounts());
        model.addAttribute("auditEvents", phaseFour.auditEvents());
        if (review != null && !review.isBlank()) model.addAttribute("reviewTransaction", transactions.transaction(review));
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
            return "redirect:/?review=" + id + "#transactions";
        }
        return "redirect:/#transactions";
    }

    @PostMapping("/transactions/{id}/category")
    public String confirmCategory(@PathVariable String id, @RequestParam String category, RedirectAttributes flash) {
        transactions.confirmCategory(id, category);
        flash.addFlashAttribute("message", "Category confirmed and dashboard updated.");
        return "redirect:/#transactions";
    }

    @PostMapping("/transactions/{id}/undo")
    public String undoCategory(@PathVariable String id, RedirectAttributes flash) {
        transactions.undoCategory(id);
        flash.addFlashAttribute("message", "Category change undone.");
        return "redirect:/?review=" + id + "#transactions";
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

    @PostMapping("/agent/message")
    public String message(@RequestParam String message, RedirectAttributes flash) {
        phaseFour.sendMessage(message);
        flash.addFlashAttribute("message", "Conversation updated from deterministic demo data.");
        return "redirect:/#agent-workspace";
    }

    @PostMapping("/agent/mode")
    public String mode(@RequestParam String mode, RedirectAttributes flash) {
        phaseFour.setMode(mode);
        flash.addFlashAttribute("message", "Agent mode changed to " + mode + ".");
        return "redirect:/#agent-workspace";
    }

    @PostMapping("/agent/plans/tuition")
    public String tuitionPlan(@RequestParam String channel, RedirectAttributes flash) {
        PhaseFourService.ActionPlan plan = phaseFour.createTuitionPlan(channel);
        flash.addFlashAttribute("message", "Tuition plan " + plan.status() + ". Approval is always required.");
        return "redirect:/#agent-workspace";
    }

    @PostMapping("/agent/plans/low-risk")
    public String lowRiskPlan(@RequestParam(defaultValue="250000") BigDecimal amount, RedirectAttributes flash) {
        PhaseFourService.ActionPlan plan = phaseFour.createLowRiskPlan(amount);
        flash.addFlashAttribute("message", "Low-risk plan status: " + plan.status() + ".");
        return "redirect:/#agent-workspace";
    }

    @PostMapping("/agent/actions/{id}/approve")
    public String approve(@PathVariable String id, RedirectAttributes flash) {
        PhaseFourService.Receipt receipt = phaseFour.approveAndExecute(id);
        PhaseFourService.ActionPlan plan = phaseFour.action(id);
        flash.addFlashAttribute("message", receipt == null
                ? "Action blocked: " + plan.status()
                : "Payment Sandbox completed: " + receipt.transactionId());
        return "redirect:/#agent-workspace";
    }

    @PostMapping("/agent/actions/{id}/retry")
    public String retry(@PathVariable String id, RedirectAttributes flash) {
        PhaseFourService.Receipt receipt = phaseFour.execute(id);
        flash.addFlashAttribute("message", receipt == null
                ? "Retry blocked by Policy Guard."
                : "Idempotent receipt: " + receipt.transactionId());
        return "redirect:/#agent-workspace";
    }

    @PostMapping("/agent/offline")
    public String offlineFallback(RedirectAttributes flash) {
        phaseFour.enableOfflineFallback();
        flash.addFlashAttribute("message", "Offline fallback active: deterministic local responses are available without the LLM.");
        return "redirect:/#agent-workspace";
    }

    @PostMapping("/agent/emergency-stop")
    public String emergencyStop(RedirectAttributes flash) {
        phaseFour.emergencyStop();
        flash.addFlashAttribute("message", "Emergency Stop active. New actions receive AGENT PAUSED.");
        return "redirect:/#agent-workspace";
    }

    @PostMapping("/agent/resume")
    public String resume(RedirectAttributes flash) {
        phaseFour.resumeAgent();
        flash.addFlashAttribute("message", "Agent resumed.");
        return "redirect:/#agent-workspace";
    }

    @PostMapping("/reset")
    public String reset(RedirectAttributes flash) {
        demoData.resetAll();
        flash.addFlashAttribute("message", "Synthetic Phase 4 data reset.");
        return "redirect:/";
    }

    @ExceptionHandler(Exception.class)
    public String error(Exception ex, RedirectAttributes flash) {
        flash.addFlashAttribute("message", "Request failed: " + ex.getMessage());
        return "redirect:/";
    }
}
