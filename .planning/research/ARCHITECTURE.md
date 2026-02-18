# Architecture Patterns

**Domain:** HTML-to-PDF template rendering (Spring Boot, Thymeleaf, OpenHTMLtoPDF)
**Researched:** 2026-02-18
**Confidence:** MEDIUM (based on training knowledge; could not verify against live docs due to tool restrictions)

## Recommended Architecture

This is a test-only demo project with no running application. The architecture is a simple linear pipeline invoked exclusively from JUnit tests.

```
JUnit Test
  |
  v
TemplateDataFactory         (builds model Map<String, Object>)
  |
  v
QrCodeGenerator             (generates QR code as base64 data URI)
  |
  v
ThymeleafRenderer           (processes HTML template + model -> rendered HTML string)
  |
  v
PdfGenerator                (rendered HTML + CSS + fonts + images -> PDF bytes)
  |
  v
File output                 (writes PDF to target/test-output/)
```

There is no web layer, no controllers, no running Spring context needed for the core pipeline. Spring Boot is used only for its auto-configuration of Thymeleaf and for test infrastructure (`@SpringBootTest` or standalone `SpringTemplateEngine` setup).

### Component Boundaries

| Component | Responsibility | Inputs | Outputs | Communicates With |
|-----------|---------------|--------|---------|-------------------|
| **TemplateDataFactory** | Builds the data model (names, addresses, dates, QR code data URI) for a given template | Template identifier, raw data values | `Map<String, Object>` context | QrCodeGenerator (to get QR image) |
| **QrCodeGenerator** | Generates QR code images as base64 data URIs | String payload (URL or text) | `String` (data URI: `data:image/png;base64,...`) | None (pure utility) |
| **ThymeleafRenderer** | Processes a Thymeleaf HTML template with a data model | Template name, `Map<String, Object>` model | `String` (fully rendered HTML) | Thymeleaf `SpringTemplateEngine` |
| **PdfGenerator** | Converts rendered HTML string to PDF bytes using OpenHTMLtoPDF | Rendered HTML string, base URI for resources | `byte[]` (PDF content) | OpenHTMLtoPDF builder API |
| **JUnit Tests** | Orchestrate the full pipeline, assert output exists, write PDF files | None (entry point) | PDF files in `target/test-output/` | All components above |

### Key Design Principle: No Spring Web Required

The project does NOT need `spring-boot-starter-web`. It needs:
- `spring-boot-starter-thymeleaf` (for template engine auto-config)
- `spring-boot-starter-test` (for JUnit + Spring test context)
- `openhtmltopdf-pdfbox` (PDF rendering)
- A QR code library (e.g., `com.google.zxing:core` + `javase`)

Thymeleaf can operate in standalone mode (without a servlet context) by using `SpringTemplateEngine` with a `ClassLoaderTemplateResolver`. This is the correct approach for a test-only project.

## Data Flow

### Step-by-step pipeline

```
1. TEST SETUP
   JUnit test method defines:
   - Template name (e.g., "template-a")
   - Placeholder values (name, address, date, etc.)
   - QR code payload (URL string)

2. QR CODE GENERATION
   QrCodeGenerator.generateDataUri("https://example.com/doc/123")
   -> Uses ZXing to create QR BitMatrix
   -> Renders to BufferedImage
   -> Encodes as PNG -> base64
   -> Returns "data:image/png;base64,iVBOR..."

3. MODEL ASSEMBLY
   Map<String, Object> model = new HashMap<>();
   model.put("recipientName", "Max Mustermann");
   model.put("recipientAddress", "Musterstraße 1, 12345 Berlin");
   model.put("qrCodeDataUri", qrDataUri);
   model.put("date", LocalDate.now());
   // ... more placeholders

4. THYMELEAF RENDERING
   ThymeleafRenderer.render("template-a", model)
   -> SpringTemplateEngine resolves "template-a" to
      classpath:templates/template-a.html
   -> Thymeleaf processes th:text, th:src, th:each etc.
   -> Returns fully rendered HTML string with all
      placeholders replaced

5. PDF GENERATION
   PdfGenerator.generatePdf(renderedHtml, baseUri)
   -> OpenHTMLtoPDF PdfRendererBuilder
   -> Sets HTML content (rendered string)
   -> Sets base URI (for resolving relative CSS/image paths)
   -> Registers custom fonts (.ttf files)
   -> Renders to OutputStream -> byte[]

6. OUTPUT
   Files.write(Paths.get("target/test-output/template-a.pdf"), pdfBytes)
   -> PDF viewable with any PDF reader
```

