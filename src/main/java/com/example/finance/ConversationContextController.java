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
    @PostMapping({"/agent/context","/agent/context/choice"})
    public String select(@RequestParam String token,@RequestParam(required=false) String language,
                         HttpServletRequest request,RedirectAttributes flash){
        String answer=conversations.enter(request.getSession(),token,"vi".equals(language),request.getRequestURI().endsWith("/choice"));
        flash.addFlashAttribute("message",answer);return "redirect:/";
    }
}
