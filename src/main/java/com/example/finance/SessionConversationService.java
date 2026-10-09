package com.example.finance;

import static com.example.finance.ModelConversationContext.*;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Session memory is transient. Bindings contain verified IDs/versions, never reusable money or quote facts. */
@Service
public class SessionConversationService {
    private static final String ATTRIBUTE = SessionConversationService.class.getName();
    private static final String WELCOME = "Ask about spending, budgets or tuition. I can prepare a tuition plan for you to review; payments always need explicit approval.";
    private final Set<State> sessions = Collections.newSetFromMap(new WeakHashMap<>());
    private final FinanceChatService chat;
    private final PhaseFourService payments;
    private final CrossBorderService crossBorder;
    private final PersonalFinanceInsights insights;
    private final LivingExpenseRunwayService runwayService;
    private final GlobalAssistantQueries queries;
    private final JdbcTemplate db;
    private final TransactionTemplate tx;

    public SessionConversationService(FinanceChatService chat, PhaseFourService payments, CrossBorderService crossBorder,
            PersonalFinanceInsights insights, LivingExpenseRunwayService runwayService, GlobalAssistantQueries queries, JdbcTemplate db, PlatformTransactionManager manager) {
        this.chat=chat; this.payments=payments; this.crossBorder=crossBorder; this.insights=insights;
        this.db=db; this.tx=new TransactionTemplate(manager);
        this.runwayService=runwayService;
        this.queries=queries;
    }

    record Binding(Integer bill, String version, String account, Channel channel, String plan,
                   String planHash, Integer workspaceBill, String workspaceAccount, Map<String,String> quotes) {}
    record Capability(Binding binding, Topic topic, LocalDateTime expires, Channel discussedChannel) {
        Capability(Binding binding, Topic topic, LocalDateTime expires){this(binding,topic,expires,Channel.NONE);}
    }
    record Choice(Capability capability, long revision, String label) {}
    public record ChoiceView(String token, String label) {}
    public record RunwayView(boolean visible, String token, BigDecimal monthly) {}
    public record View(String studentToken, String planToken, Topic topic, Pending pending, String clarification,
                       List<ChoiceView> choices, List<PhaseFourService.ConversationMessage> messages, RunwayView runway) {}

    static final class State {
        String epoch;
        long revision;
        Topic topic=Topic.NONE;
        LlmIntent.Intent lastIntent;
        Pending pending=Pending.NONE;
        Binding binding;
        Binding paymentScope, historyScope, balanceScope;
        Channel discussedChannel=Channel.NONE;
        String clarification="";
        BigDecimal monthly;
        Binding monthlyScope;
        boolean runwayRequested;
        String scenarioToken;
        LocalDateTime scenarioExpiry;
        void clearScenario(){monthly=null;monthlyScope=null;runwayRequested=false;scenarioToken=null;scenarioExpiry=null;}
        final LinkedHashMap<String,Capability> capabilities=new LinkedHashMap<>();
        final LinkedHashMap<String,Choice> choices=new LinkedHashMap<>();
        // Display-only history is private to this session and is never model input or authoritative context.
        final List<PhaseFourService.ConversationMessage> display=new ArrayList<>();
        void clear(String resetEpoch) {
            epoch=resetEpoch; revision++; topic=Topic.NONE; lastIntent=null; pending=Pending.NONE; binding=null;
            paymentScope=null; historyScope=null; balanceScope=null;
            discussedChannel=Channel.NONE;
            clarification=""; capabilities.clear(); choices.clear(); display.clear();
            clearScenario();
            display.add(new PhaseFourService.ConversationMessage("WELCOME","ASSISTANT",WELCOME,LocalDateTime.now()));
        }
    }

    private String epoch() {
        var rows=db.queryForList("SELECT id FROM audit_log WHERE event_type='DEMO_RESET' ORDER BY occurred_at DESC,id DESC FETCH FIRST 1 ROW ONLY",String.class);
        return rows.isEmpty()?"EMPTY":rows.getFirst();
    }
    private State state(HttpSession session) {
        synchronized(session) {
            State state=(State)session.getAttribute(ATTRIBUTE);
            if(state==null) { state=new State(); session.setAttribute(ATTRIBUTE,state); synchronized(sessions){sessions.add(state);} }
            synchronized(state) { String current=epoch(); if(!current.equals(state.epoch))state.clear(current); }
            return state;
        }
    }
    public void resetContexts() {
        synchronized(sessions) { for(State state:sessions) synchronized(state){state.clear(epoch());} }
    }
    public void invalidate(HttpSession session) {
        if(session==null)return;
        State state=state(session);
        synchronized(state) { state.revision++; state.binding=null; state.topic=Topic.NONE; state.lastIntent=null;
            state.paymentScope=null;state.historyScope=null;state.balanceScope=null;
            state.discussedChannel=Channel.NONE;
            state.pending=Pending.NONE; state.choices.clear(); state.clarification=""; state.clearScenario(); }
    }
    private void lock(){db.queryForObject("SELECT id FROM agent_policy WHERE id=1 FOR UPDATE",Integer.class);}
    private Integer selectedBill(){try{return crossBorder.selectedExpense().id();}catch(RuntimeException ex){return null;}}
    private String selectedAccount(){var rows=payments.paymentSourceAccounts().stream().filter(PhaseFourService.PaymentSourceAccount::selected).toList();return rows.size()==1?rows.getFirst().accountId():null;}
    private Binding billBinding(int id) {
        var bill=crossBorder.expense(id);
        return new Binding(id,bill.updatedAt().toString(),selectedAccount(),Channel.NONE,null,null,selectedBill(),selectedAccount(),Map.of());
    }
    private Binding planBinding(PhaseFourService.ActionPlan plan) {
        var bill=plan.expenseId()==null?null:crossBorder.expense(plan.expenseId());
        return new Binding(plan.expenseId(),bill==null?null:bill.updatedAt().toString(),plan.sourceAccountId(),channel(plan.channelId()),
                plan.id(),plan.actionHash(),selectedBill(),selectedAccount(),Map.of());
    }
    private PhaseFourService.ActionPlan completedPlanForBill(Integer billId) {
        if (billId == null) return null;
        var plan=payments.completedPlansByExpense().get(billId);
        return plan!=null && payments.receiptForAction(plan.id())!=null ? plan : null;
    }
    private static Channel channel(String value){try{return Channel.valueOf(value);}catch(Exception ex){return Channel.NONE;}}
    private Binding accountBinding(String account) {
        return new Binding(null,null,account,Channel.NONE,null,null,selectedBill(),selectedAccount(),Map.of());
    }
    private void remember(State state, Topic topic, Binding scope) {
        if(topic==Topic.CURRENT_BALANCE)state.balanceScope=scope;
        else if(topic==Topic.RECEIPT_BALANCE || scope!=null&&scope.plan()!=null&&"COMPLETED".equals(payments.action(scope.plan()).status()))
            state.historyScope=scope;
        else if(scope!=null&&scope.bill()!=null&&topic!=Topic.CHANNEL_UNAVAILABLE)state.paymentScope=scope;
    }
    private String issue(State state, Binding binding, Topic topic) {
        String token=UUID.randomUUID().toString();
        state.capabilities.put(token,new Capability(binding,topic,LocalDateTime.now().plusMinutes(15)));
        while(state.capabilities.size()>48)state.capabilities.remove(state.capabilities.keySet().iterator().next());
        return token;
    }
    public View view(HttpSession session, CrossBorderService.StudentExpense bill, PhaseFourService.ActionPlan plan) {
        State state=state(session);
        synchronized(state) {
            if(state.runwayRequested && state.binding!=null && validate(state.binding)!=null){state.clearScenario();state.revision++;}
            if(state.runwayRequested){state.scenarioToken=UUID.randomUUID().toString();state.scenarioExpiry=LocalDateTime.now().plusMinutes(10);}
            var completed=bill==null?null:completedPlanForBill(bill.id());
            String student=bill==null?null:issue(state,completed==null?billBinding(bill.id()):planBinding(completed),Topic.TUITION_AFFORDABILITY);
            String action=plan==null||!"TUITION".equals(plan.actionType())?null:issue(state,planBinding(plan),Topic.TUITION_PLAN);
            return new View(student,action,state.topic,state.pending,state.clarification,
                    state.choices.entrySet().stream().map(e->new ChoiceView(e.getKey(),e.getValue().label())).toList(),List.copyOf(state.display),
                    new RunwayView(state.runwayRequested,state.scenarioToken,state.monthly));
        }
    }
    public String enter(HttpSession session,String token,boolean vi,boolean choice) {
        State state=state(session);
        return tx.execute(status->{lock(); synchronized(state){
            Capability capability;
            if(choice){var item=state.choices.get(token);capability=item!=null&&item.revision()==state.revision?item.capability():null;}
            else capability=state.capabilities.get(token);
            String error=capability==null||capability.expires().isBefore(LocalDateTime.now())?"INVALID CONTEXT TOKEN":validate(capability.binding());
            if(error!=null){audit("CONTEXT_STALE");return vi?"Lựa chọn không còn hợp lệ cho phiên này. Mở lại màn hình hoặc hỏi rõ chủ đề.":"This selection is invalid or stale for this session. Reopen the screen or clarify the topic.";}
            state.revision++; state.binding=capability.binding(); state.topic=capability.topic();
            remember(state,state.topic,state.binding);
            state.discussedChannel=capability.discussedChannel();
            if(capability.topic()!=Topic.CHANNEL_UNAVAILABLE)state.clearScenario();
            state.pending=Pending.NONE;state.choices.clear();state.clarification="";
            audit(choice?"CONTEXT_CHOICE_VALIDATED":"CONTEXT_ENTRY_VALIDATED");
            String answer=vi?"Đã cập nhật chủ đề và đối tượng đã xác minh. Bạn có thể hỏi tiếp; chưa tạo kế hoạch hay thanh toán.":"Verified conversation scope updated. You can ask a follow-up; no plan or payment was created.";
            record(state,"ASSISTANT",answer);return answer;
        }});
    }
    public String send(HttpSession session,String message,String language) {
        return chat.send(message,language,payments,new Turn(state(session),message,language));
    }
    /** UI review follows this session's verified plan, never an ID extracted from model text. */
    public String reviewPlanId(HttpSession session) {
        State state=state(session);
        synchronized(state) {
            return state.topic==Topic.TUITION_PLAN && state.binding!=null && validate(state.binding)==null
                    ? state.binding.plan() : null;
        }
    }

