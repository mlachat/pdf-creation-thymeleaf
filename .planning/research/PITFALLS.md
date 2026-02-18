# Domain Pitfalls: HTML-to-PDF with OpenHTMLtoPDF + Thymeleaf

**Domain:** PDF generation from HTML/CSS templates in Spring Boot
**Researched:** 2026-02-18
**Confidence note:** Based on training data knowledge of OpenHTMLtoPDF (up to v1.0.10/1.1.x). Web research tools were unavailable during this session, so findings rely on training data. Treat as MEDIUM confidence overall; verify version-specific details against current OpenHTMLtoPDF docs and GitHub issues.

---

## Critical Pitfalls

Mistakes that cause rewrites, broken output, or major rework.

---

### Pitfall 1: CSS Flexbox and Grid Do Not Work

**What goes wrong:** Developers write HTML/CSS using modern layout techniques (flexbox, CSS grid) and get completely broken PDF output. OpenHTMLtoPDF is based on the Flying Saucer CSS 2.1 rendering engine. It does NOT support `display: flex`, `display: grid`, `gap`, `align-items`, `justify-content`, or any flexbox/grid properties.

**Why it happens:** Templates are often prototyped in a browser where flexbox/grid work perfectly. The developer assumes PDF rendering uses the same CSS engine.

**Consequences:** Layout is completely wrong in PDF. Requires full CSS rewrite using floats, tables, or `display: inline-block`. This can be a day or more of rework if discovered late.

**Prevention:**
- Use ONLY CSS 2.1 layout: `float`, `display: table` / `display: table-cell`, `display: inline-block`, `position: absolute/relative`, and `margin`/`padding`.
- For multi-column layouts (like the 2-column template), use `display: table` with `display: table-cell` children, or `float: left` with explicit widths.
- Establish a "supported CSS" reference list in Phase 1 before any template work begins.
- Test PDF output immediately when creating templates; never assume browser preview equals PDF output.

**Detection:** PDF layout looks nothing like browser preview. Elements stack vertically instead of horizontally.

**Phase:** Address in Phase 1 (project setup). Document CSS constraints before template development starts.

**Confidence:** HIGH -- this is a well-known, fundamental limitation of OpenHTMLtoPDF's rendering engine.

---

### Pitfall 2: Font Not Embedding -- Invisible or Substituted Text

**What goes wrong:** Text in the PDF renders as blank rectangles, question marks, or falls back to a default serif font. This is especially common with non-Latin characters (German umlauts, accented characters) but can happen with any custom font.

**Why it happens:** OpenHTMLtoPDF requires explicit font registration via its builder API. Simply referencing a font in CSS `font-family` is not enough. The font file (.ttf or .otf) must be:
1. Present on the classpath or filesystem
2. Registered with `builder.useFont(...)` or via `@font-face` with a resolvable `src` URL
3. The CSS `font-family` name must exactly match the registered name

**Consequences:** Text disappears or renders in wrong font. Umlauts and special characters (critical for German-language documents) may be missing entirely.

**Prevention:**
```java
// Register fonts explicitly in the PdfRendererBuilder
PdfRendererBuilder builder = new PdfRendererBuilder();
builder.useFont(
    new File("src/main/resources/fonts/DejaVuSans.ttf"),  // or supplier
    "DejaVu Sans"
);
// OR use useFont with a Supplier<InputStream> for classpath resources:
builder.useFont(
    () -> getClass().getResourceAsStream("/fonts/DejaVuSans.ttf"),
    "DejaVu Sans"
);
```
- Use DejaVuSans or a similar Unicode-complete font that covers Latin Extended (German umlauts, French accents, etc.).
- Always test with non-ASCII characters early: "Pruefbescheinigung", umlauts, etc.
- Register BOTH regular and bold/italic variants if needed; OpenHTMLtoPDF does NOT auto-synthesize bold/italic.

**Detection:** Open generated PDF -- text is missing, shows squares, or is in Times New Roman when you expected a custom font.

**Phase:** Address in Phase 1 (project setup). Font registration should be part of the core PDF service, not an afterthought.

**Confidence:** HIGH -- font registration is the single most common OpenHTMLtoPDF support question.

---

### Pitfall 3: Resource Paths Break Between App Context and Test Context

