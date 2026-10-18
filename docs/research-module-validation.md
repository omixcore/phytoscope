# Research-module validation on real plant data

## Purpose

This regression suite checks whether selected PhytoScope research-module outputs
match explicit reference calculations on pinned public Arabidopsis data, with a
separate hash-pinned protein-family fixture spanning four plant species. It is
intended to complement, not replace, the four-species annotation-parser
benchmark and the existing isoform/domain-coordinate case.

## Inputs

- Genome: bundled TAIR10 genomic FASTA, SHA-256 recorded in
  `src/main/resources/data/species/arabidopsis/genome-sequence-provenance.json`.
- Annotation: Araport11 chromosome 1 and the frozen four-locus RBCS annotation
  excerpt in `examples/arabidopsis-atlas/annotation.gff3`.
- Expression: Expression Atlas E-MTAB-7978; provenance and source hashes are in
  `examples/arabidopsis-atlas/manifest.json`.
- Cross-species protein family: four mature RBCS sequences from public UniProt
  accessions P10797, Q0INY7, O24574 and P12468, hash-pinned in
  `src/test/resources/research-validation/cross-species-rbcs-mature.faa`; two
  unrelated Arabidopsis controls are extracted from the pinned TAIR10/Araport11 data.

## Checks performed

| Module | Reference calculation | Scope |
|---|---|---|
| Promoter/interval extraction | Test-side coordinate calculation plus direct FASTA slice and independently implemented reverse complement | Eight chromosome-1 loci selected across both strands; 2,000 bp upstream + 200 bp downstream |
| Sequence QC | Test-side GC/base counts, frame, start/stop checks and expected flag reconstruction | Four real RBCS CDS sequences |
| Protein similarity | Test-side Smith–Waterman matrix/traceback compared against identity, aligned length, query/target coverage and reciprocal-best-hit flag | All positive pairwise alignments among four extracted Arabidopsis RBCS-family proteins plus two unrelated controls |
| Cross-species protein similarity | PhytoScope family-similarity classifications compared with NCBI BLASTP 2.12.0+ under shared post-alignment thresholds | Four mature RBCS proteins from Arabidopsis, rice, maize and soybean plus two unrelated Arabidopsis controls; family similarity only, not orthology |
| Expression grouping | Expected mean/median calculated from source-pinned expression values | AT1G01860 leaf-labelled conditions and AT5G38430 measured-zero conditions |
| Domain-to-CDS mapping | Existing independent Python reference check and Java real-annotation regression | 16 annotated domain/deletion records in 13 genes, including a multi-exon junction |

The test writes `target/research-validation/research-module-gold-set.json`
with locus coordinates and sequence hashes, observed QC metrics, all checked
within-family alignment results, the four-species RBCS family panel, and
expression summary values. CI uploads this directory as a validation artifact.
The external BLASTP comparison writes `blastp-comparison.json` with per-pair
classifications and observed runtimes.

## Reproduce

With JDK 21:

```bash
sh mvnw -B -Pui-tests -Dtest=ResearchModuleGoldSetTest test
python tools/check_isoform_case.py
sh mvnw -B -Pui-tests -Dtest=DomainCaseValidationTest test
```

The full validation workflow runs these tests along with parser, project
restoration, UI and packaging regressions.

## Interpretation and limits

These checks compare implementation outputs with reference calculations and
source-pinned values. The RBCS proteins are members of one paralog-rich family;
the within-family alignment exercise validates alignment metrics, **not**
orthology assignments. The added cross-species panel agrees with BLASTP for 12
non-self RBCS-family pairs and eight unrelated-family controls, but it is still
a single-family smoke test with taxonomically unbalanced negatives—not a general
precision/recall estimate. The expression panel has one assay per selected
condition; grouping leaf-labelled conditions is a calculation check, not a
replicate-based differential-expression analysis. The domain example checks
coordinates and retained provenance, not an isoform's function. None of these
tests is a new biological discovery or a user study; broader multi-family and
module-specific gold sets remain necessary.


## Observed results from CI

The [2026-10-18 three-platform validation run](https://github.com/omixcore/phytoscope/actions/runs/39116559963)
passed on Ubuntu, Windows and macOS. The real-data module report recorded:

| Check | Observed result |
|---|---|
| Promoter extraction | 8 loci, 4 on each strand; all 2,200-bp intervals matched direct FASTA slice/reverse-complement reference calculations |
| Sequence QC | 4 real RBCS CDS records; lengths 411, 561, 546 and 546 nt; all in frame, zero ambiguous bases, no QC flags |
| Protein similarity metrics | 4 Arabidopsis RBCS proteins, 16 pairwise hits; identity, alignment length, query/target coverage and reciprocal-best-hit flags matched the test-side reference implementation |
| Cross-species homology vs BLASTP | 4 species; 12/12 non-self RBCS-family pairs and 8/8 unrelated-family controls classified identically by both methods (20/20 agreement); one observed call took 21.63 ms for PhytoScope and 32.4 ms for BLASTP on the Ubuntu runner—too small and noisy for a speed claim |
| Expression grouping | AT1G01860 leaf-labelled conditions: mean = median = 16.0 TPM (n = 3); its low group mean = 2.5 (n = 2); AT5G38430's two measured zero values remain zero (n = 2) |
| Domain mapping | 16 real domain/deletion records across 13 genes were separately checked against independent Python coordinates |

The full JSON, including per-locus coordinates, sequence hashes and all alignment
metrics, is retained in the workflow artifact at
`research-validation/research-module-gold-set.json`. See the separate
[cross-species homology comparison report](../benchmarks/results/2026-10-18/cross-species-homology.md)
for thresholds, observed timing and limitations.

## Known-positive biological context: PIF6

The [PIF6 case note](case-study-pif6-known-positive.md) connects the candidate
`AT3G62090` transcript models to published work on alternative splicing and
seed dormancy. This is a known-positive context only. The current annotation's
`.2/.4` model pair is not yet conclusively matched to the historical constructs,
and this case does not satisfy the independent-new-discovery requirement.
