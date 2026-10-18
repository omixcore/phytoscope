# Validation and benchmarks

## Claims and evidence

PhytoScope is a local gene-structure and expression comparison workflow. Sequence
adaptation and standard-format parsing are correctness requirements, not evidence
of a novel biological algorithm. Selected loci are supplied by the user; the
software does not infer orthology, splicing changes or causal mechanisms.

The validation layers are deliberately distinct:

1. **Source identity:** fixed assembly, annotation release and source checksums.
2. **Model correctness:** independent Python comparison of explicit GFF3 parent
   edges, strand and exact exon/CDS/UTR coordinates against the Java inventory.
   Unsupported feature types are counted separately. CDS/UTR segments without an overlapping exon are retained separately and
   are not converted into invented exon annotations.
3. **Application behavior:** real JavaFX controls, species replacement, bounded
   browser tracks, expression matching and missingness, comparison PNG/SVG/TSV,
   project save → replace data → restore original state, and tamper rejection.
4. **Measured performance:** process/environment metadata, import time, peak
   heap-pool usage, sequence selection, search median/p95, comparison layout and
   SVG writing. Heap is not RSS, and headless tests do not measure native dialog
   behavior or hardware-dependent zoom latency.

Run the normal suite with `sh mvnw -Pui-tests verify`. Full annotation tests
require the explicit `-Dphytoscope.fullAnnotationDir=/path/to/cache` parameter;
a skipped full-archive test is not a successful full-archive validation.

## Independent coordinate checks and performance

```bash
python tools/run_validation.py --jar target/phytoscope-0.2.0.jar
# Optional: add --full-dir /path/to/cache containing arabidopsis/rice/maize/soybean.gff3.gz
```

The default run covers the complete bundled Araport11 Chr1–Chr5 and the three
other bundled source excerpts. Every run creates `performance.json`, the complete
`parsed-structure.tsv`, an SVG and `structure-validation.json`. Use fresh JVMs
for multiple cold-import measurements. Preserve raw results, versions, input
hashes, memory limits and machine information; avoid claims based only on the
fastest run. `benchmarks/results` contains the measurements actually collected
for this change; it does not substitute for testing additional hardware.

## Same-data extraction baseline

`gffutils==0.13` is an independent disk-backed GFF hierarchy implementation.
The accompanying baseline extracts the **same source loci and coordinate tuples**:

```bash
python -m pip install gffutils==0.13
python tools/compare_gffutils.py --annotation examples/arabidopsis-atlas/annotation.gff3 \
  --loci AT1G67090 AT5G38410 AT5G38420 AT5G38430 --out target/gffutils
python tools/validate_structure_inventory.py \
  --source examples/arabidopsis-atlas/annotation.gff3 \
  --parsed target/gffutils/parsed-structure.tsv --out target/gffutils/correctness.json
```

The baseline measures index construction separately from warm queries. Java
keeps its model in memory; gffutils constructs an SQLite index. Report their
storage/design differences alongside timings. This comparison establishes
structure-extraction agreement; it does not establish superiority over genome
browsers or compare interactive usability. Source documentation:
https://daler.github.io/gffutils/ and https://github.com/daler/gffutils.

## Browser/user-task study protocol

A publication-level usability comparison still requires actual users or a fully
recorded repeatable desktop task. Do not fill unmeasured cells with estimates.
Use identical source files, gene IDs, output requirements and machine conditions.
Record tool versions, setup separately from task time, prior experience, success,
errors, and whether external editing was needed. Randomize tool order across
participants. Assess these tasks separately:

| Task | Required output | Acceptance criterion |
| --- | --- | --- |
| Find loci on distinct sequences | Exact selected IDs | All requested loci, correct reference |
| Inspect isoforms | Exon/CDS/UTR coordinates | Agreement with independent inventory |
| Associate expression | Values and sample order | Exact ID matches; missing distinct from zero |
| Prepare comparison figure | Editable structure/expression figure | Correct orientation, units and shared scale |
| Resume the analysis | Restored project state | Same inputs, IDs, view and parameters |

