# Evidence package for the PhytoScope research workflow

## Proposed scientific contribution

PhytoScope links a selected plant locus to strand-aware promoter/custom-interval
extraction, explicit sample grouping, sequence screening, cross-species protein
similarity, and splice-aware domain-to-genome mapping in one recoverable desktop
project. Its contribution is workflow integration and provenance, not a new
orthology algorithm, domain predictor, or causal regulatory inference method.
The central evaluation question is whether the linked workflow reduces handoffs
and errors in a real plant analysis while preserving enough provenance to
reproduce it.

## Evidence status

| Evidence | Current evidence | Remaining work before a strong methods-paper claim |
| --- | --- | --- |
| Operational workflow | Five tabs in the desktop UI; browser locus synchronization; promoter extraction feeds sequence QC; sample groups, FASTA/domain inputs and results are saved in portable projects; analysis output exports as TSV. The browser accepts IGV-style `sequence:start-end` navigation and preserves the visible range in project view state. Automated JavaFX regressions now fire all five research-tab actions (promoter, QC, grouping, homology and domain mapping), verify visible outputs, and cover project save/replace/restore and coordinate navigation. The three-platform CI passed on 2026-10-18; full annotation-coordinate checks and standalone packaging passed on Ubuntu. | Add a native file-save-dialog interaction test and a matched, reproducible public-data case benchmark against a documented multi-tool baseline. Do not claim human time savings or usability without participant evidence. |
| Annotation correctness and scale | Independent source-to-model checks cover Arabidopsis, rice, maize and soybean. Eight inventories contain 4,641,758 exact gene/transcript/exon/CDS/UTR tuples with zero missing/extra tuples; the full-annotation checks passed in the 2026-10-18 Ubuntu CI run. | Repeat controlled performance runs on documented hardware/memory limits; the recognized-feature scope does not validate unsupported GFF types. |
| Existing-tool comparison | A matched PhytoScope vs `gffutils==0.13` benchmark passed for Arabidopsis, rice, maize and soybean fixtures, with 10/24/20/40 gene loci respectively, three alternating-order trials per species, and exact source-to-output tuple agreement on Ubuntu, Windows and macOS. Observed medians and limitations are recorded in [the CI benchmark report](../benchmarks/results/2026-10-18/cross-species-extraction.md). | The comparison is still a narrow parser/hierarchy task. Add curated module-level gold standards for homology, domain mapping, promoter extraction and expression summaries; run controlled hardware benchmarks before speed claims. |
| Research-module correctness | The real-data calculation suite checks eight 2,200-bp promoter windows across both strands against direct TAIR10 FASTA slicing/reverse-complement calculations; four real RBCS CDS QC reports against independently reconstructed metrics; all 16 within-family Arabidopsis RBCS protein alignments against a test-side Smith–Waterman reference; E-MTAB-7978 grouping arithmetic including measured zeros; and 16 domain/deletion coordinate records in 13 genes against independent Python calculations. In the three-platform CI run #192, bedtools v2.31.1 independently reproduced all 8/8 promoter sequences with zero mismatches. BLASTP 2.12.0+ agreed with PhytoScope on 12/12 non-self RBCS-family pairs and 8/8 unrelated-family controls both in the Arabidopsis-only panel and in a new four-species mature-RBCS panel (20/20 pairwise classifications per panel). On the tiny cross-species panel, one observed call took 21.63 ms in PhytoScope and 32.4 ms in BLASTP; this is startup-dominated and not a speed claim. Reports are retained in CI artifacts. | The new four-species RBCS panel is a single-family smoke test, not a multi-family curated gold standard or general precision/recall estimate; its negatives are not taxonomically balanced and it does not validate orthology. Expand independently curated multi-family homolog/domain pairs, add a multi-replicate expression gold set, and test more sequence/annotation edge cases before making broad accuracy claims. |
| Independent biological examples | The RBCS-family expression case reproduces a known pattern from Expression Atlas E-MTAB-7978. Published PIF6 work links a short splice variant to seed dormancy/germination, but the sequence-level identity gate now rejects the frozen `AT3G62090.4` candidate as that published beta isoform: the current model is 341 aa with an internal 22-aa deletion, whereas the pinned UniProtKB D0EDQ7 reference is 188 aa (the literature reports a 182-aa construct). | PIF6 remains locus-level context, not an isoform-specific positive control and not a new discovery. No new biological finding independently supported by additional evidence has been established. |
| Researcher value | A real-data calculation report and JavaFX interaction regression demonstrate linked outputs across five tabs and portable-project restoration. The PIF6 sequence-identity gate now shows that the frozen candidate is not the published beta isoform, so PIF6 is only locus-level context. | No participant study is planned. The [computational workflow-value protocol](researcher-value-study.md) compares correctness, handoffs, provenance completeness, and project round-trip behavior on frozen public inputs; it cannot establish human time savings or usability. No new biological discovery is established. |

## Fair comparison protocol

