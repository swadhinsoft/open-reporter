package io.github.swadhinsoft.openreporter.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a single named step within a test.
 * Created via {@link io.github.swadhinsoft.openreporter.OpenReporter#step},
 * {@code stepPass}, or {@code stepFail}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StepModel {

    private String description;
    private String status;    // INFO | PASS | FAIL
    private long   timestamp;

    /** Required for Jackson deserialisation. */
    public StepModel() {}

    public StepModel(String description, String status) {
        this.description = description;
        this.status      = status;
        this.timestamp   = System.currentTimeMillis();
    }

    public String getDescription() { return description; }
    public String getStatus()      { return status; }
    public long   getTimestamp()   { return timestamp; }

    public void setDescription(String v) { this.description = v; }
    public void setStatus(String v)      { this.status = v; }
    public void setTimestamp(long v)     { this.timestamp = v; }
}