### Resource Resolution Flow

```
HTML template references:
  <link rel="stylesheet" href="css/print.css" />
  <img th:src="${qrCodeDataUri}" />
  <img src="images/logo.png" />

OpenHTMLtoPDF resolves relative paths against baseUri:
  baseUri = "classpath:/templates/"  or  file path to resources

  css/print.css     -> resolved via baseUri -> loaded by OpenHTMLtoPDF
  images/logo.png   -> resolved via baseUri -> embedded in PDF
  data:image/png;.. -> inline data URI -> embedded directly (no resolution needed)
  fonts/            -> registered explicitly via builder.useFont()
```

**Critical insight:** OpenHTMLtoPDF resolves resources relative to a base URI. The simplest approach for a classpath-based project is to either:
1. Set the base URI to the filesystem path of `src/test/resources/templates/` (works reliably), or
2. Implement a custom `FSSupplier` / URI resolver for classpath resources.

Option 1 is strongly recommended for a demo project. Option 2 is production-grade but unnecessary complexity here.

## Recommended Directory Structure

```
print/
├── pom.xml
├── .planning/
│   ├── PROJECT.md
│   └── research/
│       └── ARCHITECTURE.md          (this file)
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/example/print/
│   │           ├── PrintApplication.java        (minimal @SpringBootApplication, no main logic)
│   │           ├── renderer/
│   │           │   ├── ThymeleafRenderer.java    (template processing)
│   │           │   └── PdfGenerator.java         (HTML -> PDF conversion)
│   │           ├── qr/
│   │           │   └── QrCodeGenerator.java      (QR code generation)
│   │           └── model/
│   │               └── TemplateDataFactory.java  (model building)
│   └── test/
│       ├── java/
│       │   └── com/example/print/
│       │       ├── TemplateATest.java            (JUnit: template A end-to-end)
│       │       ├── TemplateBTest.java            (JUnit: template B end-to-end)
│       │       └── renderer/
│       │           ├── ThymeleafRendererTest.java (unit: template rendering only)
│       │           └── PdfGeneratorTest.java      (unit: PDF generation only)
│       └── resources/
│           ├── templates/
│           │   ├── template-a.html               (2-page doc with QR + address)
│           │   ├── template-b.html               (1-page 2-column layout)
│           │   ├── css/
│           │   │   ├── common.css                (shared print styles)
│           │   │   ├── template-a.css            (template A specific)
│           │   │   └── template-b.css            (template B specific)
│           │   ├── fonts/
│           │   │   ├── DejaVuSans.ttf            (or other embeddable font)
│           │   │   └── DejaVuSans-Bold.ttf
│           │   └── images/
│           │       └── logo.png                  (company logo if needed)
│           └── application-test.yml              (Thymeleaf config overrides)
├── docx/
│   ├── template-a.docx                           (source Word document A)
│   └── template-b.docx                           (source Word document B)
└── target/
    └── test-output/                              (generated PDFs, gitignored)
        ├── template-a.pdf
        └── template-b.pdf
```

### Why this structure

| Decision | Rationale |
|----------|-----------|
| Source code in `src/main/java` | Even though tests are the only entry point, the renderer/generator classes are production code that tests exercise. Putting them in main makes the Maven structure conventional. |
| Templates in `src/test/resources/templates/` | Templates are test resources. They are not served by a web layer. Placing them under test keeps the intent clear. |
| CSS/fonts/images nested under `templates/` | OpenHTMLtoPDF resolves resources relative to the HTML file's base URI. Co-locating CSS, fonts, and images with templates makes relative path resolution trivial. |
| `docx/` at project root | The .docx files are source artifacts, not code. They illustrate the upstream workflow but are not processed by the application. |
| `target/test-output/` for PDFs | Standard Maven output directory. Automatically cleaned by `mvn clean`. Gitignored. |

## Patterns to Follow

### Pattern 1: Builder-based PDF Generation

**What:** Use OpenHTMLtoPDF's `PdfRendererBuilder` with method chaining.
**When:** Every PDF generation call.
**Why:** The builder encapsulates font registration, base URI, and output stream management in a clean API.

```java
public class PdfGenerator {

    private final List<FontRegistration> fonts;

    public byte[] generate(String renderedHtml, String baseUri) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(renderedHtml, baseUri);

            for (FontRegistration font : fonts) {
                builder.useFont(font.file(), font.family());
            }

            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        }
    }
}
```

### Pattern 2: Standalone Thymeleaf Engine

