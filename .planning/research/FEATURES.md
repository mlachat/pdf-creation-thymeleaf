# Feature Landscape

**Domain:** HTML-to-PDF template rendering (demo/proof-of-concept)
**Stack:** Thymeleaf + OpenHTMLtoPDF on Spring Boot
**Researched:** 2026-02-18
**Research basis:** Training data (no web verification available). Confidence adjustments noted per feature.

---

## Table Stakes

Features the demo MUST have to be convincing. Without these, the proof-of-concept fails to prove its point.

| Feature | Why Expected | Complexity | Confidence | Notes |
|---------|--------------|------------|------------|-------|
| **Text placeholders** (name, address, date) | Core value prop -- show dynamic data injection | Low | HIGH | Thymeleaf `th:text` / `th:utext`. Simplest placeholder type. |
| **QR code generation + embedding** | Explicitly required in Template A | Medium | HIGH | Generate with ZXing, embed as base64 `<img>` in Thymeleaf. OpenHTMLtoPDF renders base64 data URIs. |
| **Page size and margins** (`@page` CSS) | Print PDFs need controlled geometry | Low | HIGH | OpenHTMLtoPDF supports `@page { size: A4; margin: 2cm; }`. Core CSS Paged Media feature. |
| **Page breaks** (`page-break-before/after`) | Template A is 2 pages -- must break cleanly | Low | HIGH | OpenHTMLtoPDF supports `page-break-before: always` and `page-break-after: always`. Also `break-before`/`break-after`. |
| **Multi-column layout** | Template B requires 2-column layout | Medium | MEDIUM | OpenHTMLtoPDF supports CSS `column-count`. Verify actual rendering -- multi-column in paged media can be tricky. |
| **Font embedding** (custom fonts) | Word templates use specific fonts; PDF must match | Medium | HIGH | OpenHTMLtoPDF supports `@font-face` with TTF/OTF files. Must register fonts programmatically or via CSS. Required for visual fidelity. |
| **Base64 image embedding** | QR codes and logos need inline embedding (no external server) | Low | HIGH | `<img src="data:image/png;base64,...">` works in OpenHTMLtoPDF. Essential for test-only project with no running server. |
| **CSS print stylesheet** (`@media print`) | Separate screen/print concerns if templates are also viewed in browser | Low | HIGH | Standard CSS. OpenHTMLtoPDF uses print media by default. |
| **Thymeleaf template resolution** | Load HTML templates from classpath | Low | HIGH | Spring Boot auto-configures Thymeleaf resolver. Templates in `src/main/resources/templates/`. |
| **JUnit test producing PDF output** | The demo deliverable IS the test output | Low | HIGH | Write PDF bytes to `target/` or a test output directory. Assert file exists and is valid PDF. |

## Differentiators

Features that make the demo impressive but are not strictly required. Pick 1-2 for "wow factor."

| Feature | Value Proposition | Complexity | Confidence | Notes |
|---------|-------------------|------------|------------|-------|
| **Running headers/footers** (page numbers, document title) | Shows print-grade sophistication; CSS `@page` margin boxes | Medium | MEDIUM | OpenHTMLtoPDF supports CSS Paged Media margin boxes (`@top-center`, `@bottom-right`, etc.) with `content: counter(page)`. Verify exact syntax -- this is a less commonly tested feature. |
| **Dynamic table rendering** (repeating rows from data list) | Shows Thymeleaf iteration (`th:each`) producing tabular PDF output | Medium | HIGH | Thymeleaf `th:each` on `<tr>` elements. Straightforward but visually impressive. Table splitting across pages needs `page-break-inside: avoid` on rows. |
| **Conditional content blocks** | Show/hide template sections based on data (e.g., optional address line 2) | Low | HIGH | Thymeleaf `th:if` / `th:unless`. Trivial to implement, demonstrates template flexibility. |
| **SVG rendering in PDF** | Vector graphics (logos, diagrams) render crisply at any zoom | Medium | MEDIUM | OpenHTMLtoPDF has SVG support via `openhtmltopdf-svg-support` module. Adds dependency but produces sharp output. |
| **Barcode types beyond QR** (Code128, EAN) | Shows extensibility of the approach | Low | HIGH | ZXing supports many barcode formats. Same embedding pattern as QR. |
| **Template inheritance / fragments** | Shared header/footer across templates via Thymeleaf fragments | Medium | HIGH | Thymeleaf `th:insert` / `th:replace` with fragment expressions. Good practice for multi-template projects. |
| **Watermark or background image** | "DRAFT" watermark, company letterhead background | Medium | MEDIUM | CSS `background-image` on `@page` or fixed-position div. OpenHTMLtoPDF support may vary -- verify. |
| **PDF metadata** (title, author, keywords) | Professional touch on generated PDFs | Low | HIGH | OpenHTMLtoPDF `PdfRendererBuilder` has `.withProducer()` and PDF metadata can be set via `<meta>` tags or builder API. |
| **PDF/A compliance** | Archival-grade PDFs for regulated industries | High | MEDIUM | OpenHTMLtoPDF has some PDF/A support. Requires all fonts embedded, color profiles. Impressive but complex for a demo. |

## Anti-Features

