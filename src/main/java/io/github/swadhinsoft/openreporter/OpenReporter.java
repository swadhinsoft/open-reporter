package io.github.swadhinsoft.openreporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.swadhinsoft.openreporter.config.ReporterConfig;
import io.github.swadhinsoft.openreporter.model.StepModel;
import io.github.swadhinsoft.openreporter.model.TestResultModel;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Core singleton for OpenReporter.
 *
 * <p>Framework-agnostic — no TestNG / JUnit / Cucumber imports here.
 * Works with any Java test framework via the adapter layer.
 * Supports Selenium WebDriver and Appium AppiumDriver (both extend WebDriver).
 *
 * <h3>Lifecycle (called by adapters or by hand)</h3>
 * <pre>{@code
 * // 1. Register driver once per thread (call from @BeforeMethod / @BeforeEach / Cucumber @Before)
 * OpenReporter.getInstance().registerDriver(driver);
 * OpenReporter.getInstance().setBrowser("chrome");
 *
 * // 2. Test runs — framework adapter calls startTest / finishTest automatically
 *
 * // 3. Unregister after test (call from @AfterMethod / @AfterEach / Cucumber @After)
 * OpenReporter.getInstance().unregisterDriver();
 *
 * // 4. Generate report once after all tests (called by adapter's suite/run finish hook)
 * OpenReporter.getInstance().generateReport();
 * }</pre>
 */
public class OpenReporter {

    private static final OpenReporter INSTANCE = new OpenReporter();

    private final List<TestResultModel> results =
            Collections.synchronizedList(new ArrayList<>());

    private static final ThreadLocal<TestResultModel> current      = new ThreadLocal<>();
    private static final ThreadLocal<WebDriver>       driverHolder = new ThreadLocal<>();
    private static final ThreadLocal<String>          browserHolder = new ThreadLocal<>();

    private static final String TEMPLATE = "report-template.html";

    private OpenReporter() {}

    public static OpenReporter getInstance() { return INSTANCE; }

    // ── Driver registration ──────────────────────────────────────────────────

    /** Register the WebDriver / AppiumDriver for the current thread. */
    public void registerDriver(WebDriver driver) { driverHolder.set(driver); }

    /** Remove the WebDriver reference for the current thread (call after driver.quit()). */
    public void unregisterDriver() {
        driverHolder.remove();
        browserHolder.remove();
    }

    /** Set the browser label for the current thread (e.g. "chrome", "firefox"). */
    public void setBrowser(String browser) { browserHolder.set(browser); }

    /** Returns the registered WebDriver for the current thread, or {@code null}. */
    public WebDriver getRegisteredDriver() { return driverHolder.get(); }

    /** Returns the registered browser label, falling back to the {@code browser} system property. */
    public String getRegisteredBrowser() {
        String b = browserHolder.get();
        return (b != null && !b.isEmpty()) ? b : System.getProperty("browser", "unknown");
    }

    // ── Test lifecycle ────────────────────────────────────────────────────────

    /**
     * Called by an adapter at the start of each test.
     *
     * @param methodName  the test method / scenario name
     * @param description human-readable description (falls back to methodName if blank)
     * @param suiteName   suite / class / feature name
     * @param browser     browser label
     * @param os          OS label (may be empty)
     */
    public void startTest(String methodName, String description,
                          String suiteName, String browser, String os) {
        TestResultModel m = new TestResultModel();
        m.setTestName(methodName);
        m.setDescription(description != null && !description.isEmpty() ? description : methodName);
        m.setSuiteName(suiteName);
        m.setBrowser(browser != null ? browser : "unknown");
        m.setOs(os != null ? os : "");
        m.setStartTime(System.currentTimeMillis());
        m.addLog("Test started");
        current.set(m);
    }

    /** Returns the in-progress {@link TestResultModel} for the current thread. */
    public TestResultModel getCurrentTest() { return current.get(); }

    /**
     * Called by an adapter when a test finishes.
     *
     * @param status one of {@code PASSED}, {@code FAILED}, {@code SKIPPED}
     */
    public void finishTest(String status) {
        TestResultModel m = current.get();
        if (m == null) return;
        m.setStatus(status);
        m.setDurationMs(System.currentTimeMillis() - m.getStartTime());
        results.add(m);
        current.remove();
    }

    // ── Step logging ─────────────────────────────────────────────────────────

