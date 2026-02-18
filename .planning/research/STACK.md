# Technology Stack

**Project:** Print (HTML-to-PDF Demo)
**Researched:** 2026-02-18
**Overall confidence:** MEDIUM — versions based on training data (May 2025 cutoff), not verified against live Maven Central. Verify all version numbers before use.

## Recommended Stack

### Core Framework

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| Spring Boot | 3.4.1 | Project scaffold, DI, Thymeleaf integration | Latest stable 3.x line; provides spring-boot-starter-thymeleaf for zero-config template engine setup. Test-only usage means no web server overhead. | MEDIUM — verify latest 3.4.x or 3.5.x on Maven Central |
| Java | 21 (LTS) | Runtime | LTS release, required by Spring Boot 3.x (minimum Java 17). Java 21 adds virtual threads and modern language features. | HIGH |
| Maven | 3.9.x | Build tool | Customer ecosystem requirement per PROJECT.md. Spring Boot parent POM handles dependency management. | HIGH |

### PDF Engine

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| openhtmltopdf-pdfbox | 1.1.22 | HTML+CSS to PDF rendering | The only serious modern option for Java HTML-to-PDF with CSS support. Uses PDFBox 2.x under the hood. Handles `@page` rules, multi-page layouts, CSS columns. Last known release was 1.1.22 (late 2024). | MEDIUM — verify on Maven Central; project may have released 1.1.23+ |
| openhtmltopdf-svg-support | 1.1.22 | SVG rendering in PDFs | Required if QR codes are rendered as SVG. Same version as core. | MEDIUM |

**Critical note on OpenHTMLtoPDF:** The project is maintained by danfickle on GitHub. Development has slowed but the library is stable and widely used. There is no actively-developed alternative with equivalent CSS support in the Java ecosystem.

### Template Engine

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| spring-boot-starter-thymeleaf | (managed by Spring Boot BOM) | HTML template processing with placeholder filling | Spring Boot's default template engine. Natural HTML syntax (th:text, th:each). Can be used standalone via `SpringTemplateEngine` without a web context — perfect for test-only usage. | HIGH |
| Thymeleaf | 3.1.x | (transitive via starter) | Thymeleaf 3.1 is the current major line. Spring Boot 3.4 manages the exact version. | HIGH |

### QR Code Generation

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| com.google.zxing:core | 3.5.3 | QR code matrix generation | De facto standard for barcode/QR generation in Java. Lightweight, no native dependencies. | MEDIUM — verify latest 3.5.x |
| com.google.zxing:javase | 3.5.3 | QR code to BufferedImage rendering | Provides `MatrixToImageWriter` for converting QR matrix to PNG image bytes. | MEDIUM |

**QR integration approach:** Generate QR code as PNG byte array at template-fill time, Base64-encode it, inject into Thymeleaf template as `<img src="data:image/png;base64,..." />`. This approach is self-contained (no external file references) and works reliably with OpenHTMLtoPDF.

### Word-to-HTML Preprocessing

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| Mammoth | CLI or JS library | .docx to clean semantic HTML | Produces clean HTML without Word's garbage markup (unlike Apache POI's XWPF). Used externally as a preprocessing step — NOT a Java dependency. | HIGH |

**Usage pattern:** Mammoth is run once per template as a preprocessing step (`mammoth input.docx --output-dir=templates/`). The resulting HTML is then manually edited to insert Thymeleaf placeholders. This is NOT a runtime dependency.

**If in-app conversion is ever needed:** `org.zwobble:mammoth` (Java port) exists at version 1.8.0. But per PROJECT.md, this is explicitly out of scope.

### Testing

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| spring-boot-starter-test | (managed by BOM) | JUnit 5, AssertJ, Mockito | Standard Spring Boot test dependencies. JUnit 5 is the deliverable format. | HIGH |
| PDFBox (transitive) | 2.0.x | PDF content verification in tests | Already a transitive dependency of OpenHTMLtoPDF. Can use `PDDocument.load()` + `PDFTextStripper` to verify PDF text content in assertions. | HIGH |

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| PDF Engine | OpenHTMLtoPDF | Flying Saucer (iText renderer) | OpenHTMLtoPDF IS the maintained fork of Flying Saucer. Flying Saucer itself is abandoned. iText is commercial (AGPL). |
| PDF Engine | OpenHTMLtoPDF | wkhtmltopdf / Playwright | Requires external binary (headless browser). Adds deployment complexity, native dependencies. Overkill for this use case. |
| PDF Engine | OpenHTMLtoPDF | Apache FOP | XSL-FO based, not HTML+CSS. Completely different paradigm. Much harder to use with Word-derived templates. |
| PDF Engine | OpenHTMLtoPDF | iText (direct) | Commercial license (AGPL). Low-level API — you build PDFs programmatically, not from HTML. Wrong abstraction for template-based generation. |
| QR Code | ZXing | QRGen | QRGen is a thin wrapper around ZXing. Adds a dependency for minimal convenience. ZXing's API is simple enough to use directly. |
| Template | Thymeleaf | FreeMarker | Not Spring Boot's default. Less natural HTML syntax (uses `${}` DSL, not HTML attributes). Thymeleaf templates can be opened in a browser for preview. |
| Template | Thymeleaf | Mustache/Handlebars | Too limited — no conditional logic, no iteration with index, no expression language. |
| Build Tool | Maven | Gradle | Customer ecosystem requirement specifies Maven. |

