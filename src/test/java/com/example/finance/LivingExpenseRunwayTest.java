package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class LivingExpenseRunwayTest {
    @Test void decimalConfirmationRejectsInvalidCurrencyAndAmounts(){
        assertEquals(new BigDecimal("8000000.00"),LivingExpenseRunwayService.parseMonthly("8000000","VND"));
        for(String amount:new String[]{"0","0.00","-1","NaN","Infinity","8 triệu","8,000,000","1e6","1.001",""," 8","9999999999999","123456789012.123"})
            assertThrows(IllegalArgumentException.class,()->LivingExpenseRunwayService.parseMonthly(amount,"VND"),amount);
        assertThrows(IllegalArgumentException.class,()->LivingExpenseRunwayService.parseMonthly(null,"VND"));
        for(String currency:new String[]{"CNY","USD","vnd","",null})
            assertThrows(IllegalArgumentException.class,()->LivingExpenseRunwayService.parseMonthly("8000000",currency));
    }
    @Test void exactFixtureAndShortfallsUseDownRoundingAndNeverNegativeMonths(){
        var c=LivingExpenseRunwayService.calculate(new BigDecimal("100000000"),new BigDecimal("70760800"),new BigDecimal("3000000"),new BigDecimal("8000000"));
        assertEquals(0,c.projectedBalance().compareTo(new BigDecimal("29239200")));
        assertEquals(0,c.funds().compareTo(new BigDecimal("26239200")));
        assertEquals(new BigDecimal("3.27"),c.months());
        for(String balance:new String[]{"70000000","72000000","73760800"}){
            c=LivingExpenseRunwayService.calculate(new BigDecimal(balance),new BigDecimal("70760800"),new BigDecimal("3000000"),new BigDecimal("8000000"));
            assertEquals(0,c.funds().signum());assertEquals(new BigDecimal("0.00"),c.months());
        }
        assertThrows(IllegalArgumentException.class,()->LivingExpenseRunwayService.calculate(BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ZERO));
    }
    @Test void bothProviderSchemasAndParserKeepExactlyFourFieldsAndNoFinancialOutputs(){
        var parser=new StrictLlmIntentParser(new ObjectMapper());
        String json="{\"intent\":\"EXPLAIN_LIVING_EXPENSE_RUNWAY\",\"channelPreference\":\"NONE\",\"confidence\":0.95,\"clarificationCode\":\"NONE\"}";
        assertEquals(LlmIntent.Intent.EXPLAIN_LIVING_EXPENSE_RUNWAY,parser.parse(json).intent());
        assertThrows(LlmIntentException.class,()->parser.parse(json.replace("NONE","CHEAPEST")));
        assertThrows(LlmIntentException.class,()->parser.parse(json.replace("}",",\"months\":3.27}")));
        @SuppressWarnings("unchecked") var props=(java.util.Map<String,Object>)LlmIntentContract.schema().get("properties");
        assertEquals(java.util.Set.of("intent","channelPreference","confidence","clarificationCode"),props.keySet());
        assertTrue(props.get("intent").toString().contains("EXPLAIN_LIVING_EXPENSE_RUNWAY"));
        assertFalse((Boolean)LlmIntentContract.schema().get("additionalProperties"));
    }
}
