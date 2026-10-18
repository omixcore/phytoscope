# PIF6 literature context: the frozen candidate is not the validated beta isoform

## Decision

**Do not treat candidate DI0006 as a known-positive validation of the published
PIF6-beta phenotype.** The locus has well-established alternative-splicing
biology, but exact sequence reconciliation shows that the frozen
Araport11 `AT3G62090.4` candidate is not the published PIF6-beta construct.
This is a negative biological identity check, not a new discovery.

## Frozen candidate and reproducible sequence check

The fixed-rule screen contains candidate `DI0006` at `AT3G62090`
(PIL2/PIF6):

- Canonical model `AT3G62090.2`: 363 amino acids.
- Short model `AT3G62090.4`: 341 amino acids.
- In-frame internal deletion: amino acids 199–220 (22 aa).
- Annotated bHLH interval: amino acids 188–237; the deletion overlaps 22/50
  domain residues (44%).
- The model retains the sequence downstream of this internal deletion.

The published PIF6-beta literature describes a short form of about 182 amino
acids, while the pinned UniProtKB sequence record `D0EDQ7` is 188 amino acids
(GenBank protein `ACX35553.1`, transcript `GQ477368`). The 341-aa
`AT3G62090.4` protein is not an exact match to that sequence and retains a
long C-terminal region after its internal deletion. A locus name and domain
overlap are therefore insufficient to transfer the published phenotype to
this candidate.

The pinned sequence is stored in
`examples/arabidopsis-isoform-validation/published-pif6-beta.fasta`, with
source metadata and a SHA-256 digest in the adjacent JSON file. Reproduce the
identity check with:

```bash
python tools/validate_pif6_isoform_identity.py
```

The script verifies the published-sequence digest, confirms that the frozen
candidate reconstructs from its stated 22-aa deletion, and records that exact
sequence equivalence is false. Its machine-readable output is retained by CI
as `pif6-identity-validation.json`.

## What the independent literature establishes

Penfield, Josse and Halliday (2010) reported that overexpression of their
short PIF6-beta form, but not full-length PIF6-alpha, reduced primary seed
dormancy. Later work connected PTB1/PTB2-regulated PIF6 splicing to germination.
Those results establish biological importance for the **published PIF6 splice
variant**, not for every shorter model at the same locus.

Sources:

- Penfield S, Josse E-M, Halliday KJ. *A role for an alternative splice variant
  of PIF6 in the control of Arabidopsis primary seed dormancy*. Plant Molecular
  Biology. 2010;73:89–95. https://doi.org/10.1007/s11103-009-9571-1
- *Polypyrimidine tract binding protein homologs from Arabidopsis are key
  regulators of alternative splicing with implications in fundamental
  developmental processes*. The Plant Cell. 2012;24:4360–4375.
  https://doi.org/10.1105/tpc.112.103622
- *Structural insight into PIF6-mediated red light signal transduction of plant
  phytochrome B*. Cell Discovery. https://doi.org/10.1038/s41421-025-00802-3
- UniProtKB D0EDQ7, PIF6 splice variant:
  https://www.uniprot.org/uniprotkb/D0EDQ7/entry

## Publication boundary

PIF6 must be treated as **locus-level literature context**, not as a validated
positive control for DI0006 and not as a PhytoScope discovery. The candidate
could still have a distinct biological effect, but this requires direct
isoform-specific evidence and experiments; the current evidence does not
establish that effect. No candidate in the current 13-gene screen has met the
predeclared independent peptide threshold for a new discovery.
