# PhytoScope · A Species-Agnostic Plant Genome Structure Browser

> A lightweight, fully local desktop framework (Java + JavaFX + Spring Boot) for
> visualising gene structure from **any** plant genome annotation, with
> sample-level expression overlaid as aligned heat-map tracks.
> Ships with traceable example data from five species across the angiosperm
> phylogeny — ready to run out of the box, no download required.

---

## 1. What is this

PhytoScope is a **purely local, zero-external-service** genome structure browser
built as a **general framework rather than a single-species viewer**. It reads
standard GFF3 and GTF/GFF2 annotation files from any organism and renders genes,
transcripts, exons, CDS, UTRs and introns as zoomable, draggable tracks.
Sample-level expression is drawn as heat-map bands aligned to the same
coordinate system.

Nothing about the organism is hard-coded. Species, assembly, sequence names and
sample groups are all read from the dataset itself, so a file from TAIR,
Phytozome, Ensembl Plants, RAP-DB or NCBI drops straight in.

Typical uses:

- **Comparative genomics** — put gene models from different species side by side
  on the same footing, in the same viewer
- **Teaching** — show gene structure, alternative splicing and strand
  differences at a glance
- **QA / curation** — browse annotation files to check gene models are correct
- **Reproducible figures** — regenerate every panel of a paper from one command
  (see §7)
- **Extension base** — add variant tracks, cross-species synteny or custom
  data sources on top

## 2. Features

| Feature | Notes |
| --- | --- |
| **Species-agnostic core** | No organism assumptions in code; species, assembly and sequence spans come from the annotation header or are inferred from the data |
| **Multi-format input** | GFF3 and GTF/GFF2 through one parser core; the dialect is auto-detected from the attribute syntax |
| **Graphical import** | Open *any* annotation and expression file from the File menu; the whole interface re-populates from the new dataset |
| **Hand-drawn genome tracks** | Rendered with a JavaFX `Canvas`; no heavy external dependency, fully controllable |
| **Multi-transcript view** | Selecting a gene expands all its transcripts, distinguishing CDS (dark blue) / UTR (light blue) / introns (connector lines with direction arrows) |
| **Expression heat-map** | One panel per sample group, coloured on a log2 scale, aligned to the gene body; the number of groups is whatever the file provides |
| **Interaction** | Scroll to zoom, drag to pan, click to select, hover for a tooltip |
| **Adaptive axis** | Tick step auto-chosen from 1/2/5 × 10ⁿ; labels switch between bp / Kb / Mb |
| **Gene search** | Fuzzy match across ID, name, alias and functional description |
| **Structure statistics** | Exon count, intron count, exonic length, CDS length, exon coordinate listing |
| **Image export** | Save the whole visible track view as **PNG** (2× raster) or **SVG** (vector) |
| **Headless batch mode** | Render any number of loci to files without a display, for reproducible figures |
| **Bundled example panel** | Five species, two annotation dialects, all traceable to public sources |

## 3. Tech stack

| Layer | Choice |
| --- | --- |
| Language | Java 21 |
| Desktop UI | JavaFX 21 (controls / graphics / base) |
| Application framework | Spring Boot 3.2.5 (dependency injection, service layer) |
| Build | Maven (with Maven Wrapper, no local install needed) |
| Data formats | GFF3 / GTF (annotation), TSV / CSV (expression matrix) |

**Architecture.** A hybrid JavaFX + Spring Boot bootstrap: the Spring context is
initialised before the JavaFX thread starts, and UI components receive their data
services through dependency injection. The same jar also exposes a headless
batch entry point, so figures can be regenerated without a display.

