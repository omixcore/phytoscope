# PIF6 alternative splicing: known-positive literature control

## Why this case is included

This is a **known-positive control**, not a new biological discovery. It checks
whether PhytoScope can keep an annotated isoform/domain hypothesis connected to
published functional evidence without presenting a previously reported result
as novel.

## Frozen annotation observation

The independent isoform screen contains candidate `DI0006` at
`AT3G62090` (PIL2/PIF6):

- Longer model: `AT3G62090.2`, 363 amino acids.
- Shorter model: `AT3G62090.4`, 341 amino acids.
- In-frame deletion: amino acids 199–220 (22 aa).
- Annotated bHLH interval in the longer protein: amino acids 188–237.
- The deletion overlaps 22 of the 50 annotated domain residues (44%).
- Genomic deletion block in the frozen model: Chr3:22,989,726–22,989,791
  (1-based closed; minus strand).

These are annotation/model observations from the frozen screen, not a direct
measurement of the protein in planta.

## Independent published biological evidence

Penfield, Josse and Halliday (2010) reported two PIF6 splice variants and found
that overexpression of the shorter `PIF6-β` form, but not the full-length
`PIF6-α` form, reduced primary seed dormancy. The short form was described as
lacking the bHLH DNA-binding function.

A later study in *The Plant Cell* (2012) showed that PTB1/PTB2 regulate a PIF6
cassette-exon splicing event, detected the expected protein products from
expressed variants, and linked altered PIF6 splicing to ABA-dependent seed
germination. A later study of light-induced seed germination also summarizes
the existence of four PIF6 alternative-splicing isoforms and the domain-loss
variant.

Sources:

- Penfield S, Josse E-M, Halliday KJ. *A role for an alternative splice variant
  of PIF6 in the control of Arabidopsis primary seed dormancy*. Plant Molecular
  Biology. 2010;73:89–95. https://doi.org/10.1007/s11103-009-9571-1
- *Polypyrimidine tract binding protein homologs from Arabidopsis are key
  regulators of alternative splicing with implications in fundamental
  developmental processes*. The Plant Cell. 2012;24:4360–4375.
  https://doi.org/10.1105/tpc.112.103622
- *Alternative Splicing Regulation During Light-Induced Germination of
  Arabidopsis thaliana Seeds*. Frontiers in Plant Science. 2019.
  https://doi.org/10.3389/fpls.2019.01076

## Critical transcript-identity caveat

The frozen `AT3G62090.2/.4` pair is an in-frame 22-aa deletion. The 2010 paper
describes its `PIF6-β` construct in terms of a cryptic intron/splice event and
a predicted premature stop, while the 2012 paper describes an in-frame
cassette-exon event and additional PIF6 transcript variants. These historical
labels are not automatically interchangeable with current Araport11 transcript
IDs. The current evidence supports a **locus-level known biological context**;
it does not yet prove that `AT3G62090.4` is sequence-identical to the exact
historical construct whose seed-dormancy effect was tested. That equivalence
must be resolved from transcript/CDS sequence and construct boundaries before
making an isoform-specific causal claim.

## What this demonstrates—and what it does not

PhytoScope's frozen screen exposes a candidate deletion that overlaps a known
functional domain, and the locus has published evidence connecting alternative
splicing with seed germination. This is a useful test of traceability from gene
model to domain context to literature. It does **not** demonstrate that
PhytoScope discovered the PIF6 biology, that the exact current transcript model
has the published phenotype, that the result is unavailable through ordinary
tools, or that researchers save time using this interface.

## Reproduce the software-side checks

```bash
python tools/check_isoform_case.py
sh mvnw -B -Pui-tests -Dtest=DomainCaseValidationTest test
```

The first command verifies the frozen candidate coordinates and validation
tables; the second runs the real-annotation domain mapping regression. These
commands validate the software-side structure, not the historical construct
identity or biological function.