    /** The chat shortcut belongs to its verified conversation, not the workspace's last payment. */
    public String assistantPlanId(HttpSession session) {
        State state = state(session);
        synchronized (state) {
            if (state.binding == null || !Set.of(Topic.TUITION_PLAN, Topic.TUITION_STATUS,
                    Topic.RECEIPT_BALANCE, Topic.TUITION_AFFORDABILITY, Topic.LIVING_EXPENSE_RUNWAY)
                    .contains(state.topic) || validate(state.binding) != null) return null;
            return state.binding.plan();
        }
    }
    private static String monthlyPrompt(boolean vi){return vi
            ?"Bạn dự kiến cần bao nhiêu VND mỗi tháng để sinh hoạt? Xác nhận trong form, gồm tiền thuê nhà, ăn uống, đi lại và tiện ích. Đây chỉ là giả định kịch bản, không phải ngân sách hoặc quyền thanh toán."
            :"How much VND do you expect to need each month for living expenses? Confirm the form, including rent, food, transport and utilities. This is a scenario assumption, not a budget or payment authorization.";}
    public String monthlyExpense(HttpSession session,String token,String amount,String currency,boolean clear,boolean vi){
        State state=state(session);
        return tx.execute(status->{lock();synchronized(state){
            if(!state.runwayRequested || token==null || !token.equals(state.scenarioToken)
                    || state.scenarioExpiry==null || !state.scenarioExpiry.isAfter(LocalDateTime.now())){
                audit("CONTEXT_STALE");return LivingExpenseRunwayService.error("INVALID_RUNWAY_DATA",vi);
            }
            state.revision++;state.scenarioToken=null;
            String answer;
            if(clear){state.monthly=null;state.monthlyScope=null;state.pending=Pending.MONTHLY_EXPENSE;
                audit("RUNWAY_SCENARIO_CLEARED");answer=monthlyPrompt(vi);
            }else try{
                BigDecimal monthly=LivingExpenseRunwayService.parseMonthly(amount,currency);
                if(state.binding==null || validate(state.binding)!=null)throw new IllegalArgumentException("INVALID_RUNWAY_DATA");
                var input=runwayService.inspect(state.binding.bill(),state.binding.account(),state.binding.channel(),state.binding.plan());
                state.monthly=monthly;state.monthlyScope=state.binding;state.topic=Topic.LIVING_EXPENSE_RUNWAY;
                state.pending=Pending.NONE;state.lastIntent=LlmIntent.Intent.EXPLAIN_LIVING_EXPENSE_RUNWAY;
                state.choices.clear();state.clarification="";
                audit("RUNWAY_SCENARIO_CONFIRMED");answer=runwayService.render(input,monthly,vi);audit("RUNWAY_RESULT");
            }catch(IllegalArgumentException ex){audit("RUNWAY_INVALID_DATA");answer=LivingExpenseRunwayService.error(ex.getMessage(),vi);}
            record(state,"ASSISTANT",answer);return answer;
        }});
    }
    private String validate(Binding binding) {
        if(binding==null)return null;
        try{
            if(binding.plan()!=null){
                var plan=payments.action(binding.plan());
                if("COMPLETED".equals(plan.status())&&payments.receiptForAction(plan.id())!=null)
                    return plan.actionHash().equals(binding.planHash())?null:"PLAN CHANGED";
            }
            if(!Objects.equals(binding.workspaceBill(),selectedBill())||!Objects.equals(binding.workspaceAccount(),selectedAccount()))return "WORKSPACE CHANGED";
            if(binding.bill()!=null){var bill=crossBorder.expense(binding.bill());
                if(!bill.updatedAt().toString().equals(binding.version())||!crossBorder.verifyRecipient(bill.id()).verified())return "BILL CHANGED";
                if(binding.plan()==null&&(!bill.active()||bill.executed()||!"TUITION".equals(bill.expenseType())))return "BILL INACTIVE";
            }
            if(binding.account()!=null&&payments.paymentSourceAccounts().stream().noneMatch(a->a.accountId().equals(binding.account())))return "ACCOUNT MISSING";
            if(binding.plan()!=null&&!payments.action(binding.plan()).actionHash().equals(binding.planHash()))return "PLAN CHANGED";
            if(binding.bill()!=null&&!binding.quotes().isEmpty()){
                var quotes=crossBorder.rankedQuotesForExpense(binding.bill());
                for(var reference:binding.quotes().entrySet()){
                    var quote=quotes.stream().filter(q->q.channelId().equals(reference.getKey())).findFirst().orElse(null);
                    if(quote==null||quote.expired()||!quote.quoteId().equals(reference.getValue()))return "QUOTE STALE";
                }
            }
            return null;
        }catch(RuntimeException ex){return "REFERENCE MISSING";}
    }
    private void record(State state,String role,String text) {
        synchronized(state){
            // One display ID per reply reunites storage-sized chunks; it is never model context.
            String displayId="MSG-"+UUID.randomUUID(); int chunk=0;
            for(int offset=0;offset<text.length();){int end=Math.min(offset+990,text.length());
                if(end<text.length()){int newline=text.lastIndexOf('\n',end);if(newline>offset)end=newline+1;}
                state.display.add(new PhaseFourService.ConversationMessage(displayId+"-"+chunk++,role,text.substring(offset,end),LocalDateTime.now()));offset=end;
            }
            while(state.display.size()>32)state.display.removeFirst();
        }
    }
    private void audit(String reason){db.update("INSERT INTO audit_log VALUES (?,?,?,?,?,?,?,?)","AUD-"+UUID.randomUUID(),LocalDateTime.now(),"CONVERSATION_CONTEXT","CONTEXT_LIFECYCLE",null,"COMPLETED",reason,"Validated session context; no prompt, transcript, account or financial payload");}
    private static boolean matches(String message,String pattern){return Pattern.compile(pattern,Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE).matcher(message).find();}
    private static boolean explicitDraft(String message){return !matches(message,"do not|don't|never|without|đừng|không|compare|explain|so sánh|giải thích")&&matches(message,"\\b(create|prepare|draft|make|build|ready)\\b.*\\b(plan|draft|tuition|payment)\\b|\\bget\\b.*\\b(tuition|payment)\\b.*\\bready\\b|(tạo|chuẩn bị|lập|soạn).*?(kế hoạch|bản nháp)");}
    private static boolean personalTopic(String message){return matches(message,"budget|ngân sách|spend|chi tiêu")&&!matches(message,"tuition|học phí|after|sau");}
    private static boolean runway(String message){return matches(message,"runway|(mấy|bao nhiêu).*tháng|how many months|months.*living|living.*months");}
    private static Channel namedChannel(String message){
        var names=new LinkedHashMap<Channel,String>();
        names.put(Channel.BANK_B,"bank\\s*b|ngân hàng\\s*b");names.put(Channel.BANK_A,"bank\\s*a|ngân hàng\\s*a");
        names.put(Channel.ALIPAY,"alipay");names.put(Channel.VCB,"vietcombank|\\bvcb\\b");names.put(Channel.TCB,"techcombank|\\btcb\\b");names.put(Channel.MOMO,"momo");
        var found=names.entrySet().stream().filter(e->matches(message,e.getValue())).map(Map.Entry::getKey).toList();
        return found.size()==1?found.getFirst():Channel.NONE;
    }

