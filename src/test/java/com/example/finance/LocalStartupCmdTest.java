package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.WINDOWS)
class LocalStartupCmdTest {
    @Test void cmdRunsPowerShellPassesArgumentsAndPropagatesFailureWithoutOpeningEditor() throws Exception {
        var output=Path.of("target/local-startup-cmd-test.log");
        try(var busy=new ServerSocket(0)){
            var process=new ProcessBuilder("cmd.exe","/d","/c","scripts\\start-local.cmd","-Port",Integer.toString(busy.getLocalPort()),"-SkipWarmup")
                    .redirectErrorStream(true).redirectOutput(output.toFile()).start();
            try {
                assertTrue(process.waitFor(30,TimeUnit.SECONDS),"CMD launcher must fail promptly at an occupied port");
                String log=Files.readString(output,StandardCharsets.UTF_8);
                assertEquals(1,process.exitValue(),log);
                assertTrue(log.contains("Port "+busy.getLocalPort()+" is already occupied"),log);
                assertTrue(log.contains("start-local.cmd -Port "+(busy.getLocalPort()+1)),log);
                assertTrue(log.contains("FinBridge chua khoi dong thanh cong"),log);
                assertFalse(log.contains("Starting FinBridge (local"),log);
            } finally {if(process.isAlive())process.destroyForcibly();}
        }
    }
}
