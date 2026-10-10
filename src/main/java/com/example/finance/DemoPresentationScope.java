package com.example.finance;

import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Display projection only: never changes eligibility, ranking, quotes, plans or stored data. */
@Component
public class DemoPresentationScope {
    public static final Set<String> COMPETITION_CHANNELS = Set.of("ALIPAY","BANK_A","BANK_B");
    private final boolean competition;
    public DemoPresentationScope(@Value("${finbridge.demo.presentation:competition}") String mode) {
        if (!Set.of("competition","extension").contains(mode)) throw new IllegalArgumentException("Unknown demo presentation");
        competition="competition".equals(mode);
    }
    public boolean competition() { return competition; }
    public boolean shows(CrossBorderService.StudentExpense expense) {
        return expense==null || !competition || ("China".equals(expense.destinationCountry()) && "CNY".equals(expense.currency()));
    }
    public List<CrossBorderService.StudentExpense> expenses(List<CrossBorderService.StudentExpense> values) {
        return values.stream().filter(this::shows).toList();
    }
    public List<CrossBorderService.ChannelQuote> quotes(List<CrossBorderService.ChannelQuote> values) {
        return values.stream().filter(q->!competition || COMPETITION_CHANNELS.contains(q.channelId())).toList();
    }
}