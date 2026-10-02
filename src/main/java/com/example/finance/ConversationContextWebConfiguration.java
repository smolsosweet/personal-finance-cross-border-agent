package com.example.finance;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Context invalidation only. Business endpoints, Policy Guard and execution are unchanged. */
@Configuration
public class ConversationContextWebConfiguration implements WebMvcConfigurer {
    private final SessionConversationService conversations;
    public ConversationContextWebConfiguration(SessionConversationService conversations){this.conversations=conversations;}
    @Override public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(new HandlerInterceptor(){
            @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler){
                String path=request.getRequestURI();
                if("POST".equals(request.getMethod())){
                    if(path.equals("/reset"))conversations.resetContexts();
                    else if(path.startsWith("/student/")||path.startsWith("/agent/actions/")
                            ||path.startsWith("/agent/plans/")||path.equals("/agent/emergency-stop"))
                        conversations.invalidate(request.getSession(false));
                }else if(path.equals("/")&&request.getParameter("action")!=null)conversations.invalidate(request.getSession(false));
                return true;
            }
        });
    }
}