**What goes wrong:** Images, fonts, and CSS files load correctly when the app runs as a Spring Boot application but fail during JUnit tests (or vice versa). Paths like `/static/images/logo.png` or `classpath:` references resolve differently.

**Why it happens:** OpenHTMLtoPDF resolves relative URLs based on a "base URI". In a running Spring Boot app, the base URI might be set to the classpath root or a temp directory. In a JUnit test, the working directory and classpath are different. Common failure modes:
- `file:///` paths that work on dev machine but not in CI
- Relative paths that resolve against the wrong directory
- `classpath:` protocol not understood by OpenHTMLtoPDF's default URI resolver

**Consequences:** PDFs render without images (broken image icons or blank spaces), missing CSS, or missing fonts -- but only in tests (or only in production).

**Prevention:**
```java
// Set a proper base URI that works in both contexts
// Option A: Use classpath resource URL as base
URL baseUrl = getClass().getResource("/templates/");
builder.withUri(baseUrl.toString());

// Option B: Use a custom URI resolver
builder.useHttpStreamImplementation(new CustomResourceResolver());

// Option C: Use withHtmlContent and explicit baseUri
String html = thymeleafEngine.process("template", context);
builder.withHtmlContent(html, baseUrl.toExternalForm());
```
- Always use `builder.withHtmlContent(html, baseUri)` rather than `builder.withUri(htmlFileUri)` when working with Thymeleaf-processed strings.
- For images, use `data:` URIs (base64-encoded inline images) as a bulletproof fallback for small images like QR codes.
- Write a JUnit test that generates a PDF in Phase 1 and visually verify it opens correctly. Do NOT defer test-context verification.

**Detection:** Tests pass (no exceptions) but generated PDF files have missing images or broken layout. You must actually open the test-output PDFs to notice.

**Phase:** Address in Phase 1. The test harness must produce correct PDFs from day one.

**Confidence:** HIGH -- resource resolution is a perennial issue with PDF libraries in Java.

---

### Pitfall 4: Page Breaks Cause Content Overlap or Clipping

**What goes wrong:** Content on multi-page documents overlaps across page boundaries, gets cut off mid-line, or page breaks appear in the wrong place. Tables split in visually broken ways.

**Why it happens:** OpenHTMLtoPDF supports `page-break-before`, `page-break-after`, and `page-break-inside` CSS properties, but they behave differently from browser rendering:
- `page-break-inside: avoid` on large elements may be ignored if the element is taller than a page
- Margin/padding on elements near page boundaries can cause content to "disappear" into the gap between pages
- Running elements (headers/footers via `@page` margin boxes) consume page real estate, reducing the content area

**Consequences:** The 2-page template with QR code may have content cut off at the page boundary or the QR code may split across pages.

**Prevention:**
- Use explicit `page-break-before: always` on the second page's container element for the 2-page template.
- Avoid relying on `page-break-inside: avoid` for large blocks; instead, structure HTML so natural break points align with page boundaries.
- Define page size and margins explicitly:
```css
@page {
    size: A4;
    margin: 20mm 15mm 20mm 15mm;
}
```
- Calculate available content height: A4 = 297mm tall, minus top+bottom margins. Design content to fit within that budget per page.
- For the 2-page template: make each page a separate `<div>` with `page-break-before: always` on the second one, rather than relying on content flow.

**Detection:** Open the PDF and check page boundaries carefully. Content may look fine at a glance but have subtle clipping at the bottom of page 1.

**Phase:** Address in Phase 2 (template development). Requires iterative visual testing.

**Confidence:** HIGH -- page break behavior is well-documented as tricky in OpenHTMLtoPDF.

---

## Moderate Pitfalls

Mistakes that cause delays, debugging sessions, or accumulated technical debt.

---

### Pitfall 5: QR Code Renders Blurry or Not At All

**What goes wrong:** QR codes generated as images (e.g., via ZXing/Google ZXing) appear blurry, pixelated, or fail to render in the PDF. Sometimes the QR code works in browser preview but not in the PDF.

**Why it happens:** Multiple failure modes:
1. QR code is generated as a small raster image (e.g., 100x100 pixels) and scaled up in CSS, causing blur
2. QR code is passed as a `data:image/png;base64,...` URI but the base64 encoding is malformed or too large
3. QR code library generates SVG, but OpenHTMLtoPDF SVG support requires the separate `openhtmltopdf-svg-support` module
4. Image path resolution fails (see Pitfall 3)