**What:** Configure `SpringTemplateEngine` with a `ClassLoaderTemplateResolver` pointing to the templates directory.
**When:** In the renderer component or Spring configuration.
**Why:** No servlet context needed. Templates are resolved from classpath.

```java
public class ThymeleafRenderer {

    private final SpringTemplateEngine engine;

    public ThymeleafRenderer() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        this.engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
    }

    public String render(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        return engine.process(templateName, context);
    }
}
```

### Pattern 3: QR Code as Data URI

**What:** Generate QR codes as base64-encoded PNG data URIs, embedded directly in the HTML model.
**When:** Any template that contains a QR code.
**Why:** Data URIs are self-contained -- no file resolution needed. OpenHTMLtoPDF handles `data:` URIs natively. This avoids filesystem temp files and classpath resolution issues.

```java
public class QrCodeGenerator {

    public String generateDataUri(String content, int size) throws Exception {
        BitMatrix matrix = new MultiFormatWriter()
            .encode(content, BarcodeFormat.QR_CODE, size, size);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        return "data:image/png;base64," + base64;
    }
}
```

### Pattern 4: Test Output to Target Directory

**What:** Write generated PDFs to `target/test-output/` so they survive the test run but are cleaned by `mvn clean`.
**When:** Every end-to-end test.
**Why:** Allows manual inspection of generated PDFs. Maven-conventional output location.

```java
@Test
void shouldGenerateTemplateA() throws Exception {
    // ... pipeline steps ...
    Path outputDir = Paths.get("target", "test-output");
    Files.createDirectories(outputDir);
    Files.write(outputDir.resolve("template-a.pdf"), pdfBytes);

    assertThat(pdfBytes).isNotEmpty();
    assertThat(pdfBytes).startsWith(new byte[]{'%', 'P', 'D', 'F'});
}
```

## Anti-Patterns to Avoid

### Anti-Pattern 1: Using `@SpringBootTest` When Not Needed

**What:** Starting the full Spring context for every test.
**Why bad:** Slow startup, unnecessary for a template processing pipeline. The components are simple POJOs.
**Instead:** Use `@SpringBootTest` only for integration tests that verify Spring wiring. For unit tests of individual components (renderer, QR generator), instantiate directly. Consider a single integration test that verifies the full Spring-wired pipeline, and unit tests for everything else.

### Anti-Pattern 2: Serving Templates from `src/main/resources/templates/`

**What:** Placing templates in the main resources directory as if they were served by a web app.
**Why bad:** Misleading -- suggests a web layer that does not exist. Templates are test fixtures.
**Instead:** Place in `src/test/resources/templates/`. If you need them in main (for Spring auto-config), document clearly that no web serving occurs.

**Caveat:** If you use `@SpringBootTest` and want Thymeleaf auto-config to find templates automatically, Spring expects them in `src/main/resources/templates/` by default. You can override this with `spring.thymeleaf.prefix=classpath:/templates/` in `application-test.yml`, or configure the resolver manually (Pattern 2 above). The manual resolver approach is recommended for this project.

### Anti-Pattern 3: Filesystem Temp Files for QR Codes

**What:** Writing QR codes to temp files and referencing them with `file://` URIs in HTML.
**Why bad:** Fragile paths, cleanup burden, platform-dependent separators, race conditions in parallel tests.
**Instead:** Use data URIs (Pattern 3). Self-contained, no cleanup, works everywhere.

### Anti-Pattern 4: Relying on System Fonts

**What:** Not embedding fonts in the PDF, assuming system fonts are available.
**Why bad:** PDFs render differently on different systems. CI servers may lack fonts entirely.
**Instead:** Bundle `.ttf` fonts in the project and register them explicitly with OpenHTMLtoPDF's `useFont()`. Use only these registered fonts in CSS.

## Component Integration Details

### OpenHTMLtoPDF + Thymeleaf Integration

These two libraries do NOT know about each other. The integration point is a **String**:

```
Thymeleaf                          OpenHTMLtoPDF
   |                                    |
   | produces rendered HTML (String)    |
   |----------------------------------->|
   |                                    | consumes HTML string
   |                                    | resolves CSS/images via baseUri
   |                                    | produces PDF bytes
```

There is no plugin, adapter, or bridge library needed. Thymeleaf's output is a plain HTML string. OpenHTMLtoPDF's input is a plain HTML string. The connection is trivial.

### OpenHTMLtoPDF Resource Resolution

OpenHTMLtoPDF needs to resolve resources referenced in the HTML (CSS files, images, fonts). The `baseUri` parameter controls this:

