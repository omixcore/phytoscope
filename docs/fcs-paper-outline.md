# PhytoScope — FCS Code & Data Letter: Paper Outline & Figure Specification

> **Status**: `filled` — Abstract, Introduction, Materials and Methods, Results, Discussion,
> Data Availability and Code Availability are written out in
> `docs/fcs-paper-manuscript-draft.md`. This document remains the section/table/figure specification.
> **Figure numbering authority**: `docs/fcs-figure-numbering-map.md` (B03) — all figure references
> below follow that map. **Reference verification**: `docs/fcs-reference-verification.md` (R12).
> **DOI placeholders**: unfilled, centrally registered in `docs/fcs-zenodo-handoff.md`.
> **Language**: English (FCS is an English-language journal; this document is the submission
> skeleton, so English throughout).
> **Target**: Frontiers of Computer Science (FCS), *Code & Data Letter* (primary);
> *Software Research Article* as fallback if the comparison/user study scope expands later.
> **Corresponding PRD**: `docs/prd-fcs-submission.md`. **Gap audit**: `docs/fcs-submission-gap-audit.md`.
>
> **Hard rules carried over from the PRD** (do not violate in prose or figures):
> - No new-algorithm claim. No new-biological-discovery claim.
> - No "faster/better/easier" claim unless a fresh, declared-hardware, ≥3-run benchmark supports it.
> - Historical v0.2.0 / `8fd01ca` numbers are NOT current-tree measurements; if any appear they
>   must be explicitly labelled historical. Prefer the evidence files that carry the archiving notice.
> - Honest boundaries: species-agnostic is a *design goal validated on the tested cohort only*;
>   expression validation is an import/ID-reconciliation check, not differential-expression biology.
>
> ---

## 1. Title candidates (choose one; all are Code & Data Letter length)

1. **PhytoScope: a local, species-agnostic desktop browser for querying plant gene structure and associated data with traceable, reproducible results** *(recommended — foregrounds the three claims: generality, correctness, reproducibility)*
2. **PhytoScope: a bring-your-own-data desktop genome browser for reproducible gene-structure queries across model and portal-poor plant species**
3. **PhytoScope: local, traceable, cross-species plant gene-structure browsing without a species-specific portal**

> Avoid "novel", "fast", "first", "unified framework" in the title; they over-claim relative to the evidence.

---

## 2. Abstract — structure points (one paragraph + keywords)

> **Status: `filled`** — the five-question Abstract is written out in
> `docs/fcs-paper-manuscript-draft.md` (Abstract). The structure points below remain as the spec.

The abstract must answer five questions in order, each in ≤2 sentences:

1. **What/why**: Plant genomics is fragmented across species-specific portals; researchers working
   on species without a dedicated portal need a local way to query gene structure + sequence +
   expression without uploading data.
2. **What PhytoScope is**: a local JavaFX desktop application implementing one
   import → validate → browse/query → compare → export pipeline with zero species-specific code.
3. **What we validated (the three claims)**:
   - *Generality*: same pipeline imports four model crops (Arabidopsis, rice, maize, soybean) and
     three portal-poor non-model species (rice bean, Andrographis, Hibiscus) with byte-exact
     sequence/translation gold sets.
   - *Correctness*: seven research modules checked against independent tools (bedtools, BLASTP,
     gffutils, Salmon) with exact agreement.
   - *Reproducibility*: SHA-256 provenance, versioned manifests, an independent
     `phytoscope-reference-data` repository, and self-contained `.phytoscope` projects.
4. **Honest scope**: what it is *not* — no new algorithm, no new biological discovery, no
   performance-competition claim.
5. **Availability**: software + data URLs, license (MIT), the two repositories.

**Keywords (5–6)**: genome browser; gene structure; GFF3; reproducibility; plant genomics;
species-agnostic annotation.

---

## 3. Section-by-section outline

For each section: **goal** (what to argue) → **evidence** (files/tests) → **figures/tables**.