    private final class Turn implements FinanceChatService.ContextTurn {
        private final State state;
        private final String message;
        private final boolean vi;
        private long revision;
        private String resetEpoch;
        private Topic topic;
        private LlmIntent.Intent lastIntent;
        private Pending pending;
        private Binding binding;
        private Channel discussedChannel;
        private Topic requestedTopic=Topic.NONE;
        private String reason="CONTEXT_CLARIFICATION_REQUIRED";
        Turn(State state,String message,String language){this.state=state;this.message=message==null?"":message.trim();this.vi="vi".equals(language)||language==null&&matches(this.message,"[À-ỹ]");
            synchronized(state){revision=++state.revision;resetEpoch=state.epoch;topic=state.topic;lastIntent=state.lastIntent;pending=state.pending;binding=state.binding;discussedChannel=state.discussedChannel;}}
        public String begin(){synchronized(state){
            if(revision!=state.revision){reason="CONTEXT_STALE";return vi?"Yêu cầu cũ đã bị thay thế.":"This request has been superseded.";}
            if(personalTopic(message) && (topic!=Topic.LIVING_EXPENSE_RUNWAY || matches(message,"budget|ngân sách|recorded|chi tiêu tháng"))){binding=null;topic=Topic.NONE;}
            if(binding!=null&&binding.bill()!=null&&binding.plan()==null
                    && Objects.equals(binding.workspaceBill(),selectedBill())
                    && Objects.equals(binding.workspaceAccount(),selectedAccount())){
                var completed=completedPlanForBill(binding.bill());
                if(completed!=null&&Objects.equals(binding.account(),completed.sourceAccountId()))
                    binding=planBinding(completed);
            }
            String invalid=validate(binding);
            if(invalid!=null){reason="CONTEXT_STALE";return vi?"Ngữ cảnh hoặc báo giá đã thay đổi/hết hạn. Chọn lại đối tượng; làm mới báo giá bằng luồng có hướng dẫn nếu cần.":"Context or quote changed/expired. Choose the object again; refresh quotes through the guided flow if needed.";}
            return null;
        }}
        public ModelConversationContext summary(){return new ModelConversationContext(topic,
                topic==Topic.CHANNEL_UNAVAILABLE?discussedChannel:binding==null?Channel.NONE:binding.channel(),lastIntent,pending);}
        public Object monitor(){return state;}
        public boolean current(){synchronized(state){return revision==state.revision&&Objects.equals(resetEpoch,epoch());}}
        public void record(String role,String text){synchronized(state){if(current())SessionConversationService.this.record(state,role,text);}}
        public String reason(){return reason;}
        public void rejected(String why,boolean vietnamese){
            if("UNTRUSTED INSTRUCTION".equals(why)){reason="CONTEXT_UNSAFE_INPUT";return;}
            if("LLM UNAVAILABLE".equals(why)){reason="CONTEXT_PROVIDER_UNAVAILABLE";return;}
            if("CONTEXT_UNSUPPORTED".equals(why))return;
            if("CONTEXT_STALE".equals(why)){synchronized(state){state.binding=null;state.discussedChannel=Channel.NONE;state.topic=Topic.NONE;state.lastIntent=null;state.choices.clear();state.pending=Pending.NONE;state.clarification="";state.clearScenario();
                if(state.paymentScope!=null&&validate(state.paymentScope)!=null)state.paymentScope=null;
                if(state.historyScope!=null&&validate(state.historyScope)!=null)state.historyScope=null;
            }reason="CONTEXT_STALE";return;}
            reason="CONTEXT_CLARIFICATION_REQUIRED";
        }
        private String clarify(Pending kind,String explanation,List<Capability> options,List<String> labels){synchronized(state){
            reason="CONTEXT_CLARIFICATION_REQUIRED";state.pending=kind;state.clarification=explanation;state.choices.clear();
            if(requestedTopic!=Topic.NONE)state.topic=requestedTopic;
            for(int i=0;i<options.size();i++)state.choices.put(UUID.randomUUID().toString(),new Choice(options.get(i),state.revision,labels.get(i)));
            return explanation;
        }}
        private String clarifyTopic(){
            var topics=List.of(Topic.BUDGET,Topic.SPENDING,Topic.TUITION_CHANNELS);
            return clarify(Pending.TOPIC,vi?"Bạn muốn hỏi ngân sách còn lại, chi tiêu hay kênh học phí? Hãy chọn chủ đề.":"Do you mean remaining budget, spending, or tuition channels? Choose a topic.",
                    topics.stream().map(t->new Capability(null,t,LocalDateTime.now().plusMinutes(10))).toList(),
                    vi?List.of("Ngân sách","Chi tiêu","Kênh học phí"):List.of("Budget","Spending","Tuition channels"));
        }
        private String ensureBill(){return ensureBill(Topic.TUITION_CHANNELS);}
        private String ensureBill(Topic choiceTopic){
            var named=crossBorder.expenses().stream().filter(b->GlobalAssistantQueries.contains(message,b.paymentReference())
                    ||GlobalAssistantQueries.contains(message,b.title())).toList();
            if(!named.isEmpty()){
                if(named.size()!=1)return billChoices(named,choiceTopic);
                var bill=named.getFirst();
                var completed=completedPlanForBill(bill.id());
                binding=completed!=null?planBinding(completed):billBinding(bill.id());
                return null;
            }
            if(matches(message,"\\b(?:SZDU|TUITION)-[A-Z0-9-]+\\b"))
                return billChoices(crossBorder.expenses().stream().filter(b->b.active()&&!b.executed()
                        &&"TUITION".equals(b.expenseType())&&crossBorder.verifyRecipient(b.id()).verified()).toList(),choiceTopic);
            if(binding==null || binding.bill()==null)binding=state.paymentScope;
            if(binding!=null&&binding.bill()!=null){
                var completed=completedPlanForBill(binding.bill());
                if(completed!=null)binding=planBinding(completed);
                return null;
            }
            Integer selected=selectedBill();
            if(choiceTopic==Topic.TUITION_AFFORDABILITY&&selected!=null){
                var selectedBill=crossBorder.expense(selected);
                if(selectedBill.executed()){
                    var completed=completedPlanForBill(selected);
                    if(completed!=null){binding=planBinding(completed);return null;}
                    return clarify(Pending.BILL,vi?"Không tìm thấy biên nhận Sandbox cho hóa đơn đã thanh toán này.":"No Sandbox receipt was found for this paid bill.",List.of(),List.of());
                }
            }
            var bills=crossBorder.expenses().stream().filter(b->b.active()&&!b.executed()&&"TUITION".equals(b.expenseType())&&crossBorder.verifyRecipient(b.id()).verified()).toList();
            if(bills.size()!=1)return clarify(Pending.BILL,vi?"Hãy chọn rõ hóa đơn học phí đã xác minh cần hỏi.":"Choose the verified tuition bill you mean.",
                    bills.stream().map(b->new Capability(billBinding(b.id()),choiceTopic,LocalDateTime.now().plusMinutes(10))).toList(),
                    bills.stream().map(b->b.title()+" · "+b.paymentReference()).toList());
            binding=billBinding(bills.getFirst().id());return null;
        }
        private String billChoices(List<CrossBorderService.StudentExpense> bills,Topic next){
            return clarify(Pending.BILL,vi?"Hãy chọn rõ hóa đơn học phí cần hỏi trong chat.":"Choose the tuition bill inside chat.",
                    bills.stream().map(b->new Capability(billBinding(b.id()),next,LocalDateTime.now().plusMinutes(10))).toList(),
                    bills.stream().map(b->b.title()+" · "+b.paymentReference()).toList());
        }
        private boolean allAccounts(){return matches(message,"\\b(all|total|combined)\\b.*\\b(accounts|wallets|balances)\\b|tất cả.*(tài khoản|ví)|tổng.*(tiền|số dư|tài khoản)|mọi tài khoản");}
        private String accountChoices(List<PhaseFourService.PaymentSourceAccount> candidates,Topic next){
            return clarify(Pending.ACCOUNT,vi?"Chọn rõ một tài khoản trong chat; lựa chọn này chỉ thuộc phiên của bạn.":"Choose one account inside chat; this choice belongs only to your session.",
                    candidates.stream().map(a->new Capability(next==Topic.CURRENT_BALANCE?accountBinding(a.accountId()):withAccount(binding,a.accountId()),next,LocalDateTime.now().plusMinutes(10))).toList(),
                    candidates.stream().map(a->a.displayName()+" · "+a.maskedNumber()).toList());
        }
        private Binding withAccount(Binding source,String account){
            return new Binding(source.bill(),source.version(),account,Channel.NONE,null,null,
                    source.workspaceBill(),source.workspaceAccount(),Map.of());
        }
        private String resolveScenarioAccount(Topic next){
            var named=queries.namedAccounts(message);
            var sources=queries.accounts().stream().filter(a->a.ready()&&"VND".equals(a.currency())).toList();
            if(named.isEmpty()&&namedChannel(message)==Channel.NONE
                    &&matches(scenarioResidual(),"\\bbank\\s+[a-z]\\b|ngân hàng\\s+[a-z]\\b|(?-i)\\b(?!VND\\b|CNY\\b|USD\\b|AUD\\b)[A-Z]{2,}\\b"))
                return accountChoices(sources,next);
            if(!named.isEmpty()){
                sources=sources.stream().filter(a->named.stream().anyMatch(n->n.accountId().equals(a.accountId()))).toList();
                if(sources.size()!=1)return accountChoices(sources,next);
                var chosen=sources.getFirst();
                if(!Objects.equals(chosen.accountId(),binding.account()))binding=withAccount(binding,chosen.accountId());
            }else if(binding.account()==null){
                var hint=sources.stream().filter(PhaseFourService.PaymentSourceAccount::selected).toList();
                if(hint.size()==1)binding=withAccount(binding,hint.getFirst().accountId());
                else if(sources.size()==1)binding=withAccount(binding,sources.getFirst().accountId());
                else return accountChoices(sources,next);
            }
            Channel requested=namedChannel(message);
            if(requested!=Channel.NONE)binding=new Binding(binding.bill(),binding.version(),binding.account(),requested,
                    binding.plan(),binding.planHash(),binding.workspaceBill(),binding.workspaceAccount(),binding.quotes());
            return null;
        }
        private String scenarioResidual(){
            String residual=message;
            for(var bill:crossBorder.expenses())residual=removeReference(removeReference(residual,bill.paymentReference()),bill.title());
            for(var plan:queries.plans())residual=removeReference(residual,plan.id());
            for(var account:queries.accounts()){
                residual=removeReference(removeReference(residual,account.accountId()),account.maskedNumber());
                residual=removeReference(removeReference(residual,account.displayName()),account.institution());
            }
            return residual;
        }
        private String removeReference(String value,String reference){
            return reference==null||reference.isBlank()?value:Pattern.compile(Pattern.quote(reference),Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE).matcher(value).replaceAll("");
        }
        private boolean customScenario(){
            return matches(scenarioResidual(),"\\b(another|someone|both|cash|CNY|USD|AUD|recipient|beneficiary)\\b|tài khoản khác|tiền mặt|của (bạn|người)|đồng thời|người nhận|thụ hưởng|send to|pay to|[0-9]");
        }
        private String balanceResponse(LlmIntent intent){
            var candidates=queries.accounts();
            var named=queries.namedAccounts(message);
            if(matches(message,"other person|someone|của (bạn|người)|tiền mặt|cash|convert|quy đổi"))
                return clarify(Pending.ACCOUNT,vi?"Chỉ hỗ trợ nguồn tiền trong sổ tài khoản của hồ sơ demo, tách theo tiền tệ.":"Only sources in the demo profile’s account ledger, separately by currency, are supported.",List.of(),List.of());
            if(allAccounts()){
                if(!named.isEmpty())return accountChoices(named,Topic.CURRENT_BALANCE);
                binding=null;state.balanceScope=null;
                return success(Topic.CURRENT_BALANCE,intent,queries.balances(candidates,vi));
            }
            if(!named.isEmpty())candidates=named;
            else if(namedChannel(message)!=Channel.NONE || matches(message,"\\bbank\\s+[a-z]\\b|(?-i)\\b(?!VND\\b|CNY\\b|USD\\b|AUD\\b)[A-Z]{2,}\\b")){
                return clarify(Pending.ACCOUNT,vi?"Không có tài khoản hoặc nguồn tiền đang hoạt động khớp tên đó. Hãy chọn tài khoản có thật.":"No active money source matches that name. Choose an existing account.",
                        queries.accounts().stream().map(a->new Capability(accountBinding(a.accountId()),Topic.CURRENT_BALANCE,LocalDateTime.now().plusMinutes(10))).toList(),
                        queries.accounts().stream().map(PhaseFourService.PaymentSourceAccount::displayName).toList());
            } else {
                String prior=state.balanceScope==null?null:state.balanceScope.account();
                String hinted=prior==null?selectedAccount():prior;
                var compatible=candidates.stream().filter(a->a.accountId().equals(hinted)).toList();
                if(compatible.size()==1)candidates=compatible;
            }
            if(candidates.size()!=1)return accountChoices(candidates,Topic.CURRENT_BALANCE);
            binding=accountBinding(candidates.getFirst().accountId());
            return success(Topic.CURRENT_BALANCE,intent,queries.balances(candidates,vi));
        }
        private List<PhaseFourService.ActionPlan> namedPlans(List<PhaseFourService.ActionPlan> plans){
            return plans.stream().filter(p->GlobalAssistantQueries.contains(message,p.id())
                    ||payments.receiptForAction(p.id())!=null&&GlobalAssistantQueries.contains(message,payments.receiptForAction(p.id()).transactionId())
                    ||p.expenseId()!=null&&GlobalAssistantQueries.contains(message,crossBorder.expense(p.expenseId()).paymentReference())).toList();
        }
        private String resolvePlan(boolean receiptOnly,Topic next){
            var candidates=queries.plans().stream().filter(p->!receiptOnly || "COMPLETED".equals(p.status())&&payments.receiptForAction(p.id())!=null).toList();
            var named=namedPlans(candidates);
            if(!named.isEmpty())candidates=named;
            else if(matches(message,"(?i)\\b(?:ACT|SBOX)-[A-Z0-9-]+\\b"))
                return clarify(Pending.PLAN,vi?"Không tìm thấy kế hoạch hoặc biên nhận được tham chiếu.":"The referenced plan or receipt was not found.",List.of(),List.of());
            else if(crossBorder.expenses().stream().anyMatch(b->GlobalAssistantQueries.contains(message,b.paymentReference())
                    ||GlobalAssistantQueries.contains(message,b.title())))
                candidates=List.of();
            else {
                Binding prior=receiptOnly?state.historyScope:binding!=null&&binding.plan()!=null?binding:state.paymentScope;
                if(prior!=null&&prior.plan()!=null){
                    var compatible=candidates.stream().filter(p->p.id().equals(prior.plan())).toList();
                    if(compatible.size()==1)candidates=compatible;
                } else if(receiptOnly&&selectedBill()!=null){
                    var compatible=candidates.stream().filter(p->Objects.equals(p.expenseId(),selectedBill())).toList();
                    if(compatible.size()==1)candidates=compatible;
                }
            }
            if(candidates.size()!=1)return clarify(Pending.PLAN,
                    candidates.isEmpty()?(vi?"Chưa có biên nhận/kế hoạch phù hợp. Chưa thanh toán.":"No matching receipt/plan exists. No payment made.")
                            :(vi?"Hãy chọn rõ kế hoạch học phí cần kiểm tra; không tự chọn kế hoạch mới nhất.":"Choose the tuition plan to check; the latest plan is not selected automatically."),
                    candidates.stream().map(p->new Capability(planBinding(p),next,LocalDateTime.now().plusMinutes(10))).toList(),
                    candidates.stream().map(p->p.id()+" · "+p.status()).toList());
            binding=planBinding(candidates.getFirst());
            return null;
        }
        private String historyResponse(LlmIntent intent){
            String error=resolvePlan(true,Topic.RECEIPT_BALANCE);if(error!=null)return error;
            if(validate(binding)!=null){rejected("CONTEXT_STALE",vi);return vi?"Biên nhận đã thay đổi hoặc không còn tồn tại.":"Receipt changed or no longer exists.";}
            return success(Topic.RECEIPT_BALANCE,intent,insights.completedTuitionBalance(payments,binding.plan(),vi));
        }
        private Binding quoted(Binding original,List<CrossBorderService.ChannelQuote> quotes){return new Binding(original.bill(),original.version(),original.account(),original.channel(),original.plan(),original.planHash(),original.workspaceBill(),original.workspaceAccount(),quotes.stream().collect(Collectors.toMap(CrossBorderService.ChannelQuote::channelId,CrossBorderService.ChannelQuote::quoteId)));}
        private String success(Topic next,LlmIntent intent,String answer){synchronized(state){if(next!=Topic.LIVING_EXPENSE_RUNWAY&&next!=Topic.CHANNEL_UNAVAILABLE)state.clearScenario();state.topic=next;state.lastIntent=intent.intent();state.binding=binding;remember(state,next,binding);state.discussedChannel=next==Topic.CHANNEL_UNAVAILABLE?discussedChannel:Channel.NONE;state.pending=Pending.NONE;state.choices.clear();state.clarification="";}reason=intent.intent()==LlmIntent.Intent.CREATE_TUITION_PLAN?"CONTEXT_EXPLICIT_DRAFT":"CONTEXTUAL_READ_ONLY";return answer;}
        private String runwayResponse(LlmIntent intent){
            reason="RUNWAY_REQUESTED";audit(reason);
            if(matches(message,"\\b(another|someone|both|cash)\\b|other accounts|all accounts|tài khoản khác|tất cả tài khoản|tiền mặt|đồng thời"))
                return clarify(Pending.ACCOUNT,FinanceChatService.clarification(vi),List.of(),List.of());
            Integer selected=selectedBill();
            if(binding==null && selected!=null && crossBorder.expense(selected).executed()){
                reason="RUNWAY_INVALID_DATA";return LivingExpenseRunwayService.error("TUITION_ALREADY_PAID",vi);
            }
            String error=ensureBill(Topic.LIVING_EXPENSE_RUNWAY);if(error!=null)return error;
            Channel requested=namedChannel(message);
            if(requested!=Channel.NONE)binding=new Binding(binding.bill(),binding.version(),binding.account(),requested,
                    binding.plan(),binding.planHash(),binding.workspaceBill(),binding.workspaceAccount(),binding.quotes());
            error=resolveScenarioAccount(Topic.LIVING_EXPENSE_RUNWAY);if(error!=null)return error;
            try{
                var input=runwayService.inspect(binding.bill(),binding.account(),binding.channel(),binding.plan());
                binding=new Binding(binding.bill(),binding.version(),binding.account(),channel(input.quote().channelId()),
                    binding.plan(),binding.planHash(),binding.workspaceBill(),binding.workspaceAccount(),Map.of(input.quote().channelId(),input.quote().quoteId()));
                if(state.monthlyScope!=null && !state.monthlyScope.equals(binding))state.clearScenario();
                if(matches(message,"\\b(CNY|USD|AUD|yuan)\\b|nhân dân tệ")){
                    state.monthly=null;state.monthlyScope=null;
                    success(Topic.LIVING_EXPENSE_RUNWAY,intent,"");state.runwayRequested=true;state.pending=Pending.MONTHLY_EXPENSE;
                    reason="RUNWAY_INVALID_DATA";return LivingExpenseRunwayService.error("VND_ONLY",vi);
                }
                if(matches(message,"[0-9]")){state.monthly=null;state.monthlyScope=null;}
                success(Topic.LIVING_EXPENSE_RUNWAY,intent,"");state.runwayRequested=true;
                if(state.monthly==null){state.pending=Pending.MONTHLY_EXPENSE;reason="RUNWAY_MISSING_BASELINE";return monthlyPrompt(vi);}
                reason="RUNWAY_RESULT";return runwayService.render(input,state.monthly,vi);
            }catch(IllegalArgumentException ex){state.clearScenario();reason="RUNWAY_INVALID_DATA";return LivingExpenseRunwayService.error(ex.getMessage(),vi);}
        }
        private String draftResponse(LlmIntent intent){
            String error=ensureBill(Topic.TUITION_PLAN);if(error!=null)return error;
            if(validate(binding)!=null){rejected("CONTEXT_STALE",vi);return vi?"Hóa đơn hoặc ngữ cảnh đã thay đổi; hãy chọn lại trước khi tạo draft.":"Bill or context changed; choose it again before preparing a draft.";}
            var bill=crossBorder.expense(binding.bill());
            if(!bill.active()||bill.executed()||!"TUITION".equals(bill.expenseType()))
                return clarify(Pending.BILL,vi?"Chỉ tạo draft cho hóa đơn học phí đang hoạt động, đã xác minh và chưa thanh toán.":"Drafts require an active, verified, unpaid tuition bill.",List.of(),List.of());
            error=resolveScenarioAccount(Topic.TUITION_PLAN);if(error!=null)return error;
            var options=crossBorder.rankedQuotesForExpense(binding.bill()).stream()
                    .filter(q->Objects.equals(binding.account(),q.sourceAccountId()))
                    .filter(q->binding.channel()==Channel.NONE||q.channelId().equals(binding.channel().name())).toList();
            if(options.isEmpty()||options.stream().noneMatch(CrossBorderService.ChannelQuote::eligible))
                return clarify(Pending.CHANNEL,vi?"Kênh yêu cầu không khả dụng hoặc không thuộc tài khoản nguồn đã chọn. Không tự thay bằng kênh khác.":"The requested channel is unavailable or does not belong to the resolved source account. No alternative is substituted.",List.of(),List.of());
            var order=intent.channelPreference()==LlmIntent.ChannelPreference.FASTEST
                    ?Comparator.comparingInt(CrossBorderService.ChannelQuote::settlementMaxDays).thenComparing(CrossBorderService.ChannelQuote::landedCost)
                    :Comparator.comparing(CrossBorderService.ChannelQuote::landedCost);
            var quote=options.stream().filter(CrossBorderService.ChannelQuote::eligible).filter(q->!q.expired())
                    .min(order).orElse(null);
            if(quote==null){reason="CONTEXT_STALE";return vi?"Báo giá đã hết hạn. Làm mới rồi yêu cầu tạo draft lại; chưa tạo kế hoạch hay thanh toán.":"The quote expired. Refresh it and explicitly request the draft again; no plan or payment created.";}
            // Source, bill version and exact quote are backend-owned; the model supplies none of them.
            var plan=payments.createConversationTuitionPlan(quote.channelId(),binding.account(),binding.bill(),
                    binding.version(),quote.quoteId());
            binding=planBinding(plan);
            return success(Topic.TUITION_PLAN,intent,(vi?"Đã tạo kế hoạch ":"Prepared tuition-payment plan ")+plan.id()
                    +" · "+plan.status()+". "+planStateMessage(plan,vi));
        }
        public String respond(LlmIntent intent,boolean vietnamese){
            if(intent.intent()==LlmIntent.Intent.UNSAFE_REQUEST){reason="CONTEXT_UNSAFE_INPUT";return payments.renderIntent(intent);}
            if(intent.intent()==LlmIntent.Intent.UNSUPPORTED_REQUEST){reason="CONTEXT_UNSUPPORTED";return vi?"Yêu cầu nằm ngoài các câu hỏi tài chính và học phí hiện được hỗ trợ.":"This request is outside the supported finance and tuition questions.";}
            requestedTopic=switch(intent.intent()){
                case EXPLAIN_CURRENT_BALANCE -> Topic.CURRENT_BALANCE;
                case EXPLAIN_RECEIPT_BALANCE -> Topic.RECEIPT_BALANCE;
                case EXPLAIN_BUDGET_STATUS -> Topic.BUDGET;
                case EXPLAIN_SPENDING_SUMMARY -> Topic.SPENDING;
                case EXPLAIN_TUITION_AFFORDABILITY -> Topic.TUITION_AFFORDABILITY;
                case EXPLAIN_LIVING_EXPENSE_RUNWAY -> Topic.LIVING_EXPENSE_RUNWAY;
                case CHECK_TUITION_STATUS -> Topic.TUITION_STATUS;
                default -> Topic.TUITION_CHANNELS;
            };
            if(intent.confidence().compareTo(new BigDecimal("0.80"))<0||intent.intent()==LlmIntent.Intent.NEED_CLARIFICATION)return clarifyTopic();
            if(intent.intent()==LlmIntent.Intent.EXPLAIN_CURRENT_BALANCE)return balanceResponse(intent);
            if(intent.intent()==LlmIntent.Intent.EXPLAIN_RECEIPT_BALANCE)return historyResponse(intent);
            if(intent.intent()==LlmIntent.Intent.EXPLAIN_LIVING_EXPENSE_RUNWAY && topic==Topic.NONE && !matches(message,"tuition|học phí|living|sinh hoạt|monthly|mỗi tháng|runway"))return clarifyTopic();
            if(intent.intent()!=LlmIntent.Intent.EXPLAIN_LIVING_EXPENSE_RUNWAY
                    && !(intent.intent()==LlmIntent.Intent.EXPLAIN_TUITION_AFFORDABILITY&&matches(message,"tuition|học phí"))
                    && topic==Topic.NONE&&!personalTopic(message)&&matches(message,"còn bao nhiêu|how much.*left|what about|kênh đó|that channel|trạng thái thế|what'?s its status"))return clarifyTopic();
            if(intent.confidence().compareTo(new BigDecimal("0.80"))<0||intent.intent()==LlmIntent.Intent.NEED_CLARIFICATION)return clarifyTopic();
            if(intent.intent()==LlmIntent.Intent.EXPLAIN_LIVING_EXPENSE_RUNWAY)return runwayResponse(intent);
            if((intent.intent()==LlmIntent.Intent.EXPLAIN_BUDGET_STATUS||intent.intent()==LlmIntent.Intent.EXPLAIN_SPENDING_SUMMARY)&&FinanceChatService.scopedQuestion(message)
                    && !(intent.intent()==LlmIntent.Intent.EXPLAIN_SPENDING_SUMMARY&&insights.isSupportedSpendingScope(message)))
                return clarify(Pending.TOPIC,FinanceChatService.clarification(vi),List.of(),List.of());
            if(intent.intent()==LlmIntent.Intent.EXPLAIN_TUITION_AFFORDABILITY&&customScenario())
                return clarify(Pending.BILL,vi?"Chỉ tính hóa đơn và số tiền đã ghi nhận; không thay đổi amount hoặc currency qua câu hỏi.":"Only recorded bills and amounts are used; a question cannot change amount or currency.",List.of(),List.of());
            if(intent.intent()==LlmIntent.Intent.CREATE_TUITION_PLAN&&(!explicitDraft(message)||runway(message)))return clarifyTopic();
            if(intent.intent()==LlmIntent.Intent.CREATE_TUITION_PLAN&&customScenario())
                return clarify(Pending.BILL,vi?"Chỉ tạo draft từ hóa đơn và thông số đã ghi nhận, đã xác minh. Không đổi số tiền, tiền tệ hoặc người nhận qua tin nhắn.":"Drafts use recorded, verified bills and parameters only. A message cannot change amount, currency or recipient.",List.of(),List.of());
            if(intent.intent()==LlmIntent.Intent.CREATE_TUITION_PLAN)return draftResponse(intent);
            if(intent.intent()==LlmIntent.Intent.EXPLAIN_BUDGET_STATUS||intent.intent()==LlmIntent.Intent.EXPLAIN_SPENDING_SUMMARY){
                state.clearScenario();
                binding=null;return success(intent.intent()==LlmIntent.Intent.EXPLAIN_BUDGET_STATUS?Topic.BUDGET:Topic.SPENDING,intent,insights.render(intent.intent(),payments,vi,message));
            }
            if(intent.intent()==LlmIntent.Intent.CHECK_TUITION_STATUS){
                boolean namesBill=crossBorder.expenses().stream().anyMatch(b->GlobalAssistantQueries.contains(message,b.paymentReference())
                        ||GlobalAssistantQueries.contains(message,b.title()));
                if((queries.plans().isEmpty()||namesBill)&&!matches(message,"\\bplan\\b|\\bdraft\\b|kế hoạch|bản nháp|ACT-|SBOX-")){
                    String error=ensureBill(Topic.TUITION_STATUS);if(error!=null)return error;
                    var bill=crossBorder.expense(binding.bill());
                    return success(Topic.TUITION_STATUS,intent,(vi?"Hóa đơn ":"Bill ")+bill.paymentReference()
                            +" · "+bill.amount().toPlainString()+" "+bill.currency()+". "+(bill.executed()?(vi?"Đã thanh toán.":"Paid."):(vi?"Chưa thanh toán.":"Unpaid.")));
                }
                String error=resolvePlan(false,Topic.TUITION_PLAN);if(error!=null)return error;
                var plan=payments.action(binding.plan());
                return success(Topic.TUITION_PLAN,intent,(vi?"Kế hoạch ":"Plan ")+plan.id()+" · "+plan.status()+". "+planStateMessage(plan,vi));
            }
            String error=intent.intent()==LlmIntent.Intent.EXPLAIN_TUITION_AFFORDABILITY
                    ?ensureBill(Topic.TUITION_AFFORDABILITY):ensureBill();
            if(error!=null)return error;
            String invalid=validate(binding);if(invalid!=null){rejected("CONTEXT_STALE",vi);return vi?"Đối tượng đã thay đổi; hãy chọn lại trước khi hỏi tiếp.":"The referenced object changed; choose it again before continuing.";}
            if(intent.intent()==LlmIntent.Intent.EXPLAIN_TUITION_AFFORDABILITY&&binding.plan()!=null
                    && "COMPLETED".equals(payments.action(binding.plan()).status()))
                return success(Topic.RECEIPT_BALANCE,intent,insights.completedTuitionBalance(payments,binding.plan(),vi)
                        +(matches(message,"\\bif\\b|nếu")?(vi?"\nHóa đơn đã thanh toán; không trừ học phí lần nữa. Bạn muốn xem số dư hiện tại hay một hóa đơn chưa thanh toán khác?":"\nThis bill is paid; tuition is not subtracted again. Do you mean the current balance or another unpaid bill?"):""));
            if(intent.intent()==LlmIntent.Intent.EXPLAIN_CHANNEL_UNAVAILABLE){
                Channel named=namedChannel(message);
                Channel requested=named!=Channel.NONE?named:topic==Topic.CHANNEL_UNAVAILABLE?discussedChannel:binding.channel();
                var quotes=crossBorder.rankedQuotesForExpense(binding.bill());
                if(requested==Channel.NONE){return clarify(Pending.CHANNEL,vi?"Bạn đang hỏi kênh nào? Hãy chọn rõ kênh.":"Which channel do you mean? Choose a channel.",
                    quotes.stream().map(q->new Capability(binding,Topic.CHANNEL_UNAVAILABLE,LocalDateTime.now().plusMinutes(10),channel(q.channelId()))).toList(),quotes.stream().map(CrossBorderService.ChannelQuote::displayName).toList());}
                var quote=quotes.stream().filter(q->q.channelId().equals(requested.name())).findFirst().orElse(null);
                if(quote==null)return clarifyTopic();
                // Discussion is not a financial selection: preserve the verified source, plan and quotes.
                discussedChannel=requested;
                String answer=(vi?"Kênh ":"Channel ")+quote.displayName()+": "+channelEligibilityReason(quote,vi)+". "+(quote.eligible()?(vi?"Đủ điều kiện; chưa thực thi.":"Eligible; no execution."):(vi?"Không khả dụng và không thể thực thi.":"Unavailable and cannot be executed."));
                return success(Topic.CHANNEL_UNAVAILABLE,intent,answer);
            }
            if(intent.intent()==LlmIntent.Intent.CHECK_TUITION_STATUS){
                if(binding.plan()==null&&matches(message,"\\bplan\\b|\\bdraft\\b|kế hoạch|bản nháp")){
                    var plans=payments.recentPlans().stream().filter(p->"TUITION".equals(p.actionType())&&Objects.equals(p.expenseId(),binding.bill())).toList();
                    if(plans.size()!=1)return clarify(Pending.PLAN,vi?"Hãy chọn rõ kế hoạch học phí cần kiểm tra; không tự chọn kế hoạch mới nhất.":"Choose the tuition plan to check; the latest plan is not selected automatically.",
                            plans.stream().map(p->new Capability(planBinding(p),Topic.TUITION_PLAN,LocalDateTime.now().plusMinutes(10))).toList(),
                            plans.stream().map(p->p.id()+" · "+p.status()).toList());
                    binding=planBinding(plans.getFirst());
                }
                if(binding.plan()!=null){var plan=payments.action(binding.plan());return success(Topic.TUITION_PLAN,intent,(vi?"Kế hoạch ":"Plan ")+plan.id()+" · "+plan.status()+". "+(vi?"Phê duyệt riêng được yêu cầu trước thanh toán. Biên nhận: ":"Separate approval is required before payment. Receipt: ")+(payments.receiptForAction(plan.id())==null?(vi?"Chưa có":"No receipt"):payments.receiptForAction(plan.id()).transactionId()));}
                var bill=crossBorder.expense(binding.bill());return success(Topic.TUITION_STATUS,intent,(vi?"Hóa đơn học phí đã xác minh ":"Verified tuition bill ")+bill.paymentReference()+" · "+bill.amount().toPlainString()+" "+bill.currency()+" · "+bill.dueDate()+". "+(vi?"Chưa phê duyệt thanh toán qua chat.":"Chat does not approve payment."));
            }
            if(intent.intent()==LlmIntent.Intent.EXPLAIN_TUITION_AFFORDABILITY){
                error=resolveScenarioAccount(Topic.TUITION_AFFORDABILITY);if(error!=null)return error;
                return success(Topic.TUITION_AFFORDABILITY,intent,insights.tuitionProjection(payments,binding.bill(),binding.account(),binding.channel(),vi));
            }
            var quotes=crossBorder.rankedQuotesForExpense(binding.bill()).stream().filter(CrossBorderService.ChannelQuote::eligible).toList();
            if(quotes.isEmpty()||quotes.stream().anyMatch(CrossBorderService.ChannelQuote::expired)){reason="CONTEXT_STALE";return vi?"Báo giá đã hết hạn hoặc không khả dụng. Hãy làm mới báo giá qua màn hình so sánh; chưa tạo kế hoạch.":"Quotes expired or are unavailable. Refresh them through the comparison screen; no plan created.";}
            binding=quoted(binding,quotes);
            if(intent.intent()==LlmIntent.Intent.COMPARE_TUITION_CHANNELS){
                var comparator=intent.channelPreference()==LlmIntent.ChannelPreference.FASTEST?Comparator.comparingInt(CrossBorderService.ChannelQuote::settlementMaxDays).thenComparing(CrossBorderService.ChannelQuote::landedCost):Comparator.comparing(CrossBorderService.ChannelQuote::landedCost);
                String options=quotes.stream().sorted(comparator).limit(3).map(q->q.displayName()+": "+q.landedCost().toPlainString()+" VND; "+q.settlementMinDays()+"–"+q.settlementMaxDays()+" "+(vi?"ngày":"days")+"; "+q.quoteId()+"; "+q.quoteSource()+"; "+q.quotedAt()+"; "+q.expiresAt()).collect(Collectors.joining("\n"));
                String criterion=intent.channelPreference()==LlmIntent.ChannelPreference.FASTEST
                        ?(vi?"nhanh nhất (thời gian tối đa, rồi tổng chi phí)":"fastest (maximum settlement time, then total cost)")
                        :(vi?"tổng chi phí thấp nhất":"lowest total cost");
                return success(Topic.TUITION_CHANNELS,intent,(vi?"So sánh kênh học phí đã xác minh: ":"Verified tuition-channel comparison: ")+crossBorder.expense(binding.bill()).paymentReference()+"\n"
                        +(vi?"Hiển thị ":"Showing ")+Math.min(3,quotes.size())+(vi?" kênh đủ điều kiện · ":" eligible channels · ")+criterion+"\n"+options+"\n"
                        +(vi?"Nguồn: hóa đơn và báo giá backend hiện tại. Chưa tạo kế hoạch hay thanh toán. Học phí luôn cần phê duyệt riêng.":"Evidence: current backend bill and quotes. No plan or payment created. Tuition always requires separate approval."));
            }
            return clarifyTopic();
        }
    }

