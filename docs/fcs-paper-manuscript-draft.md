# PhytoScope — Manuscript Draft: Materials and Methods · Results · Discussion

> **Status**: first full-text draft of the three core sections. Figures are specified in
> `docs/fcs-paper-outline.md` and are drawn separately by the engineering/QA stage; this document
> references them by name only.
> **Language**: English.
> **Companion documents**: `docs/fcs-paper-outline.md` (section/table/figure spec),
> `docs/prd-fcs-submission.md` (PRD), `docs/fcs-submission-gap-audit.md` (open items).
> **Citation key**: numbers `[1]`–`[16]` follow the Reference draft in §7 of
> `docs/fcs-paper-outline.md`. Confirmed entries are cited directly; entries still awaiting
> bibliographic verification are cited as `[n — to confirm]`.

---

## Reference numbering key (see `docs/fcs-paper-outline.md` §7)

| # | Source | Status |
|---|---|---|
| [1] | TAIR (Lamesch et al. 2012; a 2024 *Genetics* update to confirm) | to confirm |
| [2] | Araport11 (Cheng et al. 2017) | to confirm |
| [3] | Ensembl Plants (Yates et al. 2022) | to confirm |
| [4] | Mergner et al. 2020, *Nature*, DOI 10.1038/s41586-020-2094-2 | confirmed |
| [5] | Lieberman, Henry & Comai 2025, figshare 10.6084/m9.figshare.28457522 | confirmed |
| [6] | Rice-bean RNA-seq, BioProject PRJNA765494 | to confirm |
| [7] | gffutils (Dale, software) | to confirm |
| [8] | Biopython (Cock et al. 2009) | confirmed |
| [9] | bedtools (Quinlan & Hall 2010) | confirmed |
| [10] | BLAST+ (Camacho et al. 2009) | confirmed |
| [11] | Salmon (Patro et al. 2017) | confirmed |
| [12] | JBrowse 2 (Diesh et al. 2023) | to confirm |
| [13] | IGV (Robinson et al. 2011) | confirmed |
| [14] | TBtools (Chen et al. 2020) | confirmed |
| [15] | GFF3 specification (Stein, Sequence Ontology) | to confirm |
| [16] | JavaFX / OpenJDK (platform; optional) | to confirm |

---

# Materials and Methods

## 2.1 Software implementation

PhytoScope is a local desktop application written in Java 21 and built with Maven; the graphical
interface uses JavaFX. The release version and tag are frozen at submission (the repository currently
tracks `v0.2.0` plus later commits; see `docs/fcs-submission-gap-audit.md` §二). The application
ships only six kilobyte-scale example annotations, performs no download at runtime, and requires a
user-imported genome FASTA for any sequence-dependent operation
(`docs/portability-and-reproducibility.md`, `phytoscope-reference-data/README.md`).

The functional core is organized around one species-independent pipeline
(*import → validate → model → browse/query → research modules → export*). There is no
species-to-chromosome-count table and no conditional species logic in the parser, service, or UI;
species entries are data, expressed as per-species manifests, never code
(`docs/non-model-portability-evidence.md`). The seven research modules and their implementation
classes are summarized in Table 2.2 (see `docs/fcs-submission-gap-audit.md` §五 for the class-level
mapping):

| Module | Implementation | What it computes |
|---|---|---|
| Promoter / interval extraction | `research/IntervalAnalysisService` | strand-aware upstream/downstream window slicing |
| Sequence QC | `research/SequenceQualityService` | GC/base counts, reading frame, start/stop codon flags |
| Protein similarity | `research/ProteinHomologyService` | Smith–Waterman pairwise similarity + family classification |
| Domain mapping | `research/DomainLinkService` | domain coordinates mapped onto CDS/exon structure |
| Expression | `service/ExpressionDataService` + `research/SampleGroupingService` | matrix import, ID reconciliation, group mean/median |
| Comparison | `comparison/ComparisonFigure` | multi-locus aligned structure/expression figure (SVG/PNG/TSV) |
| Project persistence | `project/ProjectStore` | versioned, checksummed save/reopen |

None of these modules introduces a new algorithm; each implements an established computation and is
evaluated on correctness against an independent reference, not on novelty.

## 2.2 Input specification and validation

PhytoScope accepts the formats and conventions defined in
`docs/input-format-specification.md` (status: proposed development and acceptance requirements):

- **Reference FASTA** (gzip accepted): the sequence ID is the non-empty token preceding the first
  whitespace after `>`; case and original identifiers are preserved; duplicate IDs are rejected;
  chromosome, scaffold, contig, and organelle sequences are accepted.
