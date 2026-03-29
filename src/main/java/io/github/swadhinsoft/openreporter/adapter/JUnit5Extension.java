package io.github.swadhinsoft.openreporter.adapter;

import io.github.swadhinsoft.openreporter.OpenReporter;
import io.github.swadhinsoft.openreporter.model.TestResultModel;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

/**
 * JUnit 5 adapter for OpenReporter.
 *
 * <h3>Setup</h3>
 * Annotate your test class:
 * <pre>{@code
 * @ExtendWith(JUnit5Extension.class)
 * public class MyTest { ... }
 * }</pre>
 *
 * In {@code @BeforeEach}:
 * <pre>{@code
 * OpenReporter.getInstance().registerDriver(driver);
 * OpenReporter.getInstance().setBrowser("chrome");
 * }</pre>
 *
 * In {@code @AfterEach}:
 * <pre>{@code
 * OpenReporter.getInstance().unregisterDriver();
 * }</pre>
 */
public class JUnit5Extension
        implements BeforeEachCallback, AfterEachCallback, TestWatcher, AfterAllCallback {

    private final OpenReporter reporter = OpenReporter.getInstance();

    @Override
    public void beforeEach(ExtensionContext ctx) {
        String methodName  = ctx.getRequiredTestMethod().getName();
        String description = ctx.getDisplayName();
        String suiteName   = ctx.getRequiredTestClass().getSimpleName();
        String browser     = reporter.getRegisteredBrowser();

        reporter.startTest(methodName, description, suiteName, browser, "");
        reporter.getCurrentTest().addLog("JUnit 5 test started");
        reporter.getCurrentTest().addLog("Browser: " + browser);
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        // TestWatcher callbacks below handle finishTest; nothing extra needed here.
    }

    // ── TestWatcher ───────────────────────────────────────────────────────────

    @Override
    public void testSuccessful(ExtensionContext ctx) {
        TestResultModel m = reporter.getCurrentTest();
        if (m != null) m.addLog("Test passed successfully");
        reporter.finishTest("PASSED");
    }

    @Override
    public void testFailed(ExtensionContext ctx, Throwable cause) {
        TestResultModel m = reporter.getCurrentTest();
        if (m == null) return;

        m.addLog("Test FAILED — " + cause.getMessage());
        m.setErrorMessage(cause.getMessage());
        m.setStackTrace(toStackTrace(cause));

        WebDriver driver = reporter.getRegisteredDriver();
        if (driver != null) {
            try {
                m.setScreenshotBase64("data:image/png;base64,"
                        + ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64));
                m.addLog("Screenshot captured");
            } catch (Exception e) {
                m.addLog("Screenshot capture failed: " + e.getMessage());
            }
        }

        reporter.finishTest("FAILED");
    }

    @Override
    public void testAborted(ExtensionContext ctx, Throwable cause) {
        TestResultModel m = reporter.getCurrentTest();
        if (m != null) m.addLog("Test aborted — " + (cause != null ? cause.getMessage() : ""));
        reporter.finishTest("SKIPPED");
    }

    @Override
    public void testDisabled(ExtensionContext ctx, Optional<String> reason) {
        String methodName = ctx.getTestMethod().map(java.lang.reflect.Method::getName).orElse("unknown");
        String suiteName  = ctx.getTestClass().map(Class::getSimpleName).orElse("unknown");
        reporter.startTest(methodName, ctx.getDisplayName(), suiteName,
                reporter.getRegisteredBrowser(), "");
        TestResultModel m = reporter.getCurrentTest();
        if (m != null) m.addLog("Test disabled — " + reason.orElse("no reason"));
        reporter.finishTest("SKIPPED");
    }

    // ── AfterAllCallback — flush report after the test class completes ─────────

    @Override
    public void afterAll(ExtensionContext ctx) {
        reporter.generateReport();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String toStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
