# Researcher-value study protocol (planned; no participant data yet)

## Objective

Test whether PhytoScope's linked, recoverable desktop workflow helps plant
researchers complete locus-centered tasks with fewer handoffs and errors than a
conventional multi-tool workflow. This document is a **prospective protocol**,
not evidence that the workflow is already faster, easier or more accurate.

## Design

Use a counterbalanced, within-participant crossover. Each participant completes
matched but non-identical task sets with both workflows. Randomize the workflow
order (PhytoScope first vs conventional first) and task-set assignment; balance
the order within experience strata. Do not let the software authors serve as
confirmatory participants. Freeze the data, software versions, scoring rubric
and analysis plan before recruitment.

Target 24 participants for the confirmatory study. A paired design with 24
participants has approximately 80% power to detect a standardized within-person
effect of about 0.6 at two-sided alpha 0.05; report the actual achieved sample
and exclusions. If the target population or pilot variance differs materially
from this assumption, revise the sample-size calculation before confirmatory
data collection, not after looking at the main results.

## Workflows compared

- **Integrated workflow:** PhytoScope's browser and five research tabs, with
  outputs and supported project state saved to one portable project.
- **Conventional workflow:** a predeclared, reproducible combination of a genome
  browser, bedtools or equivalent interval extraction, a separate sequence-QC
  script, a sample-summary spreadsheet/script, BLASTP, and a separate domain
  coordinate script. Pin versions and provide the same input data and
  documentation to every participant.

Both workflows must use the same assembly, annotation, sequences, coordinate
convention, expression values and predeclared thresholds. Train participants
on a separate dataset before timing begins. Do not change the baseline tools or
rules after observing results.

## Matched tasks and answer keys

| Task | Required output | Common critical error |
| --- | --- | --- |
| Strand-aware interval | Extract a defined promoter/custom interval and provide its sequence and coordinates | Wrong strand, off-by-one boundary, or wrong sequence |
| Sequence QC | Report length, GC, ambiguous bases and coding checks for the same input sequence | Incorrect metric or interpreting a QC flag as a biological conclusion |
| Expression grouping | Summarize pre-labelled samples while preserving measured zero and missing values distinctly | Dropping zeros, imputing missing values, or grouping the wrong samples |
| Protein similarity | Report accepted hits under identical thresholds and distinguish family similarity from orthology | Treating a reciprocal best hit as proof of orthology or ignoring coverage |
| Domain-to-genome mapping | Map a supplied protein-domain interval through a multi-exon CDS and export the genomic blocks | Incorrect transcript ID, exon order, strand or coordinate convention |
| Save/restore and reproduce | Save the task state, close/reopen the project, restore it and reproduce a specified output | Lost inputs/parameters, changed result, or untraceable output |

Use matched task-set A/B with equivalent difficulty and different locus IDs to
reduce learning from seeing the same answer twice. The independent answer keys
must be prepared before the study and scored without knowing which workflow
produced the output.

## Outcomes

**Primary outcomes**

1. Task success and correctness against the frozen answer key.
2. Time from task start to a correct, exportable result; cap unsuccessful tasks
   at the predeclared 30-minute limit and report failures separately.

**Secondary outcomes**

- Number of application/tool switches, intermediate files and manual
  copy/paste or format-conversion steps.
- Critical errors per task and fidelity after project restoration.
- System Usability Scale (SUS) and NASA Task Load Index (NASA-TLX), collected
  after each workflow.
- Participant confidence, analyzed separately from objective correctness.

Capture task events and timing with a consistent procedure. Screen recording
may be used only with consent and institutional approval; remove identifiers
from research data.

## Analysis plan

Report per-task denominators, exclusions, missing observations and failure
reasons. For paired outcomes, report paired differences, 95% confidence
intervals and effect sizes, not only p-values. Use a mixed-effects model with
participant as a random effect and workflow, task, order and experience stratum
as fixed effects where assumptions permit. Report task success as a paired
proportion and use an appropriate paired binary or mixed-effects analysis.
Predefine treatment of timeouts and multiplicity before data collection.

A defensible positive workflow-value claim requires non-inferior correctness
(predeclared margin: no more than a 5-percentage-point absolute loss), lower
time-to-correct-result with a confidence interval excluding no difference, and
fewer handoffs or manual transformations. If results are mixed, report the
trade-off rather than claiming overall superiority.

## Ethics and reporting

Obtain the relevant institutional ethics/IRB determination before recruiting.
Use informed consent, de-identify logs, disclose any compensation and preserve
the option to withdraw. Publish the protocol, task inputs, answer keys, scoring
code and de-identified aggregate data where consent and policy permit.

## Current status

No human-participant study has been conducted for this project. The existing
JavaFX regressions and save/restore checks are software correctness tests, not
usability evidence. This protocol prepares the next evaluation step but does not
close the researcher-value evidence gap.