```
PhytoScopeLauncher (main)
        │  batch flags present? ──▶ BatchRenderer (headless, reproducible figures)
        │
        ▼  otherwise
PhytoScopeApplication (extends Application, @SpringBootApplication)
        │  init(): start the Spring context
        │  start(): pull MainView from it and build the scene
        ▼
MainView ──┬── GeneTrackRenderer    (hand-drawn genome + expression tracks)
           ├── GeneDetailPane       (structure stats, expression summary, transcripts)
           ├── GenomeDataService    (@Service: annotation loading and queries)
           └── ExpressionDataService(@Service: expression matrix loading)
                    │                        │
                    ▼                        ▼
              Gff3Parser               ExpressionProfile
                    │                        │
                    ▼                        ▼
   Gene / Transcript / Exon / GenomicFeature / TissueExpression
                    │
                    ▼
        GenomeMetadata  (species, assembly, sequence regions)
```

The species-agnostic guarantee lives in `GenomeMetadata`: every display name,
axis caption and status-bar field is populated from it rather than from a
constant, so adding a new organism requires no code change at all.

## 4. Supporting any species

### 4.1 What the framework assumes

Nothing organism-specific. The metadata a dataset can declare in its header —
and which PhytoScope harvests when present — is:

| Directive | Meaning | Fallback when absent |
| --- | --- | --- |
| `##species` | Organism name | reported as "unknown" |
| `##assembly` | Assembly / annotation release | reported as "unknown" |
| `##sequence-region <id> <start> <end>` | Sequence name and span | inferred from the observed coordinates |
| Comment lines | Free-text provenance | shown in the status bar |

Header values are read whatever separator is used (`##species=X`,
`##species X` or `##species<TAB>X`), because real files differ.

### 4.2 What the parser accepts

| Situation | Handling |
| --- | --- |
| GFF3 attributes `key=value;` | native |
| GTF attributes `key "value";` | native; `gene_id` / `transcript_id` drive the hierarchy |
| Format not declared | auto-detected from whether column 9 uses quotes or `=` |
| Feature order in the file | irrelevant — assembly is two-pass |
| Multiple parents | all parents resolved |
| `%`-escaped values | decoded per the GFF3 spec |
| CDS/UTR with no matching exon | a carrier exon is synthesised so nothing is lost |
| mRNA with no `Parent` | resolved by ID prefix, then by coordinate overlap |
| FASTA trailer after the annotation | skipped |
| UTF-8 BOM | stripped |
| Semicolon, comma or tab expression matrices | all accepted; `NA`/`.` treated as missing |

## 5. Bundled example data

Five species across six datasets, chosen to span the angiosperm phylogeny, and two annotation
dialects to exercise the whole read path. Switch between them from the **Example
datasets** menu or the toolbar selector.

| # | Species | Common name | Clade | Assembly | Dialect |
| --- | --- | --- | --- | --- | --- |
| 1 | *Arabidopsis thaliana* | Thale cress | Rosid · Brassicaceae | TAIR10 / Araport11 | GFF3 |
| 2 | *Oryza sativa* | Rice (japonica) | Monocot · Poaceae | IRGSP-1.0 (Nipponbare) | GFF3 |
| 3 | *Glycine max* | Soybean | Rosid · Fabaceae | Wm82.a2.v1 | **GTF** |
| 4 | *Mentha spicata* | Spearmint | Asterid · Lamiaceae | plastid + mitochondrial contigs | GFF3 |
| 5 | *Zea mays* | Maize | Monocot · Poaceae | Zm-B73-REFERENCE-NAM-5.0 | GFF3 |
| 6 | *Glycine max* | Soybean reference | Rosid · Fabaceae | Glycine_max_v2.1 | GFF3 |

Each example deliberately demonstrates a different aspect:

- **Arabidopsis** — the reference model; ships **all five chromosomes**
  (Chr1–Chr5) so the sequence selector has a realistic spread to switch between.
  Chr2 opens with the NOR-proximal rRNA genes and the first protein-coding locus
  AT2G01008, Chr3 and Chr4 carry the AT3G01010–AT3G01060 and AT4G00005–AT4G00040
  clusters, and Chr5 starts with the minus-strand retinal-binding protein
  AT5G01010; `AT1G01020` carries two transcripts and shows alternative splicing