### 3.1 Introduction

> **Status: `filled`** — the Introduction is written out in
> `docs/fcs-paper-manuscript-draft.md` (Introduction).

**Argue:**
- Motivation: species-portal fragmentation; portal-poor crops/medicinals need local, upload-free
  gene-structure querying. (Cite the three non-model species as concrete portal-poor examples.)
- Gap: existing tools are either web browsers (JBrowse2/IGV require setup/upload), script libraries
  (gffutils/Biopython — no UI, no integrated provenance), or GUI tool-suites (TBtools — script/menu
  driven, weaker traceability). PhytoScope's niche: **local execution + user-supplied data +
  site-free locus querying + traceable results**.
- Contribution preview (the three claims) + the honest boundary statement.

**Evidence files:**
- `docs/portability-and-reproducibility.md` (portability matrix §1, niche statement in preamble)
- `docs/non-model-portability-evidence.md` (portal-poor examples, claim boundary)
- `docs/prd-fcs-submission.md` (§1 positioning, §3 fair-comparison principle)

**Figures/tables**: Figure 1 (pipeline + screenshots); Supplementary Table 3 (tool-comparison matrix).

### 3.2 Materials and Methods

Structure as: **software implementation** → **input specification** → **validation data** →
**independent gold standards / comparison baselines** → **benchmark & reproducibility protocol**.

**(a) Software implementation** — Java 21 + JavaFX + Maven; module map of the seven research modules
(implementing classes named). No algorithm derivation — describe what each module computes, not a
method novelty.

**Evidence**: class names from `docs/fcs-submission-gap-audit.md` §五
(`IntervalAnalysisService`, `SequenceQualityService`, `ProteinHomologyService`,
`DomainLinkService`, `ExpressionDataService`/`SampleGroupingService`, `ComparisonFigure`,
`ProjectStore`).

**(b) Input specification** — GFF3/GTF (**legacy GFF2 at compatibility level only, through the
existing `Gff3Parser` importer; no standalone GFF2 parser and no independent GFF2 validation
claim** — a minimal fixture set and `Gff2ParserSupportTest` document the boundary), FASTA, expression
TSV/CSV; coordinate conventions (1-based closed GFF3 vs 0-based half-open BED); strand handling;
measured-zero vs missing; duplicate-ID rejection; import health-check reports (JSON/MD/TSV).

**Evidence**: `docs/input-format-specification.md` (whole doc; note the BED/bedGraph "deferred" and
the standard-nuclear-code-only translation scope).

**(c) Validation data** — Table of all assemblies/annotations with accession, release, source, SHA-256.
- Model crops: Arabidopsis TAIR10/Araport11; rice IRGSP-1.0; maize Zm-B73-NAM-5.0; soybean
  Glycine_max_v2.1 (Ensembl Plants release 62).
- Non-model: rice bean `GCF_029865625.1`; Andrographis `GCF_009805555.1`; Hibiscus `GCF_006381635.1`.
- Expression: E-MTAB-7978 (Arabidopsis); rice-bean PRJNA765494 re-quantified with Salmon 2.8.0.

**Evidence**: `docs/chromosome-validation.md`, `docs/non-model-portability-evidence.md`,
`phytoscope-reference-data/MANIFEST.json`, `phytoscope-reference-data/LICENSES.md`.

**(d) Independent gold standards & comparison baselines** — the exactness methodology:
- Sequence/translation gold: independent Python reconstructs span/transcript/CDS from raw GFF3 +
  FASTA, frozen as SHA-256; PhytoScope outputs compared byte-for-byte (`tools/build_nonmodel_sequence_gold.py`,
  `multi-locus-sequence-gold.tsv`, 25 coding transcripts = 5 loci × 5 species).
- Module baselines: bedtools 2.31.1 (promoter), BLASTP 2.12.0+/2.17.0+ (homology), independent
  Smith–Waterman reference, gffutils 0.13 (structure import), Salmon 2.8.0 (expression re-quant).