    /**
     * Log a neutral step (shown with → in the report).
     * Call from within a running test to document what the test is doing.
     * <pre>{@code
     * OpenReporter.getInstance().step("Navigate to login page");
     * OpenReporter.getInstance().step("Enter credentials");
     * }</pre>
     */
    public void step(String description)     { addStep(description, "INFO"); }

    /** Log a step that explicitly passed (shown with ✓ in the report). */
    public void stepPass(String description) { addStep(description, "PASS"); }

    /** Log a step that explicitly failed (shown with ✗ in the report). */
    public void stepFail(String description) { addStep(description, "FAIL"); }

    private void addStep(String description, String status) {
        TestResultModel m = current.get();
        if (m == null) return;
        m.addStep(new StepModel(description, status));
    }

    // ── Report generation ────────────────────────────────────────────────────

    /**
     * Generates the HTML report.
     * Called once after all tests finish (by the adapter's suite/run finish hook).
     * Thread-safe — only the first thread to call wins; subsequent calls are no-ops.
     */
    public synchronized void generateReport() {
        try {
            ReporterConfig cfg = ReporterConfig.getInstance();
            String outputDir  = cfg.getOutputDir();
            String reportFile = outputDir + "/report.html";

            String html = fillTemplate(readTemplate(), cfg);
            File dir = new File(outputDir);
            if (!dir.exists()) dir.mkdirs();
            Files.write(new File(reportFile).toPath(), html.getBytes(StandardCharsets.UTF_8));

            System.out.println("\n╔══════════════════════════════════════════════════╗");
            System.out.println("║  OpenReporter → " + reportFile + "  ║");
            System.out.println("╚══════════════════════════════════════════════════╝\n");
        } catch (Exception e) {
            System.err.println("[OpenReporter] Report generation failed: " + e.getMessage());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String readTemplate() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(TEMPLATE)) {
            if (is == null) throw new IllegalStateException(TEMPLATE + " not found on classpath");
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String fillTemplate(String tpl, ReporterConfig cfg) throws Exception {
        long passed  = results.stream().filter(r -> "PASSED".equals(r.getStatus())).count();
        long failed  = results.stream().filter(r -> "FAILED".equals(r.getStatus())).count();
        long skipped = results.stream().filter(r -> "SKIPPED".equals(r.getStatus())).count();
        long total   = results.size();
        double passRate = total > 0 ? (passed * 100.0 / total) : 0;
        long totalMs    = results.stream().mapToLong(TestResultModel::getDurationMs).sum();
        String duration = totalMs >= 1000
                ? String.format("%.1fs", totalMs / 1000.0) : totalMs + "ms";
        String generated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String jsonData  = new ObjectMapper().writeValueAsString(results);
        String env       = cfg.getEnvironment().isEmpty() ? "—" : cfg.getEnvironment();

        return tpl
                .replace("{{TITLE}}",          cfg.getTitle())
                .replace("{{GENERATED_AT}}",   generated)
                .replace("{{ENVIRONMENT}}",    env)
                .replace("{{TOTAL}}",          String.valueOf(total))
                .replace("{{PASSED}}",         String.valueOf(passed))
                .replace("{{FAILED}}",         String.valueOf(failed))
                .replace("{{SKIPPED}}",        String.valueOf(skipped))
                .replace("{{PASS_RATE}}",      String.format("%.1f", passRate))
                .replace("{{TOTAL_DURATION}}", duration)
                .replace("{{LOGO_ELEMENT}}",   buildLogoElement(cfg.getLogo()))
                .replace("{{TEST_DATA_JSON}}", jsonData);
    }

    private String buildLogoElement(String path) {
        try {
            if (path == null || path.trim().isEmpty()) return "";

            File file = new File(path);
            if (!file.isAbsolute()) file = new File(System.getProperty("user.dir"), path);
            if (!file.exists() || !file.isFile()) {
                System.err.println("[OpenReporter] Logo not found: " + file.getAbsolutePath());
                return "";
            }

            byte[] bytes = Files.readAllBytes(file.toPath());
            String name  = file.getName().toLowerCase();
            String mime  = name.endsWith(".svg")  ? "image/svg+xml"
                         : name.endsWith(".jpg") || name.endsWith(".jpeg") ? "image/jpeg"
                         : name.endsWith(".gif")  ? "image/gif"
                         : "image/png";
            String src = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
            return "<img src=\"" + src + "\" alt=\"Logo\" class=\"project-logo\"/>";
        } catch (Exception e) {
            System.err.println("[OpenReporter] Could not load logo: " + e.getMessage());
            return "";
        }
    }
}
