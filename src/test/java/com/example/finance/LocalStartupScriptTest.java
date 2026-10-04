package com.example.finance;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.WINDOWS)
class LocalStartupScriptTest {
    @Test void windowsPowerShellScriptsCheckPrerequisitesFallbackAndOwnership() throws Exception {
        var log=Path.of("target/local-startup-script-test.log").toFile();
        var process=new ProcessBuilder("powershell.exe","-NoProfile","-ExecutionPolicy","Bypass","-File",
                "src/test/powershell/LocalStartup.Tests.ps1").redirectErrorStream(true).redirectOutput(log).start();
        try {
            assertTrue(process.waitFor(45,TimeUnit.SECONDS),"Script checks timed out");
            var output=java.nio.file.Files.readString(log.toPath(),StandardCharsets.UTF_8);
            assertEquals(0,process.exitValue(),output);assertTrue(output.contains("SCRIPT_CHECKS=23 PASS"),output);
        } finally { if(process.isAlive())process.destroyForcibly(); }
    }
}
