package com.example.finance;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/** Display adapter for backend-owned templates. No calculation, intent inference or execution. */
@Component("conversationPresentation")
public class ConversationPresentation {
    public record Fact(String label, String value) {}
    public record Channel(String name, String cost, String settlement, String expires, long expiryEpoch) {}
    public record Reply(String id, String role, String message, String conclusion, String subtitle,
                        boolean estimate, String language, List<String> warnings, List<Fact> facts,
                        List<String> columns, List<List<String>> rows, List<Channel> channels, long expiryEpoch) {
        public boolean rich(){return !conclusion.isEmpty();}
        public String detailsLabel(){return "vi".equals(language)?"Xem chi tiết":"View details";}
        public String estimateLabel(){return "vi".equals(language)?"ƯỚC TÍNH":"ESTIMATE";}
        public String costLabel(){return "vi".equals(language)?"Tổng chi phí":"Total cost";}
        public String settlementLabel(){return "vi".equals(language)?"Thời gian quyết toán":"Settlement time";}
        public String expiryLabel(){return "vi".equals(language)?"Báo giá hết hạn":"Quote expires";}
        public String historyLabel(){return "vi".equals(language)?"Kết quả tại thời điểm trả lời; hỏi lại để cập nhật.":"Snapshot at reply time; ask again for current data.";}
    }

