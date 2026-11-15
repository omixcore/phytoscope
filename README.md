# PhytoScope

PhytoScope is a local desktop application for exploring plant genes across species. Import an annotation, its matching genome sequence, and an optional expression matrix to inspect transcript structures, compare loci, export sequences and figures, and reopen your work as a portable project. The application runs on your files without an online service.

![PhytoScope workflow, locus queries, and validation](fig/Figure1.jpg)

[Editable Figure 1 (SVG)](fig/Figure1.svg)

## Get started

Download **v0.3.0** for Windows x64 from [Releases](https://github.com/omixcore/phytoscope/releases/tag/v0.3.0). Extract `PhytoScope-Windows-0.3.0.zip` and run `PhytoScope.exe` with its `app/` and `runtime/` folders in place. The ZIP includes a Java runtime. Verify the archive against the release `SHA256SUMS.txt` before use.

The app opens with a small Arabidopsis annotation example. Use the dataset selector for other bundled examples, or import your own files through **File → Import annotation**, **Import genome FASTA**, and **Import expression matrix**. No complete genome FASTA or measured expression matrix is bundled; sequence export and expression display require matching user-provided files.

To build from source, use JDK 21 and the Maven Wrapper:

```bash
sh mvnw clean package        # Windows: mvnw.cmd clean package
java -jar target/phytoscope-0.3.0.jar
```

## What it does

- **Import and check:** GFF3/GTF annotations, matching genomic FASTA, and tabular expression data. The import preflight checks sequence names and aliases, coordinate bounds, and file integrity.
- **Browse and compare:** search genes or intervals; inspect exon, CDS, and UTR structures; align selected loci on a common scale and export PNG/SVG with coordinates and expression values.
- **Extract and analyze:** export strand-aware genomic, transcript, CDS, UTR, and protein sequences. Five research-analysis panels cover intervals, sample groups, sequence QC, local protein similarity, and supplied domain coordinates.
- **Resume reproducibly:** save or reopen a `.phytoscope` project with its source inputs, assembly identity, alias decisions, checksums, selections, and results.

The bundled examples are small annotation subsets for Arabidopsis, rice, maize, soybean, horsemint, and spearmint plastid. Import checks and outputs depend on the actual files and releases supplied; the app does not infer missing chromosomes from a species name.

## Validation and data

Across seven plant species, independent checks found **0 missing and 0 extra** among **4,641,758 recognized structural tuples** from eight annotation inventories. Sequence products were checked at **15 loci / 25 coding transcripts** in three additional species. All **34,914** gene identifiers in a rice-bean expression matrix matched the pinned annotation. The rice-bean reads (IC426787) and reference (VRB3) represent different genotypes; this result demonstrates import and identifier compatibility, not genotype-matched biological inference.

See [non-model portability evidence](docs/non-model-portability-evidence.md), [reference validation](docs/chromosome-validation.md), and [validation commands](docs/validation-and-benchmarks.md) for inputs, denominators, and reproduction steps. [Input and project details](docs/desktop-distribution.md) and [research analysis](docs/research-analysis.md) describe further limits.

Full reference files are maintained separately and are **not bundled** with the application. Their source and license statuses are documented in [data provenance](src/main/resources/data/reference-provenance.json) and the [license decision](docs/fcs-data-license-decision.md); most full-reference files require rights-holder authorization before redistribution.

## License and citation

PhytoScope source code is [MIT licensed](LICENSE). Third-party data retain their own terms. For software citation metadata see [CITATION.cff](CITATION.cff); software and data DOIs are pending and should not be inferred from the GitHub release.
