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
| Expression | No compatible matrix validated | Not part of this case; RNA-seq evidence cited by the annotation report is not an expression matrix |

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
frozen multi-product sequence gold set covering genomic span, transcript, CDS,
protein and inferred UTRs. A targeted search located general NCBI assembly/Gene records,
not dedicated species-specific query portals; phrase this as a scoped search
result rather than proof that no such resource exists.

The paper's older rice-bean expression data were mapped against a different
cultivar/assembly. A 2026 lncRNA study also reports expression values for
transcripts from a different draft assembly and uses StringTie model IDs. Neither
is loaded as expression for GCF_029865625.1. Expression must stay out of the
evidence claim until a compatible matrix is sourced or reads are quantified
against this exact assembly with sample metadata and a reproducible workflow.

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

The completed cohort contains one full GFF3/GTF sequence-gold case and two
additional complete RefSeq GFF3 import/search/sequence-extraction checks. This
supports portability across these three portal-poor plant examples, but not an
unqualified claim that every species or annotation works unchanged. Remaining
gaps are: (1) no compatible, source-traceable expression matrix has been
validated for these exact assemblies; (2) only rice bean has a frozen
multi-product sequence gold set, while the other two cases compare
source-derived transcript/CDS sequences for one selected locus; (3) multi-locus
sequence gold should cover more genes, isoforms, missing fields, and
explicit-versus-inferred UTR cases; (4) runtime, peak memory, warnings and
export hashes need reporting on declared hardware; and (5) same-task manual
handling and IGV/JBrowse comparisons remain unexecuted. The gffutils comparison
is an objective script-workflow baseline, not a browser or usability
comparison. No participant study, time-saving claim, or new biological
discovery is claimed.