- Fair comparison protocol (alternating ≥3 fresh-process trials; setup vs task time separated;
  no usability/speed superiority claim).

**Evidence**: `docs/research-module-validation.md`, `docs/non-model-portability-evidence.md`
(§"Fair alternatives comparison"), `docs/validation-and-benchmarks.md`, `tools/*.py`.

**(e) Reproducibility protocol** — manifest v2 / schemaVersion 4, SHA-256 re-verification on load,
`reference-data` dual-mirror repository, project round-trip.

**Evidence**: `docs/portability-and-reproducibility.md` §2, `docs/input-format-specification.md`
§"Project persistence and FASTA rebinding", `phytoscope-reference-data/README.md`.

**Figures/tables** (per `docs/fcs-figure-numbering-map.md`): **Figure 2** (data model and input
formats), **Figure 3** (seven-module gold), **Figure 4** (cross-species validation and correctness),
**Figure S1** (tool comparison, supplementary); Supplementary Figure 1 (parsing/coordinates),
Supplementary Table 1 (reference-data inventory), Supplementary Table 2 (test-coverage matrix).
The old "Figure 2 = cross-species gold / Figure 4 = comparison" mapping is retired.

### 3.3 Results

**Argue** (strictly observed/measured facts, in this order):
1. **Cross-species import**: four Ensembl r62 assemblies + three non-model RefSeq — counts,
   chromosome recognition, exact structure/sequence/translation agreement (byte-exact protein
   match with NCBI RefSeq for all three non-model loci).
2. **Module correctness**: the seven modules' exact agreement with independent baselines
   (promoter 8/8 bedtools; homology 20/20 BLASTP across two conservation levels; domain 16 records;
   expression grouping; comparison layout; project round-trip).
3. **Reproducibility**: save/reopen, checksum-mismatch abort, alias recovery, schema 1–4 backward
   compatibility.
4. **Tool comparison** (Figure S1, supplementary): gffutils/Biopython frozen historical quantitative
   baselines; JBrowse2/IGV/TBtools **qualitative functional comparison only, based on public
   documentation, not on-machine measurements** (`docs/fcs-tool-comparison-final.md`). **No same-task
   wall-clock benchmark was performed** against these three GUI/web tools (no identical-task timing,
   no task-completion measurement) — the environment has no GUI session and none of the three could be
   installed and scripted onto an identical task sequence. No faster/better claim.

**Evidence**: CI results cited from `docs/research-module-validation.md` §"Observed results from CI";
`docs/chromosome-validation.md` §"Full public annotation validation" + "Reference acceptance audit";
`docs/non-model-portability-evidence.md` §"Completed public case" + "Public expression data validation".

**Figures/tables**: Figure 1, 2, 3, 4, 5 and Figure S1 (supplementary); Supplementary Figure 2
(rice-bean locus), 3 (expression), 5 (perf — see §5.1 R10 pointer).

### 3.4 Discussion

**Argue (only evidence-backed boundaries):**
- What the three claims **do** and **do not** establish: generality is proven on the tested cohort,
  not "every species"; correctness is proven on the pinned gold sets, not a general precision/recall
  estimate; reproducibility is provenance-level, not biological-validity-level.
- Design trade-offs honestly: in-memory model (interactive) vs gffutils SQLite index (setup cost);
  local BYOD (privacy, upload-free) vs web browsers (zero install, shared sessions).
- Limitations list (verbatim from evidence): genotype mismatch in rice-bean expression; 5 loci/species
  not genome-wide; expression = single assay, not DE analysis; standard nuclear code only; BED/bedGraph
  deferred; **GUI-peer comparison with JBrowse2/IGV/TBtools is qualitative-functional only — no
  same-task wall-clock benchmark was performed**; license status of 8/9 reference files unresolved
  (for the mint TPM matrix the provenance is now established as a derived product of this work, but
  the upstream read-archive redistribution terms are unconfirmed, so the file is withheld from the
  public deposit).