    public List<Reply> present(List<PhaseFourService.ConversationMessage> messages) {
        var replies=new ArrayList<Reply>();
        for(int i=0;i<messages.size();i++){
            var item=messages.get(i); String text=item.message(), id=group(item.id());
            while(i+1<messages.size() && id.equals(group(messages.get(i+1).id())) && item.role().equals(messages.get(i+1).role()))
                text+=messages.get(++i).message();
            replies.add(render(id,item.role(),text));
        }
        return List.copyOf(replies);
    }
    private static String group(String id){return id.matches("MSG-[0-9a-f-]{36}-[0-9]+")?id.substring(0,id.lastIndexOf('-')):id;}
    private static String find(String text,String regex,int group){var m=Pattern.compile(regex,Pattern.MULTILINE).matcher(text);return m.find()?m.group(group):"";}
    private static String line(String text,String prefix){return find(text,"^"+Pattern.quote(prefix)+"(.+)$",1);}
    private static long epoch(String timestamp){
        try{return LocalDateTime.parse(timestamp).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();}
        catch(RuntimeException ex){return 0;}
    }
    // Formatting strings preserves every decimal digit and sign; it never recomputes a financial value.
    private static String number(String value,boolean vi){
        if(!value.matches("-?[0-9]+(?:\\.[0-9]+)?"))return value;
        String[] parts=value.split("\\.",2);
        String whole=parts[0].replaceAll("(?<=\\d)(?=(\\d{3})+$)",vi?".":",");
        return whole+(parts.length==2 && !parts[1].matches("0+")?(vi?",":".")+parts[1]:"");
    }
    private static String money(String amount,boolean vi){return number(amount,vi)+" VND";}
    private static void fact(List<Fact> facts,String text,String prefix,String label,boolean vi){
        String value=find(text,"^"+Pattern.quote(prefix)+"(-?[0-9]+\\.[0-9]+) VND",1);
        if(!value.isEmpty())facts.add(new Fact(label,money(value,vi)));
    }
    private Reply render(String id,String role,String text){
        boolean vi=text.contains("Kỳ báo cáo:")||text.contains("ƯỚC TÍNH")||text.startsWith("So sánh kênh học phí")||text.startsWith("THANH TOÁN HỌC PHÍ ĐÃ HOÀN TẤT")||text.startsWith("SỐ DƯ TÀI KHOẢN HIỆN TẠI");
        String conclusion="",subtitle=""; boolean estimate=false;
        var facts=new ArrayList<Fact>(); var warnings=new ArrayList<String>();
        List<String> columns=List.of(); var rows=new ArrayList<List<String>>(); var channels=new ArrayList<Channel>();
        if("ASSISTANT".equals(role)){
            if(text.startsWith("SỐ DƯ TÀI KHOẢN HIỆN TẠI")||text.startsWith("CURRENT ACCOUNT BALANCES")){
                conclusion=vi?"Số dư tài khoản hiện tại":"Current account balances";
                subtitle=line(text,vi?"Quan sát lúc: ":"Observed at: ");
                columns=vi?List.of("Tài khoản","Số dư","Tiền tệ"):List.of("Account","Balance","Currency");
                var m=Pattern.compile("^(.+ · .+): (-?[0-9]+\\.[0-9]+) ([A-Z]{3})\\.$",Pattern.MULTILINE).matcher(text);
                while(m.find())rows.add(List.of(m.group(1),number(m.group(2),vi),m.group(3)));
                m=Pattern.compile("^(?:Tổng|Total) ([A-Z]{3}): (-?[0-9]+\\.[0-9]+)\\.$",Pattern.MULTILINE).matcher(text);
                while(m.find())facts.add(new Fact((vi?"Tổng ":"Total ")+m.group(1),number(m.group(2),vi)+" "+m.group(1)));
                warnings.add(vi?"Số dư Sandbox mới nhất; tách tiền tệ, không cộng planner và không trừ đệm an toàn.":"Latest Sandbox balances; currencies separate, planner excluded, no safety buffer deduction.");
            }else if(text.startsWith("ƯỚC TÍNH THỜI GIAN SINH HOẠT")||text.startsWith("LIVING-EXPENSE RUNWAY ESTIMATE")){
                String months=find(text,"^(?:Khoảng|Approximately) ([0-9]+\\.[0-9]+) (?:tháng|months)",1);
                if(!months.isEmpty()){
                    conclusion=(vi?"Đủ khoảng ":"Covers approximately ")+(vi?months.replace('.',','):months)+(vi?" tháng":" months"); estimate=true;
                    subtitle=vi?"Sau học phí và đệm an toàn · làm tròn xuống 2 chữ số":"After tuition and safety buffer · rounded DOWN to 2 decimals";
                    fact(facts,text,vi?"Tiền dành cho sinh hoạt: ":"Available runway funds: ",vi?"Tiền cho sinh hoạt":"Living funds",vi);
                    fact(facts,text,vi?"Chi sinh hoạt mỗi tháng: ":"Monthly living expense: ",vi?"Chi mỗi tháng đã xác nhận":"Confirmed monthly expense",vi);
                    fact(facts,text,vi?"Tổng học phí gồm phí và phụ phí FX: ":"Total tuition debit including fees and FX markup: ",vi?"Học phí gồm mọi phí":"Tuition including fees",vi);
                    fact(facts,text,vi?"Đệm an toàn: ":"Safety buffer: ",vi?"Đệm an toàn":"Safety buffer",vi);
                    warnings.add(vi?"Kịch bản trước thanh toán; chưa tạo kế hoạch hay thanh toán. Đây không phải quyết định đủ điều kiện thanh toán.":"Before-payment scenario; no plan or payment created. This is not a payment-permission decision.");
                }
            }else if(text.startsWith("THANH TOÁN HỌC PHÍ ĐÃ HOÀN TẤT")||text.startsWith("COMPLETED TUITION PAYMENT")){
                String remaining=find(text,"^(?:Số dư thực tế sau thanh toán|Actual balance immediately after payment): (-?[0-9]+\\.[0-9]+) VND",1);
                if(!remaining.isEmpty()){
                    conclusion=(vi?"Số dư sau thanh toán: ":"Balance after payment: ")+money(remaining,vi);
                    subtitle=vi?"Số dư ghi nhận trong biên nhận Sandbox":"Balance recorded in the Sandbox receipt";
                    fact(facts,text,vi?"Tổng tiền đã trừ: ":"Total debited: ",vi?"Đã trừ":"Debited",vi);
                    facts.add(new Fact(vi?"Thời điểm thanh toán":"Payment timestamp",line(text,vi?"Thời điểm thanh toán: ":"Payment timestamp: ")));
                    facts.add(new Fact(vi?"Biên nhận":"Receipt",line(text,vi?"Biên nhận: ":"Receipt: ")));
                    warnings.add(vi?"Số dư này là kết quả đã thực thi, không phải dự báo và không tạo thanh toán mới.":"This is an executed result, not a projection, and does not create a new payment.");
                }
            }else if(text.contains("Category budgets: VND.")||text.contains("Ngân sách danh mục: VND.")){
                String remaining=find(text,"(?:còn lại theo từng danh mục|remaining across categories) ([0-9]+\\.[0-9]+) VND",1);
                if(!remaining.isEmpty()){
                    conclusion=(vi?"Ngân sách còn ":"Budget remaining: ")+money(remaining,vi);
                    subtitle=line(text,vi?"Kỳ báo cáo: ":"Reporting period: ");
                    columns=vi?List.of("Danh mục","Hạn mức","Đã chi","Còn lại","Vượt"):List.of("Category","Limit","Spent","Remaining","Overspent");
                    var m=Pattern.compile("^(.+): (?:hạn mức|limit) ([0-9]+\\.[0-9]+), (?:đã chi|spent) ([0-9]+\\.[0-9]+), (?:còn lại|remaining) ([0-9]+\\.[0-9]+), (?:vượt|overspent) ([0-9]+\\.[0-9]+) VND\\.$",Pattern.MULTILINE).matcher(text);
                    while(m.find())rows.add(List.of(m.group(1),number(m.group(2),vi),number(m.group(3),vi),number(m.group(4),vi),number(m.group(5),vi)));
                    warnings.add(vi?"VND · Ngân sách còn lại không phải số dư tài khoản. Phần vượt hiển thị riêng; hoàn tiền không giảm chi tiêu.":"VND · Remaining budget is not an account balance. Overspending is separate; refunds do not reduce spending.");
                    fact(facts,text,vi?"Chi ngoài danh mục có ngân sách: ":"Expenses outside budgeted categories: ",vi?"Chi ngoài ngân sách":"Expenses outside budgets",vi);
                }
            }else if(text.contains("ƯỚC TÍNH SAU HỌC PHÍ")||text.contains("TUITION PROJECTION · VND")){
                String remaining=find(text,"^(?:Số dư dự kiến sau học phí|Projected balance after tuition): (-?[0-9]+\\.[0-9]+) VND",1);
                if(!remaining.isEmpty()){
                    conclusion=(vi?"Còn ":"Remaining: ")+money(remaining,vi); estimate=true;
                    subtitle=vi?"Số dư dự kiến sau học phí, trước đệm và khoản giữ trước":"Projected balance after tuition, before buffer and reservations";
                    fact(facts,text,vi?"Số dư dự kiến sau học phí: ":"Projected balance after tuition: ",vi?"Sau học phí":"After tuition",vi);
                    String after=find(text,"(?:còn sau đệm|after buffer) (-?[0-9]+\\.[0-9]+) VND",1);
                    if(!after.isEmpty())facts.add(new Fact(vi?"Sau đệm an toàn":"After safety buffer",money(after,vi)));
                    fact(facts,text,vi?"Nếu giữ thêm toàn bộ khoản planner trên: ":"If all these planner commitments are also earmarked: ",vi?"Nếu giữ thêm các khoản planner":"If planner funds are also reserved",vi);
                    warnings.add(line(text,vi?"Giới hạn: ":"Limitation: "));
                    warnings.add(vi?"Chỉ là dự báo; chưa tạo kế hoạch. Học phí luôn cần phê duyệt riêng.":"Projection only; no plan created. Tuition always requires separate approval.");
                }
            }else if(text.startsWith("So sánh kênh học phí đã xác minh:")||text.startsWith("Verified tuition-channel comparison:")){
                String heading=find(text,"^((?:Hiển thị|Showing) [0-9]+ (?:kênh đủ điều kiện|eligible channels).+)$",1);
                var m=Pattern.compile("^(.+): ([0-9]+(?:\\.[0-9]+)?) VND; ([0-9]+–[0-9]+ (?:ngày|days)); ([^;]+); ([^;]+); ([^;]+); ([^\\n]+)$",Pattern.MULTILINE).matcher(text);
                while(m.find())channels.add(new Channel(m.group(1),money(m.group(2),vi),m.group(3),m.group(7),epoch(m.group(7))));
                if(!heading.isEmpty()&&!channels.isEmpty()){
                    conclusion=heading; subtitle=vi?"Tổng chi phí đã gồm phí và phụ phí FX":"Total cost includes fees and FX markup";
                    warnings.add(vi?"Chưa tạo kế hoạch hay thanh toán. Học phí luôn cần phê duyệt riêng.":"No plan or payment created. Tuition always requires separate approval.");
                }
            }else if(text.startsWith("Kỳ báo cáo:")||text.startsWith("Reporting period:")){
                var m=Pattern.compile("^(?:Tổng chi tiêu|Expense total) ([A-Z]{3}): ([0-9]+\\.[0-9]+)\\.$",Pattern.MULTILINE).matcher(text);
                while(m.find())facts.add(new Fact((vi?"Đã chi ":"Spent ")+m.group(1),number(m.group(2),vi)+" "+m.group(1)));
                if(!facts.isEmpty()){
                    conclusion=facts.size()==1?(vi?"Đã chi ":"Spent ")+facts.getFirst().value():(vi?"Chi tiêu theo từng tiền tệ":"Spending by currency");
                    subtitle=line(text,vi?"Kỳ báo cáo: ":"Reporting period: ");
                    columns=vi?List.of("Danh mục","Đã chi","Tiền tệ"):List.of("Category","Spent","Currency");
                    m=Pattern.compile("^(.+): ([0-9]+\\.[0-9]+) ([A-Z]{3})\\.$",Pattern.MULTILINE).matcher(text);
                    while(m.find()){
                        if(m.group(1).equals(vi?"Hoàn tiền ghi nhận riêng":"Refunds recorded separately"))
                            facts.add(new Fact(m.group(1)+" · "+m.group(3),number(m.group(2),vi)+" "+m.group(3)));
                        else rows.add(List.of(m.group(1),number(m.group(2),vi),m.group(3)));
                    }
                    warnings.add(vi?"Không cộng các tiền tệ. Hoàn tiền ghi riêng; không giảm tổng chi tiêu.":"Currencies are not added. Refunds are recorded separately, not deducted from spending.");
                }
            }
            // Keep safety findings visible even when technical evidence is folded away.
            for(String value:text.split("\n"))if(value.startsWith("THIẾU TIỀN:")||value.startsWith("SHORTFALL:")||value.contains("không đủ đệm an toàn")||value.contains("safety buffer is not covered"))warnings.add(value);
        }
        long expiry=epoch(find(text," · (?:hết hạn|expires) ([0-9T:.\\-]+)\\.\\s*$",1));
        return new Reply(id,role,text,conclusion,subtitle,estimate,vi?"vi":"en",List.copyOf(warnings),List.copyOf(facts),columns,List.copyOf(rows),List.copyOf(channels),expiry);
    }
}
