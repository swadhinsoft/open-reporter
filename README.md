# OpenReporter

**Framework-agnostic HTML test reporter for Java**

One dependency. Beautiful, self-contained HTML reports for TestNG, JUnit 5, and Cucumber — with Selenium or Appium.

[![JitPack](https://jitpack.io/v/swadhinsoft/open-reporter.svg)](https://jitpack.io/#swadhinsoft/open-reporter)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 11+](https://img.shields.io/badge/Java-11%2B-orange.svg)](https://adoptium.net)

---

## Features

| | |
|---|---|
| **Interactive dashboard** | Doughnut chart, pass-rate ring, per-browser bar chart |
| **Auto screenshots** | Captured on failure, embedded as base64 |
| **Dark / Light mode** | Toggle persisted in `localStorage` |
| **Browser & OS icons** | Chrome, Firefox, Edge, Safari, Windows, macOS, Android, Linux |
| **Search & Filter** | Live search + Passed / Failed / Skipped filters |
| **Excel & PDF export** | SheetJS + jsPDF with coloured status cells |
| **Custom logo** | Path in `openreporter.json` — embedded as base64 |
| **Thread-safe** | `ThreadLocal` — safe for parallel test execution |
| **Self-contained** | One `.html` file, no external dependencies to host |

---

## Installation

Add JitPack to `pom.xml`:

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
```

Add the dependency:

```xml
<dependency>
  <groupId>com.github.swadhinsoft</groupId>
  <artifactId>open-reporter</artifactId>
  <version>1.0.0</version>
  <scope>test</scope>
</dependency>
```

**Gradle (Kotlin DSL):**
```kotlin
testImplementation("com.github.swadhinsoft:open-reporter:1.0.0")
```

---

## Quick Start

### 1. Drop `openreporter.json` in your project root

```json
{
  "title":       "My Test Report",
  "logo":        "src/test/resources/logo.png",
  "outputDir":   "target/open-reporter",
  "environment": "QA"
}
```

All fields are optional — the reporter works with no config file at all.

### 2. Wire the adapter for your framework

**TestNG** — `testng.xml`:
```xml
<listeners>
  <listener class-name="io.github.swadhinsoft.openreporter.adapter.TestNGListener"/>
</listeners>
```

**JUnit 5** — annotate your test class:
```java
@ExtendWith(JUnit5Extension.class)
public class MyTest { ... }
```

**Cucumber** — `@CucumberOptions`:
```java
@CucumberOptions(
    plugin = { "io.github.swadhinsoft.openreporter.adapter.CucumberPlugin" }
)
```

### 3. Register your driver in setUp / tearDown

```java
// @BeforeMethod / @BeforeEach / Cucumber @Before
OpenReporter.getInstance().registerDriver(driver);
OpenReporter.getInstance().setBrowser("chrome");

// @AfterMethod / @AfterEach / Cucumber @After
OpenReporter.getInstance().unregisterDriver();
```

That's it. After your test run, open `target/open-reporter/report.html`.

---

## Configuration reference

| Key | Description | Default |
|-----|-------------|---------|
| `title` | Page title shown in browser tab and header | `"Test Execution Report"` |
| `logo` | Path to logo file (PNG/JPG/SVG/GIF) — relative to project root or absolute | `""` (default icon) |
| `outputDir` | Directory where `report.html` is written | `"target/open-reporter"` |
| `environment` | Environment label in the report header | `"—"` |

**Config file lookup order:**
1. `-Dopenreporter.config=/path/to/file` JVM property
2. `openreporter.json` in project root (`user.dir`)
3. `openreporter.json` on the classpath
4. Built-in defaults (no file needed)

---

## Framework compatibility

| Framework | Adapter class | How it works |
|-----------|--------------|--------------|
| **TestNG** | `TestNGListener` | `ITestListener` + `ISuiteListener` — report on suite finish |
| **JUnit 5** | `JUnit5Extension` | `BeforeEachCallback` + `TestWatcher` + `AfterAllCallback` |
| **Cucumber 7** | `CucumberPlugin` | `io.cucumber.plugin.EventListener` — report on `TestRunFinished` |
| **Selenium** | `registerDriver(driver)` | Any `WebDriver` implementation |
| **Appium** | `registerDriver(driver)` | `AppiumDriver` extends `RemoteWebDriver` extends `WebDriver` ✓ |

---

## GitHub Actions — attach report as artifact

```yaml
- name: Run tests
  run: mvn test

- name: Upload OpenReporter
  uses: actions/upload-artifact@v4
  if: always()
  with:
    name: open-reporter-${{ github.run_number }}
    path: target/open-reporter/
    retention-days: 14
```

---

## Report output

```
target/open-reporter/
└── report.html       ← self-contained, share this file directly
```

Open it in any browser. No web server needed.

---

## License

MIT © [swadhinsoft](https://github.com/swadhinsoft)
