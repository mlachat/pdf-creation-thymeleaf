# Requirements: Print

**Defined:** 2026-02-18
**Core Value:** Prove that customer-provided Word templates can be reliably converted to HTML and rendered as pixel-accurate PDFs with dynamic placeholder data.

## v1 Requirements

### Project Setup

- [ ] **SETUP-01**: Spring Boot Maven project with latest starter, Java 21, configured for test-only usage (no runnable application)
- [ ] **SETUP-02**: OpenHTMLtoPDF, Thymeleaf, and ZXing dependencies configured with pinned versions
- [ ] **SETUP-03**: Unicode-complete font (DejaVuSans or similar) embedded and registered with OpenHTMLtoPDF
- [ ] **SETUP-04**: Base URI configuration for resolving CSS, images, and fonts from test classpath
- [ ] **SETUP-05**: CSS constraints documented (CSS 2.1 only — no Flexbox, no Grid)

### Template Rendering

- [ ] **TMPL-01**: Thymeleaf processes HTML templates with text placeholders (names, addresses, dates) via `th:text`
- [ ] **TMPL-02**: QR code generated via ZXing, embedded as base64 PNG data URI in template
- [ ] **TMPL-03**: Page breaks work correctly for multi-page documents (`page-break-before: always`)
- [ ] **TMPL-04**: 2-column layout renders correctly in PDF via CSS columns or table-based layout
- [ ] **TMPL-05**: Conditional content blocks show/hide sections based on data (`th:if`)
- [ ] **TMPL-06**: Running headers/footers with page numbers via CSS `@page` margin boxes

### Demo Templates

- [ ] **DEMO-01**: Template A — 2-page document with QR code, address/name placeholders, page header/footer
- [ ] **DEMO-02**: Template B — 1-page document with 2-column layout and placeholders
- [ ] **DEMO-03**: CSS stylesheets for both templates with print-ready `@page` rules (A4 size, margins)

### Source Artifacts

- [ ] **DOCX-01**: Demo .docx file for Template A (2-page letter with QR code placeholder area)
- [ ] **DOCX-02**: Demo .docx file for Template B (1-page 2-column layout)

### Testing

- [ ] **TEST-01**: JUnit test generates PDF from Template A and writes to `target/test-output/template-a.pdf`
- [ ] **TEST-02**: JUnit test generates PDF from Template B and writes to `target/test-output/template-b.pdf`
- [ ] **TEST-03**: PDF content assertions verify expected text (names, addresses) exists in generated PDFs
- [ ] **TEST-04**: QR code in Template A PDF is scannable (generated at 300+ pixels)

## v2 Requirements

### Extended Features

- **EXT-01**: Dynamic table rendering with `th:each` for repeating row data
- **EXT-02**: PDF metadata (title, author, creation date)
- **EXT-03**: SVG rendering support via `openhtmltopdf-svg-support` module
- **EXT-04**: Template fragments for shared header/footer across templates
- **EXT-05**: PDF/A compliance for archival-grade output

## Out of Scope

| Feature | Reason |
|---------|--------|
| Running Spring Boot application / REST API | Test-only demo — JUnit tests are the deliverable |
| Web UI | No frontend needed for this proof of concept |
| Runtime .docx → HTML conversion | Mammoth is an external preprocessing step |
| Database or persistence | Stateless template processing |
| Authentication or security | Demo project |
| JavaScript in templates | OpenHTMLtoPDF does not execute JavaScript |
| Caching or performance optimization | Demo generates a handful of PDFs |
| Multi-language / i18n | Single-language demo |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| SETUP-01 | — | Pending |
| SETUP-02 | — | Pending |
| SETUP-03 | — | Pending |
| SETUP-04 | — | Pending |
| SETUP-05 | — | Pending |
| TMPL-01 | — | Pending |
| TMPL-02 | — | Pending |
| TMPL-03 | — | Pending |
| TMPL-04 | — | Pending |
| TMPL-05 | — | Pending |
| TMPL-06 | — | Pending |
| DEMO-01 | — | Pending |
| DEMO-02 | — | Pending |
| DEMO-03 | — | Pending |
| DOCX-01 | — | Pending |
| DOCX-02 | — | Pending |
| TEST-01 | — | Pending |
| TEST-02 | — | Pending |
| TEST-03 | — | Pending |
| TEST-04 | — | Pending |

**Coverage:**
- v1 requirements: 20 total
- Mapped to phases: 0
- Unmapped: 20

---
*Requirements defined: 2026-02-18*
*Last updated: 2026-02-18 after initial definition*