- **Annotation**: GFF3 (nine tab-delimited columns, 1-based inclusive coordinates, escaped
  attributes, ID/Parent relationships including multiple parents and discontinuous features), and
  GTF (via `gene_id`/`transcript_id` attributes). Legacy GFF2 is accepted through the same importer
  for compatibility but has no dedicated fixture or validation claim in this release
  (`docs/input-format-specification.md`).
- **Expression**: UTF-8 tab-delimited matrix with one identifier column and unique sample columns
  (CSV and semicolon-delimited inputs are also supported); `#unit`/`#level` metadata; measured zero is
  preserved separately from missing/invalid values.
- **Coordinate conventions**: GFF3 is 1-based closed; the deferred BED/bedGraph track input is
  0-based half-open and conversion is explicit at import/export boundaries. Minus-strand loci are
  reverse-complemented for sequence extraction while strand is recorded verbatim.
- **Translation**: the standard nuclear genetic code only; non-standard and organellar codes are
  deferred and not claimed (`docs/input-format-specification.md`, Acceptance criteria).

Import performs joint validation and emits three health-check reports (JSON, Markdown, TSV) with a
stable machine-readable schema (`specVersion, summary, pass, errorCount, warningCount, infoCount,
files, issues`). Malformed records, out-of-bounds coordinates (import-blocking ERROR), unresolved
parents, duplicate identifiers, and invalid CDS phase are reported with affected counts and, when
available, source line numbers. Failed or cancelled imports retain the previously loaded dataset.

## 2.3 Validation data

Four model crops and three non-model species were used. Model-crop annotations are complete Ensembl
Plants release 62 GFF3 archives imported through `GenomeDataService.loadFromFile`
(`docs/chromosome-validation.md`); non-model data are NCBI RefSeq archives with frozen MD5/SHA-256
(`docs/non-model-portability-evidence.md`,
`src/test/resources/non-model/additional-nonmodel-manifest.json`).

**Model crops (Ensembl Plants release 62):**

| Species | Assembly (accession) | Nuclear chr | All sequences | Supported gene-like loci | Parsed features |
|---|---|---|---|---|---|
| Arabidopsis thaliana | TAIR10 (GCA_000001735.1) | 5 | 7 | 32,833 | 791,564 |
| Oryza sativa | IRGSP-1.0 (GCA_001433935.1) | 12 | 63 | 38,993 | 551,530 |
| Zea mays | Zm-B73-REFERENCE-NAM-5.0 (GCA_902167145.1) | 10 | 685 | 44,303 | 1,162,376 |
| Glycine max | Glycine_max_v2.1 (GCA_000004515.4) | 20 | 1,190 | 57,147 | 1,328,989 |

Source URLs are listed in `docs/chromosome-validation.md`; each archive is verified against the
provider's published checksum and pinned by SHA-256.

**Non-model species (NCBI RefSeq):**

| Species | Assembly | Annotation | Full-gold locus (strand / exons / protein) |
|---|---|---|---|
| Vigna umbellata (rice bean) | GCF_029865625.1 (Vumbellata_1.0, cv. VRB3) | GCF_029865625.1-RS_2026_09 | LOC124833352 (− / 3 / 660 aa, XP_087956893.1) |
| Andrographis paniculata | GCF_009805555.1 (ASM980555v1) | Annotation Release 100 | LOC127243311 (+ / 11 / 1,123 aa, XP_051119226.1) |
| Hibiscus syriacus | GCF_006381635.1 (ASM638163v2) | Annotation Release 100 | LOC120212642 (− / 2 / 207 aa, XP_039066805.1) |

The rice-bean case includes the complete official GFF3 (232 sequence IDs) and GTF (99 sequence IDs;
the formats of one release do not cover identical sequence sets) with asserted import counts, plus a
matching chromosome FASTA (NC_142174.1). Andrographis (27,712 genes) and Hibiscus (106,303 genes)
each contribute a full per-locus gold set on an unplaced scaffold. A frozen multi-locus gold
(`src/test/resources/non-model/multi-locus-sequence-gold.tsv`) records 25 coding transcripts across
15 loci (five loci per non-model species, including all coding isoforms) with source-derived bounds,
strand, exon/CDS lengths, and SHA-256 hashes for genomic span, spliced transcript, and CDS.

