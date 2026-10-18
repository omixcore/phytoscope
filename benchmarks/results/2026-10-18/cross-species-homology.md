# Cross-species protein-family comparison (2026-10-18)

## Result

The validation workflow compared PhytoScope's coverage-filtered protein-similarity
hits with NCBI BLASTP 2.12.0+ using the same post-alignment acceptance rules:
identity ≥30%, alignment length ≥20 amino acids, query coverage ≥50%, subject
coverage ≥50%, and E-value ≤1e-5. One HSP per query/subject pair was retained.
Four self-pairs were excluded from the accuracy calculation.

| Panel | Species represented | Positive non-self pairs | Negative-control pairs | PhytoScope positives / negatives | BLASTP positives / negatives | Pairwise agreement |
|---|---:|---:|---:|---:|---:|---:|
| Arabidopsis-only reference panel | 1 | 12 | 8 | 12 / 0 | 12 / 0 | 20/20 (100%) |
| Cross-species mature RBCS panel | 4 | 12 | 8 | 12 / 0 | 12 / 0 | 20/20 (100%) |

The cross-species panel uses mature RBCS family sequences from Arabidopsis
(P10797), rice (Q0INY7), maize (O24574) and soybean (P12468), plus two unrelated
Arabidopsis proteins as negative controls. Exact sequence lengths, mature-chain
boundaries, accessions and SHA-256 digests are recorded in the
[fixture manifest/README](../../../examples/cross-species-homology-gold-set/README.md).

## Timing observed on the Ubuntu CI runner

| Implementation | One observed call | Interpretation |
|---|---:|---|
| PhytoScope local similarity service | 21.63 ms | Four queries against six proteins |
| BLASTP 2.12.0+ | 32.4 ms | Same query/subject sequences and shared classification thresholds |

This is one timing observation on a tiny 6-protein panel. It is sensitive to
process startup, runtime noise and implementation overhead and **must not be
presented as evidence that PhytoScope is faster than BLASTP**. The comparison
measures classification agreement, not an advantage over a tuned full-database
search.

## Failure boundaries and interpretation

- The cross-species fixture covers **one highly conserved, paralog-rich family**.
  It is a family-similarity check, not a one-to-one orthology gold standard.
- The eight negative pairs use two Arabidopsis proteins, so the negative panel
  is not taxonomically balanced.
- The 20 pairwise classifications share query/target sequences and are not
  statistically independent. The 100% agreement is descriptive for this
  fixture, not a general precision/recall estimate.
- The benchmark does not cover remote homologs, multi-domain proteins,
  paralog-rich family disambiguation, low-complexity sequences, fragmented
  proteins, proteome-wide search, or domain-to-genome mapping.
- A broader evaluation still needs multiple independently curated families,
  hard negatives, reference orthology resources where orthology is the target,
  and module-specific domain-mapping and expression-summary gold sets.

## Reproduction

The CI job runs:

```bash
sh mvnw -B -Pui-tests -Dtest=ResearchModuleGoldSetTest test
python tools/compare_homology_blastp.py \
  --report target/research-validation/research-module-gold-set.json \
  --out target/research-validation/blastp-comparison.json
```

The report and per-pair BLASTP rows are retained in the
[three-platform CI artifact](https://github.com/omixcore/phytoscope/actions/runs/39116559963).
The cross-species fixture itself is local and hash-pinned; CI does not query
UniProt at runtime.
