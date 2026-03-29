package io.github.swadhinsoft.openreporter.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.InputStream;

/**
 * Loads OpenReporter configuration from {@code openreporter.json}.
 *
 * <p>Lookup order:
 * <ol>
 *   <li>JVM system property: {@code -Dopenreporter.config=/path/to/file}</li>
 *   <li>{@code openreporter.json} in the project root ({@code user.dir})</li>
 *   <li>{@code openreporter.json} on the classpath (e.g. {@code src/test/resources})</li>
 *   <li>Built-in defaults — no file required</li>
 * </ol>
 *
 * <p>Minimal {@code openreporter.json}:
 * <pre>{@code
 * {
 *   "title":       "My Test Report",
 *   "logo":        "src/test/resources/logo.png",
 *   "outputDir":   "target/open-reporter",
 *   "environment": "QA"
 * }
 * }</pre>
 */
public class ReporterConfig {

    private static final ReporterConfig INSTANCE = new ReporterConfig();

    private String title       = "Test Execution Report";
    private String logo        = "";
    private String outputDir   = "target/open-reporter";
    private String environment = "";

    private ReporterConfig() { load(); }

    public static ReporterConfig getInstance() { return INSTANCE; }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getTitle()       { return title; }
    public String getLogo()        { return logo; }
    public String getOutputDir()   { return outputDir; }
    public String getEnvironment() { return environment; }

    // ── Private ───────────────────────────────────────────────────────────────

    private void load() {
        try {
            JsonNode root = readConfigNode();
            if (root == null) return;
            title       = root.path("title").asText(title);
            logo        = root.path("logo").asText(logo);
            outputDir   = root.path("outputDir").asText(outputDir);
            environment = root.path("environment").asText(environment);
        } catch (Exception e) {
            System.err.println("[OpenReporter] Config load warning — using defaults: " + e.getMessage());
        }
    }

    private JsonNode readConfigNode() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // 1. System property
        String sysProp = System.getProperty("openreporter.config");
        if (sysProp != null && !sysProp.trim().isEmpty()) {
            File f = new File(sysProp);
            if (f.exists()) return mapper.readTree(f);
            System.err.println("[OpenReporter] Config file from -Dopenreporter.config not found: " + sysProp);
        }

        // 2. Project root
        File rootFile = new File(System.getProperty("user.dir"), "openreporter.json");
        if (rootFile.exists()) return mapper.readTree(rootFile);

        // 3. Classpath
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("openreporter.json")) {
            if (is != null) return mapper.readTree(is);
        }

        return null; // use defaults
    }
}