- **Rice** — excerpts on all **12 nuclear chromosomes**; `Os01g0100600` carries two splice isoforms
- **Soybean (legacy example)** — three chromosome excerpts, written in **GTF** with no `##sequence-region` directive, proving
  both dialect support and coordinate inference; `Glyma.01G000300` carries two
  isoforms
- **Mint** — an aromatic medicinal herb; the plastid genome is gene-dense and
  supplies the classic multi-intron loci `rpl2`, `ndhA`, `clpP` and `ycf2`, plus
  a strongly leaf-biased expression pattern

Every bundled sequence reports its published full length — the five TAIR10
chromosomes (Chr1 30.4 Mb, Chr2 19.7 Mb, Chr3 23.5 Mb, Chr4 18.6 Mb,
Chr5 27.0 Mb), IRGSP-1.0 Chr1 43.3 Mb, Wm82.a2.v1 chromosome 1 55.9 Mb and the
plastome Pt 152 kb — so the axis reads in true megabase units and the sequence
selector offers the real chromosomes under their real names. Any other naming
scheme works just as well: the selector lists whatever ids the annotation file
itself declares, so a legume scaffold (`Glyma01`) or a plastome (`Pt`) is
handled by the same code path with no per-species configuration.

Because a bundled file is only an excerpt of one chromosome arm, the view opens
on the span the data actually covers, so the genes are visible immediately.
**View → Show full chromosome** widens the window to the whole sequence to put
those genes at their true coordinates, and **View → Fit all genes** returns to
the tightest window that still contains every feature.
Provenance is recorded in each file header and in §10 below.

### Adaptive chromosome selection

The sequence dropdown and Chromosome menu are rebuilt from each imported
annotation, not from a species-name lookup or a fixed chromosome limit.
Repeated `##sequence-region` declarations are merged; sequences found only in
feature rows are included too. GTF files without declarations infer their
sequence names and coordinate bounds from the annotation. Loading a new dataset
clears the previous selection and starts on its first populated sequence.

The toolbar reports **sequences present in the loaded file**. This may include
scaffolds and organelles and may be smaller than the species' whole-genome
chromosome count when importing an excerpt. PhytoScope does not invent missing
chromosomes from a species name.

The main reference examples now include Arabidopsis (5 nuclear chromosomes),
rice (12), maize (10) and soybean v2.1 (20). Maize and soybean v2.1 contain two
real gene families per chromosome, with full sequence lengths, and no expression
measurements. The older soybean GTF example remains available separately as a
three-chromosome excerpt. See [validation details](docs/chromosome-validation.md).

## 6. Running

### Requirements

- JDK 21 (Temurin or Oracle JDK recommended)
- No Maven install needed — the project ships a Maven Wrapper

### Start (either)

**Option A — run directly (best during development)**

```bash
# Windows
mvnw.cmd javafx:run

# macOS / Linux
./mvnw javafx:run
```

**Option B — build a runnable jar**

```bash
mvnw.cmd clean package
java -jar target/phytoscope-0.1.0.jar
```

> The first run downloads Maven and dependencies; timing depends on your network.

A `Unsupported JavaFX configuration: classes were loaded from 'unnamed module'`
warning on startup is expected when launching from the fat jar and is harmless.

## 7. Reproducible figures — headless batch mode

The same jar can render any number of loci to files without a display, which is
how the panels of a paper can be regenerated identically from a script.

```bash
# one gene of a bundled species, both formats
java -jar target/phytoscope-0.1.0.jar \
     --example mint --loci rbcL,clpP --out figures --format both

# every gene of a local annotation, vector only, larger canvas
java -jar target/phytoscope-0.1.0.jar \
     --gff3 annotation.gff3 --expression tpm.tsv \
     --out figures --format svg --width 1600 --height 900
```

