# Portability, reproducibility and evidence

This document states what PhytoScope's local, bring-your-own-data workflow can
do today, with the evidence that supports each claim, and what is deliberately
listed as future work. It avoids "first/only" claims: other tools also address
non-model species (e.g. automated knowledge-base builders such as NoAC, or
orphan-crop browsers). PhytoScope's specific niche is **local execution,
user-supplied data, and site-free locus querying with traceable results** — no
per-species web database, no data upload.

## 1. Portability matrix: one pipeline, many species

Every bundled dataset goes through the same import → browse → query → export
pipeline with zero species-specific code. Species entries are data, not code.

| Dataset | Species | Format | Size | Import | Browse/query | Expression panel | Export | Evidence |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Arabidopsis | *A. thaliana* Col-0 | GFF3 (5 parts) + genome FASTA | 17 MB gz + 36 MB FASTA | ✔ | ✔ | ✔ 32,833 genes × 6 conditions (E-MTAB-7978) | ✔ TSV/PNG/SVG | `ReferenceDatasetIntegrityTest`, `SpeciesAgnosticTest` |
| Rice | *O. sativa* Nipponbare | GFF3 | 50 KB | ✔ | ✔ | – | ✔ | `AdaptiveChromosomeUiTest` |
| Maize | *Z. mays* B73 | GFF3 | 6 KB | ✔ | ✔ | – | ✔ | `AdaptiveChromosomeUiTest` |
| Soybean | *G. max* Williams 82 | GFF3 | 100 KB | ✔ | ✔ | – | ✔ | `AdaptiveChromosomeUiTest` |
| Mint (complete) | *M. longifolia* Long3 | GTF→GFF3 (gz) | 10.7 MB gz | ✔ | ✔ 94,883 genes | ✔ 94,883 × 4 leaf TPM | ✔ | `ReferenceDatasetIntegrityTest`, `MintTranscriptomeUiTest` |
| Mint (subset) | *M. longifolia* CMEN585 | GFF3 | 70 KB | ✔ | ✔ 12 chromosomes | – | ✔ | `AdaptiveChromosomeUiTest` |
| Mint plastid | *M. spicata* MG256495 | GFF3 (INSDC feature table) | 59 KB | ✔ | ✔ | – | ✔ | `ReferenceDatasetIntegrityTest` |
| User files | any | GFF3, GTF/GFF2, gz | any | ✔ | ✔ | ✔ (TSV/CSV matrix) | ✔ | `GoldenRegressionTest`, `ExpressionDelimiterTest` |

Test inputs and expected outputs are published in the repository itself:
`GoldenRegressionTest` embeds a synthetic GFF3 + GTF + FASTA + expression matrix
fixture with golden structures, sequences and reconciliation counts, so every
commit re-verifies that optimization does not change results.

## 2. Reproducible project units

A saved `.phytoscope` project is a versioned, self-contained unit:

- `schemaVersion` and `applicationVersion` in the manifest;
- every annotation source embedded with its **SHA-256**, re-verified on load
  (`Checksum mismatch` aborts a stale restore);
- the original source label and the full **import report** are retained;
- expression matrices embed a canonical snapshot plus the original import
  diagnostics; view state (sequence, selected gene, window) and research
  parameters (promoter windows, interval format) are stored and restored.

Reopening the same project on another machine therefore reproduces the same
structure, expression and analysis state. Implementation: `project/ProjectStore.java`.

## 3. Expression data linked to loci

Current behaviour, visible in the UI on every selection:

- the unit declared in the matrix header (`unit: TPM` / counts) is carried to
  axis and legend labels; no silent unit conversion;
- the import report states the **match rate**: `N matched of M input IDs
  against K annotation IDs`, lists unmatched IDs, and now adds actionable hints
  for case-variant and `.version`-suffix near matches
  (`service/ExpressionDataService.reconcile`);
- duplicate sample headings and duplicate gene identifiers are rejected with
  line numbers instead of being silently aggregated.

Future work (documented, not yet implemented): multi-experiment grouping and
time-course browsing across several matrices, and per-sample replicate display.
Single-dataset expression panels are a **data-browsing capability** and are not
presented as genotype-level biological conclusions.

## 4. Auditable data-quality reporting

On import, PhytoScope reports rather than hides input problems:

| Check | Behaviour | Where |
| --- | --- | --- |
| Malformed/unusable annotation rows | counted; status bar warns "N malformed annotation rows were skipped" | `parser/Gff3Parser`, `ui/MainView` |
| Duplicate gene/sample identifiers | hard error with line number | `ExpressionDataService`, `GeneSequenceService` |
| Missing/invalid expression cells | counted and shown in the import report dialog | `ExpressionDataService` |
| Unmatched expression IDs | listed, with near-match hints (case, `.version`) | `ExpressionDataService.reconcile` |
| FASTA ↔ annotation sequence names | coverage summary: all chromosomes covered, or "N of M have no FASTA record" with examples | `ui/MainView.describeFastaAlignment` |
| Source integrity | SHA-256 of every source recorded and re-verified on project restore | `project/ProjectStore` |

## 5. Performance evidence

Measured with the built-in benchmark runner (cold process, one import then
warm queries; heap pool peaks, not RSS). Reproduce with:

```bash
java -jar phytoscope-0.2.0.jar --benchmark --example mint-genome --out benchmark-results
```

Machine: Windows 11, amd64, 16 processors, JDK 21.0.6, `-Xmx3g`. Raw JSON:
`docs/performance.json` (2026-10-30 run).

| Metric | Value |
| --- | --- |
| Complete horsemint import (909,424 feature rows, gz-streamed) | **2,162 ms** |
| Genes loaded | 94,883 on 12 sequences |
| Switch across all 12 chromosomes | 3.5 ms total |
| Gene-ID search (100 runs, 4 IDs) | median 24.7 ms, p95 32.3 ms |
| Transcript/exon/CDS/UTR hierarchy query | median 0.004 ms, p95 0.009 ms |
| 10-locus comparison figure layout | 89.6 ms (SVG write 22.2 ms) |
| Heap pool peak (old gen) | ~0.9 GB |

The browser renders only genes overlapping the current view window and caps
overlapping tracks at 100 loci with an explicit message, which keeps panning
and zooming interactive at full-genome scale.
