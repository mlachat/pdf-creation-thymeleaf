---
phase: 03-test-suite
plan: 01
subsystem: testing
tags: [junit, integration-test, pdfbox, zxing, qr-decode, thymeleaf]

dependency-graph:
  requires: [01-foundation-pipeline, 02-demo-templates]
  provides: [end-to-end-test-coverage, pdf-output-verification, qr-roundtrip-proof]
  affects: []

tech-stack:
  added: []
  patterns: [integration-test-with-pdf-text-extraction, qr-code-roundtrip-verification]

file-tracking:
  key-files:
    created:
      - src/test/java/com/example/print/TemplateAIntegrationTest.java
      - src/test/java/com/example/print/TemplateBIntegrationTest.java
    modified: []

decisions: []

metrics:
  duration: 2min
  completed: 2026-02-18
---

# Phase 3 Plan 1: Integration Test Suite Summary

End-to-end integration tests for both templates proving the full pipeline (QR -> Thymeleaf -> OpenHTMLtoPDF -> PDF) with text extraction assertions and QR code decode verification.

## What Was Done

### Task 1: Template A Integration Test with QR Verification
**Commit:** `5f85bfe`

Created `TemplateAIntegrationTest.java` with 3 test methods:
- `testGenerateTemplateAPdf()` -- generates PDF, verifies file exists, %PDF header, >10KB size
- `testTemplateAContentAssertions()` -- PDFBox text extraction verifies "Hans Müller", "Dr. Anna Schmidt", "ACME GmbH", "Wichtige Mitteilung"
- `testTemplateAQrCodeScannable()` -- renders PDF page as BufferedImage at 150 DPI, decodes QR via ZXing, asserts URL matches "https://acme-gmbh.de/doc/A-2026-001"

Output: `target/test-output/template-a.pdf` (14KB)

### Task 2: Template B Integration Test with Content Assertions
**Commit:** `4af2765`

Created `TemplateBIntegrationTest.java` with 2 test methods:
- `testGenerateTemplateBPdf()` -- generates PDF, verifies file exists, %PDF header, >5KB size
- `testTemplateBContentAssertions()` -- PDFBox text extraction verifies "Thomas Müller", "Produktinformation", "EUR 2.499", "ACME GmbH", "Sonderangebot"

Output: `target/test-output/template-b.pdf` (14KB)

## Task Commits

| Task | Commit | Description |
|------|--------|-------------|
| 1 | `5f85bfe` | Template A integration test with QR verification |
| 2 | `4af2765` | Template B integration test with content assertions |

## Verification Results

- `mvn test`: 14 tests, 0 failures (9 existing + 3 Template A + 2 Template B)
- `target/test-output/template-a.pdf`: exists, 14KB
- `target/test-output/template-b.pdf`: exists, 14KB
- QR code in Template A PDF successfully decoded via ZXing
- German umlauts (Müller, Königstraße, München) render correctly in both PDFs

## Deviations from Plan

None -- plan executed exactly as written.

## Decisions Made

None -- followed established patterns from PipelineIntegrationTest.

## Next Phase Readiness

This is the final phase. The project is complete:
- Phase 1: Foundation pipeline (QR, Thymeleaf, PDF generation)
- Phase 2: Demo templates (formal letter + product info sheet)
- Phase 3: Integration test suite proving end-to-end correctness

All 14 tests pass. Both template PDFs are generated and verified.

## Self-Check: PASSED
