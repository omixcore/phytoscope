# Changelog

## 0.2.0

- Add an explicit multi-locus comparison workspace with all isoforms, shared
  structure scales, strand-aware alignment, optional intron compression, measured
  expression cells and editable SVG / PNG / coordinate-table export.
- Add portable, checksum-verified schema-v1 projects with source annotation bytes,
  expression snapshots, view/selection/settings and retained import diagnostics.
- Reject expression duplicate IDs, blank/duplicate headers and malformed row
  widths transactionally. Report invalid/missing cells, undeclared units and
  exact-ID mapping quality; distinguish missing from measured zero and gene
  measurements from transcript measurements.
- Preserve shared exon/CDS/UTR parent edges and support additional standard RNA
  transcript types; distinguish gene-owned miRNA transcripts from mature products.
- Retain source CDS/UTR segments without matching exons separately; do not invent
  exon annotations or splice junctions.
- Stage imports and project restoration in background workers with progress,
  cancellation and retention of the previous dataset on failure.
- Bound browser detail tracks with an explicit notice, preserve full data/search,
  prevent mixed-sequence coordinate tracks, and make toolbar actions readable.
- Bundle a traceable E-MTAB-7978 RBCS expression case; document reference versions
  separately from assembly names and remove stale example descriptions.
- Add independent structure checks, measured benchmark commands, a gffutils
  extraction baseline, runtime-inclusive desktop image builds and cross-platform CI.

Automated correctness evidence does not establish a new biological mechanism,
comparative human usability, or native-dialog behavior on untested hardware.
