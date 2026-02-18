# CSS Constraints for OpenHTMLtoPDF

OpenHTMLtoPDF renders HTML+CSS to PDF. It does NOT use a browser engine.
Always test in PDF output, never rely on browser preview alone.

## Supported (CSS 2.1 baseline)

- Standard box model: `margin`, `padding`, `border`, `width`, `height`
- `display: block`, `inline`, `inline-block`, `table`, `table-row`, `table-cell`, `none`
- `position: static`, `relative`, `absolute` (within page context)
- `float: left`, `float: right`, `clear`
- `@page` rules: `size` (A4, Letter, custom), `margin`, `page-break-before`, `page-break-after`, `page-break-inside`
- `column-count` (multi-column layout, paged-media support may vary)
- Font embedding via `@font-face` or builder API
- Colors: `rgb()`, `rgba()`, hex, named colors
- `background-color`, `background-image` (with data URIs)
- `text-align`, `vertical-align`, `line-height`, `letter-spacing`
- `list-style-type`, `list-style-position`
- Base64 data URIs for images (`<img src="data:image/png;base64,..."/>`)
- `border-radius` (basic support)
- `overflow: hidden`

## NOT Supported

- `display: flex` (Flexbox) -- use `display: table` or `float` instead
- `display: grid` (CSS Grid) -- use `display: table` layout instead
- CSS custom properties (`var()`)
- CSS animations and transitions
- JavaScript execution
- Viewport units (`vw`, `vh`, `vmin`, `vmax`)
- `calc()` expressions
- Media queries (ignored)
- `position: fixed` (use `@page` margin boxes for headers/footers)
- `box-shadow`, `text-shadow` (limited or no support)
- CSS3 selectors (`:nth-child`, `:not`, etc.) -- limited support

## Recommendations

1. Use `display: table` + `table-cell` for side-by-side layouts
2. Use `float` for simple two-column arrangements
3. Use `@page` rules for page size, margins, and breaks
4. Embed fonts explicitly for Unicode coverage (umlauts, special chars)
5. All HTML must be well-formed XHTML (self-close void elements)
6. Test every layout change by generating the actual PDF
