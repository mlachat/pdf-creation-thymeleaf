# Roadmap: Print

## Overview

This project proves that customer-provided Word templates can be converted to HTML and rendered as pixel-accurate PDFs with dynamic data. The roadmap moves from establishing a working PDF generation pipeline (Maven, fonts, QR codes, Thymeleaf), through building two complete demo templates with print-ready CSS, to a verification phase with JUnit tests that produce and assert on generated PDFs.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Foundation + Pipeline** - Working PDF generation pipeline with Thymeleaf and QR code support
- [ ] **Phase 2: Demo Templates** - Both templates with print-ready CSS, page layouts, and source .docx files
- [ ] **Phase 3: Test Suite** - JUnit tests producing verified PDFs with content assertions

## Phase Details

### Phase 1: Foundation + Pipeline
**Goal**: A working end-to-end pipeline that takes an HTML template with placeholders, fills data via Thymeleaf, embeds a QR code, and produces a valid PDF with correct fonts
**Depends on**: Nothing (first phase)
**Requirements**: SETUP-01, SETUP-02, SETUP-03, SETUP-04, SETUP-05, TMPL-01, TMPL-02
**Success Criteria** (what must be TRUE):
  1. Maven project compiles and runs tests with Spring Boot, Thymeleaf, OpenHTMLtoPDF, and ZXing dependencies
  2. A minimal HTML template with `th:text` placeholders renders to a PDF containing the expected text (including German umlauts)
  3. A QR code generated via ZXing appears as a visible image in the PDF output
  4. CSS constraints (CSS 2.1 only, no Flexbox/Grid) are documented in the project
  5. Resources (CSS, fonts, images) resolve correctly from test classpath via configured base URI
**Plans**: 2 plans

Plans:
- [ ] 01-01-PLAN.md — Maven project setup, PdfGenerator, QrCodeGenerator, font embedding, CSS constraints
- [ ] 01-02-PLAN.md — ThymeleafRenderer integration and end-to-end pipeline test

### Phase 2: Demo Templates
**Goal**: Two complete, print-ready HTML templates with CSS that demonstrate the full range of PDF layout features (multi-page, QR codes, columns, conditional content, headers/footers)
**Depends on**: Phase 1
**Requirements**: TMPL-03, TMPL-04, TMPL-05, TMPL-06, DEMO-01, DEMO-02, DEMO-03, DOCX-01, DOCX-02
**Success Criteria** (what must be TRUE):
  1. Template A renders as a 2-page PDF with a page break between pages, QR code, and filled address/name placeholders
  2. Template B renders as a 1-page PDF with a working 2-column layout and filled placeholders
  3. Both templates have print-ready CSS with A4 page size, proper margins, and `@page` rules
  4. Conditional content blocks show or hide sections based on data values
  5. Two .docx source files exist as artifacts representing the customer-provided originals
**Plans**: TBD

Plans:
- [ ] 02-01: TBD
- [ ] 02-02: TBD

### Phase 3: Test Suite
**Goal**: JUnit tests that exercise the full pipeline for both templates, produce viewable PDF files, and verify content correctness
**Depends on**: Phase 2
**Requirements**: TEST-01, TEST-02, TEST-03, TEST-04
**Success Criteria** (what must be TRUE):
  1. Running `mvn test` produces `target/test-output/template-a.pdf` and `target/test-output/template-b.pdf`
  2. Generated PDFs are viewable in a standard PDF reader with correct layout and styling
  3. PDF content assertions confirm expected text (names, addresses) exists in both generated PDFs
  4. QR code in Template A PDF is rendered at 300+ pixels and is scannable
**Plans**: TBD

Plans:
- [ ] 03-01: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation + Pipeline | 0/2 | Planned | - |
| 2. Demo Templates | 0/TBD | Not started | - |
| 3. Test Suite | 0/TBD | Not started | - |
