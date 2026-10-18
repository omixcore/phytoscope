# Researcher-value evaluation protocol

**Status: protocol and blank instrument only. No human participants, task-time
results, usability scores or user-study conclusions have been collected.**

## Research question

Does a single, portable PhytoScope project let plant researchers complete a
locus-centered analysis with fewer handoffs and more complete provenance than a
documented conventional workflow, without reducing output correctness?

This is a workflow-value question, not a claim that PhytoScope has more biological
features than IGV or JBrowse. For browser-only locus navigation and track inspection,
use IGV or JBrowse as the comparator. For an end-to-end locus-centered task, freeze
a documented multi-tool baseline that uses IGV/JBrowse for genome browsing plus
the appropriate annotation parser/query tool, sequence-analysis script, table
tool for expression grouping, and figure editor. Keep each task's baseline limited
to functions those tools actually implement; record setup time separately from
task time and do not treat a multi-tool workflow as a single application.

## Design

Use a paired, counterbalanced task study. Each participant completes equivalent
tasks in both workflows. Use two matched locus/data sets (A and B), and rotate
the workflow/data-set pairing across participants so that the second attempt is
not always easier. Give the same written task instructions and acceptance rules
for both workflows. Provide a short practice task that is excluded from analysis.

Recruit plant researchers who routinely inspect gene models or expression data.
Include a range of computational experience and record experience using genome
browsers, command-line tools and scripting. Run a small pilot first; determine the
confirmatory sample size from pilot variability and a prespecified meaningful
paired effect, rather than choosing a sample size after seeing the results.
Obtain the relevant institutional ethics/consent review before recruiting people
or retaining participant-level logs.

## Tasks and objective acceptance criteria

| Task | Required output | Gold-standard acceptance criterion |
| --- | --- | --- |
| Find and compare loci | Selected gene IDs, sequences and coordinate windows | Every requested locus resolves to the correct sequence and coordinates |
| Inspect transcript structure | Transcript, exon, CDS and UTR intervals | Exact agreement with a frozen source-derived feature inventory |
| Summarize expression | Sample-to-group mapping, group summaries and missingness | Values match an independent calculation; measured zero remains distinct from missing |
| Extract and inspect a sequence interval | Promoter/custom interval, sequence QC flags | Exact sequence and strand-aware boundary match against an independent script |
| Link a protein domain to a genomic isoform | Amino-acid interval and genomic CDS blocks | Exact transcript/protein mapping, exon boundaries and strand match the frozen coordinate map |
| Export and resume | Figure, TSV and restored project | Output is readable/editable; after close/reopen, inputs, selections, parameters and results match the saved state |

Use the same input files, assembly/annotation release, gene IDs, coordinate
convention and expected outputs in both workflows. Freeze the expected outputs
before collecting participant results. An independent assessor should score
exported results against the gold files without knowing which workflow produced
them, where feasible.

## Outcomes

**Primary endpoint:** proportion of tasks completed correctly without a critical
error, reported by task and workflow.

**Secondary endpoints:**

- Task completion time, including a separately reported setup time.
- Number and type of errors, recoveries and manual corrections.
- Number of application/tool handoffs and intermediate files required.
- Provenance completeness: source/release, input checksum, parameters, output
  and restoration state all present and internally consistent.
- Exact project round-trip success and exact output agreement.
- Perceived usability (System Usability Scale) and workload (NASA-TLX or a
  prespecified shorter validated instrument), reported separately from objective
  performance.

A workflow with fewer clicks is not automatically better: task success and
scientific correctness take priority over speed. Record timeouts, crashes,
unsupported input formats, missing-data states and any task that cannot be
completed. Never replace a failed task with an unreported manual workaround.

## Analysis and reporting

Predefine the primary comparison, exclusion rules, and treatment of incomplete
tasks. Report paired differences with confidence intervals and raw task-level
summaries. Use a paired test suitable for each outcome and the sample size; do
not pool unlike tasks into one unqualified speed score. Publish anonymized
task logs, task instructions, frozen gold outputs, tool versions, machine
specifications and analysis code when consent permits.

## Claim boundary

Until the study is actually run, the repository may claim only that it provides
a reproducible workflow and that its software tests demonstrate specified
technical behaviors. It may not claim reduced researcher time, better usability,
fewer human errors, or increased discovery rates. A task study can demonstrate
workflow value; a new biological discovery still requires a separate case with
independent biological evidence.

## Rationale and relevant literature

- Weber LM et al. (2019). Essential guidelines for computational method
  benchmarking. *Genome Biology* 20, 125.
  https://doi.org/10.1186/s13059-019-1738-8
- Diesh C et al. (2023). JBrowse 2: a modular genome browser with views of
  synteny and structural variation. *Genome Biology* 24, 74.
  https://doi.org/10.1186/s13059-023-02914-z
- Sargent L et al. (2020). G-OnRamp: Generating genome browsers to facilitate
  undergraduate-driven collaborative genome annotation. *PLoS Computational
  Biology* 16, e1007863.
  https://doi.org/10.1371/journal.pcbi.1007863
