# Research-module validation results — 2026-10-18

**Repository:** `omixcore/phytoscope`  
**Latest validation run:** [GitHub Actions #176](https://github.com/omixcore/phytoscope/actions/runs/39115541048)  
**Earlier calculation baseline:** [GitHub Actions #39110969072](https://github.com/omixcore/phytoscope/actions/runs/39110969072)  
**Platforms:** Ubuntu, Windows and macOS; all jobs passed.

## Real-data calculation checks

| Module | Inputs and reference | Result |
|---|---|---|
| Promoter/interval extraction | TAIR10 Chr1; direct 1-based closed FASTA slicing and independent reverse-complement calculation | 8 loci matched exactly: 4 plus-strand and 4 minus-strand loci, each 2,200 bp |
| Sequence quality | Four Araport11 RBCS CDS models; test-side base-count/frame/start/stop oracle | All 4 matched expected metrics; lengths 411, 561, 546 and 546 nt; zero ambiguous bases and no flags |
| Protein similarity | Four extracted RBCS proteins; test-side Smith–Waterman reference | All 16 positive within-family pairs matched identity, alignment length, query/target coverage and reciprocal-best-hit flags |
| Expression grouping | Expression Atlas E-MTAB-7978 source values | AT1G01860 leaf-labelled conditions: mean and median 16.0 TPM across 3 observations; low group mean 2.5 across 2 observations; AT5G38430 retained two measured zero values with mean 0 |
| Domain mapping | 16 real domain/deletion records in 13 genes | Coordinates separately checked by the independent Python validator and Java real-data regression |
| Independent promoter baseline | Eight TAIR10 strand-aware 2,200-bp intervals; bedtools v2.31.1 `getfasta -s` | 8/8 exact sequence matches; 0 mismatches; observed tool call 0.107 s for this tiny set only |
| Independent protein-similarity baseline | Four Arabidopsis RBCS proteins and two unrelated-family controls; BLASTP 2.12.0+, one HSP per pair and shared thresholds (identity ≥30%, alignment ≥20 aa, query/subject coverage ≥50%, E-value ≤1e-5) | Excluding 4 self-pairs: 12/12 non-self family pairs and 8/8 negative-control pairs agree with PhytoScope (20/20 classifications); no negative-control hits |

The detailed machine-readable report, including per-locus coordinates, sequence
hashes and all pairwise alignment metrics, is retained in the CI artifact at
`research-validation/research-module-gold-set.json`.

## UI and project workflow

The JavaFX regression exercised the visible controls for promoter extraction,
sequence QC, sample-group assignment and summary, protein comparison, and
domain-to-genome mapping. It also verified that the domain tab reports genomic
blocks rather than merely echoing the submitted transcript identifier. Existing
regressions cover project save, data replacement, restoration, and the six
tissue-expression bars. The same CI run passed on all three operating systems.

## Boundaries

- The four proteins are members of one paralog-rich RBCS family. These alignment
  checks validate metrics and candidate-ranking flags, **not orthology**.
- Expression values are single-assay conditions. The grouping check validates
  arithmetic and zero/missing handling, **not differential expression**.
- The domain mapping screen contains existing annotated transcript models.
  Independent peptide validation still yielded zero candidates meeting the
  predeclared two-study/two-peptide threshold.
- The PIF6 locus has prior published alternative-splicing/seed-dormancy biology,
  but exact equivalence of the current `AT3G62090.4` model to historical
  constructs remains unresolved. It is a known-positive context, not a new
  discovery.
- The bedtools comparison contains only eight intervals on one chromosome;
  the BLASTP comparison is a small same-species family discrimination test.
  Neither establishes broad accuracy, orthology, or genome-wide sensitivity/
  specificity.
- These results do not establish a human usability/time-saving benefit or
  superiority over all ordinary workflows. Curated multi-species domain/homology
  benchmarks, a multi-replicate expression gold set, a counterbalanced user
  study, and a new independently validated biological finding remain open.