| Option | Meaning |
| --- | --- |
| `--gff3 <file>` | annotation file (GFF3 or GTF) |
| `--expression <file>` | optional expression matrix (TSV / CSV) |
| `--example <id>` | bundled example: `arabidopsis`, `rice`, `soybean`, `mint`, `maize`, `soybean-v2` |
| `--loci <id[,id…]>` | genes to render; all genes when omitted |
| `--out <dir>` | output directory (default `phytoscope-exports`) |
| `--format <png\|svg\|both>` | output format (default `both`) |
| `--width`, `--height` | canvas size in pixels (default 1400 × 700) |
| `--padding <ratio>` | window padding around each gene (default 0.35) |
| `--no-expression` | omit the expression heat-map tracks |
| `-h`, `--help` | print usage |

Exit codes are `0` on success, `2` for a usage error and `3` for a load or render
failure, so a batch job can be scripted safely.

Each gene is rendered on its own focused canvas, so every figure is
self-contained. File names encode locus and window, e.g.
`rbcL_1-1480.svg`, so successive runs never overwrite each other.

## 8. Interface

```
+---------------------------------------------------------------------+
| File | Example datasets | View | Help                                |
+---------------------------------------------------------------------+
| PhytoScope  [dataset ▾]  [search] [x Expr] [−][+][Fit][Focus][Export▾]|
+-----------+-------------------------------------+-------------------+
| Genes     |   Genome structure tracks            |  Gene Details     |
|           |   ─── coordinate axis ───            |                   |
| AT1G01010 |   AT1G01010 ▬▬▬▬▬▬▬▬▬▬▬▬              |  Location/Length  |
| AT1G01020 |   AT1G01020 ▬▬▬▬▬▬▬▬▬▬▬▬              |  Strand/Transcript|
| AT1G01030 |     Expression                       |  Sample expression|
|           |       Root    ████████████ 14.3      |  Exon coordinates |
|           |       Stem    ███           5.1      |                   |
+-----------+-------------------------------------+-------------------+
| source | species | format/assembly | genes | features | samples | window|
+---------------------------------------------------------------------+
```

**Controls**

| Action | Effect |
| --- | --- |
| Dataset selector | Switch between the bundled datasets |
| File ▸ Import annotation | Load any GFF3/GTF file from disk |
| File ▸ Import expression matrix | Attach a matching expression matrix |
| Scroll up / down | Zoom in / out about the canvas centre |
| Left drag | Pan the view window |
| Click a gene bar | Select it and expand the multi-transcript view |
| Hover a gene | Show a tooltip with location, length and top sample |
| "Expression tracks" checkbox | Show or hide the expression heat-map block |
| Export ▸ Export view as PNG… | Save the whole visible view as a 2× raster PNG |
| Export ▸ Export view as SVG… | Save the whole visible view as a vector SVG |

**Expression colour scale.** Values are mapped to colour after a log2 transform
(log2(value+1)), so low-abundance transcripts remain distinguishable. The ramp
runs pale blue → teal → amber → deep red; a compact legend is drawn at the right
edge of each expression block. The unit label (TPM, FPKM…) is taken from the
data file rather than assumed.

## 9. Data formats

### Annotation

Any GFF3 or GTF/GFF2 file. The minimum useful content is a `gene` feature with
child `mRNA`/`transcript` and `exon` features; `CDS` and `UTR` features are
optional and enrich the drawing.

### Expression matrix

A delimited text file whose first column holds a gene identifier and whose
remaining columns hold measurements. Comment lines starting with `#` are
ignored, so provenance can live inside the file.

```
## Unit: TPM
GeneID   Root   Stem   Leaf
AT1G01010 14.27 6.74  6.74
```

Tab, comma and semicolon are all accepted. The column headings become the
sample-group labels, so "tissues" here can equally be organs, developmental
stages or treatments.

## 10. Project layout

