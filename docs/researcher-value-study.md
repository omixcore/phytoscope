# Computational workflow-value evaluation protocol

## Status and claim boundary

**Computational-only protocol. No human participants, wet-lab experiments, or
new biological samples are required or planned.** This protocol measures
objective workflow properties on frozen public data. It cannot establish
human usability, researcher time savings, or increased discovery rates.

## Research question

For a fixed locus-centered plant analysis, does PhytoScope produce the required
correct outputs with complete provenance and fewer manual handoffs than a
documented conventional multi-tool workflow?

This is a workflow-integration question, not a claim that PhytoScope replaces
IGV/JBrowse for all genome-browsing tasks or improves a domain/homology algorithm.

## Frozen case and comparator

Use one fixed public case package and freeze its checksums before comparison:
the same genome assembly, annotation release, transcript/protein sequences,
sample metadata, expression inputs, locus list and expected outputs must be
used in both workflows.

The conventional baseline should use tools only for functions they implement:
IGV or JBrowse for genome navigation, a documented annotation parser/query tool
for gene models, a sequence script for promoter/custom intervals, a standard
expression table workflow for grouping/summaries, and an independent alignment
or domain-coordinate script where needed. Record each tool and version. Do not
pretend the multi-tool workflow is one application.

## Objective tasks and gold-standard criteria

| Task | Required output | Acceptance criterion |
| --- | --- | --- |
| Locus navigation | Selected locus and coordinate window | Correct sequence, strand and coordinates |
| Transcript structure | Transcript, exon, CDS and UTR intervals | Exact agreement with frozen source-derived inventory |
| Expression summary | Sample-to-group mapping, group summaries and missingness | Values match independent calculation; measured zero differs from missing |
| Promoter/custom interval | Sequence and QC flags | Exact strand-aware sequence/boundary match against an independent script |
| Domain-to-genome mapping | Amino-acid interval and genomic CDS blocks | Exact transcript/protein mapping and exon-boundary agreement |
| Save/reopen | Restored project and exports | Inputs, selections, parameters and results survive round-trip unchanged |

Freeze gold outputs before running the comparison. Validate them with a separate
implementation or authoritative source, not by comparing PhytoScope with itself.

## Metrics

Report per task and per workflow:

- Correctness: pass/fail for each expected output, plus critical errors.
- Manual handoffs: number of distinct tools/apps invoked and intermediate files
  needed to complete the task.
- Provenance completeness: source/accession and version, input checksum,
  parameters, output path and restoration state present and consistent.
- Reproducibility: exact output agreement across repeated runs and project
  save/close/reopen round-trip.
- Runtime and peak memory where measurable, with hardware, OS, cache state,
  trial order and setup time recorded separately.

Run at least three alternating-order trials per workflow on the same machine.
Report raw per-trial values and medians/ranges; do not treat a small smoke test
as a general speed benchmark. Use the same acceptance rules and do not repair
one workflow's outputs manually without recording the intervention.

## How this supports researcher value

A successful result can support a bounded claim that the integrated workflow
reduces tool handoffs, preserves provenance, and reproduces correct outputs for
the tested task. It does **not** establish that human researchers finish faster,
find more discoveries, or prefer the interface. Those claims would require a
separate participant study, which is outside the current plan.

## Current evidence status

The repository already has cross-species annotation tuple checks, promoter
sequence checks against bedtools, small homology checks against BLASTP, real-data
calculation regressions, and project save/restore regressions. These demonstrate
specific technical properties but do not yet establish broad module-level
accuracy or general researcher value. The next step is to add a frozen,
independently checked public biological case and report its source data,
expected outputs, correctness, handoffs and provenance completeness.

## Relevant guidance

- Weber LM et al. (2019). Essential guidelines for computational method
  benchmarking. *Genome Biology* 20, 125.
  https://doi.org/10.1186/s13059-019-1738-8
- Diesh C et al. (2023). JBrowse 2: a modular genome browser with views of
  synteny and structural variation. *Genome Biology* 24, 74.
  https://doi.org/10.1186/s13059-023-02914-z
