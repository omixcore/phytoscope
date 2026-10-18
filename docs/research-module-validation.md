# Research-module validation on real plant data

## Purpose

This regression suite checks whether selected PhytoScope research-module outputs
match explicit reference calculations on pinned public Arabidopsis data. It is
intended to complement, not replace, the four-species annotation-parser
benchmark and the existing isoform/domain-coordinate case.

## Inputs

- Genome: bundled TAIR10 genomic FASTA, SHA-256 recorded in
  `src/main/resources/data/species/arabidopsis/genome-sequence-provenance.json`.
- Annotation: Araport11 chromosome 1 and the frozen four-locus RBCS annotation
  excerpt in `examples/arabidopsis-atlas/annotation.gff3`.
- Expression: Expression Atlas E-MTAB-7978; provenance and source hashes are in
  `examples/arabidopsis-atlas/manifest.json`.

## Checks performed

| Module | Reference calculation | Scope |
|---|---|---|
| Promoter/interval extraction | Test-side coordinate calculation plus direct FASTA slice and independently implemented reverse complement | Eight chromosome-1 loci selected across both strands; 2,000 bp upstream + 200 bp downstream |
| Sequence QC | Test-side GC/base counts, frame, start/stop checks and expected flag reconstruction | Four real RBCS CDS sequences |
| Protein similarity | Test-side Smith–Waterman matrix/traceback compared against identity, aligned length, query/target coverage and reciprocal-best-hit flag | All positive pairwise alignments among four extracted RBCS-family proteins |
| Expression grouping | Expected mean/median calculated from source-pinned expression values | AT1G01860 leaf-labelled conditions and AT5G38430 measured-zero conditions |
| Domain-to-CDS mapping | Existing independent Python reference check and Java real-annotation regression | 16 annotated domain/deletion records in 13 genes, including a multi-exon junction |

The test writes `target/research-validation/research-module-gold-set.json`
with locus coordinates and sequence hashes, observed QC metrics, all checked
within-family alignment results and expression summary values. CI uploads this
directory as a validation artifact.

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
orthology assignments. The expression panel has one assay per selected
condition; grouping leaf-labelled conditions is a calculation check, not a
replicate-based differential-expression analysis. The domain example checks
coordinates and retained provenance, not an isoform's function. None of these
tests is a new biological discovery, a user study, or a substitute for curated
cross-species positive/negative gold sets and a matched multi-tool task study.