## What NOT to Use

| Library | Why Not |
|---------|---------|
| iText 5/7 | AGPL license. Commercial use requires paid license. OpenHTMLtoPDF uses PDFBox (Apache 2.0) instead. |
| Flying Saucer (original) | Abandoned. OpenHTMLtoPDF is its maintained successor. |
| Apache POI for HTML export | Produces horrible HTML from .docx. Mammoth exists specifically because POI's output is unusable for CSS styling. |
| Jsoup (for template filling) | Manual DOM manipulation is fragile. Thymeleaf is purpose-built for template processing with data binding. |
| WeasyPrint / Prince | Python/commercial. Not Java ecosystem. |
| JasperReports | XML-based report definition. Completely wrong paradigm for HTML template-based generation. Enormous dependency footprint. |

## Maven POM Structure

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.1</version> <!-- VERIFY: check Maven Central for latest 3.4.x or 3.5.x -->
    <relativePath/>
</parent>

<properties>
    <java.version>21</java.version>
    <openhtmltopdf.version>1.1.22</openhtmltopdf.version> <!-- VERIFY -->
    <zxing.version>3.5.3</zxing.version> <!-- VERIFY -->
</properties>

<dependencies>
    <!-- Thymeleaf for template processing (no web starter needed) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>

    <!-- OpenHTMLtoPDF for HTML+CSS -> PDF -->
    <dependency>
        <groupId>com.openhtmltopdf</groupId>
        <artifactId>openhtmltopdf-pdfbox</artifactId>
        <version>${openhtmltopdf.version}</version>
    </dependency>
    <dependency>
        <groupId>com.openhtmltopdf</groupId>
        <artifactId>openhtmltopdf-svg-support</artifactId>
        <version>${openhtmltopdf.version}</version>
    </dependency>

    <!-- ZXing for QR code generation -->
    <dependency>
        <groupId>com.google.zxing</groupId>
        <artifactId>core</artifactId>
        <version>${zxing.version}</version>
    </dependency>
    <dependency>
        <groupId>com.google.zxing</groupId>
        <artifactId>javase</artifactId>
        <version>${zxing.version}</version>
    </dependency>

    <!-- Test dependencies -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- No spring-boot-maven-plugin needed — no runnable app -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

**Key Maven decisions:**
- Use `spring-boot-starter-thymeleaf` NOT `spring-boot-starter-web` — we need the template engine without starting a web server
- OpenHTMLtoPDF is NOT managed by Spring Boot BOM, so explicit version required
- ZXing is NOT managed by Spring Boot BOM, so explicit version required
- No `spring-boot-maven-plugin` since there is no runnable application
- Surefire plugin runs JUnit tests via `mvn test`

## Thymeleaf Standalone Configuration

Since this project has no web context, configure Thymeleaf as a standalone template engine:

```java
@Configuration
public class ThymeleafConfig {
    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
```

Templates go in `src/main/resources/templates/`. Thymeleaf processes them with a `Context` object containing placeholder data, outputs rendered HTML string, which is then fed to OpenHTMLtoPDF.

## Version Verification Checklist

All versions below need verification against Maven Central before use. My training data has a May 2025 cutoff — newer versions may exist.

| Library | Listed Version | Verify At |
|---------|---------------|-----------|
| spring-boot-starter-parent | 3.4.1 | https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-parent |
| openhtmltopdf-pdfbox | 1.1.22 | https://mvnrepository.com/artifact/com.openhtmltopdf/openhtmltopdf-pdfbox |
| zxing core | 3.5.3 | https://mvnrepository.com/artifact/com.google.zxing/core |

## Sources

- OpenHTMLtoPDF GitHub: https://github.com/danfickle/openhtmltopdf (training data, not live-verified)
- ZXing GitHub: https://github.com/zxing/zxing (training data, not live-verified)
- Spring Boot docs: https://docs.spring.io/spring-boot/docs/current/reference/html/ (training data, not live-verified)
- Mammoth: https://github.com/mwilliamson/mammoth.js (training data, not live-verified)
- Thymeleaf docs: https://www.thymeleaf.org/documentation.html (training data, not live-verified)

**Honesty note:** WebSearch, WebFetch, and Bash tools were unavailable during this research session. All version numbers are from training data (cutoff May 2025) and must be verified against Maven Central before writing the final pom.xml. The library choices themselves are HIGH confidence — these are the established, standard tools for this problem space. Only the exact version numbers are uncertain.