- Future work: multi-experiment/time-course expression, interval/signal tracks, larger multi-family
  gold sets, a same-task quantitative/timed comparison against GUI peers (JBrowse2/IGV/TBtools) in a
  GUI-capable environment, a participant user study (explicitly not performed).

> **Comparison scope — fixed wording (do not soften).** JBrowse2 / IGV / TBtools comparisons are
> **qualitative functional comparisons** derived from public documentation. **No same-task wall-clock
> benchmark was performed**; no timing/memory/throughput figure exists for these three and none may be
> added from memory or estimate. Quantitative comparison in Figure S1 is limited to gffutils and
> Biopython (frozen `historical_measured_not_new` baselines).

**Evidence**: the "Interpretation and limits" / "Remaining evidence gaps" sections of
`docs/research-module-validation.md` and `docs/non-model-portability-evidence.md`.

### 3.5 Data Availability

> **Status: `filled`** — written out in `docs/fcs-paper-manuscript-draft.md` (Data Availability).

- `phytoscope-reference-data` repository (**dual mirrors**: gitee + github, both at `66054b7`):
  **9 files / 68,518,847 B** in the repository; **public deposit = 8 files / 65,124,010 B**
  (mint TPM matrix withheld), `MANIFEST.json` + `SHA256SUMS.txt` + `LICENSES.md`;
  `sha256sum -c SHA256SUMS.txt` → **9/9 OK**.
- Each dataset's accession/release/source URL/SHA-256/access date (→ Supplementary Table 1).
- License status: **1 confirmed** (horsemint CC BY 4.0) + **8 待确认 / needs-confirmation**
  (Araport11 Chr1–5, TAIR10 FASTA, E-MTAB-7978, mint 4-leaf TPM — provenance documented in
  `PROVENANCE-mint-tpm.md`, redistribution terms still unconfirmed).
  **结论 ≠ 已获授权** (`docs/fcs-data-license-decision.md`); data deposit must not be merged into
  CC BY 4.0 while the 8 are unfinalized.
- Zenodo dataset DOI `{{DATA_DOI}}` (software `{{SOFTWARE_DOI}}`, paper `{{PAPER_DOI}}`):
  **pending / 未取得**, unfilled, centrally registered in `docs/fcs-zenodo-handoff.md`.
- Small KB-scale examples ship inside the software repo; full data retrieved from the data repo.

### 3.6 Code Availability

- Software repository URL (gitee + github mirrors) + MIT license + application version **0.3.0**
  (3.2.5 is the Spring Boot parent POM, not the application version); tag `v0.3.0` retained
  alongside `v0.2.0`.
- Build/reproduce commands (from `docs/chromosome-validation.md` §"Reproduce" and
  `docs/research-module-validation.md` §"Reproduce").
- Zenodo DOI: **pending / 未取得** — placeholder `{{SOFTWARE_DOI}}` (dataset `{{DATA_DOI}}`, paper
  `{{PAPER_DOI}}`, `{{ORCID}}`, `{{AFFILIATION}}`) **unfilled**, centrally registered in
  `docs/fcs-zenodo-handoff.md`; "待 Zenodo 发布后回填". `CITATION.cff` carries only the placeholder
  annotation, no real DOI.

### 3.7 Acknowledgments

- Data providers (TAIR/Araport, Ensembl Plants, EMBL-EBI Expression Atlas, NCBI, figshare, WSU Mint
  Genome Resources, SRA/ENA). Funding placeholder. Reviewer/colleague placeholder.

### 3.8 References

See §6 below.

---

## 4. Five main figures + one supplementary figure — specification

