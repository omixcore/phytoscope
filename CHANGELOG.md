## 2026-11-14 — FCS submission candidate: slim data, intake health-check, cross-species portability

- Slimmed the bundled data: the complete reference data (TAIR10 genome FASTA,
  Araport11 Chr1–Chr5 annotation, horsemint genome + 4-leaf TPM, E-MTAB-7978
  atlas matrix) was moved to the independent `phytoscope-reference-data`
  repository with a plain `git rm` and a new commit (baseline `8fd01ca`); git
  history was not rewritten. Six KB-scale examples remain bundled and no genome
  FASTA ships with any example.
- Added the genome-import wizard with an intake health-check (D1–D6): exact-first
  sequence-alias resolution (no silent first-match wins), user-confirmed alias
  mapping, hash-verified file identity before load, stale-report invalidation on
  file change, annotation-only imports that clear the previous FASTA, project
  reopen that auto-restores the matching FASTA, FASTA-header tab/whitespace
  truncation, coordinate-bounds checks, and JSON/Markdown/TSV health reports.
- Added manifest v2 / schemaVersion 4 projects that persist the import health-check
  metadata (spec version, assembly identity, annotation metadata, per-file
  integrity results and the confirmed alias map); schema versions 1–4 load.
- Hardened cross-species portability: rice bean (*Vigna umbellata*,
  GCF_029865625.1) full gold (GFF3 + GTF, negative-strand three-exon locus with
  transcript/CDS/protein extraction and Salmon re-quantification), plus two
  additional non-model RefSeq cases (Andrographis, Hibiscus).
- Added the species-independent input/project specification and re-reviewed data
  provenance and licensing; the horsemint annotation is confirmed CC BY 4.0 and
  the mint 4-leaf TPM matrix source remains unconfirmed (see
  `phytoscope-reference-data/LICENSES.md`).
- Measured the slim build (JAR 21.28 MiB, Windows app-image ZIP 44.01 MiB); no new
  Release has been published for this tree yet.

## 2026-10-16 — Validation hardening

- Fixed quote-aware delimiter detection for expression matrices.
- Preserved saved browser sequence/renderer state when restoring projects with a search query.
- Enforced exact gene-ID resolution for comparison CLI loci.
- Applied PNG raster safety limits after the default export scale.
- Fixed macOS desktop packaging version validation.

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
