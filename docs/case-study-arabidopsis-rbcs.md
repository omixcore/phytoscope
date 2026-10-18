# Arabidopsis RBCS family case

## Question

Do the four Arabidopsis Rubisco small-subunit loci show the same tissue-level
expression pattern, and can their measurements be reviewed alongside their
individual gene models in a recoverable PhytoScope project?

## Reproducible inputs

- Expression Atlas experiment: [E-MTAB-7978](https://www.ebi.ac.uk/gxa/experiments/E-MTAB-7978)
- Source study: Mergner et al. (2020), [doi:10.1038/s41586-020-2094-2](https://doi.org/10.1038/s41586-020-2094-2)
- Reference: TAIR10 genome and Araport11 annotation
- Loci: `AT1G67090` (RBCS1A), `AT5G38410` (RBCS3B), `AT5G38420` (RBCS2B), and `AT5G38430` (RBCS1B)
- Conditions: root, two rosette-leaf samples, cauline leaf, dry seed, and pollen

The bundled matrix contains 32,833 source gene IDs and six selected single-assay
conditions. The builder verifies the pinned source hashes and requires the five
summary components in each source cell to agree before retaining the value. It
does not average, impute, rename identifiers, or infer transcript-level
expression. Exact input and output hashes are recorded in
[`../examples/arabidopsis-atlas/manifest.json`](../examples/arabidopsis-atlas/manifest.json).

## Observed measurements

TPM values below are the source values as bundled. They are descriptive; each
condition has one assay, so the table has no replicate-based uncertainty or
differential-expression test.

| Locus | Root | Rosette leaf 1 | Rosette leaf 8 | Cauline leaf | Dry seed | Pollen |
|---|---:|---:|---:|---:|---:|---:|
| AT1G67090 / RBCS1A | 229 | 9,981 | 17,494 | 7,580 | 13 | 3 |
| AT5G38410 / RBCS3B | 5 | 1,233 | 2,766 | 1,350 | 0.3 | 5 |
| AT5G38420 / RBCS2B | 0.3 | 1,275 | 1,872 | 959 | 0.1 | 0.5 |
| AT5G38430 / RBCS1B | 0.8 | 328 | 357 | 192 | 0 | 0 |

All four loci have higher TPM in the sampled leaf conditions than in root,
seed, or pollen. RBCS1A is the most abundant in all three sampled leaf conditions.
RBCS3B ranks second in rosette leaf 8 and cauline leaf; in rosette leaf 1,
RBCS2B (1,275 TPM) is slightly more abundant than RBCS3B (1,233 TPM). These are
observed ranks, without a statistical test of the differences.

The independent genetic study by Izumi et al. (2012) found that mutations in
RBCS1A and RBCS3B had additive effects on Rubisco accumulation and leaf
photosynthetic capacity ([doi:10.1093/jxb/err434](https://doi.org/10.1093/jxb/err434)).
That study provides functional context for these two loci; it does not validate
the tissue-expression rankings in this atlas subset. Neither the atlas
inspection nor this literature comparison establishes a new biological finding.

## What PhytoScope demonstrates

The example connects the four gene IDs to their chromosome locations, transcript
models, exons, CDS, UTRs, tissue values, comparison figure, exported tables, and
portable project restoration. It gives a concrete test of locus-to-expression
linkage across a highly similar gene family and checks that the ordinary-gene
display is populated from measured values.

The example does **not** establish differential expression, a causal role for
gene structure, new RBCS biology, or an advantage over every existing browser
or expression resource. The atlas values are gene-level estimates and should
not be read as transcript-isoform measurements. A stronger novelty claim needs
a different case with an independently validated result and a direct comparison
against the ordinary multi-tool workflow.

## Reproduction

Follow the commands and validation details in
[`../examples/arabidopsis-atlas/README.md`](../examples/arabidopsis-atlas/README.md).