> Every figure: **title · content · data source (real file/test) · renderer (toolchain)**.
> `[DATA FILL]` marks values the engineer/QA must compute and paste before submission.
> **Numbering authority**: `docs/fcs-figure-numbering-map.md` — Figure 1 architecture / 2 data model
> and input formats / 3 seven-module gold / 4 cross-species validation / 5 reproducibility /
> S1 tool comparison. The table below follows that map; the retired "Figure 2 = cross-species
> portability / Figure 4 = fair comparison" mapping must not be reintroduced.
> **Figure S1 data sources are the B02-frozen `docs/paper-figures/data/*.json`**, never the
> `target/phase3-comparison/` runtime paths (design §7.6).

| Fig | Title (caption) | Content | Data source | Renderer |
|---|---|---|---|---|
| **Figure 1** | PhytoScope overview: one local pipeline from import to export | (a) pipeline schematic: Import → Health-check → Model → Browse/Query → Research modules → Export; (b) 2×3 grid of UI screenshots (import wizard + health-check report, gene-structure view, sequence/QC panel, homology/domain panel, expression panel, comparison figure); (c) annotation of the six bundled KB examples | app screenshots (re-capture post-slim, replace legacy `Figure1.jpg`); `docs/input-format-specification.md` for the health-check report layout | screenshot capture + SVG schematic (Inkscape) for the pipeline |
| **Figure 2** | Standard data model and input-format compatibility | (a) three input formats (GFF3/GTF/legacy GFF2) mapped to one unified model; (b) coordinate/value conventions (1-based closed GFF3 vs 0-based half-open BED, strand handling, measured-zero vs missing); (c) a real rice-bean `LOC124833352` minus-strand locus showing the GFF3-vs-GTF `stop_codon` boundary (1,983-nt vs 1,980-nt CDS, both translating to RefSeq `XP_087956893.1`, 660 aa) | `docs/input-format-specification.md`; `src/test/resources/non-model/vigna-umbellata/manifest.json` | matplotlib (Python) + SVG locus strip |
| **Figure 3** | Core-module golden-standard agreement | (a) panel of module-vs-baseline agreement: promoter 8/8 (bedtools), homology 20/20 (BLASTP, two families), domain 16/16, sequence QC 4/4, expression grouping; (b) one worked homology example (RBCS + CYP71A panels) with BLASTP vs PhytoScope classification matrix | `docs/research-module-validation.md` §"Observed results from CI"; `research-validation/research-module-gold-set.json`; `blastp-comparison.json` | matplotlib (Python) |
| **Figure 4** | Multi-species data validation and correctness | (a) imported gene counts for 4 model (Ensembl r62) + 3 non-model (RefSeq) assemblies; (b) CDS length vs protein aa for 3 full-gold loci; (c) byte-exact agreement grid (span/transcript/CDS/protein = independent gold / RefSeq protein) + 25 source-derived coding transcripts (5 loci × 3 species) | `docs/chromosome-validation.md` (four-species table); `docs/non-model-portability-evidence.md`; `src/test/resources/non-model/additional-nonmodel-manifest.json`; `src/test/resources/non-model/vigna-umbellata/manifest.json`; `src/test/resources/non-model/multi-locus-sequence-gold.tsv` | matplotlib (Python) bar/heatmap + SVG |
| **Figure 5** | Reproducibility: provenance and self-contained projects | (a) manifest v2 (schemaVersion 4) recording per-file SHA-256, alias map, provenance; (b) round-trip: save → reopen → verify stored hash; a checksum mismatch aborts a stale restore; (c) independent reference-data dual-mirror + `sha256sum -c` 9/9 OK | `docs/portability-and-reproducibility.md` §2; `docs/input-format-specification.md` §"Project persistence"; `phytoscope-reference-data/MANIFEST.json` + `README.md` §2 | SVG schematic (hand/Inkscape) |
| **Figure S1 (supplementary)** | Fair comparison with existing tools: shared-task agreement + design differences | (a) quantitative: gffutils 0.13 vs PhytoScope structure-tuple identity (8/8, 3 alternating trials; timings reported separately, labelled "different storage designs"); Biopython 1.88 SeqIO vs PhytoScope byte-exact sequence/strand identity (3 loci × 6 products); (b) qualitative **functional** capability matrix (PhytoScope vs JBrowse2/IGV/TBtools) — **no same-task wall-clock benchmark performed against these three**, no timing/memory/throughput panel, no superiority claim | `docs/fcs-phase3-comparison-plan.md` §3/§4.1; **`docs/paper-figures/data/gffutils-summary.json`**; **`docs/paper-figures/data/biopython-summary.json`** (frozen, `historical_measured_not_new`); `tools/compare_public_case.py`; `tools/compare_biopython_seqio.py` | matplotlib (Python) matrix + bar; SVG for capability matrix |

