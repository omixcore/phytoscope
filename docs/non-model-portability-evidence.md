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

## Completed public case: rice bean (*Vigna umbellata*)

The first full non-model portability case uses the underutilized crop rice bean,
RefSeq assembly **GCF_029865625.1, Vumbellata_1.0** (cultivar VRB3), annotation
release **GCF_029865625.1-RS_2026_09**. The NCBI annotation report lists 11
assembled chromosomes, 221 unplaced scaffolds, 41,713 gene/pseudogene records
and 32,671 protein-coding genes. The GFF3 contains 232 sequence IDs; the
corresponding GTF covers 99 sequence IDs. These counts are intentionally
reported separately: the formats from the same release do not cover identical
sequence sets.

The end-to-end regression imports the complete official GFF3 and GTF archives,
checks counts and searches the same gene in both, then loads a matching
chromosome FASTA and checks a minus-strand three-exon locus
(`LOC124833352`, chromosome `NC_142174.1`) against a frozen independent sequence
gold. Its GFF3 CDS exactly matched NCBI's RefSeq CDS record `XP_087956893.1`
when the gold was generated. This example also tests a real format boundary:
GFF3 includes the terminal stop codon inside its 1,983-nt CDS, while NCBI GTF
records a 1,980-nt CDS and a separate three-base `stop_codon`. PhytoScope keeps
that codon distinct, avoids mislabeling it as inferred UTR, and translates both
representations to the same RefSeq protein. The annotation has no explicit UTR
rows, so the two UTR values are labelled **inferred**, not source-annotated.
The small frozen sequence gold and RefSeq
protein record are checked in at `src/test/resources/non-model/vigna-umbellata/`;
the complete source archives are fetched and SHA-256 verified by
`tools/fetch_vigna_reference.py`. Source URLs, access date, checksums and
expected feature counts are in `manifest.json`.

To repeat locally with Java 21:

```sh
python tools/fetch_vigna_reference.py --directory target/non-model/vigna-umbellata
sh mvnw -B -Dtest=NonModelVignaReferenceTest \
  -Dphytoscope.vignaReferenceDir=target/non-model/vigna-umbellata test
```

This is evidence that PhytoScope can import these real non-model source files
without species-specific code. It does not demonstrate support for every
annotation dialect, every incomplete source, or every species. The targeted
search found NCBI's generic assembly/annotation pages, not a dedicated
rice-bean gene query portal; avoid the stronger claim that no web resource of
any kind exists.

| Item | Public identifier/source | Verification status |
| --- | --- | --- |
| Assembly | GCF_029865625.1 | Matching chromosome 10 FASTA loaded |
| Annotation | NCBI RefSeq release GCF_029865625.1-RS_2026_09 | Complete GFF3 and GTF imports/counts asserted |
| Locus structure | LOC124833352 / XM_088100767.1 | Coordinates, minus strand, three exons and 1,983-nt CDS asserted |
| Sequence products | NC_142174.1 plus RefSeq CDS/protein records | Genomic span, transcript, CDS, UTR inference and translation compared with frozen gold |
| Expression | PRJNA765494; SRR16122599, SRR16122608, SRR16122607 | Full paired-end raw reads re-quantified against this RefSeq RNA FASTA; 34,914 gene rows, exact RefSeq ID reconciliation, zero unmatched IDs |

### Public expression data validation

The rice-bean seed-development study deposited raw reads under BioProject
PRJNA765494. It includes IC426787 and IC552985 at 5 and 10 days post-anthesis;
this check uses the three IC426787 5-DPA biological replicates
SRR16122599, SRR16122608 and SRR16122607. The ENA FASTQ URLs, byte sizes and
MD5 values, RefSeq source checksums, sample labels and per-run Salmon reports
are frozen in
[`public-expression-manifest.json`](../src/test/resources/non-model/vigna-umbellata/public-expression-manifest.json).

The original study's reads were not treated as a pre-made expression matrix
for this assembly. They were re-quantified with Salmon 2.8.0 against the
54,178-sequence RefSeq RNA FASTA for GCF_029865625.1, using transcript-to-gene
parents from the matching RefSeq GFF3. Salmon retained 34,914 gene-level
targets; all 34,914 output IDs match the imported GFF3 exactly (0 unmatched
IDs out of 41,713 annotation gene/pseudogene IDs). The matrix has three sample
columns, no missing or invalid cells, and each TPM column sums to 1,000,000
within rounding. Salmon inferred library format IU and reported a 1.0
compatible-fragment ratio for each run. The per-run assigned fragment counts
are in the manifest.

[`public-expression.tsv`](../src/test/resources/non-model/vigna-umbellata/public-expression.tsv)
is generated by Salmon `--geneMap`, which aggregates transcript estimates to
their exact GFF3 parent gene IDs. It is not tximport length-corrected and is
included to validate import, TPM parsing and ID reconciliation; it is not used
for differential-expression inference or a biological claim. The source
genotype is IC426787, whereas this RefSeq assembly is cultivar VRB3. Therefore
this demonstrates that public reads can be re-quantified and joined to the
exact assembly's gene IDs, not genotype-matched expression biology.

Reproduce after obtaining Salmon 2.8.0 and the official RefSeq inputs:

