# Print

## What This Is

A demo Spring Boot Maven project that showcases PDF generation from Word-derived HTML templates. Customers provide .docx files which are converted to HTML (via Mammoth as a preprocessing step), then manually enhanced with Thymeleaf placeholders. JUnit tests demonstrate the full pipeline: HTML+CSS template + data → Thymeleaf rendering → PDF output via OpenHTMLtoPDF.

## Core Value

Prove that customer-provided Word templates can be reliably converted to HTML and rendered as pixel-accurate PDFs with dynamic placeholder data (names, addresses, QR codes).

## Requirements

### Validated

- ✓ Spring Boot Maven project with latest starter, configured for test-only usage — v1.0
- ✓ Thymeleaf templating for HTML placeholder filling (names, addresses, etc.) — v1.0
- ✓ OpenHTMLtoPDF for HTML+CSS → PDF rendering with DejaVuSans Unicode font — v1.0
- ✓ Demo template A: 2-page document with QR code and address/name placeholders — v1.0
- ✓ Demo template B: 1-page document with 2-column layout and placeholders — v1.0
- ✓ 2 corresponding .docx files created as source artifacts — v1.0
- ✓ JUnit tests that process each template, verify content, and output viewable PDFs — v1.0
- ✓ CSS styling for print-ready layout (page breaks, margins, A4 @page rules) — v1.0

### Active

(None — v1.0 complete, project delivered)

### Out of Scope

- Running Spring Boot application / REST API — this is test-only
- Web UI — no frontend needed
- Runtime .docx → HTML conversion — Mammoth is used externally as a preprocessing step
- Database or persistence layer — stateless template processing
- Authentication or security — demo project

## Context

Shipped v1.0 with 1,060 LOC across Java, HTML, and CSS.
Tech stack: Spring Boot 3.5.10, OpenHTMLtoPDF 1.1.37, ZXing 3.5.4, PDFBox (transitive), Thymeleaf.
14 tests passing — full pipeline verified for both templates with text extraction and QR decode.

## Constraints

- **Tech stack**: Java 21, Spring Boot 3.5.10, Maven — customer ecosystem requirement
- **Template engine**: Thymeleaf — chosen for Spring Boot integration
- **PDF engine**: OpenHTMLtoPDF 1.1.37 — CSS 2.1 only (no Flexbox/Grid)
- **Word conversion**: Mammoth (external tool) — chosen for clean semantic HTML output
- **Scope**: Demo/proof-of-concept only — no production concerns

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Mammoth for .docx → HTML | Produces cleanest semantic HTML, also available as Java lib for future in-app use | ✓ Good |
| Thymeleaf for templating | Spring Boot default, natural HTML syntax, wide ecosystem support | ✓ Good |
| OpenHTMLtoPDF for rendering | Modern CSS support, actively maintained, handles print layouts well | ✓ Good |
| Test-only project | Demo purpose — JUnit tests are the deliverable, not a running service | ✓ Good |
| DejaVuSans 2.37 via temp file | builder.useFont() requires File; classpath extraction with cleanup | ✓ Good |
| useFastMode() on PdfRendererBuilder | Better performance, no visible quality difference | ✓ Good |
| Table-based 2-column layout | CSS 2.1 constraint prevents Flexbox/Grid; HTML table works reliably | ✓ Good |
| Raw Open XML ZIP for .docx | No Apache POI dependency needed for simple demo artifacts | ✓ Good |

---
*Last updated: 2026-02-18 after v1.0 milestone*