| Resource Type | How Referenced in HTML | How Resolved |
|---------------|----------------------|--------------|
| CSS files | `<link href="css/print.css">` | Relative to baseUri |
| Images | `<img src="images/logo.png">` | Relative to baseUri |
| QR codes | `<img th:src="${qrCodeDataUri}">` | Inline data URI, no resolution |
| Fonts | Not in HTML | Registered via `builder.useFont()` |

**Base URI strategy for this project:**

```java
// Resolve the filesystem path to the templates directory
// This works because test resources are on the classpath
URL resource = getClass().getClassLoader().getResource("templates/");
String baseUri = resource.toExternalForm();
// Result: "file:/path/to/target/test-classes/templates/"
```

This allows OpenHTMLtoPDF to resolve `css/print.css` to `file:/path/to/target/test-classes/templates/css/print.css`, which works reliably.

### CSS for Print Layout

OpenHTMLtoPDF supports CSS 2.1 plus selected CSS3 properties. Key print-specific CSS:

```css
/* Page setup */
@page {
    size: A4;
    margin: 20mm 15mm 20mm 15mm;
}

/* Page breaks */
.page-break {
    page-break-after: always;
}

/* Avoid breaking inside elements */
.no-break {
    page-break-inside: avoid;
}

/* Two-column layout (for template B) */
.two-column {
    column-count: 2;
    column-gap: 10mm;
}
```

**Important limitation:** OpenHTMLtoPDF does NOT support Flexbox or CSS Grid. Use floats, tables, or CSS columns for layout. This is a critical constraint when converting Word-derived HTML, which may contain complex layouts.

## Build Order (Suggested Implementation Sequence)

Components should be built in dependency order, bottom-up:

```
Phase 1: Foundation
  1. Maven project setup (pom.xml with all dependencies)
  2. Minimal SpringBootApplication class
  3. QrCodeGenerator (standalone utility, no dependencies)

Phase 2: Template Engine
  4. ThymeleafRenderer (depends on: Thymeleaf dependency)
  5. First HTML template (template-a.html) with CSS
  6. Unit test: ThymeleafRenderer produces expected HTML

Phase 3: PDF Pipeline
  7. PdfGenerator (depends on: OpenHTMLtoPDF dependency)
  8. Font registration
  9. Unit test: PdfGenerator produces valid PDF from static HTML

Phase 4: Integration
  10. End-to-end test: template-a (Thymeleaf + QR + PDF)
  11. Template B (template-b.html with 2-column layout)
  12. End-to-end test: template-b

Phase 5: Polish
  13. .docx source files
  14. CSS refinement for pixel-accuracy
  15. README / documentation
```

**Dependency rationale:**
- QR generator is standalone: build first, test first, done.
- Thymeleaf renderer depends only on its library: build and test with a trivial template.
- PDF generator depends only on OpenHTMLtoPDF: test with hardcoded HTML before integrating with Thymeleaf.
- Integration tests combine everything: only after individual components are proven.

## Scalability Considerations

This is a demo project, but for reference:

| Concern | Demo (this project) | Production scale |
|---------|--------------------|--------------------|
| Template count | 2 templates, hardcoded | Template registry, database-driven |
| PDF generation speed | Synchronous, single-threaded | Thread pool, async generation |
| Font loading | Load per PDF generation | Cache font files, load once at startup |
| Memory | Small documents fit in memory | Stream large PDFs to disk |
| Thymeleaf engine | Create once, reuse | Spring-managed singleton bean |

## Sources and Confidence

| Claim | Confidence | Basis |
|-------|-----------|-------|
| OpenHTMLtoPDF builder API (`PdfRendererBuilder`) | MEDIUM | Training data; could not verify current API against live docs |
| Thymeleaf standalone mode with `ClassLoaderTemplateResolver` | HIGH | Well-established pattern, stable across versions |
| QR code via ZXing as data URI | HIGH | Standard approach, stable library |
| OpenHTMLtoPDF CSS support (no Flexbox/Grid) | MEDIUM | Training data; may have changed in recent versions |
| Resource resolution via base URI | MEDIUM | Training data; consistent across OpenHTMLtoPDF versions in training |
| `useFastMode()` on builder | LOW | May have been renamed or removed; verify against current docs |
| CSS `column-count` support | MEDIUM | Documented in training data but should be verified |

**Gaps to verify during implementation:**
- Exact OpenHTMLtoPDF Maven artifact coordinates and latest version
- Whether `useFastMode()` still exists in current API
- CSS column support details (may need testing)
- Font registration API -- exact method signature may vary by version
