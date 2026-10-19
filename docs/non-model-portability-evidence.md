# Non-model portability: evidence and limits

## Claim under test

PhytoScope is intended as a local, species-agnostic framework for querying gene
annotations and associated user-supplied sequence/expression data, including for
plants without a dedicated species portal. “Species-agnostic” means that the
same parser, search and view code is used after importing compatible files; it
does not mean every assembly, annotation dialect or biological question is
supported without qualification. A claim of portability requires tests on
public data from species outside the four current crop/model fixtures.

## What the current evidence establishes

The checked-in reference excerpts cover Arabidopsis, rice, maize and soybean.
They exercise GFF3 and GTF, multiple chromosome naming schemes, multichromosome
search and exact gene/transcript/exon/CDS/UTR tuple comparisons. These are useful
parser regression datasets, but they are not evidence that a non-model species
with no dedicated portal has been imported end to end. The mini gold set in
`Gff3ParserEdgeCaseTest` independently specifies accession-style sequence IDs,
minus-strand coordinates, missing optional metadata, and exact CDS/UTR lengths;
it tests parser behavior, not a real species.

The legacy spearmint expression table is illustrative only. Its values have no
verified RNA-seq accession and must not be used as biological validation. The
legacy test fixture remains solely for UI/parser regression and is not shipped
as a species dataset.

## Candidate public case and current blocker

The 2026 chromosome-level *Primula kwangtungensis* genome paper reports assembly
GCA_054094925.1, nine chromosomes, a public GFF3/genome/sequence supplement
(Figshare article 29479610), and six public RNA-seq samples (SRX26340530–35).
This is a strong candidate because it is a recently resourced, rare plant. It is
not yet a completed PhytoScope validation case: the supplement download failed
in the current evaluation environment, and the six raw RNA-seq runs are tens of
gigabytes compressed. Do not count it as an imported dataset or expression
validation until files are successfully retrieved, checksummed and processed.

| Item | Public identifier | Verification status |
| --- | --- | --- |
| Genome assembly | GCA_054094925.1 | Paper/NCBI metadata checked; FASTA not imported here |
| Annotation | Figshare article 29479610, `gene.gff3` | File metadata/checksum checked; download blocked here |
| Expression reads | SRX26340530–35 | Run metadata checked; no quantification performed |
| End-to-end import/search/structure/export | — | Not yet tested |

The evaluation must not infer expression levels from annotation, genome size or
RNA-seq accession metadata. Expression requires a count/abundance matrix or a
reproducible quantification workflow, with sample metadata and reference
assembly compatibility documented.

## Required end-to-end portability record

For each additional species, freeze and publish a manifest containing the
scientific name, assembly accession/version, annotation release/source, source
URLs, retrieval date, license, file sizes and SHA-256 checksums. Record whether
the genome FASTA, annotation, transcript/CDS/protein FASTA, expression matrix
and sample metadata are supplied or absent. Import with the released app and
unchanged code; record parser version, warnings, sequence IDs discovered, gene
counts, index/build time, peak memory, query results and exported file hashes.
Keep species-specific configuration in data manifests, never conditional
species logic in parser or UI code.

Construct an independent gold set from source rows and FASTA slices before
running PhytoScope. For each selected locus include sequence ID, strand,
gene/transcript IDs, transcript bounds, sorted exon/CDS/UTR intervals and
strand-aware extracted sequence. Include positive and negative searches,
multi-isoform and minus-strand genes, and records with absent optional fields.
Report exact matches and missing/extra tuples separately. For large inputs,
report complete import success, memory and time on declared hardware; do not
extrapolate from excerpts.

## Fair alternatives comparison

For the same frozen files, locus list, gold set and output requirements, compare
these objective properties:

| Workflow | Correctness evidence | Countable workflow properties |
| --- | --- | --- |
| PhytoScope | Exact source-derived structure/sequence gold set; save/reopen and export hashes | Invoked applications, manual intermediate files, provenance fields retained |
| Manual annotation handling | Explicit commands/editor steps, same independent gold set | Commands, handoffs and edits required; source files and scripts retained |
| General genome browser (IGV or JBrowse) | Same loci and visible coordinates; record whether each required output is directly available | Browser setup/import requirements, external tools/files required for structure export and sequence derivation |
| Script workflow (e.g. gffutils plus independent FASTA slicing) | Exact same gold set | Script/dependency versions, commands, index files, handoffs and reproducibility |

Use at least three alternating-order runs on the same machine. Freeze inputs
and acceptance criteria first; report per-task correctness, applications,
intermediate files, provenance completeness, output reproducibility, runtime
and peak memory. Separate setup from task execution. Without participants,
report only those observed workflow properties. Do not claim that PhytoScope is
easier, saves researchers time, improves usability or increases discovery.

## Information users must supply; information annotations cannot establish

The annotation identifies the features and coordinates encoded by its source;
it does not independently prove that those gene models are biologically
correct. A matching genome FASTA is required for sequence extraction. Chromosome
lengths are reliable only when declared in the annotation or supplied from the
matching assembly. Expression values, units, normalization, sample identities,
replicate structure and tissue/condition labels require a separate matrix and
metadata. Functional descriptions, orthology, causal effects, expression
patterns and missing isoforms cannot be inferred solely from feature
coordinates. Missing fields stay missing; the software must not invent them.

## Publication claim boundary

The software-framework claim can be supported by successful, independently
checked imports and repeatable queries on several genuinely non-model public
datasets. A biological discovery is an optional case study, not a prerequisite
for demonstrating portability and not a substitute for parser/sequence gold
standards. Until at least one non-model case is downloaded and validated end to
end, describe the current evidence as cross-species format regression plus a
non-model portability evaluation plan, rather than demonstrated coverage of
portal-poor species.
