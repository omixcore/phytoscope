# Research analysis modules

The research package adds reusable analysis services for the next PhytoScope
workflow. All genomic coordinates exposed by these services are 1-based and
closed, matching the annotation model.

## Intervals and promoters

`IntervalAnalysisService` derives a strand-aware promoter around a gene TSS and
extracts custom intervals from a matching genome FASTA. BED input is converted
from 0-based half-open coordinates; custom tab-delimited intervals use
sequence, start, end, optional label, score and strand columns with 1-based
closed coordinates. Promoters at sequence start are clipped to base
1. FASTA and annotation assemblies must match; PhytoScope cannot infer assembly
compatibility from sequence names alone.

## Sample groups

`SampleGroupingService` stores an explicit sample-to-group map and summarizes
present values by mean or median. Missing values are excluded and measured zero
is retained. The original imported sample columns remain intact. Grouping does
not imply biological replication; users should only combine samples with a
defensible experimental design and should report the number of non-missing
observations per group.

## Sequence checks

`SequenceQualityService` reports length, GC percentage, ambiguous symbols,
coding frame and start/stop-codon flags. These are screening indicators only:
partial CDS annotations, organellar genetic codes and non-canonical starts can
be valid. It does not silently repair sequence or annotation.

## Cross-species similarity

`ProteinHomologyService` performs Smith–Waterman local protein alignment and
reports identity, aligned length, query/target coverage and reciprocal best-hit
status. The implementation caps a pairwise dynamic-programming matrix at four
million cells to bound memory use. Similarity or reciprocal best hit is a
candidate relationship, not a curated orthology assignment; large-scale use
should use a dedicated search/index workflow and a documented parameter set.

## Domain links

`DomainLinkService` maps imported 1-based amino-acid intervals to one or more
genomic CDS blocks in transcription order. It requires an exact protein ID
match between the domain record and transcript. It preserves exon boundaries
and flags coordinates extending beyond the annotated CDS. Protein isoform,
translation phase and domain database release should be checked before
interpreting a mapped region.

These services form the analysis layer for subsequent interactive panels and
portable project persistence. They do not alter the source annotation or
expression matrix.
