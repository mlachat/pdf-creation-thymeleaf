# Project Milestones: Print

## v1.0 PDF Pipeline Demo (Shipped: 2026-02-18)

**Delivered:** Proof-of-concept demonstrating customer Word templates converted to HTML and rendered as pixel-accurate PDFs with dynamic data, QR codes, and full integration test coverage.

**Phases completed:** 1-3 (5 plans total)

**Key accomplishments:**
- Maven project with OpenHTMLtoPDF 1.1.37 PDF pipeline and DejaVuSans Unicode font embedding
- ZXing QR code generation embedded as base64 PNG data URIs in templates
- Standalone ThymeleafRenderer with placeholder filling and conditional content blocks
- Template A: 2-page German business letter with QR code, page breaks, running headers/footers
- Template B: 1-page product info sheet with CSS 2.1 table-based 2-column layout
- Integration test suite verifying PDF text content via PDFBox and QR scannability via ZXing decode

**Stats:**
- 41 files created
- 1,060 lines of code (Java, HTML, CSS)
- 3 phases, 5 plans, 10 tasks
- 1 day from start to ship (2026-02-18)

**Git range:** `chore(01-01)` → `docs(03)`

**What's next:** Project complete — demo proof-of-concept delivered.

---