**Expression data.** For Arabidopsis, the EBI Expression Atlas E-MTAB-7978 tissue atlas matrix
(DOI 10.1038/s41586-020-2094-2 [4]) is used; provenance and source hashes are in
`examples/arabidopsis-atlas/manifest.json`. For rice bean, raw RNA-seq reads deposited under
BioProject PRJNA765494 [6] (three IC426787 5-DPA biological replicates SRR16122599, SRR16122608,
SRR16122607) were re-quantified with Salmon 2.8.0 [11] against the 54,178-sequence RefSeq RNA FASTA
for GCF_029865625.1, using transcript-to-gene parents from the matching RefSeq GFF3. Salmon retained
34,914 gene-level targets, all of which match the imported GFF3 gene IDs exactly (0 unmatched of
41,713 gene/pseudogene IDs). This demonstrates import + ID reconciliation, not genotype-matched
expression biology: the source genotype (IC426787) differs from the reference cultivar (VRB3)
(`docs/non-model-portability-evidence.md`).

## 2.4 Independent gold standards and comparison baselines

Correctness is established against **independently derived** references, not against PhytoScope's own
outputs.

**Sequence and translation gold.** `tools/build_nonmodel_sequence_gold.py` reconstructs, directly
from raw GFF3 feature rows and the matching accession FASTA *without* the PhytoScope parser, the
strand-aware genomic span, spliced transcript, CDS, translated protein, and inferred 5'/3' UTRs for
each selected locus. Each product is frozen as a SHA-256 hash in
`src/test/resources/non-model/<species>/gold-<locus>.fasta` and in
`multi-locus-sequence-gold.tsv`; Java tests then assert PhytoScope exports match the frozen values
byte-for-byte. For the three non-model full-gold loci, the translated protein was additionally
compared byte-for-byte with the NCBI RefSeq protein record (XP_087956893.1, XP_051119226.1,
XP_039066805.1) — identical in all three cases.

**Module baselines.** Promoter intervals are checked against `bedtools getfasta -s` (bedtools 2.31.1
[9]) on eight Arabidopsis chromosome-1 loci (four plus-strand, four minus-strand; 2,000 bp upstream +
200 bp downstream). Protein family similarity is checked against NCBI BLASTP [10] on two families of
different conservation level: the high-conservation, paralog-rich RBCS family (four species) and the
medium-conservation CYP71A cytochrome P450 family (Arabidopsis, pairwise identity 50.6–60.3%;
`target/research-validation/blastp-comparison-qa.json`). Domain
coordinates are checked against an independent Python reference. Structure import is checked against
gffutils 0.13 [7] (see §2.6).

**Comparison baselines and fair-comparison protocol.** For the same frozen files, locus list, gold
set, and output requirements, we compare only *shared* tasks; design differences are reported
separately and are not ranked as better/worse. Quantitative baselines (gffutils, Biopython) run as
three fresh-process trials in alternating order; setup time (index build / import) is reported
separately from task time (selected-locus query). Qualitative comparisons (JBrowse 2, IGV, TBtools)
are capability/design comparisons based on public documentation, not on-machine timings. No
usability, time-saving, or discovery-rate claim is made
(`docs/non-model-portability-evidence.md` §"Fair alternatives comparison",
`docs/fcs-phase3-comparison-plan.md`).

## 2.5 Reproducibility protocol

A saved `.phytoscope` project is a versioned, self-contained unit
(`docs/portability-and-reproducibility.md` §2; `project/ProjectStore.java`):

- manifest v2 records `schemaVersion` (4), `applicationVersion`, assembly identity, annotation
  metadata (format/version/source), per-file SHA-256 integrity results, and the user-confirmed
  sequence-name alias map; schema versions 1–4 remain readable, and older manifests without v2 fields
  load with those fields recorded as `unknown`;
- the exact file bytes on disk (including compression) are hashed and re-verified on load — a
  checksum mismatch aborts a stale restore;
- the original source label and full import report are retained, and view state (sequence, selected
  gene, window) plus research parameters (promoter windows, interval format) are restored.

Complete reference data are distributed in the independent `phytoscope-reference-data` repository
(9 files, 68,518,847 bytes, dual gitee/github mirrors at commit `acac2cf`), governed by
`MANIFEST.json` (per-file bytes, SHA-256, source URL, release, license status) and `LICENSES.md`;
`SHA256SUMS.txt` allows `sha256sum -c` self-verification (9/9 OK). License status is itemized in
`phytoscope-reference-data/LICENSES.md` (one file confirmed CC BY 4.0; the remainder pending author
confirmation, including one mint TPM matrix whose source is not yet established).