---

## 5. Supplementary material — specification

### 5.1 Supplementary Figures

| Sup Fig | Title | Content | Data source |
|---|---|---|---|
| Sup Fig 1 | Parsing & coordinate conventions | GFF3 1-based closed vs BED 0-based half-open; +/- strand handling; GFF3-vs-GTF `stop_codon` difference (rice-bean 1,983-nt vs 1,980-nt CDS); FASTA header parsing (first-whitespace ID) | `docs/input-format-specification.md`; `docs/non-model-portability-evidence.md` (rice-bean locus) |
| Sup Fig 2 | Rice-bean end-to-end non-model case | `LOC124833352` / `XM_088100767.1` minus-strand 3-exon structure, transcript/CDS/protein alignment to RefSeq `XP_087956893.1`, Salmon re-quantification reconciliation (34,914 genes, 0 unmatched) | `docs/non-model-portability-evidence.md`; `src/test/resources/non-model/vigna-umbellata/` |
| Sup Fig 3 | Expression matrix handling | measured-zero vs missing distinction, unit/level metadata, sample grouping (mean=median=16.0 example), ID reconciliation report | `docs/research-module-validation.md` (expression rows); `docs/input-format-specification.md` §Expression |
| Sup Fig 4 | Software architecture | class / sequence diagrams (export existing mermaid to vector); module dependency map | `docs/*.mermaid` (architecture diagrams) |
| Sup Fig 5 | Performance benchmark | import time, peak memory, search latency, comparison layout on declared hardware (fresh runs; label historical vs current) | **R10 pointer only**: all performance data live in `docs/fcs-performance-benchmark.md`, which distinguishes `current_measured` from `historical` (v0.2.0 / `8fd01ca`) and declares hardware/data/task/≥3 alternating cold starts. **RSS = `not measured`** (Windows lacks Python `resource`); no number is hard-coded in this outline. Historical `docs/portability-and-reproducibility.md` §5 values must not be read as current. |

### 5.2 Supplementary Tables

| Sup Table | Title | Content | Data source |
|---|---|---|---|
| Sup Table 1 | Reference-data inventory & provenance | 9 files (public deposit: 8): path, bytes, SHA-256, source URL, release, license status | `phytoscope-reference-data/MANIFEST.json` + `LICENSES.md` |
| Sup Table 2 | Test/evidence coverage matrix | requirement → test class / script → evidence doc → status (P0/P1/P2) | `docs/prd-fcs-submission.md` requirement pool; `docs/fcs-submission-gap-audit.md` 30-item table |
| Sup Table 3 | Tool capability & design-difference matrix | PhytoScope vs JBrowse2/IGV/TBtools/gffutils/Biopython: shared task, correctness evidence, workflow properties, design difference (no superiority) | `docs/prd-fcs-submission.md` §3.1; `docs/non-model-portability-evidence.md` §"Fair alternatives comparison" |
| Sup Table 4 | Software & reproduction information | version/tag, DOI (pending), run environment (JDK 21, Maven), reproduce commands, platform status | `docs/chromosome-validation.md` §Reproduce; `docs/desktop-distribution.md`; gap audit §二 |

---

## 6. Core contribution statement (for Abstract & Discussion framing)

**Three technical contributions (all evidence-backed, none algorithmic):**

