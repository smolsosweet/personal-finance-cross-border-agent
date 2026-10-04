package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/** Opt-in: a launcher-owned app and a separate target/ verification DB must already be running. */
class LocalStartupLiveIT {
    @Test void powershellWarmupCallsRealFinBridgeAndPreservesAllFinancialTables() throws Exception {
        String url=System.getProperty("finbridge.startup.test.url","http://localhost:8112");
        String jdbc=System.getProperty("finbridge.startup.test.jdbc-url");
        assertTrue(url.startsWith("http://localhost:"));assertNotNull(jdbc,"Specify the isolated launcher verification DB");
        assertTrue(jdbc.contains("target/local-launcher-verification"),"Never run this check against the user's database");
        try(var connection=DriverManager.getConnection(jdbc,"sa","")) {
            var db=new JdbcTemplate(new SingleConnectionDataSource(connection,true));
            var before=PersonalFinanceAiIntegrationTest.snapshot(db);
            long calls=db.queryForObject("SELECT COUNT(*) FROM audit_log WHERE event_type='INTENT_CLASSIFIED'",Long.class);
            var log=Path.of("target/local-startup-live-test.log").toFile();
            var process=new ProcessBuilder("powershell.exe","-NoProfile","-ExecutionPolicy","Bypass","-File",
                    "scripts/prepare-demo.ps1","-FinBridgeUrl",url).redirectErrorStream(true).redirectOutput(log).start();
            try {
                assertTrue(process.waitFor(80,TimeUnit.SECONDS));
                String output=java.nio.file.Files.readString(log.toPath());
                assertEquals(0,process.exitValue(),output);assertTrue(output.contains("FinBridge live intent warmup PASS"),output);
                assertEquals(before,PersonalFinanceAiIntegrationTest.snapshot(db));
                assertEquals(calls+1,db.queryForObject("SELECT COUNT(*) FROM audit_log WHERE event_type='INTENT_CLASSIFIED'",Long.class));
                assertEquals("EXPLAIN_BUDGET_STATUS",db.queryForObject("SELECT reason_code FROM audit_log WHERE event_type='INTENT_CLASSIFIED' ORDER BY occurred_at DESC,id DESC FETCH FIRST 1 ROW ONLY",String.class));
                assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM action_plans",Integer.class));
                assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM approvals",Integer.class));
                assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM sandbox_transactions",Integer.class));
                assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM sandbox_ledger_entries",Integer.class));
                System.out.println("LOCAL_STARTUP_LIVE actual_intent=EXPLAIN_BUDGET_STATUS schema=VALID financial_snapshot=UNCHANGED plans=0 approvals=0 payments=0 ledger=0 "+output.trim());
            } finally { if(process.isAlive())process.destroyForcibly(); }
        }
    }
}
