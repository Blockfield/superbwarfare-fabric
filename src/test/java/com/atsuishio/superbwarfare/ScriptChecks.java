package com.atsuishio.superbwarfare;

import com.atsuishio.superbwarfare.script.ScriptManager;

import org.mozillaa.javascript.Context;
import org.mozillaa.javascript.RhinoException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class ScriptChecks {
    public static void main(String[] args) throws IOException {
        int count = 0;
        try (var files = Files.walk(Path.of("src/main/resources"))) {
            for (var file : files.filter(p -> p.toString().endsWith(".js")).sorted().toList()) {
                var script =
                        ScriptManager.INSTANCE.createSafeScript(
                                file.toString(), Files.readString(file));
                check(script != null, "resource must compile in Rhino: " + file);
                count++;
            }
        }
        check(count > 0, "resource script checks must not be empty");
        check(Context.getCurrentContext() == null, "compilation must release its thread context");

        check(
                ScriptManager.INSTANCE.createSafeScript("incomplete", "function incomplete(")
                        == null,
                "incomplete scripts must be rejected");
        var script =
                ScriptManager.INSTANCE.createSafeScript(
                        "context-check", "function scaled(value) { return base + value * 2; }");
        check(script != null, "valid function must compile");
        script.exec();
        script.putProperty("base", 3);
        near(((Number) script.callFunction("scaled", 2)).doubleValue(), 7);
        check(script.callFunction("missing") == null, "absent hooks must return null");
        check(Context.getCurrentContext() == null, "calls must release their thread context");

        CompletableFuture.runAsync(
                        () -> {
                            check(
                                    Context.getCurrentContext() == null,
                                    "worker starts without a Rhino context");
                            near(((Number) script.callFunction("scaled", 4)).doubleValue(), 11);
                            check(
                                    Context.getCurrentContext() == null,
                                    "worker call must release its own context");
                        })
                .join();

        var throwing =
                ScriptManager.INSTANCE.createSafeScript(
                        "exception-check", "function fail() { throw new Error('expected'); }");
        check(throwing != null, "throwing function must compile");
        throwing.exec();
        try {
            throwing.callFunction("fail");
            throw new AssertionError("script exceptions must propagate");
        } catch (RhinoException expected) {
            check(Context.getCurrentContext() == null, "failed calls must release their context");
        }

        var safe = ScriptManager.INSTANCE.createSafeScript("safe-check", "typeof Packages");
        check(
                safe != null && "undefined".equals(safe.exec()),
                "safe scope must exclude Java packages");
        check(Context.getCurrentContext() == null, "safe execution must release its context");
        System.out.println(count + " resource scripts and script context regressions passed");
    }

    private static void near(double actual, double expected) {
        if (!Double.isFinite(actual) || Math.abs(actual - expected) > 1e-9)
            throw new AssertionError("expected " + expected + ", got " + actual);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
