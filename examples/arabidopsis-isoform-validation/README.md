# Arabidopsis domain-deletion isoforms: independent validation attempt

**Outcome: no independently validated new biological discovery.** A fixed-rule
genome-wide screen produced 16 domain-deletion records in 13 genes. One CIPK15
junction has eligible peptide evidence from one original study, below the
required two-study/two-peptide threshold. All failures remain in `validation.tsv`.
This is a reproducible candidate assessment, not a positive discovery claim.

## Question and separation of evidence

Do annotated internal deletions remove a substantial part of a protein domain,
and is translation across the resulting junction supported by independent
proteomics projects? `protocol.json` records analyst-defined rules before this
screen and peptide inspection; it is not an externally registered preregistration.

Discovery uses the bundled TAIR10 genome, full Araport11 Chr1–Chr5 annotation,
and a snapshot of 16,421 reviewed Arabidopsis UniProt records. Complete CDSs must
start with ATG, terminate with a stop, have consistent phases, and contain no
internal stop or ambiguous codon. The canonical translation must exactly match
the UniProt sequence digest. The shorter isoform must differ by one unambiguous
internal deletion of at least 10 residues, removing at least 10 residues and 25%
of a domain, with at least 10 identical residues on either side.

Validation uses published processed peptide identifications from
[Reyes et al. (2026), NAR, gkag400](https://doi.org/10.1093/nar/gkag400),
Supplemental Table 2, sheet `2 Isoform specific peptides` (CC BY 4.0).
This source combines new experiments with the original acquisitions of
[Mergner et al. (2020)](https://doi.org/10.1038/s41586-020-2094-2).
These are separate biological acquisitions but share the Reyes computational
reprocessing. Validation requires an eligible junction peptide from each source
and at least two distinct peptides overall. Isoleucine/leucine are treated as
indistinguishable; matches to other annotated genes disqualify a peptide.

| Stage | Result |
| --- | ---: |
| Complete CDS models screened | 48,002 |
| Incomplete/noncanonical CDS models excluded from discovery | 4,058 |
| Background models for peptide specificity, including partial CDS | 52,060 |
| Canonical-matched genes with multiple complete isoforms | 6,538 |
| Ambiguous deletion pairs excluded | 285 |
| Candidate domain records / genes | 16 / 13 |
| Published peptide rows for all candidate genes | 26 |
| Eligible junction peptides | 1 |
| Candidates meeting independent validation | 0 |

Background models include empty translations where no CDS is available; this
does not constitute a complete search of unannotated proteins or contaminants.

## CIPK15: useful lead, insufficient validation

AT5G01810.3 encodes a 376-residue annotated sequence, compared with 421 residues
for the canonical AT5G01810.1 / UniProt P92937 sequence. An exact internal deletion
removes canonical residues 284–328, covering the entire annotated NAF domain
(299–323). The kinase domain (12–266) is retained in the sequence.

| Feature | Protein coordinates, 1-based inclusive | Genomic coordinates, 1-based inclusive |
| --- | --- | --- |
| Deleted segment, canonical isoform | 284–328 | Chr5:311309–311443 (+) |
| NAF, canonical isoform | 299–323 | Chr5:311354–311428 (+) |
| Junction peptide, short isoform | 276–287 | Chr5:311285–311308; Chr5:311444–311455 (+) |

The peptide `ESVEEETEEEMR` is reported at Excel row 24784, FragPipe search,
group `FL` (flower, stage 15), originating from the Mergner study. It crosses
the deletion junction, is absent from the canonical sequence and matches no
other gene in the reconstructed annotation background. It does **not** have
support from the new Reyes experiments and is only one distinct peptide.

NAF-mediated CBL interaction is annotated in the pinned `P92937.json`; loss of
that sequence suggests a testable interaction hypothesis. No CBL-binding assay,
kinase assay, stress-response experiment, intact-protein validation or causal
functional effect was performed here. The annotated isoform and the peptide
identification predate this analysis. Neither is a newly discovered sequence.
See [literature audit](literature-audit.md) for the limits of novelty checks.

## Reproduce and inspect

From the repository root, Python 3.10+ with the standard library is sufficient:

```bash
python tools/test_isoform_case.py
python tools/check_isoform_case.py
```

The second command verifies pinned input hashes, reruns the full screen,
reconstructs specificity sequences and compares every candidate and validation
table with the frozen outputs. It uses the complete 26-row candidate-gene subset
of the original 32,110-row peptide sheet; no selected candidate rows are omitted.
Generated caches and reports go to `target/independent-isoforms/`.

To verify against the original publisher archive as well:

```bash
python tools/fetch_isoform_validation_sources.py
python tools/validate_domain_isoforms.py \
  --case examples/arabidopsis-isoform-validation \
  --proteins target/independent-isoforms/specificity-proteins.json \
  --archive target/independent-isoforms/gkag400_supplemental_files.zip \
  --out target/independent-isoforms/full-archive-validation
```

The downloader rejects changed archive hashes. `sources.json` records original
URLs, checksums, transformations and source independence. The compact UniProt
snapshot retains every record and domain feature, replacing sequences with
their SHA-256 digests and lengths; screening requires exact translation matches.

Open `annotation.gff3` in PhytoScope, select `AT5G01810`, and enter these tab-delimited
rows in the research workspace's Domains tab:

```tsv
AT5G01810.1	NAF	299	323
AT5G01810.3	Junction peptide (single-study evidence)	276	287
```

Exact transcript IDs are accepted because the source annotation lacks protein
IDs. Gene IDs are not expanded across isoforms. Run the mapping, export the TSV,
and save the project to preserve input and result. The Java regression
`DomainCaseValidationTest` checks all 16 domain mappings and their translations
against the Python results, the two-block CIPK15 peptide coordinates, and project
restoration. Its outputs include a portable project and analysis TSV under
`target/research-validation/`.

## What this case establishes and what remains

The independent Python calculation provides a coordinate baseline for the
application, not a speed or usability comparison with IGV or another tool.
There is no measured advantage over a well-designed manual workflow. Dedicated
multi-species gold sets, timings and a matched task comparison remain necessary.

The source identifications were not re-searched against raw spectra here.
Peptide-level evidence does not establish an intact isoform; lack of detection
does not establish absence. The fixed rules intentionally miss substitutions,
terminal truncations, ambiguous repeat-mediated deletions and noncanonical CDSs.
The next biological test would require independent junction evidence and an
isoform-specific functional experiment. No threshold was relaxed to turn the
current negative validation result into a positive result.


## PIF6 locus context: candidate-specific validation rejected

Published PIF6-beta work is a useful example of why locus-level literature
cannot be transferred automatically to every annotated isoform. The frozen
`AT3G62090.4` candidate is 341 aa with an internal 22-aa deletion; the published
PIF6-beta sequence record is 188 aa (with a 182-aa construct reported in the
literature). Exact sequence identity fails, and the candidate retains downstream
C-terminal sequence. The four protein-coding models in the frozen GFF3 are
346, 363, 324 and 341 aa; none matches the 182/188-aa published beta lengths.
Therefore DI0006 is **not** a validated positive control
for the published dormancy phenotype. Reproduce the independent identity gate
with `python tools/validate_pif6_isoform_identity.py`; see the
[PIF6 case note](../../docs/case-study-pif6-known-positive.md) and
[expanded literature audit](literature-audit.md).