For each of the five functions, define the input and output before choosing a
baseline. Use identical species, assembly, annotation release, sequences,
coordinate convention and thresholds. Freeze tool versions and parameters;
record setup and runtime separately. Include curated positive and negative
homolog pairs, exact domain-to-CDS mappings, promoter boundary cases, and
missing-versus-zero sample values. Report accuracy, runtime, memory where
measurable, and failure/rejection boundaries per task. A structure parser
comparison cannot stand in for homology, domain mapping or end-user task
performance. The current four-species RBCS family-level comparison is a first
cross-species smoke test; see the [fixture](../examples/cross-species-homology-gold-set/README.md)
and [BLASTP results](../benchmarks/results/2026-10-18/cross-species-homology.md).

## Scientific claim boundary

The software currently supports reproducible locus-centered inspection and
hypothesis generation. It does not establish orthology from reciprocal best
hits, predict domains, test differential expression, infer promoter function,
or demonstrate that researchers make a discovery unavailable through other
workflows. A methods-paper claim should be limited to the operational workflow
until curated module-level benchmarks and an independent, reproducible plant
case have been completed.

## Independent isoform validation attempt (2026-10-18)

The [frozen case](../examples/arabidopsis-isoform-validation/README.md) screens
48,002 complete CDS models using exact UniProt canonical matches and fixed
deletion/domain-overlap rules. It retains 16 records in 13 genes, all 26 source
peptide rows for those genes, and every failed validation outcome. One CIPK15
junction peptide is eligible, but comes only from the Mergner original study.
No candidate meets the required two-study/two-peptide standard. This is a
reproducible negative validation result, not completion of the new-discovery
requirement. Missing detection does not establish absence of an isoform.

The software correction accepts exact transcript IDs for domain mapping when
annotations lack protein IDs. A real-data regression compares 16 mappings and
translated isoforms with independent Python results, checks the CIPK15 junction
across two CDS blocks, and restores a portable project with the failed-validation
status retained. This supplies coordinate correctness evidence for these records;
it does not replace curated multi-species benchmarks or a matched user-task study.

The targeted public-record audit also found existing RefSeq alternative-splicing
records at multiple candidate loci, including WAK3, CIPK15, 3BETAHSD/D1,
AT3G57800, PIF6, EXPA1, CPK7, CML38, AT5G56610 and WNK6. This is evidence that
the candidate set is not a set of newly discovered transcript sequences. RefSeq
records do not, by themselves, establish translation or function of every
specific domain-deletion model; see the [candidate-by-candidate literature audit](../examples/arabidopsis-isoform-validation/literature-audit.md).


## Follow-up evidence work (2026-10-18)

A reproducible four-species extraction comparison has been added to CI. It
compares the exact annotation hierarchy output from PhytoScope with
`gffutils==0.13` on source-verified Arabidopsis, rice, maize and soybean
release-62 excerpts, with alternating trial order and source-to-output
coordinate checks. Its results are retained as CI artifacts. The comparison
is intentionally limited to parsing/hierarchy extraction and does not support
a general speed ranking.

The latest PR CI run #176 adds two independent external-tool checks on Ubuntu:
bedtools v2.31.1 returns exact matches for all eight strand-aware 2,200-bp
promoter intervals (zero mismatches), and BLASTP 2.12.0+ agrees with PhytoScope
on 12 non-self within-family RBCS pairs and eight unrelated-family negative
controls (20/20 pairwise classifications; no negative-control hits). The
homology benchmark excludes the four trivial self-pairs from its accuracy
denominator. It remains a small same-species smoke test, not an orthology
benchmark or a general sensitivity/specificity estimate.

The workflow-value protocol is now computational-only: it defines frozen public
inputs, source-derived gold outputs, correctness, handoffs, provenance completeness,
and project round-trip metrics against a documented multi-tool baseline. No
participant study or human time-saving claim is planned.

The independent biological-discovery gap remains open. The 13-gene screen
still yields zero candidates meeting its predeclared independent-evidence
threshold. The CIPK15 junction peptide is from the Mergner source rather than the new Reyes
experiments. UniProtKB already lists a computationally mapped 376-aa potential
isoform (F4KAV7), and NCBI lists three RefSeq mRNA variants; this is not a new
transcript discovery. The next step is source-deduplicated analysis of public
Iso-Seq/short-read and independent proteomics data, not wet-lab work. No new
functional effect or novel biological finding is claimed.


## Recorded CI results (2026-10-18)

The [cross-species extraction report](../benchmarks/results/2026-10-18/cross-species-extraction.md)
records run #156: all three operating-system jobs passed, all four species
passed three-trial exact-tuple comparison, and eight source-to-model inventories
contained 4,641,758 tuples with no missing/extra records. The benchmark reports
in-memory import and SQLite index construction separately; it is not a speed
ranking.