**Prevention:**
- Generate QR codes at a sufficient resolution: at least 300x300 pixels for print-quality PDF, even if displayed smaller.
- Use PNG format with `data:image/png;base64,...` inline in the HTML. This avoids all path resolution issues.
- If using SVG QR codes, add the `openhtmltopdf-svg-support` Maven dependency:
```xml
<dependency>
    <groupId>com.openhtmltopdf</groupId>
    <artifactId>openhtmltopdf-svg-support</artifactId>
    <version>${openhtmltopdf.version}</version>
</dependency>
```
  And register the SVG drawer: `builder.useSVGDrawer(new BatikSVGDrawer());`
- Set explicit `width` and `height` attributes on the `<img>` tag, not just CSS, for reliable sizing.

**Detection:** Scan the QR code in the generated PDF with a phone. If it fails to scan, the resolution or rendering is broken.

**Phase:** Address in Phase 2 (template with QR code). Test QR scannability as part of acceptance criteria.

**Confidence:** MEDIUM -- based on common patterns with ZXing + OpenHTMLtoPDF integration.

---

### Pitfall 6: Maven Dependency Version Conflicts

**What goes wrong:** Runtime errors like `NoSuchMethodError`, `ClassNotFoundException`, or `AbstractMethodError` related to PDF rendering, XML parsing, or image processing.

**Why it happens:** OpenHTMLtoPDF pulls in transitive dependencies (Batik for SVG, Apache PDF-BOX, JSoup, ICU4J) that can conflict with versions brought in by Spring Boot's dependency management. Common conflicts:
- **Batik** (for SVG support) vs other XML/SVG libraries
- **pdfbox** version mismatches if another library also uses PDFBox
- **JSoup** version (OpenHTMLtoPDF uses JSoup internally; Spring Boot may bring a different version)
- **BouncyCastle** if PDF encryption is used

**Consequences:** App compiles fine but crashes at runtime during PDF generation. Errors are cryptic and hard to trace to dependency issues.

**Prevention:**
- Use a consistent OpenHTMLtoPDF BOM or align all `openhtmltopdf-*` artifact versions:
```xml
<properties>
    <openhtmltopdf.version>1.0.10</openhtmltopdf.version>
</properties>

<dependencies>
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
</dependencies>
```
- Run `mvn dependency:tree` early and check for duplicate artifacts with different versions.
- Pin transitive dependency versions if conflicts appear.
- Verify the OpenHTMLtoPDF version is compatible with your Spring Boot version's managed dependencies.

**Detection:** `mvn dependency:tree | grep -i conflict` or runtime `NoSuchMethodError` during PDF generation.

**Phase:** Address in Phase 1 (project setup). Lock dependency versions before any feature work.

**Confidence:** MEDIUM -- dependency conflicts are common in Java ecosystems; specific conflicts depend on Spring Boot version.

---

### Pitfall 7: Thymeleaf Template Mode Mismatch

**What goes wrong:** Thymeleaf processes the template successfully, but the resulting HTML is not well-formed XML, causing OpenHTMLtoPDF to fail with parse errors.

**Why it happens:** OpenHTMLtoPDF requires well-formed XML (XHTML). Thymeleaf's default HTML mode produces HTML5 which may contain:
- Void elements without self-closing (`<br>`, `<img ...>`, `<hr>`)
- Unquoted attributes
- Boolean attributes without values (`<input disabled>`)

OpenHTMLtoPDF's parser chokes on these.

**Consequences:** `SAXParseException` or similar XML parsing errors at PDF generation time. Template renders fine in browser but fails for PDF.

**Prevention:**
- Write templates as valid XHTML: self-close void elements (`<br/>`, `<img ... />`), quote all attributes, use lowercase tag names.
- Alternatively, configure OpenHTMLtoPDF to use JSoup for HTML parsing (more lenient):
```java
builder.withHtmlContent(html, baseUri);
// JSoup-based parsing is the default in recent versions when using withHtmlContent
```
- If using `withW3cDocument()`, pre-process the HTML through JSoup to clean it:
```java
org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(html);
jsoupDoc.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);
String xhtml = jsoupDoc.html();
```
- Validate templates produce valid XHTML during development. A simple unit test that parses the output as XML catches this early.