No human task-time study or native Windows/macOS dialog evaluation is claimed by
this repository's automated evidence. Those results require access to actual
participants and target machines.


## Four-species matched extraction benchmark

The CI workflow also runs `tools/run_cross_species_extraction_benchmark.py`
against the source-verified release-62 annotation excerpts for Arabidopsis,
rice, maize and soybean. It uses the same source file and selected loci for
PhytoScope and `gffutils==0.13`, runs at least three fresh-process trials with
alternating execution order, compares exact gene/transcript/exon/CDS/UTR tuples,
and independently checks both outputs against the source inventory. Raw
per-trial timing and correctness reports are retained in the
`cross-species-extraction` CI artifact.

The fixtures sample two annotated genes per nuclear chromosome. They are useful
for multi-species parser/query regression but are **not** whole-genome functional
gold standards. PhytoScope in-memory import time and SQLite index construction
time are reported separately; they are not used to assert general speed
superiority. This benchmark does not validate homology, domain prediction,
promoter function, or researcher usability.

## Researcher-value study

The protocol and blank data-collection template are in
[researcher-value evaluation](researcher-value-study.md) and
`benchmarks/researcher-value/task-log-template.tsv`. These files define a
paired, counterbalanced comparison, task acceptance criteria, provenance scoring
and reporting rules. They are instruments for a future study, **not user-study
results**. No human task times or usability scores are claimed until actual
participants complete the study.


## Recorded multi-platform result (2026-10-18)

The exact observed timings, source-inventory counts and test outcomes for
[CI run #156](https://github.com/omixcore/phytoscope/actions/runs/39109020214)
are preserved in
[the cross-species benchmark report](../benchmarks/results/2026-10-18/cross-species-extraction.md).
All three operating-system jobs passed. The timings are runner-specific
observations, not a speed-superiority claim; the report distinguishes
PhytoScope in-memory import from SQLite index construction.


## Independent module-level tool checks (CI run #192)

The Ubuntu job runs two additional comparisons, with machine-readable results
retained in the `validation-ubuntu-latest` artifact under
`research-validation/`:

- **Promoter sequence extraction:** PhytoScope's eight pinned TAIR10 promoter
  intervals are compared with `bedtools getfasta -s` (bedtools v2.31.1).
  BED conversion is explicit (1-based closed start becomes start minus one;
  the end coordinate is unchanged). All 8 sequences matched exactly, with
  zero mismatches. The observed 0.107-second bedtools call is for only eight
  intervals on one chromosome and is not a comparative performance result.
- **Protein-similarity candidates:** the four extracted Arabidopsis RBCS
  proteins and two annotated unrelated-family controls are compared with
  NCBI BLASTP 2.12.0+ using one HSP per pair and the same acceptance thresholds
  as PhytoScope: identity at least 30%, alignment at least 20 amino acids,
  query and subject coverage at least 50%, and E-value at most 1e-5. Four
  trivial self-pairs are excluded from the classification denominator. All
  12 non-self within-family pairs and all 8 negative-control pairs agreed
  (20/20 pairwise classifications; no negative-control hits).
- **Cross-species protein-family check:** the same comparison is repeated for
  hash-pinned mature RBCS sequences from Arabidopsis, rice, maize and soybean,
  plus the two unrelated Arabidopsis controls. BLASTP and PhytoScope agree on
  12/12 non-self RBCS-family pairs and 8/8 negative controls (20/20
  classifications). One Ubuntu-runner call measured 21.63 ms for PhytoScope and
  32.4 ms for BLASTP; this tiny single-call result is not a speed claim. See the
  [four-species homology report](../benchmarks/results/2026-10-18/cross-species-homology.md)
  and [frozen fixture](../examples/cross-species-homology-gold-set/README.md).

These remain small correctness smoke tests. The cross-species panel covers one
highly conserved paralog-rich family, uses taxonomically unbalanced negatives,
and is not an orthology benchmark or a general sensitivity/specificity estimate.
It does not replace multi-family/domain gold sets, a replicate-based expression
benchmark, controlled hardware measurements, an actual researcher task study,
or an independently validated new biological discovery.
