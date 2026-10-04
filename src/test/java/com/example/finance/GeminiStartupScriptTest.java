package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.WINDOWS)
class GeminiStartupScriptTest {
    @Test void powershell51VerifiesNoOllamaNoWarmupAndProcessOwnership() throws Exception {
        var log=Path.of("target/gemini-startup-script-test.log");
        var process=new ProcessBuilder("powershell.exe","-NoProfile","-ExecutionPolicy","Bypass","-File",
                "src/test/powershell/GeminiStartup.Tests.ps1").redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(process.waitFor(45,TimeUnit.SECONDS),"Gemini script checks timed out");
            String output=Files.readString(log,StandardCharsets.UTF_8);
            assertEquals(0,process.exitValue(),output);assertTrue(output.contains("GEMINI_SCRIPT_CHECKS=15 PASS"),output);
        } finally {if(process.isAlive())process.destroyForcibly();}
    }
}
