# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-18)

**Core value:** Prove that customer-provided Word templates can be reliably converted to HTML and rendered as pixel-accurate PDFs with dynamic placeholder data.
**Current focus:** Phase 2 - Demo Templates

## Current Position

Phase: 2 of 3 (Demo Templates)
Plan: 1 of 2 in current phase
Status: In progress
Last activity: 2026-02-18 — Completed 02-01-PLAN.md

Progress: [███████░░░] 75%

## Performance Metrics

**Velocity:**
- Total plans completed: 3
- Average duration: 2.3min
- Total execution time: 0.12 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-foundation-pipeline | 2/2 | 5min | 2.5min |
| 02-demo-templates | 1/2 | 2min | 2min |

**Recent Trend:**
- Last 5 plans: 01-01 (2min), 01-02 (3min), 02-01 (2min)
- Trend: stable

*Updated after each plan completion*

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- OpenHTMLtoPDF 1.1.37 with `io.github.openhtmltopdf` group ID (migrated from `com.openhtmltopdf`)
- DejaVuSans 2.37 font embedded via temp file extraction (builder.useFont requires File)
- useFastMode() enabled on PdfRendererBuilder
- CSS 2.1 only: no Flexbox, no Grid, table-based layouts required
- CSS link paths must be relative to base URI root (test-classes), not template file location
- Standalone ThymeleafRenderer wrapping SpringTemplateEngine (no Spring Boot auto-config needed)
- @page margin boxes for running headers/footers (may need fallback if OpenHTMLtoPDF doesn't render them)
- Raw Open XML ZIP for .docx artifacts (no Apache POI dependency needed)

### Pending Todos

None.

### Blockers/Concerns

- CSS `column-count` in paged media has MEDIUM confidence -- may need table-based fallback for Template B
- CSS `@page` margin boxes with `running()` elements have MEDIUM confidence -- running headers/footers may need iteration
- @page margin boxes not yet runtime-verified with OpenHTMLtoPDF -- Template A header/footer may need CSS fallback

## Session Continuity

Last session: 2026-02-18T08:57Z
Stopped at: Completed 02-01-PLAN.md
Resume file: None
