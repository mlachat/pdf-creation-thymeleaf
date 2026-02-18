# print — Template-Based PDF Generation

A Java library that replaces JasperReports with a simpler pipeline:

**Word template (.docx) → HTML + Thymeleaf → PDF**

The pipeline uses three components: Thymeleaf for template rendering, OpenHTMLtoPDF for PDF generation, and ZXing for QR codes. No Spring context required — all classes are plain POJOs.

## Dependencies

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.2</version>
</parent>

<properties>
    <java.version>21</java.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    <dependency>
        <groupId>io.github.openhtmltopdf</groupId>
        <artifactId>openhtmltopdf-pdfbox</artifactId>
        <version>1.1.37</version>
    </dependency>
    <dependency>
        <groupId>com.google.zxing</groupId>
        <artifactId>core</artifactId>
        <version>3.5.4</version>
    </dependency>
    <dependency>
        <groupId>com.google.zxing</groupId>
        <artifactId>javase</artifactId>
        <version>3.5.4</version>
    </dependency>
</dependencies>
```

## Quick Start

```java
// 1. Generate a QR code (optional)
String qrDataUri = QrCodeGenerator.generateDataUri("https://example.com/doc/123", 300);

// 2. Build your template model
Map<String, Object> model = new HashMap<>();
model.put("recipientName", "Hans Müller");
model.put("date", "18. Februar 2026");
model.put("qrCodeDataUri", qrDataUri);
model.put("showNotice", true);
model.put("noticeText", "Bitte beachten Sie die Änderungen.");

// 3. Render HTML via Thymeleaf
ThymeleafRenderer renderer = new ThymeleafRenderer();
String html = renderer.render("template-a", model);

// 4. Generate PDF
String baseUri = getClass().getClassLoader().getResource("").toExternalForm();
PdfGenerator pdfGenerator = new PdfGenerator();
byte[] pdf = pdfGenerator.generatePdf(html, baseUri);

// 5. Write to file or return as response
Files.write(Paths.get("output.pdf"), pdf);
```

## The Three Components

### `ThymeleafRenderer`

Renders a Thymeleaf template with a variable map into an HTML string.

```java
ThymeleafRenderer renderer = new ThymeleafRenderer();
String html = renderer.render("template-name", model);
```

- Resolves templates from `classpath:templates/{name}.html`
- No Spring application context needed
- Returns fully rendered HTML (no Thymeleaf attributes remain)

### `PdfGenerator`

Converts an HTML string to a PDF byte array.

```java
PdfGenerator generator = new PdfGenerator();
byte[] pdf = generator.generatePdf(html, baseUri);
```

- `html` — rendered HTML string (must be well-formed XHTML)
- `baseUri` — base URI for resolving relative CSS/image paths. Use `getClass().getClassLoader().getResource("").toExternalForm()` to point to your classpath root
- Bundles DejaVuSans.ttf for full Unicode support (umlauts, special characters)

### `QrCodeGenerator`

Generates a QR code as a base64 data URI, ready for `<img src="...">`.

```java
String dataUri = QrCodeGenerator.generateDataUri("https://example.com", 300);
// Returns: "data:image/png;base64,iVBOR..."
```

- `size` — pixel dimension (use 300+ for print quality)
- Output works directly in HTML `<img>` tags, no file I/O needed

## Converting Word Templates to HTML

Before you can use a Word template in this pipeline, you need to convert it to clean HTML once. The recommended online tool is **[WordHTML.com](https://wordhtml.com/)**.

### Why WordHTML.com

- Free, no signup required
- Drag-and-drop `.docx` upload
- View and edit the generated HTML source with syntax highlighting (CodeMirror)
- Cleanup buttons to strip Word cruft (inline styles, empty tags, classes, comments)
- Download result as `.html`
- Images embedded as base64 data URIs (works directly with OpenHTMLtoPDF)

### Conversion Workflow

1. **Upload** — drag your `.docx` file onto [wordhtml.com](https://wordhtml.com/)
2. **Clean** — click the cleanup buttons to remove inline styles, empty tags, and classes
3. **Switch to HTML tab** — review the source code
4. **Save** — download as `.html` or copy the source
5. **Add Thymeleaf** — replace static text with `th:text="${variable}"` attributes
6. **Extract CSS** — move inline styles to a separate `.css` file with `@page` rules
7. **Fix XHTML** — self-close void elements (`<br/>`, `<img/>`, `<meta/>`)

### Alternative: Mammoth.js Demo

For the cleanest semantic output (headings, lists, tables — no styling), use the [Mammoth.js browser demo](https://jstool.gitlab.io/demo/mammoth-js-word-docx-preview-and-convert/). It runs entirely client-side (no upload to any server) and produces minimal HTML by mapping Word styles to semantic elements (`Heading 1` → `<h1>`, bold → `<strong>`). The tradeoff is that all visual styling is stripped — you write the CSS from scratch.

## How to Migrate a Jasper Template

### Step 1: Create the HTML Template

Convert your `.jrxml` layout to an XHTML file with Thymeleaf placeholders:

```html
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8"/>
    <link rel="stylesheet" href="css/your-template.css"/>
