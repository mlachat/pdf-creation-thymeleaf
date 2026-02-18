---
phase: 01-foundation-pipeline
plan: 01
subsystem: pdf-pipeline
tags: [openhtmltopdf, zxing, maven, pdf, qr-code, unicode, dejavu-font]

# Dependency graph
requires: []
provides:
  - "Maven project with Spring Boot 3.5.10, OpenHTMLtoPDF 1.1.37, ZXing 3.5.4"
  - "PdfGenerator: HTML+CSS to PDF rendering with embedded DejaVuSans font"
  - "QrCodeGenerator: text to base64 PNG data URI"
  - "CSS constraints reference (CSS 2.1 only, no Flexbox/Grid)"
affects: [01-02, 02-template-system, 03-api-layer]

# Tech tracking
tech-stack:
  added: [spring-boot-starter-thymeleaf, openhtmltopdf-pdfbox-1.1.37, zxing-core-3.5.4, zxing-javase-3.5.4]
  patterns: [XHTML-compliant templates, font-via-temp-file extraction, base64-data-uri images]

key-files:
  created:
    - pom.xml
    - src/main/java/com/example/print/pdf/PdfGenerator.java
    - src/main/java/com/example/print/qr/QrCodeGenerator.java
    - src/main/resources/fonts/DejaVuSans.ttf
    - src/test/java/com/example/print/pdf/PdfGeneratorTest.java
    - src/test/java/com/example/print/qr/QrCodeGeneratorTest.java
    - src/test/resources/templates/test-minimal.html
    - src/test/resources/css/test-minimal.css
    - CSS-CONSTRAINTS.md
  modified: []

key-decisions:
  - "OpenHTMLtoPDF 1.1.37 with io.github.openhtmltopdf group ID (migrated from com.openhtmltopdf)"
  - "DejaVuSans 2.37 from SourceForge for full Unicode/umlaut coverage"
  - "Font loaded via temp file extraction from classpath (builder.useFont requires File)"
  - "useFastMode() enabled on PdfRendererBuilder"

patterns-established:
  - "XHTML compliance: all templates must be well-formed XML with self-closed void elements"
  - "Font registration: extract from classpath to temp file, register with builder, cleanup in finally"
  - "Test output: PDF artifacts written to target/test-output/ for manual inspection"

# Metrics
duration: 2min
completed: 2026-02-18
---

# Phase 1 Plan 1: Foundation Pipeline Summary

**Maven project with OpenHTMLtoPDF 1.1.37 PDF pipeline, ZXing QR generator, and DejaVuSans Unicode font rendering German umlauts**

## Performance

- **Duration:** 2 min
- **Started:** 2026-02-18T08:35:19Z
- **Completed:** 2026-02-18T08:37:40Z
- **Tasks:** 2
- **Files modified:** 9

## Accomplishments
- Maven project compiles with all dependencies resolved (Spring Boot 3.5.10, OpenHTMLtoPDF, ZXing)
- PdfGenerator renders HTML+CSS to valid PDF with embedded DejaVuSans font showing German umlauts
- QrCodeGenerator produces valid base64 PNG data URIs verified by PNG signature
- CSS constraints documented (CSS 2.1 only, no Flexbox/Grid)
- All 4 unit tests pass, test-minimal.pdf generated at 9KB

## Task Commits

Each task was committed atomically:

1. **Task 1: Create Maven project with all dependencies and font** - `21f94e5` (chore)
2. **Task 2: Implement PdfGenerator, QrCodeGenerator, and unit tests** - `15deb51` (feat)

## Files Created/Modified
- `pom.xml` - Maven project with Spring Boot parent, OpenHTMLtoPDF, ZXing dependencies
- `src/main/java/com/example/print/pdf/PdfGenerator.java` - HTML+CSS to PDF renderer with font embedding
- `src/main/java/com/example/print/qr/QrCodeGenerator.java` - QR code to base64 PNG data URI generator
- `src/main/resources/fonts/DejaVuSans.ttf` - Unicode font (757KB, DejaVu 2.37)
- `src/test/java/com/example/print/pdf/PdfGeneratorTest.java` - PDF generation tests (hardcoded HTML + template file)
- `src/test/java/com/example/print/qr/QrCodeGeneratorTest.java` - QR code tests (data URI prefix + PNG signature)
- `src/test/resources/templates/test-minimal.html` - XHTML test template with German umlauts
- `src/test/resources/css/test-minimal.css` - Test CSS with @page A4 and DejaVuSans font-family
- `CSS-CONSTRAINTS.md` - OpenHTMLtoPDF CSS limitations reference

## Decisions Made
- Used `io.github.openhtmltopdf` group ID (not `com.openhtmltopdf`) as the project migrated to this new group
- Downloaded DejaVuSans 2.37 from SourceForge (GitHub raw URLs failed due to Git LFS)
- Font loaded via temp file extraction from classpath since `PdfRendererBuilder.useFont()` requires a `File` object
- Enabled `useFastMode()` on PdfRendererBuilder for better performance

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- GitHub raw download URLs for DejaVuSans.ttf returned HTML pages instead of the font binary (Git LFS issue). Resolved by downloading the full release zip from SourceForge and extracting the TTF.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- PDF pipeline foundation complete, ready for Thymeleaf template integration (Plan 01-02)
- `useFastMode()` confirmed working in OpenHTMLtoPDF 1.1.37 API
- CSS constraint: confirmed no Flexbox/Grid support, table-based layouts required

---
*Phase: 01-foundation-pipeline*
*Completed: 2026-02-18*

## Self-Check: PASSED