    private String planStateMessage(PhaseFourService.ActionPlan plan, boolean vi) {
        var receipt=payments.receiptForAction(plan.id());
        return switch(plan.status()){
            case "COMPLETED" -> (vi?"Thanh toán hoàn tất. Biên nhận: ":"Payment completed. Receipt: ")
                    +(receipt==null?(vi?"Không tìm thấy; tải lại trạng thái.":"Not found; reload state."):receipt.transactionId());
            case "AWAITING_APPROVAL" -> vi?"Cần phê duyệt riêng ở màn hình xem lại. Chưa thanh toán.":"Separate approval on the review screen is required. No payment yet.";
            case "INVALIDATED","BLOCKED","CANCELED" -> (vi?"Kế hoạch không thể thực thi. Xem lý do và kiểm tra lại dữ liệu trước khi tạo draft mới: ":"Plan cannot execute. Review the reason and data before preparing a new draft: ")+plan.risk();
            default -> vi?"Kiểm tra lại trạng thái chính thức trên màn hình kế hoạch; chat không thực thi thanh toán.":"Reload authoritative plan state; chat does not execute payments.";
        };
    }
    private String channelEligibilityReason(CrossBorderService.ChannelQuote quote, boolean vi) {
        if (!vi) return quote.eligibilityReason();
        return switch (quote.eligibilityReason()) {
            case "Reference only: no connected Bank B account" ->
                    "Chỉ để tham khảo: chưa kết nối tài khoản Bank B";
            case "Unavailable: the student does not have a Bank B account" ->
                    "Không khả dụng: sinh viên chưa có tài khoản Bank B";
            default -> quote.eligible()
                    ? "Hồ sơ đã xác minh đáp ứng điều kiện sử dụng kênh này"
                    : "Hồ sơ đã xác minh chưa đáp ứng điều kiện sử dụng kênh này";
        };
    }
}
