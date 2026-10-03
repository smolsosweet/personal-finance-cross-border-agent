package com.example.finance;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ConversationContextController {
    private final SessionConversationService conversations;
    public ConversationContextController(SessionConversationService conversations){this.conversations=conversations;}
    @PostMapping("/agent/runway/monthly-expense")
    public String monthly(@RequestParam String token,@RequestParam(required=false) String amount,
                          @RequestParam(required=false) String currency,@RequestParam(defaultValue="false") boolean clear,
                          @RequestParam(required=false) String language,HttpServletRequest request,RedirectAttributes flash){
        String answer=conversations.monthlyExpense(request.getSession(),token,amount,currency,clear,"vi".equals(language));
        flash.addFlashAttribute("message",answer.startsWith("ƯỚC TÍNH")||answer.startsWith("LIVING-EXPENSE")
                ? "Living-expense scenario updated; estimate only, no payment." : answer);return "redirect:/";
    }
    @PostMapping({"/agent/context","/agent/context/choice"})
    public String select(@RequestParam String token,@RequestParam(required=false) String language,
                         HttpServletRequest request,RedirectAttributes flash){
        String answer=conversations.enter(request.getSession(),token,"vi".equals(language),request.getRequestURI().endsWith("/choice"));
        String reviewAction=conversations.reviewPlanId(request.getSession());
        if(reviewAction!=null)flash.addFlashAttribute("conversationReviewAction",reviewAction);
        flash.addFlashAttribute("message",answer);return "redirect:/";
    }
}