```sh
python tools/fetch_vigna_reference.py --directory target/non-model/vigna-umbellata
curl -fsSL -o target/non-model/vigna-umbellata/GCF_029865625.1_rna.fna.gz \
  https://ftp.ncbi.nlm.nih.gov/genomes/all/GCF/029/865/625/GCF_029865625.1_Vumbellata_1.0/GCF_029865625.1_Vumbellata_1.0_rna.fna.gz
gzip -dc target/non-model/vigna-umbellata/GCF_029865625.1_rna.fna.gz \
  > target/non-model/vigna-umbellata/GCF_029865625.1_rna.fna
python tools/requantify_public_expression.py \
  --rna-fasta target/non-model/vigna-umbellata/GCF_029865625.1_rna.fna \
  --gff target/non-model/vigna-umbellata/GCF_029865625.1_genomic.gff.gz \
  --reads-dir /dev/shm/phytoscope-vigna \
  --salmon /path/to/salmon \
  --output target/non-model/vigna-umbellata/expression \
  --threads 4 --fetch-reads --temporary-reads
```

The script checks all six FASTQs against the frozen ENA sizes and MD5s, maps
only transcript IDs found in both exact-version RefSeq inputs, and records the
generated matrix and run-level quantification hashes. The test
`publicRnaSeqQuantificationImportsAndReconcilesToExactRefSeqGenes` loads that
matrix through `ExpressionDataService` alongside the full GFF3 and asserts all
34,914 expression IDs reconcile to imported gene IDs.

Two additional RefSeq cases broaden the import/search cohort to unrelated
medicinal and ornamental plants: *Andrographis paniculata*
(GCF_009805555.1, Annotation Release 100; target LOC127243311 on an unplaced
scaffold) and *Hibiscus syriacus* (GCF_006381635.1, Annotation Release 100;
target LOC120212642 on an unplaced scaffold). Their complete GFF3 files are
fetched during CI and checked against the corresponding NCBI
`md5checksums.txt`; the generated runtime manifest records MD5, SHA-256, size
and source URL. Tests compare full gene/sequence inventories to NCBI release
counts and verify exact target coordinates and searchable IDs. These two cases
validate annotation import, sequence recognition, locus search and structure
presence; the test also fetches each target RefSeq scaffold FASTA, independently
reconstructs at least one transcript and CDS from source GFF3 coordinates, and
compares them with PhytoScope exports. Rice bean remains the only case with a
full per-locus gold set covering genomic span, transcript, CDS, protein and
inferred UTRs; the additional cases now have multi-locus hashes for span,
transcript and CDS. A targeted search located general NCBI assembly/Gene records,
not dedicated species-specific query portals; phrase this as a scoped search
result rather than proof that no such resource exists.

The frozen sequence gold was broadened to five coding loci on the tested
sequence accession in each species. The checked-in
`src/test/resources/non-model/multi-locus-sequence-gold.tsv` contains 25
coding-transcript records, including all coding isoforms for the selected loci,
with source-derived gene bounds, strand, exon/CDS lengths and SHA-256 hashes for
genomic span, spliced transcript and CDS. The independent builder
`tools/build_nonmodel_sequence_gold.py` reconstructs these sequences directly
from GFF3 feature rows and the matching accession FASTA without using the
PhytoScope parser. Java tests compare PhytoScope exports and locus metadata to
the frozen values. After fetching the verified public inputs, regenerate with:

```sh
python tools/fetch_vigna_reference.py --directory target/non-model/vigna-umbellata
python tools/fetch_additional_nonmodel_refs.py --directory target/additional-nonmodel
python tools/build_nonmodel_sequence_gold.py
```

The paper's older rice-bean expression data were mapped against a different
cultivar/assembly. A 2026 lncRNA study also reports expression values for
transcripts from a different draft assembly and uses StringTie model IDs. Neither
is loaded as expression for GCF_029865625.1. Expression must stay out of the
evidence claim until a compatible matrix is sourced or reads are quantified
against this exact assembly with sample metadata and a reproducible workflow.
The application-side matrix contract is tested separately in
`AdditionalNonModelReferenceTest`: a two-row synthetic probe using real NCBI
RefSeq gene IDs verifies that user-supplied sample columns, declared units,
exact-ID reconciliation and measured zero values are handled. The values are
explicitly synthetic and provide no biological expression evidence.

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

For the rice-bean case, `tools/compare_public_case.py` now implements the
scripted part of this comparison: PhytoScope and gffutils 0.13 import the same
full GFF3, query the same frozen locus, and must produce identical structure
tuples that also match the independent source GFF3 inventory. It runs three
fresh-process trials in alternating order and records index/import time,
selected-locus query latency, memory metric units, commands and versions in the
GitHub Actions artifact `non-model-comparison`. Since gffutils builds a SQLite
index while PhytoScope uses an in-memory model, the cold setup measurements are
reported separately. This does not compare manual handling or the IGV/JBrowse
interfaces; those remain open, and there is no user-effort or usability claim.

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

## Remaining evidence gaps and publication claim boundary

The completed cohort contains one full GFF3/GTF sequence-gold case, two
additional complete RefSeq GFF3 import/search/sequence-extraction checks, and
frozen source-derived multi-locus sequence gold across all three assemblies.
This supports portability across these three portal-poor plant examples, but
not an unqualified claim that every species or annotation works unchanged.
Remaining gaps are: (1) the expression validation covers only the 34,914 gene
targets emitted from the matching RefSeq RNA FASTA; absence from that matrix
does not mean zero expression for other annotation models, and the input
genotype differs from the VRB3 reference cultivar; (2) sequence gold covers
five loci per species on the tested sequence accession, not genome-wide loci,
additional annotation dialects, or all optional-field/UTR edge cases; (3)
runtime and peak memory need reporting on declared hardware; and (4) same-task
manual handling and IGV/JBrowse comparisons remain unexecuted. The
Andrographis root/leaf study also does not provide a compatible matrix for the
tested RefSeq release. The gffutils comparison is an objective script-workflow
baseline, not a browser or usability comparison. No participant study,
time-saving claim, genotype-matched biology claim, or new biological discovery
is claimed.
