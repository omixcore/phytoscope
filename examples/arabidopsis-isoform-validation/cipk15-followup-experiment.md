# CIPK15 independent-validation protocol (prospective)

## Status and claim boundary

**Status: not yet performed.** This protocol is a preregisterable plan for
testing the only current protein-level lead in the frozen screen. It must not
be cited as evidence that the isoform is translated, functionally distinct, or
newly discovered.

## Biological hypothesis

The frozen model `AT5G01810.3` encodes a 376-aa CIPK15 isoform compared with the
421-aa canonical model `AT5G01810.1`. The predicted 45-aa deletion
(canonical residues 284–328) removes the complete annotated NAF domain
(residues 299–323) while leaving the annotated kinase domain (residues 12–266)
outside the deleted interval. UniProtKB P92937 describes NAF as the regulatory
CBL-binding element of CIPK proteins and lists CIPK15 interactions with CBL1
and CBL4. The testable hypothesis is that, **if the shorter isoform is expressed
as a stable protein**, it has reduced CBL-dependent regulation compared with
the canonical protein.

This is a domain-based hypothesis, not a demonstrated phenotype. The transcript
is already annotated; its existence is not a new transcript discovery.

## Evidence available before the experiment

- The source audit contains one eligible junction peptide:
  `ESVEEETEEEMR`, short-protein positions 276–287, mapped only to
  `AT5G01810.3` by FragPipe.
- The source table labels the group `FL` and records workbook row 24784.
  **Resolve the meaning of `FL` in the original sample metadata before choosing
  tissue or making tissue-specific claims.**
- This peptide comes from the Mergner acquisition included in the Reyes et al.
  processed resource. It is one peptide from one biological source. It is not
  a second independent study, and the raw spectra have not been re-searched.
- The peptide is not described in the source article as a functional discovery.
  No isoform-specific interaction, kinase-activity, or phenotype experiment has
  been performed.

## Stage 1 — transcript identity

1. Retrieve the original sample metadata and resolve the `FL` group label.
2. Use at least three independent biological samples from the relevant tissue
   or condition. Include a common CIPK15 amplicon and an isoform-specific
   amplicon whose primer crosses the predicted splice junction; include
   no-RT and no-template controls.
3. Sequence the isoform-specific amplicon by Sanger sequencing or an equivalent
   junction-resolving method.

**Pass criterion:** the predicted junction sequence is confirmed in at least
two of three biological samples, with clean negative controls. A transcript
pass validates the RNA structure only; it does not establish translation.

## Stage 2 — independent protein evidence

1. Reanalyse the original raw file independently if it is accessible, retaining
   spectrum-level evidence and search parameters. Treat this as an independent
   computational reanalysis of the same acquisition, not as a second biological
   study.
2. Acquire a new targeted PRM experiment with a stable-isotope-labelled
   `ESVEEETEEEMR` standard and at least two common CIPK15 peptides as protein
   controls. Predefine retention-time tolerance, diagnostic fragment ions,
   peptide-level false-discovery threshold, and replicate requirements before
   examining the result.
3. For the targeted assay, require co-elution with the heavy standard, at least
   three interpretable fragment ions including two sequence-discriminating
   ions, peptide-level FDR at most 1%, and detection in at least two independent
   biological samples. If tryptic digestion cannot produce a second distinct
   junction-spanning peptide, evaluate an alternative protease prospectively;
   do not silently relax the predeclared two-peptide/two-study discovery
   criterion.

**Pass criterion:** the junction peptide is confirmed by the targeted assay
with the prespecified spectral and replicate rules. A PRM pass supports the
peptide sequence in the new acquisition, not the intact 376-aa protein or a
functional effect. If no peptide is detected, report “not detected under these
assay conditions,” not “the isoform is absent.”

## Stage 3 — isoform-specific function

Only proceed after Stage 1 and adequate protein-expression controls. Express
canonical CIPK15.1 and CIPK15.3 in matched backgrounds and verify comparable
protein abundance. Compare CBL1/CBL4 interaction using co-immunoprecipitation
or a validated orthogonal interaction assay; measure kinase activity or a
validated downstream readout. Include empty-vector, input, expression and
interaction-positive controls.

Before unblinding, set the minimum biologically meaningful effect from pilot
assay variance and specify the primary endpoint, normalization, biological
replicates and statistical model. Require a reproducible isoform-dependent
effect with a confidence interval excluding no effect across independent
biological replicates. Do not infer salt tolerance or another organismal
phenotype from loss of the NAF domain alone; a phenotype claim requires a
separate, appropriately controlled experiment.

## Decision table

| Outcome | Permitted conclusion |
|---|---|
| RNA junction confirmed only | The transcript junction is independently supported. |
| RNA plus targeted peptide confirmed | The isoform has independent transcript and peptide support; intact protein and function remain unproven. |
| CBL interaction differs at matched protein abundance | Evidence supports an isoform-dependent interaction effect under the tested conditions. |
| Interaction and a prespecified phenotype both reproduce | A bounded functional claim may be considered, limited to the tested conditions. |
| Negative or inconclusive result | Retain the candidate as unvalidated; do not change thresholds after observing the data. |

## Sources

- Frozen UniProtKB P92937 record in this repository: `P92937.json`.
- Published peptide resource: Reyes et al., *Nucleic Acids Research* (2026),
  https://doi.org/10.1093/nar/gkag400; the audited peptide is from the Mergner
  acquisition within the processed resource.
- This protocol is prospective. No experimental outcome is reported here.
