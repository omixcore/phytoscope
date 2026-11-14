# Arabidopsis CYP71A medium-conservation family-level homology fixture

This small, manually curated fixture tests **medium-conservation protein-family
similarity classification**, not one-to-one orthology inference. It contains
four full-length cytochrome P450 subfamily 71A (CYP71A) proteins from
*Arabidopsis thaliana*. It complements the high-conservation four-species RBCS
fixture so that the Homology module is checked across two conservation levels.

## Why CYP71A is "medium conservation"

CYP71A is a single-domain enzyme family whose fold is conserved over the whole
~490-500 aa sequence, but whose pairwise amino-acid identity (~50-60%) is clearly
lower than the high-conservation RBCS family (~70-80% across the four reference
species). Both families pass the shared post-alignment thresholds, so the
difference is a conservation-level contrast, not a threshold-boundary artifact.

## Source records and sequence handling

| Species | UniProt accession | Gene | Length | SHA-256 of fixture sequence |
|---|---|---|---:|---|
| *Arabidopsis thaliana* | [O49340](https://www.uniprot.org/uniprotkb/O49340/entry) | CYP71A12 | 497 aa | `99caa61beeceaee5a875e4ea8a99639eab1c5c0a5d6e5f8c1697c00f0200a4de` |
| *Arabidopsis thaliana* | [Q9FH66](https://www.uniprot.org/uniprotkb/Q9FH66/entry) | CYP71A16 | 497 aa | `17bc76e9a792562f1f06a620aa42d2ac2a9ff4d9249b41dfc6a6a37e60f0940c` |
| *Arabidopsis thaliana* | [Q9T0K0](https://www.uniprot.org/uniprotkb/Q9T0K0/entry) | CYP71A19 | 490 aa | `30a757cb510a18ac43110aa1220db4250abb0b3a1d8572b4fb3bac698e597c33` |
| *Arabidopsis thaliana* | [Q9STL2](https://www.uniprot.org/uniprotkb/Q9STL2/entry) | CYP71A21 | 490 aa | `46004d275640258162484ecc965ae46b4f54a2c27f80154b06ef15914ef8ce39` |

Full-length sequences are used (CYP71A has no cleavable targeting peptide that
would dominate the comparison, unlike the RBCS chloroplast transit peptide).
Accession IDs, species name and gene symbols are encoded in each FASTA header.
The sequences are pinned locally; CI does not depend on live database access.

Two Arabidopsis proteins from the existing real-data test—NAC001 and histone
H3.1—serve as unrelated-family negative controls. They are extracted from the
pinned TAIR10 genome and Araport11 annotation during the test rather than
duplicated in this fixture.

## Frozen comparison

- Queries: four CYP71A sequences from *Arabidopsis thaliana*.
- Targets: the same four CYP71A sequences plus the two unrelated Arabidopsis
  controls.
- Shared classification thresholds: identity ≥30%, alignment length ≥20 aa,
  query coverage ≥50%, target coverage ≥50%, and E-value ≤1e-5 for BLASTP.
- Expected non-self comparisons: 12 CYP71A-family positives and 8
  unrelated-family negatives. The four self-pairs are excluded from accuracy
  calculations.
- External baseline: NCBI BLASTP; the PhytoScope local-alignment implementation
  and BLASTP receive the same sequences and post-alignment thresholds.

## Interpretation boundary

Passing this fixture supports only a small, source-traceable **CYP71A family
similarity** check in one species. It does not constitute a one-to-one
orthology gold standard, a proteome-wide benchmark, or a general
sensitivity/specificity estimate. The RBCS panel and this CYP71A panel together
span two conservation levels, but both remain tiny curated fixtures, and
broader multi-family gold sets remain necessary.
