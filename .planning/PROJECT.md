# Print

## What This Is

A demo Spring Boot Maven project that showcases PDF generation from Word-derived HTML templates. Customers provide .docx files which are converted to HTML (via Mammoth as a preprocessing step), then manually enhanced with Thymeleaf placeholders. JUnit tests demonstrate the full pipeline: HTML+CSS template + data → Thymeleaf rendering → PDF output via OpenHTMLtoPDF.

## Core Value

Prove that customer-provided Word templates can be reliably converted to HTML and rendered as pixel-accurate PDFs with dynamic placeholder data (names, addresses, QR codes).

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Spring Boot Maven project with latest starter, configured for test-only usage (no runnable app)
- [ ] Thymeleaf templating for HTML placeholder filling (names, addresses, etc.)
- [ ] OpenHTMLtoPDF for HTML+CSS → PDF rendering
- [ ] Demo template A: 2-page document with QR code and address/name placeholders
- [ ] Demo template B: 1-page document with 2-column layout and placeholders
- [ ] 2 corresponding .docx files created as source artifacts (simulating customer-provided templates)
- [ ] JUnit tests that process each template and output viewable PDF files
- [ ] CSS styling for print-ready layout (page breaks, margins, columns)

### Out of Scope

- Running Spring Boot application / REST API — this is test-only
- Web UI — no frontend needed
- Runtime .docx → HTML conversion — Mammoth is used externally as a preprocessing step
- Database or persistence layer — stateless template processing
- Authentication or security — demo project

## Context

- The real-world workflow: customer provides .docx → someone converts with Mammoth CLI → someone edits HTML to insert Thymeleaf placeholders → app fills data and renders PDF
- This demo focuses on the last two steps (template filling + PDF rendering) and includes sample .docx files to illustrate the full pipeline
- OpenHTMLtoPDF is a maintained fork of Flying Saucer with modern CSS support
- Thymeleaf is Spring's default templating engine, natural fit for HTML templates
- QR codes in templates may be embedded as base64 images or generated at render time

## Constraints

- **Tech stack**: Java, Spring Boot (latest), Maven — customer ecosystem requirement
- **Template engine**: Thymeleaf — chosen for Spring Boot integration
- **PDF engine**: OpenHTMLtoPDF — chosen for modern CSS support
- **Word conversion**: Mammoth (external tool) — chosen for clean semantic HTML output
- **Scope**: Demo/proof-of-concept only — no production concerns

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Mammoth for .docx → HTML | Produces cleanest semantic HTML, also available as Java lib for future in-app use | — Pending |
| Thymeleaf for templating | Spring Boot default, natural HTML syntax, wide ecosystem support | — Pending |
| OpenHTMLtoPDF for rendering | Modern CSS support, actively maintained, handles print layouts well | — Pending |
| Test-only project | Demo purpose — JUnit tests are the deliverable, not a running service | — Pending |

---
*Last updated: 2026-02-18 after initialization*
