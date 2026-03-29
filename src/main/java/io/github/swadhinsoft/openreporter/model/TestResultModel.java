package io.github.swadhinsoft.openreporter.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the result data for a single test case.
 * Serialised to JSON and embedded into the HTML report.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestResultModel {

    private String testName;
    private String description;
    private String status;           // PASSED | FAILED | SKIPPED
    private String browser;
    private String os;
    private String suiteName;
    private long   startTime;
    private long   durationMs;
    private String errorMessage;
    private String stackTrace;
    private String screenshotBase64; // full data URI: "data:image/png;base64,..."
    private List<String> logs = new ArrayList<>();

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getTestName()          { return testName; }
    public String getDescription()       { return description; }
    public String getStatus()            { return status; }
    public String getBrowser()           { return browser; }
    public String getOs()                { return os; }
    public String getSuiteName()         { return suiteName; }
    public long   getStartTime()         { return startTime; }
    public long   getDurationMs()        { return durationMs; }
    public String getErrorMessage()      { return errorMessage; }
    public String getStackTrace()        { return stackTrace; }
    public String getScreenshotBase64()  { return screenshotBase64; }
    public List<String> getLogs()        { return logs; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setTestName(String v)         { this.testName = v; }
    public void setDescription(String v)      { this.description = v; }
    public void setStatus(String v)           { this.status = v; }
    public void setBrowser(String v)          { this.browser = v; }
    public void setOs(String v)               { this.os = v; }
    public void setSuiteName(String v)        { this.suiteName = v; }
    public void setStartTime(long v)          { this.startTime = v; }
    public void setDurationMs(long v)         { this.durationMs = v; }
    public void setErrorMessage(String v)     { this.errorMessage = v; }
    public void setStackTrace(String v)       { this.stackTrace = v; }
    public void setScreenshotBase64(String v) { this.screenshotBase64 = v; }
    public void setLogs(List<String> v)       { this.logs = v; }

    public void addLog(String entry) {
        if (this.logs == null) this.logs = new ArrayList<>();
        this.logs.add(entry);
    }
}
