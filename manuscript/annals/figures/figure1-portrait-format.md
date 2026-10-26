# Figure 1 portrait format

- Page: A4 portrait, 210 × 297 mm.
- Layout: approximately 160 mm wide, within the nominal 25 mm page margins; page space is retained around the artwork.
- Font: Arial Regular, 7 pt throughout; no bold or italic text.
- Text: pure black (#000000).
- Strokes: pure black (#000000), 0.5 pt throughout. Pale fills distinguish modules and sequence features.
- Arrows: every shaft and arrowhead is one continuous SVG path object; no detached markers.
- PDF: vector artwork with embedded Arial; actual extracted text size is 7 pt.
- Editable SVG: live Arial text, physical A4 size and preserved aspect ratio.
- Outlined SVG: text converted to paths to prevent font substitution after copying; exact letter shapes are retained. Use this for copying into illustration/layout software.
- PNG: 600 dpi, A4 portrait.
- Maintain proportional scaling when resizing. At 100% physical size, text remains 7 pt and strokes 0.5 pt; resizing the entire figure changes those physical sizes.
- Build: install Arial Regular, Inkscape and Pillow, then run `python manuscript/annals/figures/build_portrait_figure.py` from the repository root. Font files are not redistributed.
- Source values are retained in `figure1-source-data.json`; the English and Chinese legends are in `figure1-portrait-legend.md`.