1. **A single species-agnostic pipeline for portal-poor plants** — one parser/model/view path, zero
   species-specific code, demonstrated on 4 model crops + 3 non-model species with byte-exact
   sequence & protein gold sets. *Species entries are data (manifests), never code.*

2. **Independent cross-tool correctness verification** — seven research modules validated against
   independent tools (bedtools, BLASTP across two conservation levels, gffutils, Salmon) with exact
   agreement, plus frozen source-derived gold sets for sequence/translation.

3. **Traceable, reproducible local workflow** — SHA-256 provenance, versioned manifests
   (schemaVersion 1–4 backward compatible), an independent dual-mirror data repository, and
   self-contained `.phytoscope` projects that reproduce structure/expression/analysis state.

**What PhytoScope is NOT (state explicitly to pre-empt reviewer misreading):**
- **Not a new algorithm** for structure prediction, orthology, or expression analysis — it implements
  established computations (coordinate slicing, reverse-complement, Smith–Waterman family similarity,
  mean/median grouping) and is judged on correctness, not novelty.
- **Not a performance/competition benchmark** — no "faster than X" claim; gffutils/Biopython baselines
  compare correctness and workflow properties only.
- **Not a new biological discovery** — no new genes, isoforms, orthology, or expression findings
  claimed (the PIF6 note is a known-positive context, not a discovery).
- **Not a user-usability study** — no time-saving / easier-to-use / participant study claim.
- **Not unqualified species-independence** — validated on the tested cohort; BED/bedGraph and
  non-standard genetic codes are explicitly out of scope.

---

## 7. Reference list — verified (R12, synchronized with `docs/fcs-reference-verification.md`)

Legend: ✅ = `confirmed` (DOI resolved via Crossref / DataCite with matching title, author, volume,
pages and year) · ⚠️ = `unverifiable` (no resolvable DOI / no publisher machine-readable record;
**not used as a confirmed journal citation**). Numbering slots `[1]`–`[16]` are kept stable; `[1a]`
is an added verified update. Channels and exact bibliographic detail are in
`docs/fcs-reference-verification.md` §2. **No DOI, author, volume or page has been invented.**

**Primary data & software sources (must cite):**
1. TAIR — Lamesch P et al. (2012) *The Arabidopsis Information Resource (TAIR): improved gene annotation and new tools.* Nucleic Acids Res 40:D1202–D1210, DOI `10.1093/nar/gkr1090`. ✅
1a. TAIR 2024 — Reiser L et al. (2024) *The Arabidopsis Information Resource in 2024.* GENETICS 227(1):iyae027, DOI `10.1093/genetics/iyae027`. ✅ (replaces the old "⏳ likely Reiser" conjecture)
2. Araport11 — Cheng CY et al. (2017) *Araport11: a complete reannotation of the Arabidopsis thaliana reference genome.* Plant J 89:789–804, DOI `10.1111/tpj.13415`. ✅
3. Ensembl Plants — Yates AD et al. (2022) *Ensembl Genomes 2022: an expanding genome resource for non-vertebrates.* Nucleic Acids Res 50(D1):D996–D1003, DOI **`10.1093/nar/gkab1007`**. ✅ — **DOI corrected**: the previously noted `10.1093/nar/gkab1042` is a different paper (Dey et al. 2021, telomerase RNA). The specific "release-62 mapping" to these four GFF3 archives is documented only in `docs/chromosome-validation.md`; it has no DOI/Crossref record and is ⚠️ `unverifiable` as a bibliographic claim.
4. EBI Expression Atlas / Mergner — Mergner J et al. (2020) *Mass-spectrometry-based draft of the Arabidopsis proteome.* Nature 579:409–414, DOI `10.1038/s41586-020-2094-2`. ✅
5. Horsemint genome — Lieberman, Henry & Comai (2025), *Chromosome-scale diploid unphased assembly of horsemint (Mentha longifolia)*, figshare `10.6084/m9.figshare.28457522` (CC BY 4.0). ✅ (DataCite)
6. Rice-bean RNA-seq — BioProject **PRJNA765494** (ENA: SRP339385, *Vigna umbellata* Raw sequence reads, ICAR-NBPGR New Delhi). ⚠️ `unverifiable` as a journal citation — **no DOI, no primary publication**; cite by accession + submitter, not as a paper.

