# Manuscript figures

## Selected main figures

| Figure | File stem | Build script |
| --- | --- | --- |
| 1 | `figure1-portrait-A4` | `build_portrait_figure.py` |
| 2 | `figure2-coverage` | `build_figures.py` |
| 3 | `figure3-annotation-tuples` | `build_figures.py` |
| 4 | `figure4-expression-reconciliation` | `build_figures.py` |
| 5 | `figure5-module-checks` | `build_figures.py` |

The selected Figure 1 is now the A4 portrait design, using Arial Regular 7 pt, black text and 0.5 pt black strokes. Use `figure1-portrait-legend.md` for its English and Chinese legends and `figure1-portrait-format.md` for copying and sizing details. Earlier `figure1-workflow` and `figure1-highlights` designs are retained for comparison.

## Rebuild the earlier overview design

Run from the repository root with Python 3, Pillow and Inkscape available:

```sh
python manuscript/annals/figures/build_graphical_abstract.py
```

The builder reads the frozen structural validation reports, locus manifest, sequence gold, expression matrix and expression provenance manifest directly. It writes editable SVG, vector PDF, 5,400-pixel-wide PNG and `figure1-source-data.json`. Coordinates, sequence lengths and example expression values are source-derived; plant symbols, document icons and the shared-model hierarchy are schematics. Displayed structure and expression examples use different, explicitly labelled loci.

The SVG is 180 × 136.25 mm. The PNG has approximately 762 dpi at that physical width. SVG text remains editable and requests Arial with Nimbus Sans and sans-serif fallbacks. This environment has Nimbus Sans, which is embedded in the PDF; the PDF is not an Arial-embedded file. Use the editable SVG to substitute the journal's required installed font if necessary. Data and text are not rasterized in the SVG or PDF. Fine line widths and legibility should be checked after any journal-requested resizing.

The visual organization draws on the supplied reference figures' directional data flow, visible module internals, consistent muted colors and separate validation band. No artwork, article results, author names or tables were copied from those references. The references are used for layout only and do not supply PhytoScope evidence.