The latest [PR validation run #176](https://github.com/omixcore/phytoscope/actions/runs/39115541048)
also passed on Ubuntu, Windows and macOS. On Ubuntu it completed the full
annotation-coordinate checks, the bedtools promoter comparison, the BLASTP
similarity comparison and standalone packaging. The small external-tool
comparisons improve the evidence base but do not close the curated module-level
gold-standard gap or the computational workflow-value comparison on a frozen biological case.


## PIF6 sequence-identity gate and claim boundary

The [PIF6 case note](case-study-pif6-known-positive.md) and
`tools/validate_pif6_isoform_identity.py` now compare the frozen
`AT3G62090.2/.4` pair with the published PIF6-beta sequence record. The four protein-coding models in the frozen Araport11 annotation are 346,
363, 324 and 341 aa; none matches either the 188-aa UniProtKB D0EDQ7 reference
or the 182-aa construct reported in the literature. The current 341-aa `.4`
model reconstructs from an internal 22-aa deletion and retains downstream
C-terminal sequence. Thus the published
seed-dormancy phenotype cannot be transferred to DI0006. PIF6 is locus-level
context only, not a valid isoform-specific positive control. This is a negative
identity validation; the independent new-discovery requirement remains open.

## Independent PIF6 identity check (2026-10-18)

A new standard-library script checks the frozen candidate against a hash-pinned
published PIF6-beta protein sequence. It confirms the 22-aa deletion reconstructs
the current `AT3G62090.4` model, but rejects exact identity with published
PIF6-beta: 341 aa versus 188 aa in UniProtKB D0EDQ7 (a 182-aa construct is
reported in the literature). The candidate retains downstream C-terminal
sequence, so the prior seed-dormancy phenotype cannot be assigned to this
annotation. The script and JSON output are included in the latest CI artifact.
This strengthens the negative validation boundary; it does not establish a new
biological discovery.

## CIPK15: public-data reanalysis candidate, not a validated discovery

Candidate `DI0003` (`AT5G01810.3`) is the most actionable public-data reanalysis
candidate from the frozen screen. The 45-aa deletion (canonical residues
284–328) removes the complete annotated 25-aa NAF domain (299–323) while
leaving the annotated kinase domain (12–266) outside the deleted interval.
However, this is an existing annotated isoform: UniProtKB P92937 already lists
the computationally mapped potential 376-aa isoform F4KAV7, and NCBI Gene
830556 lists three RefSeq mRNA splice variants. This candidate cannot be
presented as a newly discovered transcript. UniProtKB P92937 describes
the NAF domain as the regulatory CBL-binding element. This supports a
**testable hypothesis** that the shorter protein may have altered CBL-dependent
regulation; it does not establish that the isoform is translated or that its
activity differs.

The only eligible peptide in the audited source table is
`ESVEEETEEEMR` (short-protein positions 276–287), mapped only to `AT5G01810.3`
by a FragPipe search and listed in the source group `FL` (source workbook row
24784). The group label must be resolved against the source metadata before
choosing tissues. The peptide comes from the Mergner acquisition within the
Reyes et al. processed resource; it is one peptide from one biological source,
not an independent second study. No raw-spectrum re-search or isoform-specific
functional experiment has been completed.

The [CIPK15 public-data-only protocol](../examples/arabidopsis-isoform-validation/cipk15-public-data-validation.md)
sets source-deduplicated transcript-junction and public-proteomics checks with
predeclared criteria. The accession inventory includes PRJNA1418670 Iso-Seq
data and the independent Zhang et al. 2019 proteomics source group
(PXD012708/PXD012710/PXD014032). These datasets have been identified but the
candidate-specific reanalysis is not yet complete; their existence is not
evidence that the candidate passed. No wet-lab work is planned.

## Public-data-only biological evidence plan (2026-10-18)

No wet-lab experiments are required or planned. The next biological-evidence
step is to test the frozen candidate models against source-deduplicated public
RNA and proteomics acquisitions. The CIPK15 376-aa model is already represented
by UniProtKB's computationally mapped potential isoform F4KAV7 and NCBI's three
RefSeq mRNA variants, so it is not a new transcript discovery. Its only eligible
junction peptide in the current audit comes from the Mergner source and does not
meet the independent evidence threshold.

The [public-data-only protocol](../examples/arabidopsis-isoform-validation/cipk15-public-data-validation.md)
prioritizes PRJNA1418670 PacBio Iso-Seq roots under salinity for exact exon-chain
support; the multi-study public Arabidopsis alternative-splicing compendium for
short-read junction counts; and the Zhang et al. 2019 public proteomics source
group (PXD012708, PXD012710, PXD014032) for an independent sequence-specific
peptide audit. The [accession inventory](../examples/arabidopsis-isoform-validation/public-data-source-inventory.tsv)
marks what has been identified versus what has actually been analyzed. No
candidate-specific independent RNA/protein result is claimed yet. The protocol
requires exact source accession tracking, sample/study deduplication, frozen
thresholds and honest inconclusive outcomes.
