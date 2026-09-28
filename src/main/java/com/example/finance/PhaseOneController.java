package com.example.finance;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PhaseOneController {
    private final TransactionService transactions;
    public PhaseOneController(TransactionService transactions) { this.transactions = transactions; }
    @Bean ApplicationRunner seedPhaseOne(TransactionService transactions) { return args -> transactions.seedIfEmpty(); }
    @GetMapping("/") public String home(Model model) {
        model.addAttribute("profile", transactions.profile());
        model.addAttribute("accounts", transactions.accounts());
        model.addAttribute("transactions", transactions.transactions());
        model.addAttribute("eventCount", transactions.eventCount());
        return "home";
    }
    @PostMapping("/events/simulate") public String simulate(@RequestParam(defaultValue="expense") String scenario, RedirectAttributes flash) {
        String id = transactions.simulate(scenario);
        flash.addFlashAttribute("message", "Simulated Bank Event normalized: " + id);
        return "redirect:/#transactions";
    }
    @PostMapping("/reset") public String reset(RedirectAttributes flash) {
        transactions.reset();
        flash.addFlashAttribute("message", "Synthetic demo data reset to 22 transactions.");
        return "redirect:/";
    }
    @ExceptionHandler(Exception.class) public String error(Exception ex, RedirectAttributes flash) {
        flash.addFlashAttribute("message", "Request failed: " + ex.getMessage());
        return "redirect:/";
    }
}
