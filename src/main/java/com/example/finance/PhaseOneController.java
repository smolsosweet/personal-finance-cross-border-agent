package com.example.finance;

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

    public PhaseOneController(TransactionService transactions) {
        this.transactions = transactions;
    }

    @Bean
    ApplicationRunner seedDemo(TransactionService transactions) {
        return args -> transactions.seedIfEmpty();
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

    @PostMapping("/reset")
    public String reset(RedirectAttributes flash) {
        transactions.reset();
        flash.addFlashAttribute("message", "Synthetic Phase 2 data reset to 22 transactions.");
        return "redirect:/";
    }

    @ExceptionHandler(Exception.class)
    public String error(Exception ex, RedirectAttributes flash) {
        flash.addFlashAttribute("message", "Request failed: " + ex.getMessage());
        return "redirect:/";
    }
}
