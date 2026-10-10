package com.example.finance;

import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.UUID;

/** A fresh namespace per test JVM; generated evidence never overwrites tracked artifacts. */
final class TestEvidence {
    private static final Path ROOT=Path.of("target","test-evidence",UUID.randomUUID().toString()).toAbsolutePath().normalize();
    private TestEvidence() {}
    static Path path(String first,String... more) {
        Path relative=Path.of(first,more);
        if (relative.isAbsolute()) throw new IllegalArgumentException("Evidence paths must be relative to this run");
        Path result=ROOT.resolve(relative).normalize();
        if (!result.startsWith(ROOT)) throw new IllegalArgumentException("Evidence must stay inside this run");
        try { Files.createDirectories(result.getParent()); }
        catch (java.io.IOException failure) { throw new UncheckedIOException(failure); }
        return result;
    }
}