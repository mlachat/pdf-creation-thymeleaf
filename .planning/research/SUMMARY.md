# Project Research Summary

**Project:** Print (HTML-to-PDF Demo)
**Domain:** Java HTML-to-PDF template rendering — Spring Boot, Thymeleaf, OpenHTMLtoPDF
**Researched:** 2026-02-18
**Confidence:** MEDIUM — library choices are HIGH confidence; exact API details and version numbers need verification against current Maven Central and OpenHTMLtoPDF docs before implementation.

## Executive Summary

This is a test-only demo project proving that Word-derived HTML templates can be filled with dynamic data and rendered as print-quality PDFs in a pure Java stack. The deliverable is a pair of JUnit tests — not a running application — that produce two concrete PDFs: a 2-page letter with QR code (Template A) and a 1-page 2-column layout (Template B). The architecture is a simple 4-step pipeline: Thymeleaf fills placeholders, ZXing generates QR codes as base64 data URIs, OpenHTMLtoPDF converts rendered HTML to PDF, and tests write output to `target/test-output/`. No web layer, no REST API, no Spring context beyond Thymeleaf auto-config.

The recommended stack is unambiguous: OpenHTMLtoPDF (the only maintained Java HTML+CSS-to-PDF library with real CSS support), Thymeleaf (Spring Boot's default template engine, operates standalone without a servlet context), and ZXing (de facto standard for QR generation in Java). All three libraries compose without a bridge layer — Thymeleaf produces a String, OpenHTMLtoPDF consumes a String. Maven 3.9 with Spring Boot 3.4.x parent POM handles dependency management for everything except OpenHTMLtoPDF and ZXing, which need explicit version pins.

The critical risk in this project is not architectural complexity — the pipeline is simple. The risk is OpenHTMLtoPDF's CSS limitations. It does not support Flexbox or CSS Grid. Templates must use CSS 2.1 layout (floats, tables, inline-block). Discovering this late — after templates are prototyped in a browser using modern CSS — requires a full CSS rewrite. The mitigation is establishing a CSS constraints document in Phase 1, before any template work begins, and testing PDF output immediately rather than relying on browser preview.

## Key Findings

### Recommended Stack

Spring Boot 3.4.x provides the project scaffold and Thymeleaf auto-config. Java 21 LTS is the runtime (required by Spring Boot 3.x). Maven 3.9 is the build tool per customer requirements. The stack is lean: no web starter, no persistence layer, no application server. The POM uses Spring Boot's parent for dependency management plus two explicit version pins for OpenHTMLtoPDF and ZXing.

**Core technologies:**
- **Spring Boot 3.4.x + Java 21**: Project scaffold, DI, Thymeleaf integration — Spring Boot parent POM handles transitive dependency management and provides test infrastructure
- **OpenHTMLtoPDF 1.1.22** (openhtmltopdf-pdfbox): HTML+CSS to PDF — the only maintained Java library with real CSS Paged Media support; built on Apache PDFBox (Apache 2.0 license, no AGPL concerns)
- **Thymeleaf 3.1.x** (via spring-boot-starter-thymeleaf): Template processing — operates standalone via `SpringTemplateEngine` + `ClassLoaderTemplateResolver`; no web context required
- **ZXing 3.5.3** (core + javase): QR code generation — de facto standard, lightweight, no native deps; output embedded as base64 PNG data URI in HTML
- **Mammoth** (CLI, not a Maven dependency): One-time preprocessing of .docx to HTML; run externally, not at runtime

**Key Maven decisions:** Use `spring-boot-starter-thymeleaf` not `spring-boot-starter-web`. No `spring-boot-maven-plugin` since there is no runnable application. OpenHTMLtoPDF and ZXing are not managed by the Spring Boot BOM and require explicit `<version>` pins. All versions listed need verification against current Maven Central — research used training data with a May 2025 cutoff.

### Expected Features

**Must have (table stakes) — all required for a convincing demo:**
- Text placeholders (name, address, date) via Thymeleaf `th:text`
- QR code generation and embedding (ZXing + base64 data URI)
- Page size and margins via CSS `@page { size: A4; margin: ... }`
- Page breaks via `page-break-before: always` (Template A is 2 pages)
- 2-column layout via CSS `column-count: 2` (Template B)
- Font embedding via `builder.useFont()` with TTF files (visual fidelity)
- Base64 inline image embedding (self-contained, no external server)
- JUnit tests writing PDFs to `target/test-output/`

**Should have (differentiators — pick 2):**
- Running headers/footers with page numbers via CSS `@page` margin boxes — high perceived value for print-grade output
- Conditional content blocks via Thymeleaf `th:if` — near-zero effort, demonstrates template flexibility

**Defer to post-demo:**
- Dynamic table rendering (`th:each`) — valuable but not needed for the two specified templates
- SVG support — only if a template actually needs vector graphics
- Template fragments (`th:insert`) — only valuable with 3+ templates
- PDF/A compliance — production concern, not demo concern
- Dynamic table rendering — not needed for the two defined templates

**Anti-features (do not build):** REST API, runtime .docx conversion, async/batch generation, database-driven templates, JavaScript in templates (OpenHTMLtoPDF does not execute JS).

### Architecture Approach

The architecture is a linear pipeline invoked exclusively from JUnit tests. There is no web layer, no controllers, and no persistent state. Each component is a focused POJO with a single responsibility; Thymeleaf and OpenHTMLtoPDF connect via a plain String (rendered HTML). Spring Boot is used only for Thymeleaf auto-config and test infrastructure.

**Major components:**
1. **QrCodeGenerator** — pure utility; ZXing BitMatrix to PNG to base64 data URI; no dependencies on other components
2. **ThymeleafRenderer** — wraps `SpringTemplateEngine` with `ClassLoaderTemplateResolver`; takes template name + model Map, returns rendered HTML String
3. **PdfGenerator** — wraps `PdfRendererBuilder`; takes rendered HTML String + base URI + font registrations, returns `byte[]`
4. **TemplateDataFactory** — assembles `Map<String, Object>` model (calls QrCodeGenerator); one factory method per template
5. **JUnit Tests** — orchestrate the full pipeline; write PDFs to `target/test-output/`; entry point for the whole system

Resource resolution: OpenHTMLtoPDF resolves relative CSS/image paths against a base URI. Use `getClass().getClassLoader().getResource("templates/").toExternalForm()` to get a `file://` URI pointing to the compiled test-classes directory. QR codes use data URIs and bypass this entirely. Fonts are registered via `builder.useFont()`, not resolved by URL.

### Critical Pitfalls

1. **No Flexbox or Grid** — OpenHTMLtoPDF is CSS 2.1 only. Templates using `display: flex` or `display: grid` produce completely broken PDF layout. Prevention: document the CSS constraints list before any template work; test in PDF immediately, never trust browser-only preview. (ARCHITECTURE.md flags this too; both sources agree, HIGH confidence.)

2. **Font registration is mandatory** — Referencing a font in CSS `font-family` is not enough. Every font file must be registered with `builder.useFont(supplier, "Family Name")`. Missing registration causes invisible text or substituted fonts. Umlauts and special characters disappear entirely. Prevention: register at least one Unicode-complete font (DejaVuSans) in Phase 1 before template work; test with German umlauts in the first PDF.

3. **Resource path resolution breaks in tests** — OpenHTMLtoPDF resolves CSS/image paths relative to a base URI. If the base URI is wrong, PDFs silently render without images or styles. Prevention: use `getClassLoader().getResource("templates/").toExternalForm()` as base URI; write a "hello world" PDF test with an image in Phase 1 to validate resolution.

4. **XHTML compliance required** — OpenHTMLtoPDF requires well-formed XML. Thymeleaf's default HTML5 mode produces self-closing `<br>` and void elements without `/>`; these cause SAXParseException at PDF generation time. Prevention: write all templates as XHTML from day one; self-close all void elements (`<br/>`, `<img/>`); establish template conventions in Phase 1.

5. **Page breaks need explicit structure** — `page-break-inside: avoid` may be silently ignored for large blocks. Template A's 2-page structure should use explicit `page-break-before: always` on the second page's container div, not rely on content flow. Prevention: structure HTML with page intent explicit, test 2-page output immediately.

## Implications for Roadmap

Based on research, the natural build order is bottom-up dependency resolution: standalone utilities first, Thymeleaf layer second, PDF pipeline third, then integration. All Phase 1 setup pitfalls must be resolved before template work begins; discovering CSS constraints late is the highest-impact risk.

### Phase 1: Foundation and Guardrails

**Rationale:** Multiple critical pitfalls (CSS limitations, font registration, resource path resolution, XHTML compliance, encoding) must be established as project conventions before any template work. Discovering these in Phase 2 or 3 requires rework of all templates. The Maven POM, font registration, base URI strategy, and CSS constraints document must be locked first.

**Delivers:** Working Maven project; `PdfGenerator` that renders a minimal hardcoded HTML page to a valid PDF with a registered font, an image, and a German umlaut; `QrCodeGenerator` with unit test; CSS constraints reference doc; XHTML template conventions established.

**Addresses pitfalls:** CSS layout constraints (Pitfall 1), font embedding (Pitfall 2), resource paths (Pitfall 3), XHTML compliance (Pitfall 7), UTF-8 encoding (Pitfall 8), dependency conflicts (Pitfall 6), test output cleanup (Pitfall 12).

**Stack elements used:** Spring Boot parent POM, OpenHTMLtoPDF + PDFBox, ZXing, spring-boot-starter-test, DejaVuSans TTF.

### Phase 2: Thymeleaf Template Rendering

**Rationale:** Once PDF generation is proven with static HTML, add the template layer. Thymeleaf is a well-understood component; the main risk here is getting the standalone configuration right (no web context) and establishing the rendering pipeline.

**Delivers:** `ThymeleafRenderer` with `ClassLoaderTemplateResolver`; unit test verifying template renders expected HTML output; a simple template with text placeholders confirmed to produce valid PDF via the Phase 1 `PdfGenerator`.

**Addresses pitfalls:** Standalone Thymeleaf config (no servlet context), template resolver classpath configuration.

**Stack elements used:** spring-boot-starter-thymeleaf, SpringTemplateEngine, ClassLoaderTemplateResolver.

### Phase 3: Template A (2-page letter with QR)

**Rationale:** Template A exercises the most complex features: multi-page layout, page breaks, QR code embedding, and font-accurate text rendering. Building it before Template B allows early validation of the full pipeline under real conditions.

**Delivers:** `template-a.html` with CSS; `TemplateDataFactory` for Template A; end-to-end `TemplateATest` producing a scannable 2-page PDF in `target/test-output/`; QR code scannability verified manually.

**Implements:** Full pipeline (QrCodeGenerator -> TemplateDataFactory -> ThymeleafRenderer -> PdfGenerator -> file output).

**Avoids:** Page break pitfall (Pitfall 4 — use explicit page-break-before div); QR blurriness (Pitfall 5 — generate at 300+ px, use PNG data URI).

### Phase 4: Template B (1-page 2-column layout)

**Rationale:** Template B is simpler than Template A (no QR, no page breaks) but exercises CSS multi-column layout, which has MEDIUM confidence and needs empirical validation. Building it second means the pipeline is already proven; the only new risk is `column-count` rendering.

**Delivers:** `template-b.html` with 2-column CSS layout; end-to-end `TemplateBTest`; visual verification that column layout renders correctly.

**Avoids:** CSS flex/grid temptation (Pitfall 1 — use `column-count` or table layout as fallback if `column-count` fails).

### Phase 5: Polish and Documentation

**Rationale:** Running headers/footers (the recommended differentiator) and conditional blocks are added after both templates are proven. These are additive, low-risk changes. Documentation of the Mammoth preprocessing workflow and the project's architectural decisions completes the demo.

**Delivers:** Running page headers/footers with page numbers (CSS `@page` margin boxes); conditional `th:if` blocks; `docx/` source files; README documenting Mammoth CLI usage and PDF generation pipeline.

**Avoids:** `position: fixed` confusion (Pitfall 10 — use running elements pattern instead).

### Phase Ordering Rationale

- Foundation before templates because CSS constraints and font registration, if discovered late, require rewriting all template CSS.
- QrCodeGenerator in Phase 1 because it is a standalone utility with zero dependencies on other components; early validation is free.
- Template A before Template B because it exercises more of the stack (QR, multi-page, page breaks) and gives earlier confidence in the full pipeline.
- Polish in final phase because running headers/footers involve CSS Paged Media features with MEDIUM confidence that need experimentation; isolating that risk to the end protects Template A and B delivery.

### Research Flags

Phases likely needing a spike or early validation before committing to implementation:

- **Phase 1 (OpenHTMLtoPDF API):** Verify `useFastMode()` still exists in current API (LOW confidence per ARCHITECTURE.md). Verify exact `useFont()` method signature. Check latest version on Maven Central.
- **Phase 3 (QR in PDF):** QR resolution and scannability requires empirical testing — documented as working but exact size thresholds are not specified in research.
- **Phase 4 (CSS multi-column):** `column-count` in paged media is MEDIUM confidence. If it does not render correctly, fallback is `display: table` with `table-cell` for 2-column layout.
- **Phase 5 (running headers/footers):** CSS `@page` margin boxes with `running()` elements are MEDIUM confidence. This is the highest-uncertainty feature in the demo. Test early in Phase 5.

Phases with well-documented, standard patterns (skip deep research):
- **Phase 2 (Thymeleaf standalone):** `SpringTemplateEngine` + `ClassLoaderTemplateResolver` is a stable, well-documented pattern. HIGH confidence.
- **Phase 3 (Thymeleaf th:text, th:if):** Core Thymeleaf features. HIGH confidence.
- **Phase 4 (page-break-before):** Explicitly supported in OpenHTMLtoPDF. HIGH confidence for explicit page breaks.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | MEDIUM | Library choices are HIGH confidence (established, no real alternatives). Exact version numbers are MEDIUM — training data cutoff May 2025, need Maven Central verification before writing pom.xml |
| Features | HIGH | Table stakes features are well-established Thymeleaf + OpenHTMLtoPDF patterns. MEDIUM for multi-column and running headers/footers. |
| Architecture | MEDIUM | Pipeline structure is HIGH confidence. OpenHTMLtoPDF builder API specifics (useFastMode, useFont signature) are MEDIUM — may have changed in recent releases |
| Pitfalls | HIGH | CSS 2.1 limitations, font registration, and resource path issues are extensively documented and cross-validated across multiple research sources. No web verification was possible but training data is consistent. |

**Overall confidence:** MEDIUM-HIGH — the approach is correct and the pitfalls are well-understood. The main uncertainty is exact API surface of the current OpenHTMLtoPDF version.

### Gaps to Address

- **OpenHTMLtoPDF version:** Verify 1.1.22 is current on Maven Central; check for 1.1.23+. Verify `useFastMode()` exists. Verify `useFont()` exact signature. Do this in Phase 1 before writing PdfGenerator.
- **Spring Boot version:** Verify 3.4.1 vs latest 3.4.x or 3.5.x before writing pom.xml.
- **CSS column-count in paged media:** Needs a working test in Phase 4 to confirm or identify fallback approach. Not validated with live testing.
- **CSS @page margin boxes:** Running headers/footers with `running()` elements need early empirical testing in Phase 5. The exact CSS syntax may require iteration.
- **Mammoth output quality:** The HTML produced by Mammoth from the specific .docx files has not been previewed. Template CSS may need significant adjustment depending on Mammoth's output for these particular documents.

## Sources

### Primary (HIGH confidence)
- OpenHTMLtoPDF GitHub repository (github.com/danfickle/openhtmltopdf) — CSS support matrix, resource resolution, font registration patterns
- Thymeleaf documentation (thymeleaf.org) — standalone engine configuration, template processing
- ZXing GitHub (github.com/zxing/zxing) — QR generation API, MatrixToImageWriter

### Secondary (MEDIUM confidence)
- Spring Boot reference docs — Thymeleaf auto-configuration, BOM dependency management
- CSS Paged Media Module Level 3 (W3C spec) — basis for @page, page-break, column-count claims
- Training data knowledge of Java HTML-to-PDF ecosystem — library comparison (Flying Saucer, iText, FOP, wkhtmltopdf)

### Tertiary (LOW confidence)
- Specific OpenHTMLtoPDF API method names (`useFastMode`, exact `useFont` signature) — training data only, verify before use

**Research note:** Web search and fetch tools were unavailable during this research session. All findings are based on training data with a cutoff of approximately May 2025. Library choices and architectural patterns are stable and unlikely to have changed materially. Exact version numbers and specific API method signatures must be verified against current documentation before implementation begins.

---
*Research completed: 2026-02-18*
*Ready for roadmap: yes*
