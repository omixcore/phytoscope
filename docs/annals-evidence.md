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
| Operational workflow | Five tabs in the desktop UI; browser locus synchronization; promoter extraction feeds sequence QC; sample groups, FASTA/domain inputs and results are saved in portable projects; analysis output exports as TSV. The browser accepts IGV-style `sequence:start-end` navigation and preserves the visible range in project view state. Automated JavaFX regressions now fire all five research-tab actions (promoter, QC, grouping, homology and domain mapping), verify visible outputs, and cover project save/replace/restore and coordinate navigation. The three-platform CI passed on 2026-10-18; full annotation-coordinate checks and standalone packaging passed on Ubuntu. | Add a native file-save-dialog interaction test and measure researcher task performance with actual participants. |
| Annotation correctness and scale | Independent source-to-model checks cover Arabidopsis, rice, maize and soybean. Eight inventories contain 4,641,758 exact gene/transcript/exon/CDS/UTR tuples with zero missing/extra tuples; the full-annotation checks passed in the 2026-10-18 Ubuntu CI run. | Repeat controlled performance runs on documented hardware/memory limits; the recognized-feature scope does not validate unsupported GFF types. |
| Existing-tool comparison | A matched PhytoScope vs `gffutils==0.13` benchmark passed for Arabidopsis, rice, maize and soybean fixtures, with 10/24/20/40 gene loci respectively, three alternating-order trials per species, and exact source-to-output tuple agreement on Ubuntu, Windows and macOS. Observed medians and limitations are recorded in [the CI benchmark report](../benchmarks/results/2026-10-18/cross-species-extraction.md). | The comparison is still a narrow parser/hierarchy task. Add curated module-level gold standards for homology, domain mapping, promoter extraction and expression summaries; run controlled hardware benchmarks before speed claims. |
| Research-module correctness | The real-data calculation suite checks eight 2,200-bp promoter windows across both strands against direct TAIR10 FASTA slicing/reverse-complement calculations; four real RBCS CDS QC reports against independently reconstructed metrics; all 16 within-family Arabidopsis RBCS protein alignments against a test-side Smith–Waterman reference; E-MTAB-7978 grouping arithmetic including measured zeros; and 16 domain/deletion coordinate records in 13 genes against independent Python calculations. In the three-platform CI run #192, bedtools v2.31.1 independently reproduced all 8/8 promoter sequences with zero mismatches. BLASTP 2.12.0+ agreed with PhytoScope on 12/12 non-self RBCS-family pairs and 8/8 unrelated-family controls both in the Arabidopsis-only panel and in a new four-species mature-RBCS panel (20/20 pairwise classifications per panel). On the tiny cross-species panel, one observed call took 21.63 ms in PhytoScope and 32.4 ms in BLASTP; this is startup-dominated and not a speed claim. Reports are retained in CI artifacts. | The new four-species RBCS panel is a single-family smoke test, not a multi-family curated gold standard or general precision/recall estimate; its negatives are not taxonomically balanced and it does not validate orthology. Expand independently curated multi-family homolog/domain pairs, add a multi-replicate expression gold set, and test more sequence/annotation edge cases before making broad accuracy claims. |
| Independent biological examples | The RBCS-family expression case reproduces a known pattern from Expression Atlas E-MTAB-7978. A second known-positive context is the PIF6 (`AT3G62090`) alternative-splicing literature: published studies link a shorter PIF6 splice variant to seed dormancy/germination and PTB-regulated splicing. The frozen `DI0006` model overlaps the annotated bHLH region, but its exact equivalence to the historical constructs still requires sequence-level reconciliation. | Both are prior biological knowledge, not new discoveries. The PIF6 case must not be used as validation of the exact `.4` transcript until construct/model equivalence is established. A new finding independently supported by additional evidence remains outstanding. |
| Researcher value | A real-data calculation report and JavaFX interaction regression now demonstrate linked outputs across the five tabs and portable-project restoration. The PIF6 case note gives a known-positive traceability example from transcript model to domain context and published phenotype. | No human participants or measured task-time savings exist yet. A [prospective researcher-value study protocol](researcher-value-study-protocol.md) now defines the counterbalanced task design, objective endpoints and analysis rules, but it is not participant evidence. The known PIF6 finding is reproducible with ordinary tools too; it demonstrates integration/provenance, not a unique discovery. Run the preregistered task study and continue the search for a new independently validated result. |

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

A separate researcher-value protocol now defines paired tasks, frozen gold
outputs, task success/error/time/handoff/provenance metrics, and a blank
anonymized log template. No human participants have been measured, so
researcher time savings and usability remain unproven.

The independent biological-discovery gap remains open. The 13-gene screen
still yields zero candidates meeting its predeclared independent-evidence
threshold. The CIPK15 junction peptide is from the Mergner source rather than
the new Reyes experiments; the published transcript annotation predates this
analysis. No new functional effect or novel biological finding is claimed.


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
gold-standard gap or the human task study.


## Known-positive PIF6 context and claim boundary

The [PIF6 case note](case-study-pif6-known-positive.md) documents prior
literature on alternative splicing and seed dormancy at `AT3G62090`. It is a
useful positive-control context for checking that a transcript/domain view can
be linked to external biology, but it is not novel and the exact equivalence of
the current Araport11 `.2/.4` pair to the historical constructs remains
unresolved. The independent new-discovery requirement remains open.