**Software baselines & tools (must cite):**
7. gffutils — Dale R (software; PyPI/GitHub). ⚠️ `unverifiable` — **no DOI**. The old "optionally Pertea & Pertea 2020 *GFF Utilities: GffRead and GffCompare*, F1000Research 9:304" has been **removed**: that DOI (`10.12688/f1000research.23297.2`) belongs to a *different tool* and must not be substituted for gffutils.
8. Biopython — Cock PJA et al. (2009) *Biopython: freely available Python tools for computational molecular biology and bioinformatics.* Bioinformatics 25:1422–1423, DOI `10.1093/bioinformatics/btp163`. ✅
9. bedtools — Quinlan AR & Hall IM (2010) *BEDTools: a flexible suite of utilities for comparing genomic features.* Bioinformatics 26(6):841–842, DOI `10.1093/bioinformatics/btq033`. ✅
10. BLAST/BLAST+ — Camacho C et al. (2009) *BLAST+: architecture and applications.* BMC Bioinformatics 10(1):421, DOI `10.1186/1471-2105-10-421`. ✅
11. Salmon — Patro R et al. (2017) *Salmon provides fast and bias-aware quantification of transcript expression.* Nat Methods 14:417–419, DOI `10.1038/nmeth.4197`. ✅
12. JBrowse 2 — Diesh C et al. (2023) *JBrowse 2: a modular genome browser with views of synteny and structural variation.* Genome Biol 24(1):74, DOI `10.1186/s13059-023-02914-z`. ✅
13. IGV — Robinson JT et al. (2011) *Integrative genomics viewer.* Nat Biotechnol 29:24–26, DOI `10.1038/nbt.1754`. ✅
14. TBtools — Chen C et al. (2020) *TBtools: An Integrative Toolkit Developed for Interactive Analyses of Big Biological Data.* Mol Plant 13(8):1194–1202, DOI `10.1016/j.molp.2020.06.009`. ✅

**Format specifications / platform (not numbered journal citations):**
15. GFF3 specification — Sequence Ontology, `github.com/The-Sequence-Ontology/Specifications/blob/master/gff3.md`. ⚠️ `unverifiable` — no DOI, no volume/pages; the old `sequenceontology.org/resources/gff3.html` returns HTTP 404. Cite as a specification URL only.
16. JavaFX / OpenJDK — platform/framework, **no DOI**. ⚠️ `unverifiable`; **not retained as a numbered citation** (cite only if journal style requires the platform).

**Removed / downgraded (no unverifiable detail kept as confirmed)**: #3 release-62 mapping,
#6 primary publication, #7 Pertea & Pertea substitution, #15 "Stein 2013" attribution,
#16 platform citation. R12 is **C class**: this is a verification conclusion with per-item channels,
not a claim that all references are confirmed; items needing publisher full-text or rights-holder
confirmation remain ⚠️ and are not written as `confirmed`.

---

## 8. Handoff notes for downstream writers (engineers / QA / writers)

- Fill every `[DATA FILL]` in the figures from the cited files — do not invent numbers.
- Figures 2/3/4 charts are generated by committed `tools/*.py` scripts; render once from the frozen
  gold sets and cite the generating script in the figure legend.
- Any historical (v0.2.0 / `8fd01ca`) number reused in text MUST carry the "historical" label from
  the source doc's archiving notice.
- Before final prose: resolve the license/gap items (Zenodo DOI, version tag, license status,
  citation verification) tracked in `docs/fcs-submission-gap-audit.md`.
