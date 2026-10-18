# Cross-species extraction benchmark results

**CI run:** [Validate and package PhytoScope #156](https://github.com/omixcore/phytoscope/actions/runs/39109020214)  
**Commit:** `ed6c85dd42b2fc839bd826bcb1f9bf31c831a545`  
**Date:** 2026-10-18  
**Comparator:** `gffutils==0.13`  
**Scope:** exact annotation hierarchy extraction, not orthology or functional prediction.

## Correctness outcome

All four source-verified Ensembl Plants release-62 GFF3 excerpts passed on
Ubuntu, Windows and macOS. Each species used three fresh-process trials with
alternating execution order. In every trial, PhytoScope and gffutils emitted
identical gene/transcript/exon/CDS/UTR tuples, and both outputs matched the
independent source-derived inventory with zero missing and zero extra tuples.

| Species | Assembly in pinned manifest | Gene loci in fixture |
|---|---|---:|
| *Arabidopsis thaliana* | TAIR10 | 10 |
| *Oryza sativa* | IRGSP-1.0 | 24 |
| *Zea mays* | Zm-B73-REFERENCE-NAM-5.0 | 20 |
| *Glycine max* | Glycine_max_v2.1 | 40 |

The fixtures contain two annotated genes per nuclear chromosome. They are
multi-species parser regression inputs, **not** whole-genome functional
gold standards. The source URLs, assembly metadata and fixture checksums are
pinned in `src/test/resources/chromosome-validation/manifest.json`.

## Observed median timings

Times are milliseconds, median across three fresh-process trials on each hosted
CI platform. PhytoScope reports annotation import into its in-memory model;
gffutils reports SQLite index construction. These are different operations and
the values must **not** be interpreted as a direct speed ranking.

| Species | Loci | Ubuntu: PhytoScope import | Ubuntu: SQLite index | Windows: PhytoScope import | Windows: SQLite index | macOS: PhytoScope import | macOS: SQLite index |
|---|---:|---:|---:|---:|---:|---:|---:|
| Arabidopsis | 10 | 47.36 | 85.43 | 79.47 | 50.49 | 66.03 | 49.70 |
| Rice | 24 | 36.20 | 127.13 | 60.81 | 33.51 | 35.64 | 18.97 |
| Maize | 20 | 44.58 | 59.55 | 63.44 | 35.39 | 35.16 | 17.96 |
| Soybean | 40 | 55.30 | 70.99 | 89.38 | 70.75 | 52.36 | 40.77 |

Hosted-runner performance varies with operating system, file system, Java/Python
runtime and concurrent load. These measurements are reproducibility/smoke data,
not controlled hardware benchmarking. Use the per-trial CI artifact for the
complete runtime metadata and raw measurements.

## Full-annotation source-to-model checks

The same Ubuntu CI run separately checked eight source/fixture inventories:

| Dataset | Exact tuples | Missing | Extra |
|---|---:|---:|---:|
| Arabidopsis bundled annotation | 811,827 | 0 | 0 |
| Arabidopsis full release-62 annotation | 791,557 | 0 | 0 |
| Maize source excerpt | 377 | 0 | 0 |
| Maize full annotation | 1,161,691 | 0 | 0 |
| Rice source excerpt | 347 | 0 | 0 |
| Rice full annotation | 550,921 | 0 | 0 |
| Soybean source excerpt | 839 | 0 | 0 |
| Soybean full annotation | 1,324,199 | 0 | 0 |
| **Total** | **4,641,758** | **0** | **0** |

The comparison covers explicit parent edges, strand and exact 1-based inclusive
coordinates for recognized gene, transcript, exon, CDS and UTR features.
Unsupported feature types are counted and disclosed by the validator. This
result does not claim validation of the unsupported types or of biological
interpretation.

## CI and interpretation

- Ubuntu, Windows and macOS jobs: completed successfully.
- Java/JavaFX test suite: 130 tests, zero failures and zero errors on each
  platform; one test was skipped in the standard suite because it requires the
  full-annotation directory.
- The dedicated Ubuntu full-annotation validation then ran four tests with
  zero failures/errors/skips, and all eight source inventories passed.
- Standalone application packaging completed on all three platforms.

## Remaining limitations

This comparison validates parser/hierarchy extraction only. It does not measure
homology precision/recall, orthology inference, domain-to-CDS accuracy on a
curated multi-species gold set, promoter-sequence correctness against an
independent genome tool, expression-summary correctness on a multi-replicate
gold set, native file-dialog usability, or researcher task completion. Those
need separate matched-input benchmarks. No overall superiority claim is made.
