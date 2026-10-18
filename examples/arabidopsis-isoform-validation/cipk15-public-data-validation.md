# CIPK15 public-data-only validation protocol

## Scope and status

**Computational/public-data analysis only. No wet-lab experiments, new samples,
PCR, targeted mass spectrometry, or functional assays are required or planned.**

The goal is to determine whether the existing `AT5G01810.3` model has
independent public-data support and whether that support adds anything beyond
existing annotation. This is a candidate-level evidence audit, not a claim of
novel isoform discovery.

## Why CIPK15 needs a stricter annotation check

The frozen Araport11 pair is `AT5G01810.1` (421 aa) and `AT5G01810.3`
(376 aa), with a 45-aa internal deletion at canonical residues 284–328 that
overlaps the annotated NAF region. The deletion removes genomic bases
Chr5:311309–311443 in the frozen coordinate map; the short model's candidate
junction connects the flanking coding segments at Chr5:311308 and 311444.

This is **not a newly discovered transcript model**. UniProtKB P92937 already
lists a computationally mapped potential CIPK15 isoform, F4KAV7, of 376 aa,
and NCBI RefSeq records three mRNA splice variants for CIPK15. The older
canonical cDNA AF302111 / protein AAK16692.1 encodes the 421-aa canonical
sequence. These records establish prior annotation/history, not independent
proof that the specific 376-aa protein is translated or has a different
function. See [UniProtKB P92937](https://www.uniprot.org/uniprotkb/P92937/entry),
[NCBI CIPK15 Gene](https://www.ncbi.nlm.nih.gov/gene/830556), and the
[historical CIPK/CBL family study](https://academic.oup.com/plphys/article/134/1/43/6112285).

The only eligible peptide in the frozen candidate audit is
`ESVEEETEEEMR` (short-protein positions 276–287), from the Mergner
proteomics source. It is one peptide from one original source, not independent
replication. The Reyes et al. 2026 processed resource includes reanalysis of
public acquisitions, so the same peptide row or same raw files must not be
counted again as a new independent study.

## Public datasets to audit

The accession inventory is maintained in
[`public-data-source-inventory.tsv`](public-data-source-inventory.tsv).
Priority sources are:

1. **Full-length RNA evidence:** NCBI BioProject
   [PRJNA1418670](https://www.ncbi.nlm.nih.gov/bioproject/1418670), PacBio
   Iso-Seq from Arabidopsis roots under control and salinity time points
   (Hernández-Urrieta et al., 2026). Inspect the original long-read molecules
   or author-provided transcript models for the exact `AT5G01810.3` exon chain.
   Dataset availability is verified; gene-specific support has not yet been
   established.
2. **Independent short-read RNA evidence:** the public Arabidopsis alternative-
   splicing compendium described in
   [the multi-study landscape analysis](https://pmc.ncbi.nlm.nih.gov/articles/PMC7807721/),
   which assembled 516 SRA experiments from 63 studies. Recover original
   accessions and sample metadata, deduplicate samples/studies, then test the
   candidate junction. The compendium's existence alone is not gene-specific
   evidence.
3. **Independent proteomics acquisitions:** the Zhang et al. 2019 Arabidopsis
   proteome spectral-library data, [PXD012708](https://proteomecentral.proteomexchange.org/?pxid=PXD012708),
   [PXD012710](https://proteomecentral.proteomexchange.org/?pxid=PXD012710), and
   [PXD014032](https://proteomecentral.proteomexchange.org/?pxid=PXD014032).
   These three accessions belong to **one publication/source-study group**, not
   three independent studies. Inspect available processed identification or
   spectral-library files first; search raw spectra only if needed and feasible.
4. **Existing-source control, not independent confirmation:**
   [PXD013868](https://proteomecentral.proteomexchange.org/?pxid=PXD013868)
   and related Mergner records are the source of the current peptide evidence.
   The same source acquisition cannot be counted twice.
5. **Processed aggregate, not an extra independent study:** Reyes et al. 2026
   reports the large-scale Arabidopsis alternative-splicing proteome analysis
   and its deposited accessions [PXD068586](https://proteomecentral.proteomexchange.org/?pxid=PXD068586),
   [PXD068589](https://proteomecentral.proteomexchange.org/?pxid=PXD068589),
   [PXD068593](https://proteomecentral.proteomexchange.org/?pxid=PXD068593), and
   RNA-seq [GSE286870](https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=GSE286870).
   Trace each raw-file/source accession back to its original study before
   counting it. Reprocessing the same raw file is not a second biological source.

## Frozen analysis plan

### 1. Transcript structure and expression

- Compare the Araport11 `.1/.3` transcript/CDS structures with the RefSeq
  transcripts, UniProt's 376-aa potential isoform, historical cDNA records, and
  available AtRTD/AtRTD3 reference transcripts. Record exact sequence hashes,
  exon chains, coding frames, splice-site coordinates, and annotation versions.
- In full-length Iso-Seq data, count high-quality full-length molecules that
  support the complete candidate exon chain, not merely reads overlapping the
  locus. Keep sample-level counts and treatment/time metadata.
- In short-read RNA-seq, count uniquely mapped reads supporting the exact
  candidate junction (Chr5:311308 to 311444 in the frozen 1-based coordinate
  map) and the competing canonical path. Report per-sample junction counts,
  library strandedness, mapping quality, and effective coverage.
- Treat transcript support as independently replicated only when the exact
  junction is supported in at least two independent original studies, with
  reproducible evidence in biological replicates where those are available.
  The long-read and short-read datasets must be source-deduplicated.
- A missing junction under low coverage is **inconclusive**, not evidence that
  the isoform is absent. Gene-level expression does not prove isoform usage.

### 2. Protein translation

- Search publicly available processed peptide identifications/spectral libraries
  from the independent source-study group(s) above for all peptides unique to
  `AT5G01810.3`, especially the junction peptide. Do not search only the
  one peptide already reported.
- Where raw spectra are available and a reliable search is feasible, use a
  frozen target database containing all Araport11 proteins, the candidate
  isoform, common contaminants and decoys; record software/version, digestion,
  modifications, precursor/fragment tolerances, and peptide-level FDR.
- Validate uniqueness against the complete proteome and I/L ambiguity. Retain
  spectrum IDs, source-file accessions, study metadata, PSM scores, fragment
  evidence, and FDR. An identification table without inspectable supporting
  spectra is weaker evidence than a reproducible raw-spectrum search.
- Keep the Mergner source as the positive/source-control record. It does not
  count as an independent second source.
- Preserve the predeclared rule: at least two distinct isoform-specific peptides
  overall and support from at least two independent original study groups are
  needed to mark the candidate as independently protein-supported. Count a
  study once even if it has multiple PRIDE accessions. Do not treat failure to
  detect a peptide as proof of absence.

### 3. Functional interpretation without new experiments

- Compare the predicted 376-aa sequence and NAF deletion with CIPK15 orthologs,
  curated domain boundaries, and published CIPK15/CBL interaction literature.
- Separate three evidence levels: (a) prior gene/locus function, (b) predicted
  consequence of the annotated deletion, and (c) independent evidence about
  this exact isoform. Only level (c) can support an isoform-specific claim.
- The fact that the deletion overlaps the NAF region can motivate a hypothesis
  about altered CBL regulation; it does not establish loss of interaction,
  altered kinase activity, stress tolerance, or any phenotype.
- Because the 376-aa model already appears in public annotation, even positive
  RNA/protein support would validate an existing isoform, not make it a newly
  discovered transcript. A novel biological claim would need a genuinely new,
  independently supported result beyond this annotation and prior literature.

## Decision rules

| Public-data outcome | Allowed conclusion |
|---|---|
| Existing annotation only; no independent junction reads | The isoform is annotated, but independent transcript support was not established in the datasets assessed. |
| Exact junction replicated in independent public RNA studies | The transcript structure has independent public-data support; translation remains unresolved. |
| Independent source spectra support unique isoform peptides under frozen FDR rules | Public proteomics supports translation of the isoform-specific sequence fragment(s); intact protein/function are not automatically proven. |
| Both independent RNA and protein criteria pass | The existing isoform has convergent public-data support; novelty still must be assessed against annotation and literature. |
| No qualifying support or insufficient coverage | Report negative/inconclusive evidence with accessions and coverage; do not claim absence or a new discovery. |

## Reproducibility outputs

Retain an accession/sample manifest, exact annotation and sequence hashes,
junction-count table, per-study peptide/PSM table, duplicate-source audit,
commands and tool versions, and a concise decision report. No wet-lab work or
new biological samples are part of this plan.