Features to deliberately NOT build in this demo. Building these would waste time, add complexity, or mislead about the project's purpose.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| **Runtime .docx-to-HTML conversion** | Out of scope per PROJECT.md. Mammoth is an external preprocessing step. Mixing it in blurs the demo boundary. | Include sample .docx files as artifacts. Document the Mammoth CLI command in README. |
| **REST API for PDF generation** | No running application per requirements. A REST layer adds boot time, error handling, HTTP concerns -- all noise for a demo. | JUnit tests ARE the API. They show input/output clearly. |
| **Template editor / WYSIWYG** | Massive scope creep. Not the point of this demo. | Provide well-commented HTML templates that are self-explanatory. |
| **Caching or performance optimization** | Demo generates a handful of PDFs. No performance concern. Premature optimization obscures the code. | Keep rendering synchronous and simple. One template, one render call, one PDF. |
| **Database-driven template storage** | Stateless per requirements. Templates live on classpath. | `src/main/resources/templates/` with Thymeleaf resolver. |
| **Multi-language / i18n support** | Adds message bundles, locale handling -- unnecessary complexity for a demo. | Hardcode demo data in one language. |
| **HTML email rendering** | Different rendering engine, different constraints. Conflating email and PDF muddies the demo. | Stay focused on PDF output only. |
| **Async or batch PDF generation** | Implies production workload patterns. Misleading for a demo. | Synchronous, one-at-a-time in tests. |
| **Digital signatures on PDF** | Complex PKI setup, not related to template rendering. | Mention as a possible extension in docs if needed. |
| **Complex JavaScript in templates** | OpenHTMLtoPDF does NOT execute JavaScript. Attempting it will fail silently or error. | Use Thymeleaf server-side logic for all dynamic content. No JS in templates. |

## Feature Dependencies

```
Font files (TTF/OTF)
  └── Font embedding (@font-face CSS)
       └── Visual fidelity of rendered PDF

ZXing library
  └── QR code generation (byte array)
       └── Base64 encoding
            └── QR code in template (<img src="data:...">)

Thymeleaf template resolution
  ├── Text placeholders (th:text)
  ├── Conditional blocks (th:if)
  ├── Table iteration (th:each)
  └── Template fragments (th:insert)

OpenHTMLtoPDF core
  ├── @page CSS (size, margins)
  ├── Page breaks
  ├── Font embedding (programmatic registration)
  ├── Base64 image rendering
  └── Multi-column (column-count)

OpenHTMLtoPDF SVG module (optional)
  └── SVG rendering

CSS Paged Media (@page margin boxes)
  └── Running headers/footers + page numbers
       (depends on OpenHTMLtoPDF core)
```

### Critical Path for Template A (2-page letter with QR)

1. Thymeleaf template resolution (foundation)
2. Text placeholders -- name, address (core value)
3. QR code generation + base64 embedding (key feature)
4. Font embedding (visual fidelity)
5. Page breaks between pages (structural)
6. Page margins via `@page` (layout)

### Critical Path for Template B (1-page 2-column)

1. Thymeleaf template resolution (foundation)
2. Text placeholders (core value)
3. Multi-column CSS layout (key feature)
4. Font embedding (visual fidelity)
5. Page margins via `@page` (layout)

## MVP Recommendation

**For a convincing demo, implement ALL table stakes features.** They are individually low-to-medium complexity and collectively prove the concept.

**Pick exactly 2 differentiators** to add polish:

1. **Running headers/footers with page numbers** -- Shows print-grade output. Customers care about this. Medium effort but high perceived value.
2. **Conditional content blocks** (`th:if`) -- Near-zero effort, demonstrates template flexibility.

**Defer everything else to post-demo** if the project evolves:
- Dynamic tables: Valuable but not needed for the two specified templates
- SVG support: Only if a template actually needs vector graphics
- PDF/A: Production concern, not demo concern
- Template fragments: Only valuable with 3+ templates

## Complexity Budget

| Category | Feature Count | Estimated Effort | Notes |
|----------|--------------|-----------------|-------|
| Table stakes | 10 | 2-3 days | Most are configuration, not code |
| Recommended differentiators | 2 | 0.5 days | Conditional blocks are trivial; headers/footers need CSS experimentation |
| Total MVP | 12 | ~3 days | Conservative estimate including font/CSS debugging |

## Confidence Notes

- **HIGH confidence items:** Thymeleaf features, basic OpenHTMLtoPDF rendering, base64 images, ZXing QR generation. These are well-established, widely documented patterns.
- **MEDIUM confidence items:** Multi-column layout in paged media, running headers/footers via CSS margin boxes, watermarks. These work in OpenHTMLtoPDF but exact CSS syntax and edge cases should be verified against current OpenHTMLtoPDF documentation or tested early.
- **Key uncertainty:** OpenHTMLtoPDF's CSS Paged Media support depth. It implements a subset of the W3C spec. Features like `@page` margin boxes and `column-count` are supported but may have quirks. Recommend: build a minimal spike for multi-column and headers/footers early to validate.

## Sources

- OpenHTMLtoPDF GitHub repository (github.com/danfickle/openhtmltopdf) -- training data, not live-verified
- Thymeleaf documentation (thymeleaf.org) -- training data, not live-verified
- ZXing barcode library (github.com/zxing/zxing) -- training data, not live-verified
- CSS Paged Media Module Level 3 (W3C spec) -- training data basis for CSS feature claims
- **Note:** Web search and fetch tools were unavailable during this research. All claims are based on training data (cutoff ~May 2025). Recommend verifying OpenHTMLtoPDF CSS support claims against current docs before implementation.
