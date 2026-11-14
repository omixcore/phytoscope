# Species-independent input and project specification

Status: proposed development and acceptance requirements. See the README for
currently implemented behavior. Joint validation, user-confirmed sequence
aliases and FASTA rebinding are implemented and verified for the genome-FASTA +
GFF3 (+ optional expression) import health-check. BED and bedGraph tracks remain
out of scope for this release. This document does not claim that all species or
annotation conventions have been validated.

## Scope and compatibility

The application derives sequence names and features from the supplied data,
without assuming a species, chromosome count, ploidy, or identifier prefix.
Existing annotation-only browsing, supported annotation and expression formats,
and project schema versions 1–4 remain readable. Annotation is required for the
existing gene-structure workspace; a matching genome FASTA is required for
sequence-dependent operations. Expression and tracks are optional.

| Input | Format | Coordinate convention |
| --- | --- | --- |
| Reference | FASTA; gzip accepted | Sequence positions |
| Annotation | GFF3 or supported GTF; legacy GFF2 accepted via the same importer (not separately validated); gzip accepted | 1-based, closed |
| Expression | Delimited text with header | Gene or transcript ID |
| Intervals | BED3 or BED6 *(deferred — v2 target, not yet implemented)* | 0-based, half-open |
| Signal | bedGraph *(deferred — v2 target, not yet implemented)* | 0-based, half-open |
| Saved project | .phytoscope | Record the convention of each source |

## Reference FASTA

Use the nonempty identifier preceding the first whitespace after `>` as the
sequence ID. Whitespace is the first space **or** the first tab, so both
`>seq1 description` and `>seq1\tdescription` yield the id `seq1` and discard the
remainder of the header line (spec unification, see §Reference FASTA parsing).
Preserve case and original identifiers. Accept chromosome, scaffold, contig and
organelle sequences. Reject duplicate identifiers and report their names.
Report invalid characters and ambiguous bases. Sequence name and length
agreement cannot establish assembly identity; record provenance and request the
user's confirmation of the matching assembly.

## Annotation

Parse GFF3 as nine tab-delimited columns with 1-based inclusive coordinates,
honoring escaped attributes and ID/Parent relationships, including multiple
parents and discontinuous features. Parse GTF using its gene_id and
transcript_id attributes under the existing importer. Legacy GFF2 is accepted
through the same importer for compatibility, but has no dedicated fixture or
validation claim in this release; validated coverage is GFF3 and GTF only.
Preserve source feature types, coordinates, sequence identifiers, strand (`+` /
`-`) and CDS phase. Minus-strand loci are reversed and reverse-complemented for
sequence extraction (see the README "Gene sequence details"); the input spec
records strand verbatim. Do not assume all genes have protein-coding transcripts
or complete CDS models.

Report malformed records, and coordinates that fall outside the FASTA contig
length (out-of-bounds; an import-blocking ERROR) where a reference FASTA is
available, unresolved parents, duplicate or inconsistent identifiers, and
invalid CDS phase. Distinguish fatal errors, warnings, notices and checks not
performed. Report affected counts and examples with source line numbers when
available. Existing parser behavior and supported inputs remain compatible.

## Expression

The recommended template is UTF-8 tab-delimited text with one identifier column
followed by unique sample columns. Existing CSV and semicolon-delimited inputs
and existing first-column headers remain supported.

```text
# unit=TPM
# level=gene
feature_id	sample_1	sample_2	sample_3
gene_001	12.5	10.8	0
gene_002	3.2		4.1
```

Metadata follows the existing importer: level is gene or transcript and
defaults to gene; unit defaults to unspecified. Preserve measured zero
separately from missing or invalid values. Reject blank or duplicate sample
names, duplicate row IDs and inconsistent row widths before replacing loaded
data. Report matching and invalid-value counts. Retain existing exact and
unambiguous annotation-prefix matching. Any additional aliases or version
mapping require an explicit user-supplied, confirmed mapping.

## Interval and signal tracks

> **Deferred (out of scope for this release; targeted for a future v2).**
> BED3/BED6 and bedGraph track parsing, validation and display are **not**
> implemented in the current genome-import health-check. The descriptions below
> record the intended contract for a later version and are not yet enforced.

