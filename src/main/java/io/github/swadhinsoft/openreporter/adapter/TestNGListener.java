package io.github.swadhinsoft.openreporter.adapter;

import io.github.swadhinsoft.openreporter.OpenReporter;
import io.github.swadhinsoft.openreporter.model.TestResultModel;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * TestNG adapter for OpenReporter.
 *
 * <h3>Setup</h3>
 * Add to {@code testng.xml}:
 * <pre>{@code
 * <listeners>
 *   <listener class-name="io.github.swadhinsoft.openreporter.adapter.TestNGListener"/>
 * </listeners>
 * }</pre>
 *
 * In your {@code @BeforeMethod}:
 * <pre>{@code
 * OpenReporter.getInstance().registerDriver(driver);
 * OpenReporter.getInstance().setBrowser("chrome");
 * }</pre>
 *
 * In your {@code @AfterMethod}:
 * <pre>{@code
 * OpenReporter.getInstance().unregisterDriver();
 * }</pre>
 */
public class TestNGListener implements ITestListener, ISuiteListener {

    private final OpenReporter reporter = OpenReporter.getInstance();

    // ── ITestListener ────────────────────────────────────────────────────────

    @Override
    public void onTestStart(ITestResult result) {
        String browser = param(result, "browser");
        if (browser.isEmpty()) browser = reporter.getRegisteredBrowser();

        String os    = param(result, "os");
        String suite = result.getTestContext().getName();
        String desc  = result.getMethod().getDescription();

        reporter.startTest(result.getMethod().getMethodName(), desc, suite, browser, os);
        reporter.getCurrentTest().addLog("Browser: " + browser);
        if (!os.isEmpty()) reporter.getCurrentTest().addLog("OS: " + os);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        TestResultModel m = reporter.getCurrentTest();
        if (m != null) m.addLog("Test passed successfully");
        reporter.finishTest("PASSED");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        TestResultModel m = reporter.getCurrentTest();
        if (m == null) return;

        m.addLog("Test FAILED — " + result.getThrowable().getMessage());
        m.setErrorMessage(result.getThrowable().getMessage());
        m.setStackTrace(toStackTrace(result.getThrowable()));

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
    public void onTestSkipped(ITestResult result) {
        TestResultModel m = reporter.getCurrentTest();
        if (m != null) {
            String reason = result.getThrowable() != null
                    ? result.getThrowable().getMessage() : "No reason provided";
            m.addLog("Test skipped — " + reason);
        }
        reporter.finishTest("SKIPPED");
    }

    @Override public void onTestFailedButWithinSuccessPercentage(ITestResult r) {}
    @Override public void onStart(ITestContext ctx)  {}
    @Override public void onFinish(ITestContext ctx) {}

    // ── ISuiteListener ────────────────────────────────────────────────────────

    @Override public void onStart(ISuite suite) {}

    @Override
    public void onFinish(ISuite suite) {
        reporter.generateReport();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String param(ITestResult result, String name) {
        String val = result.getTestContext().getCurrentXmlTest().getParameter(name);
        return val != null ? val : "";
    }

    private String toStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