**Detection:** Runtime exception during `builder.run()` mentioning XML/SAX parsing errors.

**Phase:** Address in Phase 1 (template setup conventions). Establish XHTML discipline from the first template.

**Confidence:** HIGH -- this is a fundamental requirement of OpenHTMLtoPDF's W3C DOM-based rendering.

---

### Pitfall 8: Character Encoding -- UTF-8 Not Declared Properly

**What goes wrong:** Special characters (umlauts, accents, symbols) render as garbled text or question marks in the PDF, even though the font supports them.

**Why it happens:** The HTML template lacks a proper `<meta charset="UTF-8"/>` declaration, or the Thymeleaf template file itself is saved in a non-UTF-8 encoding, or the Java string is read with the wrong charset.

**Consequences:** German text like "Pruefbescheinigung" with real umlauts displays incorrectly. This is especially problematic because the issue may not appear in browser preview (browsers are more forgiving about encoding).

**Prevention:**
- Always include in templates: `<meta charset="UTF-8"/>`
- Ensure all `.html` template files are saved as UTF-8 (check IDE settings).
- When reading template content programmatically, specify charset: `new String(bytes, StandardCharsets.UTF_8)`
- In `application.properties`:
```properties
spring.thymeleaf.encoding=UTF-8
server.servlet.encoding.charset=UTF-8
```
- Combine with proper font registration (Pitfall 2) -- encoding and font support are both required for correct character rendering.

**Detection:** Generate a PDF with known special characters (umlauts, euro sign, etc.) and verify in Phase 1.

**Phase:** Address in Phase 1 (project setup). Encoding must be correct from the start.

**Confidence:** HIGH -- encoding issues are universal in Java text processing.

---

## Minor Pitfalls

Mistakes that cause annoyance or minor rework but are straightforward to fix.

---

### Pitfall 9: CSS `@media print` Rules Ignored

**What goes wrong:** Developer adds `@media print { ... }` rules expecting them to apply to PDF output. They are ignored.

**Why it happens:** OpenHTMLtoPDF does not process `@media print` queries the same way browsers do. It renders all CSS as if it were the target medium. The `@media` support is limited.

**Prevention:** Do not rely on `@media print` to differentiate screen vs PDF styles. Instead, use a separate CSS file for PDF rendering, or use Thymeleaf conditionals to include different CSS.

**Detection:** Styles that work in browser print preview do not appear in PDF.

**Phase:** Phase 2 (template development). Minor impact, easy to work around.

**Confidence:** MEDIUM -- behavior may vary by OpenHTMLtoPDF version.

---

### Pitfall 10: CSS `position: fixed` Behaves Differently

**What goes wrong:** Elements styled with `position: fixed` do not stay in the same position on every page as they would in a browser print.

**Why it happens:** In OpenHTMLtoPDF, `position: fixed` places the element relative to the page box, but it appears only once (on the page where it occurs in the flow), not repeated on every page.

**Prevention:** For repeating headers/footers across pages, use CSS `@page` margin boxes with `running()` elements:
```css
@page {
    @top-center {
        content: element(runningHeader);
    }
}
.header {
    position: running(runningHeader);
}
```
For the 2-page template, this is likely unnecessary -- just place content explicitly on each page.

**Detection:** Fixed-position elements appear only on one page instead of all pages.

**Phase:** Phase 2 (template development). Only relevant if repeating headers/footers are needed.

**Confidence:** MEDIUM -- running elements are documented in OpenHTMLtoPDF but behavior details may vary.

---

### Pitfall 11: CSS Box Model Differences -- Borders and Padding

**What goes wrong:** Table borders, cell padding, or element borders render slightly differently than in browser. Double borders on tables, unexpected spacing.

**Why it happens:** OpenHTMLtoPDF's CSS 2.1 box model interpretation may differ from modern browser quirks mode. `border-collapse`, `border-spacing`, and box-sizing behave strictly per CSS 2.1 spec.

**Prevention:**
- Always specify `border-collapse: collapse` on tables explicitly.
- Use `box-sizing: border-box` if supported (verify with your OpenHTMLtoPDF version).
- Avoid fractional pixel values; use whole numbers or `mm`/`pt` units for print.
- Test with visible borders during development to catch spacing issues early.