## 2.6 Comparison and benchmark protocol (phase 6)

**gffutils (structure import).** `tools/compare_public_case.py` imports the same 726,726-line rice-bean
GFF3 with both PhytoScope (`--benchmark`, `-Xmx3g`) and gffutils 0.13 (on-disk SQLite index), queries
the same frozen locus `gene-LOC124833352`, and compares both outputs to structure tuples derived
independently from the source GFF3 (8 tuples: 1 gene + 1 mRNA + 3 exon + 3 CDS; the annotation has no
explicit UTR rows). Three fresh-process trials run in alternating order; cold setup (PhytoScope
import vs gffutils index build) and selected-locus query latency are reported separately.
(`docs/fcs-phase3-comparison-plan.md` §3.1.)

**Biopython (sequence reconstruction).** `tools/compare_biopython_seqio.py` uses Biopython 1.88
`SeqIO`/`Seq` [8] plus source GFF3 rows to reconstruct strand-aware genomic span, spliced transcript,
CDS, translation, and inferred UTRs for the three non-model loci, and compares each product
byte-for-byte with the frozen gold FASTA (the same files PhytoScope's Java tests are asserted against).
Three trials per species; timings are per-product wall-clock medians in a single process, not a speed
claim. (`docs/fcs-phase3-comparison-plan.md` §3.2.)

**Qualitative tools.** JBrowse 2 [12] was not installed in the environment; IGV 2.19.3 [13] and
TBtools-II [14] are desktop GUIs without a scriptable headless snapshot in this environment. All three
are therefore reported as qualitative capability/design comparisons only
(`docs/fcs-phase3-comparison-plan.md` §4).

**Environment.** gffutils/Biopython trials ran on Windows 11 amd64, Python 3.13.12, gffutils 0.13,
Biopython 1.88, Java 21 (OpenJDK 21.0.6). bedtools (2.31.1) and the BLASTP 2.12.0+ module checks were
executed in the CI Ubuntu runner (workflow run #192), not on this machine.

---

# Results

Results follow the five main figures of `docs/fcs-paper-outline.md` §4.

## 3.1 One local pipeline across model and non-model species (Figure 1)

The same import → validate → browse/query → export path loads seven assemblies without
species-specific code. The four model-crop annotations import at full scale with the counts in
Table 2.3 (`docs/chromosome-validation.md` §"Full public annotation validation"): Arabidopsis
(7 sequences, 32,833 genes, 791,564 features), rice (63 / 38,993 / 551,530), maize (685 / 44,303 /
1,162,376), and soybean (1,190 / 57,147 / 1,328,989). Each was checked against an independent
streaming inventory for sequence IDs, gene counts, and per-sequence coordinate extents, with a
reference-acceptance audit confirming assembly accessions GCA_000001735.1, GCA_001433935.1,
GCA_902167145.1, and GCA_000004515.4 (`docs/chromosome-validation.md` §"Reference acceptance audit").
The non-model species import end-to-end through the same path (Table 2.4).

## 3.2 Cross-species portability with byte-exact sequence gold (Figure 2)

For the three non-model full-gold loci, PhytoScope's exported genomic span, spliced transcript, CDS,
translated protein, and inferred UTRs match the independently derived frozen gold byte-for-byte
(SHA-256), and the translated proteins match the NCBI RefSeq records XP_087956893.1 (rice bean,
660 aa), XP_051119226.1 (Andrographis, 1,123 aa), and XP_039066805.1 (Hibiscus, 207 aa) exactly
(`docs/non-model-portability-evidence.md`,
`src/test/resources/non-model/additional-nonmodel-manifest.json`). The rice-bean locus
LOC124833352 (minus strand, three exons) exercises a real format boundary: its GFF3 CDS is 1,983 nt
(terminal stop codon included) while the GTF records a 1,980-nt CDS plus a separate `stop_codon`;
PhytoScope keeps the codon distinct, avoids mislabeling it as inferred UTR, and translates both
representations to the same RefSeq protein. The broader multi-locus gold covers 25 coding transcripts
across 15 loci (`multi-locus-sequence-gold.tsv`).

On the expression side, the rice-bean re-quantification (Salmon 2.8.0) produced 34,914 gene rows
whose IDs reconcile to the imported GFF3 exactly (0 unmatched) and whose three TPM sample columns sum
to 1,000,000 within rounding (`docs/non-model-portability-evidence.md` §"Public expression data
validation").

## 3.3 Core-module golden-standard agreement (Figure 3)

Seven modules were checked against independent references
(`docs/research-module-validation.md` §"Observed results from CI"):

- **Promoter**: 8 loci (4 plus, 4 minus strand), all 2,200-bp intervals matched the direct FASTA
  slice/reverse-complement reference and `bedtools getfasta -s` (8/8 exact).
- **Sequence QC**: 4 real RBCS CDS (lengths 411, 561, 546, 546 nt), all in frame, zero ambiguous
  bases, no QC flags.
- **Protein similarity**: 4 Arabidopsis RBCS proteins, 16 pairwise hits; identity, aligned length,
  query/target coverage, and reciprocal-best-hit flags matched the reference Smith–Waterman
  implementation.
- **Cross-species homology vs BLASTP**: 4 species; 12/12 non-self RBCS-family pairs and 8/8
  unrelated-family controls classified identically (20/20 agreement).
- **Medium-conservation homology vs BLASTP**: Arabidopsis CYP71A (pairwise identity 50.6–60.3%);
  12/12 within-family pairs and 8/8 unrelated controls classified identically (20/20, 0 mismatches).
- **Expression grouping**: AT1G01860 leaf-labelled conditions mean = median = 16.0 TPM (n = 3), low
  group mean = 2.5 (n = 2); AT5G38430's two measured zeros remain zero (n = 2).
- **Domain mapping**: 16 domain/deletion records across 13 genes, including a multi-exon junction,
  matched the independent Python coordinates.

Across the two homology families, agreement with BLASTP is 20/20 in each panel (agreement = 1.0).
These are exactness checks on pinned inputs, not a general precision/recall estimate.

## 3.4 Fair comparison with existing tools (Figure 4)

**gffutils (structure import).** PhytoScope and gffutils 0.13 produced identical structure tuples for
the frozen rice-bean locus, and both matched the source-derived gold in all three trials (3/3)
(`docs/fcs-phase3-comparison-plan.md` §3.1). Setup and task times were reported separately because
the storage designs differ (PhytoScope is in-memory; gffutils builds an on-disk SQLite index):

| Measure | PhytoScope | gffutils 0.13 |
|---|---|---|
| Cold setup (median of 3) | 2,670.76 ms (import) | 57,801.95 ms (index build) |
| Selected-locus query, 100 runs (median / p95) | 13.46 ms / 16.16 ms | 0.24 ms / 0.28 ms |
| Import scale | 41,713 genes / 726,726 features / 232 sequences | (same GFF3) |
| Peak heap (PhytoScope, pool peak, −Xmx3g) | Old Gen ≈ 1.58 GB, Eden ≈ 0.49 GB | RSS not measured (Windows lacks `resource`) |

The cold-setup difference reflects the in-memory vs SQLite design choice, **not** a speed
superiority; the query-latency difference likewise reflects an in-memory scan vs an indexed lookup.
No "faster" claim is made.

**Biopython (sequence reconstruction).** Biopython 1.88 `SeqIO` reconstruction of all five products
(genomic span, transcript, CDS, translation, inferred UTRs) matched the frozen gold byte-for-byte in
all three non-model species, with correct strand handling on the two minus-strand loci (rice bean,
Hibiscus) and the one plus-strand locus (Andrographis)
(`docs/fcs-phase3-comparison-plan.md` §3.2).

**JBrowse 2 / IGV / TBtools (qualitative).** These were compared on shared tasks only, from public
documentation, not on-machine (Table 3.4, Supplementary Table 3). All three can locate genes and
display structure tracks; the matrix-level sample-grouping semantics, native multi-locus aligned
comparison figure, and self-contained project restore are PhytoScope-specific capabilities, while the
three browsers provide broader coverage of alignment/variant/quantitative tracks that PhytoScope does
not attempt. These are design differences listed without ranking.

## 3.5 Reproducibility: provenance and self-contained projects (Figure 5)

Project save/reopen round-trips reproduce structure, expression, and analysis state
(`docs/portability-and-reproducibility.md` §2; `project/ProjectStoreTest`). On reopen, the stored
reference SHA-256 is verified before automatic FASTA rebinding; a hash mismatch or missing file
requires an explicit user choice, and a tampered restore aborts. Manifest v2 (`schemaVersion` 4)
remains backward-readable across schema versions 1–4 with missing fields recorded as `unknown`. The
independent `phytoscope-reference-data` repository self-verifies with `sha256sum -c SHA256SUMS.txt`
(9/9 OK), and its `MANIFEST.json` + `LICENSES.md` record per-file provenance and license status
(`phytoscope-reference-data/README.md` §2).

---

# Discussion

## 4.1 What PhytoScope contributes

PhytoScope makes three evidence-backed contributions, none of them algorithmic:

1. **A single species-agnostic pipeline for portal-poor plants.** One parser/model/view path with no
   species-specific code imports four model crops and three non-model species and reproduces
   byte-exact sequence and protein gold sets. Species entries are data (manifests), never code.
2. **Independent cross-tool correctness verification.** Seven research modules are validated against
   independent tools — bedtools (8/8 promoters), BLASTP (20/20 agreement on two conservation levels),
   gffutils (3/3 identical structure), and Salmon (34,914 genes, 0 unmatched IDs) — plus frozen
   source-derived gold sets for sequence and translation.
3. **A traceable, reproducible local workflow.** SHA-256 provenance, versioned manifests
   (schemaVersion 1–4 backward compatible), an independent dual-mirror data repository, and
   self-contained `.phytoscope` projects that reproduce structure/expression/analysis state.

The value proposition is therefore *portable, correct, and reproducible local querying* for species
without a dedicated portal — not a faster or more novel method.

## 4.2 What PhytoScope is not (honest boundaries)

To prevent misreading, we state the boundaries explicitly:

1. **Not a new algorithm.** Structure prediction, orthology, and expression analysis are not
   re-derived; the modules implement established computations (coordinate slicing, reverse
   complement, Smith–Waterman family similarity, mean/median grouping) and are judged on correctness.
2. **Not a performance-competition tool.** The gffutils cold-setup difference (2,670.76 ms import vs
   57,801.95 ms index build) and query-latency difference (13.46 ms vs 0.24 ms) reflect the
   in-memory vs SQLite storage design, not speed superiority; both directions are reported without a
   "faster" claim.
3. **Not a new biological discovery.** No new genes, isoforms, orthology, or expression findings are
   claimed; the PIF6 note in `docs/case-study-pif6-known-positive.md` is a known-positive context, not
   a discovery.
4. **Not a user-usability study.** No participant study, time-saving, easier-to-use, or
   discovery-rate claim is made; JBrowse 2 / IGV / TBtools are qualitative design comparisons only.
5. **Not unqualified species-independence.** Generality is demonstrated on the tested cohort;
   BED/bedGraph tracks and non-standard genetic codes are out of scope, and "species-agnostic" means
   the same code path, not that every assembly or annotation dialect is supported
   (`docs/non-model-portability-evidence.md` §"Claim under test").

## 4.3 Limitations

- **Expression scope.** The rice-bean expression check validates import and ID reconciliation only;
   the source genotype (IC426787) differs from the reference cultivar (VRB3), and no differential
   expression is inferred. Andrographis and Hibiscus have no compatible public matrix (recorded as
   "not applicable").
- **Gold-set breadth.** Sequence gold covers five loci per non-model species (25 transcripts), not
   genome-wide; the homology checks span two families with taxonomically unbalanced negatives and
   are not a general precision/recall estimate.
- **Runtime/memory.** gffutils peak RSS was not measurable on Windows (no `resource` module); PhytoScope
   memory is reported as JVM pool peak (Old Gen ≈ 1.58 GB under `-Xmx3g`), not RSS. Current-tree
   performance numbers from phase 6 are declared-hardware, 3-trial values; historical v0.2.0 /
   `8fd01ca` numbers in `docs/portability-and-reproducibility.md` §5 are archived and must not be read
   as current.
- **Comparison scope.** JBrowse 2 / IGV / TBtools are not measured on-machine; manual handling and
   participant studies were not performed.
- **Data licensing.** License status is resolved for only 1 of 9 reference files (horsemint CC BY 4.0);
   the remaining items, including a mint TPM matrix whose source is not yet established, require author
   confirmation before redistribution (`phytoscope-reference-data/LICENSES.md`).

## 4.4 Future work

Multi-experiment/time-course expression browsing, BED/bedGraph interval and signal tracks,
non-standard genetic-code and organellar translation, broader multi-family and negative-set gold
standards, declared-hardware cross-platform benchmarking, and a participant user study are all
documented as future work and are not claimed here
(`docs/portability-and-reproducibility.md` §3, `docs/input-format-specification.md`).

---

*End of draft sections. Introduction, Abstract, Data/Code Availability, Acknowledgments, and the
finalized References are written against the outline in `docs/fcs-paper-outline.md` once the
gap-audit items (version tag, DOI, license status, citation verification) are closed.*
