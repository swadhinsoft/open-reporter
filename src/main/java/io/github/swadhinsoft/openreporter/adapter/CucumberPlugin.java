package io.github.swadhinsoft.openreporter.adapter;

import io.cucumber.plugin.EventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestRunFinished;
import io.github.swadhinsoft.openreporter.OpenReporter;
import io.github.swadhinsoft.openreporter.model.TestResultModel;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Cucumber 7+ adapter for OpenReporter.
 *
 * <h3>Setup</h3>
 * Add to {@code @CucumberOptions}:
 * <pre>{@code
 * @CucumberOptions(plugin = {"io.github.swadhinsoft.openreporter.adapter.CucumberPlugin"})
 * }</pre>
 *
 * In your Cucumber {@code @Before} hook:
 * <pre>{@code
 * OpenReporter.getInstance().registerDriver(driver);
 * OpenReporter.getInstance().setBrowser("chrome");
 * }</pre>
 *
 * In your Cucumber {@code @After} hook:
 * <pre>{@code
 * OpenReporter.getInstance().unregisterDriver();
 * }</pre>
 */
public class CucumberPlugin implements EventListener {

    private final OpenReporter reporter = OpenReporter.getInstance();

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseStarted.class,  this::onTestCaseStarted);
        publisher.registerHandlerFor(TestCaseFinished.class, this::onTestCaseFinished);
        publisher.registerHandlerFor(TestRunFinished.class,  e -> reporter.generateReport());
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    private void onTestCaseStarted(TestCaseStarted event) {
        String name    = event.getTestCase().getName();
        String browser = reporter.getRegisteredBrowser();
        String tags    = event.getTestCase().getTags().toString();

        reporter.startTest(name, name, "Cucumber", browser, "");
        reporter.getCurrentTest().addLog("Scenario: " + name);
        reporter.getCurrentTest().addLog("Browser: " + browser);
        if (!tags.isEmpty() && !tags.equals("[]")) {
            reporter.getCurrentTest().addLog("Tags: " + tags);
        }
    }

    private void onTestCaseFinished(TestCaseFinished event) {
        TestResultModel m = reporter.getCurrentTest();
        String rawStatus  = event.getResult().getStatus().name();

        String status;
        switch (rawStatus) {
            case "PASSED":
                status = "PASSED";
                if (m != null) m.addLog("Scenario passed");
                break;
            case "FAILED":
                status = "FAILED";
                if (m != null && event.getResult().getError() != null) {
                    Throwable error = event.getResult().getError();
                    m.addLog("Scenario FAILED — " + error.getMessage());
                    m.setErrorMessage(error.getMessage());
                    m.setStackTrace(toStackTrace(error));

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
                }
                break;
            default: // SKIPPED, PENDING, UNDEFINED, AMBIGUOUS
                status = "SKIPPED";
                if (m != null) m.addLog("Scenario " + rawStatus.toLowerCase());
                break;
        }

        reporter.finishTest(status);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String toStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