**Detection:** Visual inspection of PDF output shows unexpected gaps or double borders.

**Phase:** Phase 2 (template development).

**Confidence:** MEDIUM.

---

### Pitfall 12: Test-Generated PDFs Accumulate and Are Not Cleaned Up

**What goes wrong:** JUnit tests generate PDF files to a target or temp directory. Over time, old PDFs accumulate, CI disk fills up, or developers accidentally commit generated PDFs.

**Why it happens:** Tests write PDFs for visual verification but there is no cleanup strategy.

**Prevention:**
- Write PDFs to `target/test-output/` so `mvn clean` removes them.
- Add `target/` to `.gitignore` (standard Maven practice).
- In tests, use `@TempDir` (JUnit 5) for PDFs that do not need manual inspection:
```java
@Test
void generatePdf(@TempDir Path tempDir) throws Exception {
    Path output = tempDir.resolve("test.pdf");
    // generate PDF to output
    assertTrue(Files.exists(output));
    assertTrue(Files.size(output) > 0);
}
```
- For PDFs that DO need manual inspection, write to a known location under `target/` and document it.

**Detection:** Large files in unexpected locations; CI build disk warnings.

**Phase:** Phase 1 (test setup).

**Confidence:** HIGH -- standard Java testing practice.

---

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| Phase 1: Project Setup | Dependency conflicts (Pitfall 6) | Run `mvn dependency:tree` immediately after adding OpenHTMLtoPDF deps |
| Phase 1: Project Setup | Font registration missing (Pitfall 2) | Register at least one Unicode font before any template work |
| Phase 1: Project Setup | Resource paths broken in tests (Pitfall 3) | Write a "hello world" PDF test that includes an image |
| Phase 1: Project Setup | Encoding misconfigured (Pitfall 8) | Include umlaut test string in first PDF test |
| Phase 1: Project Setup | XHTML compliance (Pitfall 7) | Establish template conventions document |
| Phase 2: Template Dev | CSS layout using flex/grid (Pitfall 1) | CSS constraints doc from Phase 1 prevents this |
| Phase 2: Template Dev | Page breaks wrong (Pitfall 4) | Use explicit page break divs, test 2-page template immediately |
| Phase 2: Template Dev | QR code blurry/missing (Pitfall 5) | Generate QR at 300+ pixels, use base64 inline, test scannability |
| Phase 2: Template Dev | Position fixed confusion (Pitfall 10) | Use running elements for repeated content |

## Summary of CSS Support in OpenHTMLtoPDF

For quick reference during template development:

| CSS Feature | Supported? | Notes |
|-------------|-----------|-------|
| `display: block/inline/table/table-cell/inline-block` | YES | Core layout tools |
| `display: flex` | NO | Use table or float layout instead |
| `display: grid` | NO | Use table layout instead |
| `float: left/right` | YES | Primary layout mechanism |
| `position: absolute/relative` | YES | Works within containing block |
| `position: fixed` | PARTIAL | Only on current page, not repeated |
| `@page` with size/margin | YES | Essential for print layout |
| `page-break-before/after` | YES | Use `always` for explicit breaks |
| `page-break-inside: avoid` | PARTIAL | May be ignored for large elements |
| `@font-face` | YES | Requires font file to be resolvable |
| `background-image` | YES | Path resolution applies (Pitfall 3) |
| `border-radius` | PARTIAL | Basic support in recent versions |
| `box-shadow` | NO | Not supported |
| `opacity` | PARTIAL | Basic support |
| `transform` | NO | Not supported |
| `@media print` | LIMITED | Do not rely on media queries |
| `calc()` | NO | Not supported |
| `var()` / CSS custom properties | NO | Not supported |
| `overflow: hidden` | YES | Works for clipping |
| `word-break` / `overflow-wrap` | PARTIAL | Basic support |

## Sources

- OpenHTMLtoPDF GitHub repository and wiki (https://github.com/danfickle/openhtmltopdf)
- Training data knowledge of OpenHTMLtoPDF up to v1.0.10 -- MEDIUM confidence, verify current version
- General Java/Spring Boot PDF generation patterns -- HIGH confidence for structural advice
- Note: Web research tools were unavailable during this research session. All findings should be validated against current OpenHTMLtoPDF documentation before implementation.