```
phytoscope/
├── pom.xml
├── mvnw / mvnw.cmd                      Maven Wrapper
├── README.md
└── src/
    ├── main/java/io/gitee/omixcore/phytoscope/
    │   ├── PhytoScopeLauncher.java          entry point: GUI or headless
    │   ├── PhytoScopeApplication.java       JavaFX + Spring bootstrap
    │   ├── cli/
    │   │   ├── BatchRenderer.java           headless batch rendering
    │   │   └── BatchOptions.java            command-line parsing
    │   ├── model/
    │   │   ├── GenomicFeature.java          interval feature base class
    │   │   ├── Gene.java / Transcript.java / Exon.java
    │   │   ├── FeatureType.java             feature type enum
    │   │   ├── GenomeMetadata.java          species / assembly / sequences
    │   │   ├── SequenceRegion.java          one declared sequence span
    │   │   ├── ExpressionProfile.java       per-gene expression
    │   │   └── TissueExpression.java        one sample measurement
    │   ├── parser/
    │   │   ├── Gff3Parser.java              GFF3 + GTF parser core
    │   │   └── AnnotationFormat.java        supported dialects
    │   ├── service/
    │   │   ├── GenomeDataService.java       annotation loading and queries
    │   │   ├── ExpressionDataService.java   expression matrix loading
    │   │   └── ExampleDataset.java          bundled example catalogue
    │   └── ui/
    │       ├── MainView.java                three-pane layout
    │       └── component/
    │           ├── GeneTrackRenderer.java   canvas rendering
    │           ├── GeneDetailPane.java      detail panel
    │           ├── SvgExporter.java         vector export
    │           └── ImageExporter.java       raster export
    ├── main/resources/
    │   ├── application.properties
    │   └── data/species/
    │       ├── arabidopsis/                 GFF3 + expression
    │       ├── rice/                        GFF3 + expression
    │       ├── soybean/                     GTF  + expression
    │       └── mint/                        GFF3 + expression
    └── test/java/io/gitee/omixcore/phytoscope/
        ├── Gff3ParserTest.java              parser and expression loading
        ├── SpeciesAgnosticTest.java         cross-species generality
        ├── SvgExporterTest.java             export encoding
        └── BatchOptionsTest.java            command line
```

## 11. Testing

```bash
./mvnw test        # Windows: mvnw.cmd test
```

The suite covers hierarchy assembly, alternative splicing, minus-strand exon
ordering, intron computation, attribute parsing, gene search, expression matrix
loading, SVG encoding and command-line handling. `SpeciesAgnosticTest` is the
generality guarantee: **every bundled species must load through the same code
path and report its own species, assembly and sequence regions**, and both
annotation dialects must be exercised.

## 12. Roadmap

- [ ] Read FASTA and show the sequence at base-level zoom
- [ ] Additional expression panels (developmental stages, stress conditions)
- [ ] Variant track (VCF)
- [x] Export the current view as PNG / SVG
- [x] Multi-species support with a bundled example panel
- [x] Headless batch rendering for reproducible figures
- [ ] Side-by-side multi-species comparison view (synteny)

## 13. License

MIT License

## 14. Data attribution

Bundled example coordinates come from the following public releases, and are
included for demonstration and teaching purposes only:

| Species | Source |
| --- | --- |
| *Arabidopsis thaliana* | TAIR10 / Araport11 annotation (The Arabidopsis Information Resource, <https://www.arabidopsis.org/>) |
| *Oryza sativa* | IRGSP-1.0 / RAP-DB annotation via NCBI AP014957 (Nipponbare) |
| *Glycine max* | Phytozome Glycine max Wm82.a2.v1 annotation (illustrative subset) |
| *Mentha spicata* | Complete plastid genome GenBank MG256495 (Wang et al. 2017, *Mitochondrial DNA Part B*) |

Expression values are curated from public plant transcriptome resources — PEO /
AtGenExpress (Arabidopsis), RiceXPro (rice), SoyBase (soybean) and public
*M. spicata* RNA-seq (mint) — and are intended to illustrate the visualisation
rather than to serve as a quantitative reference.
