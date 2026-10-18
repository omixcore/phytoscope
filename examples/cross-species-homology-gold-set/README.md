# Four-species RBCS family-level homology fixture

This small, manually curated fixture tests **cross-species protein-family
similarity classification**, not one-to-one orthology inference. It contains
one mature Rubisco small-subunit (RBCS) protein from each of four reference
plants already represented in the PhytoScope evaluation: Arabidopsis, rice,
maize and soybean.

## Source records and sequence handling

| Species | UniProt accession | Source record length | Mature segment | Fixture length | SHA-256 of mature segment |
|---|---|---:|---:|---:|---|
| *Arabidopsis thaliana* | [P10797](https://www.uniprot.org/uniprotkb/P10797/entry) | 181 aa | 55–181 | 127 aa | `4d34355ca5b0382da6663560c2d07401deeefce886d1c853a0f840942bdafa47` |
| *Oryza sativa* subsp. *japonica* | [Q0INY7](https://www.uniprot.org/uniprotkb/Q0INY7/entry) | 175 aa | 48–175 | 128 aa | `9f3c57c46a58b0beed51b8c163cb58204a4b49fb3706726543a1d06f75bfecf0` |
| *Zea mays* (B73) | [O24574](https://www.uniprot.org/uniprotkb/O24574/entry) | 170 aa | 47–170 | 124 aa | `3654785407b5e88d0af1cb857ef158b8eb4ec75c46dd7c44b19f44c2d7ff4ce8` |
| *Glycine max* | [P12468](https://www.uniprot.org/uniprotkb/P12468/entry) | 178 aa | 55–178 | 124 aa | `aa25d4ec06281b2609b8ab5062ba0482caf5bb4b42291ecdcf413916f4f391a3` |

The FASTA contains the mature-chain segments rather than chloroplast transit
peptides so that the comparison is not dominated by differences in targeting
sequences. Accession IDs and species names are encoded in each FASTA header.
The sequences are pinned locally; CI does not depend on live database access.

Two Arabidopsis proteins from the existing real-data test—NAC001 and histone
H3.1—serve as unrelated-family negative controls. They are extracted from the
pinned TAIR10 genome and Araport11 annotation during the test rather than
duplicated in this fixture.

## Frozen comparison

- Queries: four RBCS sequences, one from each species.
- Targets: the same four RBCS sequences plus the two unrelated Arabidopsis
  controls.
- Shared classification thresholds: identity ≥30%, alignment length ≥20 aa,
  query coverage ≥50%, target coverage ≥50%, and E-value ≤1e-5 for BLASTP.
- Expected non-self comparisons: 12 RBCS-family positives and 8 unrelated-family
  negatives. The four self-pairs are excluded from accuracy calculations.
- External baseline: NCBI BLASTP; the PhytoScope local-alignment implementation
  and BLASTP receive the same sequences and post-alignment thresholds.

## Interpretation boundary

Passing this fixture supports only a small, source-traceable **RBCS family
similarity** check across four species. RBCS is a paralog-rich family; these
four representatives do not constitute a one-to-one orthology gold standard,
a proteome-wide benchmark, or a general sensitivity/specificity estimate.
Performance on this tiny set is a smoke measurement, not a speed-superiority
claim. Broader curated families, difficult negatives, multi-copy families and
independent orthology references remain necessary.