</head>
<body>
    <h1 th:text="${title}">Default Title</h1>
    <p th:text="${bodyText}">Default body text</p>

    <!-- Conditional blocks -->
    <div th:if="${showWarning}" class="warning">
        <p th:text="${warningText}">Warning text</p>
    </div>

    <!-- QR code -->
    <img th:src="${qrCodeDataUri}" alt="QR" style="width: 150px; height: 150px;"/>

    <!-- Page break for multi-page documents -->
    <div style="page-break-before: always;">
        <h2>Page 2 Content</h2>
    </div>
</body>
</html>
```

**Template rules:**
- Must be valid XHTML (self-close void elements: `<br/>`, `<img ... />`, `<meta ... />`)
- Use `th:text` for dynamic text, `th:if` for conditional blocks, `th:src` for dynamic images
- Place in `src/main/resources/templates/` (or `src/test/resources/templates/` for tests)
- Default text between tags serves as preview content

### Step 2: Create the Print CSS

Create a matching CSS file for print layout:

```css
@page {
    size: A4;
    margin: 2.5cm 2cm;

    @top-center {
        content: "Company Name";
        font-family: 'DejaVuSans', sans-serif;
        font-size: 8pt;
        color: #666;
    }

    @bottom-center {
        content: "Seite " counter(page) " von " counter(pages);
        font-family: 'DejaVuSans', sans-serif;
        font-size: 8pt;
    }
}

body {
    font-family: 'DejaVuSans', sans-serif;
    font-size: 11pt;
}
```

Place CSS files in `src/main/resources/css/` (same classpath root as templates).

### Step 3: Map Jasper Parameters to Template Variables

| Jasper Concept | Replacement |
|---|---|
| `$P{paramName}` | `th:text="${paramName}"` |
| `$F{fieldName}` | `th:text="${fieldName}"` |
| `<band>` sections | HTML `<div>` elements |
| Subreports | Include via `th:replace` or inline |
| Conditional printing (`printWhenExpression`) | `th:if="${condition}"` |
| Page header/footer | `@page { @top-center { ... } }` |
| Page break | `page-break-before: always` |
| Static text | Plain HTML text |
| Images | `<img>` with `th:src` or inline `src` |
| Barcodes/QR | `QrCodeGenerator.generateDataUri(...)` |

### Step 4: Wire It Up

Replace your JasperReports fill+export code:

```java
// BEFORE (JasperReports)
JasperReport report = JasperCompileManager.compileReport("template.jrxml");
JasperPrint print = JasperFillManager.fillReport(report, parameters, dataSource);
byte[] pdf = JasperExportManager.exportReportToPdf(print);

// AFTER (this library)
ThymeleafRenderer renderer = new ThymeleafRenderer();
PdfGenerator pdfGenerator = new PdfGenerator();

Map<String, Object> model = new HashMap<>();
model.put("title", "Document Title");
model.put("bodyText", "Content here");

String html = renderer.render("your-template", model);
String baseUri = getClass().getClassLoader().getResource("").toExternalForm();
byte[] pdf = pdfGenerator.generatePdf(html, baseUri);
```

## CSS Constraints

OpenHTMLtoPDF implements **CSS 2.1**, not a full browser engine.

**Supported:** box model, `display: block/inline/table/table-cell`, `float`, `position: static/relative/absolute`, `@page` rules, `page-break-*`, `border-radius`, `background-color`, base64 data URIs, `@font-face`

**Not supported:** Flexbox, Grid, `calc()`, `var()`, `box-shadow`, `text-shadow`, media queries, viewport units, JavaScript

**Practical consequence:** Use `<table>` for multi-column layouts instead of Flexbox/Grid. See [CSS-CONSTRAINTS.md](CSS-CONSTRAINTS.md) for the full reference.

## Project Structure

```
src/main/java/com/example/print/
    pdf/PdfGenerator.java           # HTML → PDF
    qr/QrCodeGenerator.java         # Text → QR data URI
    template/ThymeleafRenderer.java # Template + model → HTML

src/main/resources/
    fonts/DejaVuSans.ttf            # Bundled Unicode font

src/test/resources/
    templates/template-a.html       # Example: 2-page business letter
    templates/template-b.html       # Example: 1-page product sheet
    css/template-a.css              # Print CSS for template A
    css/template-b.css              # Print CSS for template B
```

## Included Example Templates

### Template A — Business Letter (2 pages)

Variables: `recipientName`, `recipientStreet`, `recipientCity`, `date`, `subject`, `bodyText`, `showNotice`, `noticeText`, `qrCodeDataUri`, `senderName`, `termsText`, `showDisclaimer`, `disclaimerText`, `contactInfo`

### Template B — Product Information Sheet (1 page, 2 columns)

Variables: `title`, `subtitle`, `productName`, `productDescription`, `price`, `availability`, `contactPerson`, `phone`, `email`, `companyAddress`, `showSpecialOffer`, `specialOfferText`, `footerText`

Run `mvn test` to generate example PDFs in `target/test-output/`.
