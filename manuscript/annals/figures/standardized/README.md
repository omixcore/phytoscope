# Selected manuscript graphics

Use this folder's Figure_1 through Figure_5 and Table_S1 through Table_S4 for the current upload set. All exported figure/table PDFs were checked for embedded Arial Regular at 7 pt, pure black text, black 0.5 pt outlines and lines, fixed A4 dimensions, and preserved proportions. Pale/color fills encode the original modules or data. No graphical data values were changed. The exact original Figure 1 overview layout is retained.

Every graphic has an editable SVG and a text-outlined SVG to avoid font substitution during copying. Keep proportional scaling; changing physical size changes physical font and line size. Main figures are A4 landscape with at least 25 mm margins; tables are A4 portrait inside 25 mm margins. Content is not stretched to fill the page. Five figure PNGs are 600 dpi.

The coverage grid was converted from its embedded flat bitmap to equivalent vector cells sampled from that same source; all PDFs now contain vector graphics only. The three Figure 4 arrow shafts and heads were fused into continuous paths.

Rebuild figures with `python manuscript/annals/figures/standardize_figures.py` and table exports with `python manuscript/annals/figures/build_supplementary_tables.py`. Install Arial Regular, Inkscape and Pillow first. Audit JSON files document actual exported-PDF checks. The Word Supplementary Information contains the same styled editable tables.