The intended first implementation supports BED3 and BED6, and four-column
bedGraph. Columns are tab-separated. BED3 is chrom, start, end. BED6 adds name,
score and strand. bedGraph adds a numeric value to BED3. Validate column count,
numeric fields, score and strand against the applicable format, sequence IDs,
bounds, and nonempty intervals. Reject or report malformed records with line
numbers. Reject overlapping intervals within one bedGraph track, identify the
conflicts and allow correction and retry. Other tracks may overlap.

The interval GFF3 chrA:1–100 and BED chrA 0 100 both cover 100 bases.
Record source coordinates and convention, and convert explicitly at import and
export boundaries. Track display, source metadata and export must preserve the
meaning of the original intervals.

## Sequence aliases

Use exact identifiers first. Display possible aliases for user review without
assuming that transformations such as adding or removing `chr` are correct.
Require a confirmed, unambiguous mapping in the relevant file context. Report
unknown and ambiguous names with affected record counts. Preserve original and
mapped identifiers and the user's confirmed mapping. Reference sequences with
no annotation remain available.

## Joint import and validation

1. Inspect each selected file and its format and compression.
2. Validate each file's internal identifiers, coordinates and feature structure.
3. Compare annotation sequence names and bounds with the reference when present.
4. Match expression IDs at the declared feature level.
5. Check track names, coordinates and values against the reference when present.
6. Present diagnostics and allow explicit alias confirmation before applying.
7. Export three health-check reports for the selected inputs:
   - **JSON** (`intake-health-report.json`) — machine/CI-readable, stable fields
     in the order `specVersion, summary, pass, errorCount, warningCount,
     infoCount, files, issues`.
   - **Markdown** (`intake-health-report.md`) — human-readable summary.
   - **TSV** (`intake-health-report.tsv`) — tabular, with columns
     `severity, code, file, lineOrCoord, message`.

The report records file, check, status, affected count, examples and suggested
action. It identifies checks that could not run. Failed or cancelled imports
retain the previously loaded dataset and settings. Large-file processing should
remain cancellable and expose progress.

## Project persistence and FASTA rebinding

Maintain backward compatibility with existing project schemas. Manifest v2
(schemaVersion 4) records the specification version, assembly identity,
annotation metadata (format/version/source), per-file integrity results and the
user-confirmed sequence-name alias map in addition to the historical fields;
schema versions 1–4 remain readable and older manifests without the v2 fields
load with those fields recorded as unknown. Hash the exact file bytes on disk,
including compression. Preserve confirmed aliases, track settings, diagnostics
and analysis state. A missing field in an older project is recorded as unknown;
never invent provenance.

On reopening, resolve the recorded reference path or let the user locate it.
Verify the stored hash before automatic rebinding. If a hash is unavailable or
differs, explain the status and require an explicit user choice. A hash proves
file identity, not biological agreement with an annotation. Existing embedded
annotation and expression data remain usable when an external FASTA is absent.

## Acceptance criteria

- Validate real chromosome-scale and fragmented assemblies, including organelle
  sequences, and data from multiple sources and taxonomic groups. Record exact
  assemblies, annotation releases, expected outputs and test results.
- Verify positive and negative strands, first and last bases, BED/GFF3 coordinate
  equivalence, shared parents, discontinuous features, ambiguous aliases,
  missing references, duplicate IDs, bounds errors and cancellation.
- Compare sequence extraction and structural interpretation against independent
  reference outputs for selected loci; include incomplete and noncoding models.
- Reopen old projects and new projects with moved, missing, unchanged and
  modified references. Confirm checksum and alias behavior.
- Verify expression missingness, measured zeros, duplicate detection, exact and
  ambiguous ID matching, and preservation of pre-existing supported formats.
- Nonstandard genetic codes and organellar translation are **deferred** (not
  implemented): translation currently uses the standard nuclear genetic code
  only. Test such cases before claiming correct translation for them. Display
  the genetic code used for translation.
- Report tested species and file conventions. General species independence is
  a design goal; validation claims are limited to tested inputs and features.
