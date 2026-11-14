# PhytoScope — paper main figures (Figure 1–5) + one supplementary figure

Self-contained, reproducible figure generation. One script renders five main
figures and one supplementary figure from the committed, frozen evidence files;
no numbers are invented.

**Regenerate** (from the repository root):

```sh
python docs/paper-figures/make_paper_figures.py
```

**Outputs** (each figure is written as both print-resolution PNG @300 dpi and
vector SVG): `Figure1–5.png/svg` + `FigureS1.png/svg` in this directory.

**Toolchain**: Python 3.13 + matplotlib 3.11 (`Agg` headless backend); no
Java/Maven required to render.

---

## Figure captions

- **Figure 1** — *PhytoScope overview: one local pipeline from import to export.*
  Pipeline schematic (import → health-check/validate → unified in-memory model →
  browse/query → export/project) with the seven research modules and the
  `.phytoscope` reopen loop. **Representative/schematic** (workflow diagram, not a
  measured pipeline); class names are the real implementing classes.

- **Figure 2** — *Standard data model and input-format compatibility.* (a) three
  input formats mapped to one unified model; (b) coordinate/value conventions;
  (c) real rice-bean `LOC124833352` minus-strand locus showing the GFF3
  (1,983-nt CDS incl. terminal stop) vs GTF (1,980-nt CDS + separate `stop_codon`)
  boundary, both translating to RefSeq `XP_087956893.1` (660 aa).
  **Panels (a)/(b) schematic; panel (c) real data.**

- **Figure 3** — *Core research modules and their golden-standard agreement.*
  Seven modules with observed CI results: promoter 8/8 (bedtools), sequence QC
  4/4, homology 20/20 per family (RBCS + CYP71A, BLASTP), domain 16/16,
  expression grouping + 34,914/34,914 gene reconciliation (Salmon), comparison
  layout, project round-trip. **Real data**, except the Comparison module which
  is marked "representative" (regression-tested layout, no numeric gold).

- **Figure 4** — *Multi-species data validation and correctness.* (a) imported
  gene counts for 4 model (Ensembl r62) + 3 non-model (RefSeq) assemblies;
  (b) CDS length vs protein aa for 3 full-gold loci; (c) byte-exact agreement
  grid (span/transcript/CDS/protein = independent gold / RefSeq protein), plus
  25 source-derived coding transcripts (5 loci × 3 species). **Real data.**

- **Figure 5** — *Reproducibility: provenance and self-contained projects.*
  (a) manifest v2 (schemaVersion 4) recording per-file SHA-256, alias map and
  provenance; (b) self-contained project round-trip (save → reopen → verify
  stored hash; a checksum mismatch aborts a stale restore); (c) independent
  `phytoscope-reference-data` repository — 9 files / 68,518,847 B, dual
  gitee/github mirrors, `sha256sum -c SHA256SUMS.txt` → 9/9 OK.
  **Panels (a)/(b) schematic; panel (c) real data.**

- **Figure S1 (supplementary)** — *Fair comparison with existing tools:
  shared-task agreement and design differences.* (a) gffutils 0.13 vs PhytoScope
  (8/8 structure tuples identical across 3/3 alternating trials; timings
  reported separately and labelled "different storage designs", not a speed
  comparison); (b) Biopython 1.88 SeqIO byte-exact vs frozen gold (3 loci × 6
  products); (c) qualitative capability matrix vs IGV/JBrowse2/TBtools.
  **(a)/(b) real measured data; (c) qualitative (documentation-based, not
  measured).** Demoted from a main figure per team-lead decision.

---

## Data sources (all real, committed files)

| Figure | Source files |
|---|---|
| 1 | schematic; class names from `src/main/java/io/gitee/omixcore/phytoscope/**` |
| 2 | `src/test/resources/non-model/vigna-umbellata/manifest.json` (locus/coordinates/CDS/GTF stop-codon, real) · `docs/input-format-specification.md` (coordinate conventions) |
| 3 | `docs/research-module-validation.md` §Observed results from CI · `src/test/resources/non-model/vigna-umbellata/manifest.json` (expression reconciliation) |
| 4 | `docs/chromosome-validation.md` §Full public annotation validation (4 model counts) · `src/test/resources/non-model/additional-nonmodel-manifest.json` (Andrographis/Hibiscus gene counts + loci) · `src/test/resources/non-model/vigna-umbellata/manifest.json` (Vigna counts + locus) · `src/test/resources/non-model/multi-locus-sequence-gold.tsv` (25 transcripts) |
| 5 | `docs/portability-and-reproducibility.md` §2 + `docs/input-format-specification.md` §Project persistence (manifest schema / round-trip, schematic) · `docs/fcs-paper-outline.md` §3.5 + `docs/design-slim-bundled-data.md` §2.1 + `docs/fcs-paper-manuscript-draft.md` §2.5 (reference-data: 9 files / 68,518,847 B / commit `acac2cf` / dual mirror / 9/9 OK, real) |
| S1 | `target/phase3-comparison/gffutils/summary.json` (measured) · `target/phase3-comparison/biopython/summary.json` (measured) · `docs/fcs-phase3-comparison-plan.md` §4.1 (qualitative matrix) |

Notes on transcription: the four model-species counts (Arabidopsis/rice/maize/
soybean) and the nine reference-data file sizes exist only as Markdown tables in
the cited docs, so they are transcribed verbatim into the script with inline
source comments (not invented). Every other number is read programmatically
from JSON/TSV.

---

## Honesty / boundary notes

- **Figure S1 (a)**: gffutils builds an on-disk SQLite index while PhytoScope
  uses an in-memory model. Cold import/index-build times are therefore
  **reported separately** and the panel explicitly states no speed/usability
  superiority is claimed. The headline result is correctness (tuple identity).
- **Figure S1 (c)**: JBrowse2 / IGV / TBtools rows are **qualitative**
  (documentation-based, `docs/fcs-phase3-comparison-plan.md` §4.1) and marked
  "not measured".
- **Figure 3** Comparison module and **Figure 1 / 2(a)(b) / 5(a)(b)** are
  labelled "representative/schematic" where no quantitative gold exists.
- No finance-style red/green colour semantics are used; green = "match/agreement/
  OK" check, orange = "needs config", red = "checksum mismatch / tampered", gray
  = "not native / not measured".

## Numbering note (team-lead decision, 2026-11-14)

Final main-figure numbering is per the team-lead instruction:

1. architecture · 2. data model & input format · 3. seven modules ·
4. multi-species validation · 5. reproducibility.

The earlier "tool comparison" main figure was demoted to **Figure S1**
(supplementary), since it corresponds to the fair-comparison + capability-matrix
content rather than the reproducibility contribution.

Residual mismatch to flag: `docs/fcs-paper-outline.md` §4 currently titles
"Figure 2" = cross-species portability and "Figure 4" = fair comparison. The
team-lead list above instead places cross-species validation at Figure 4 and
the data-model/input-format panel at Figure 2. The figures themselves are all
present; only the outline's Figure-2/Figure-4 titles would need a one-line edit
to match, if the team-lead wishes the outline to mirror this README exactly.
