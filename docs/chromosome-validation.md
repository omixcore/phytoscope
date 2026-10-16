# Adaptive chromosome validation

Validated on 2026-10-15 with OpenJDK 21.0.12.1 and Maven 3.9.9.

## Behaviour

Sequence IDs and counts come from the loaded GFF3/GTF annotation. There is no
species-to-chromosome-count table in the parser, service or UI. Declarations and
observed sequence IDs are merged, preserving file order. Duplicate declarations
are counted once, invalid declarations are ignored, and coordinate bounds are
extended to include observed features. An annotation without declarations uses
its feature rows. The initial selection prefers a sequence containing genes.

A per-sequence gene index prevents repeated whole-genome filtering during
selection. Both the toolbar selector and Chromosome menu use the same service
state. Loading another dataset replaces the sequence list and selection. The
count label and status bar report loaded **sequences**, which can include
scaffolds and organelles. They do not claim to infer an organism's chromosome
complement from a partial annotation or its species name.

## Full public annotation validation

Downloaded the complete Ensembl Plants release 62 GFF3 archives below. Each was
decompressed and imported through `GenomeDataService.loadFromFile`. An independent
streaming inventory supplied expected sequence IDs and gene counts. Tests
selected every sequence, checked its gene membership and coordinate extent,
and separately counted the numeric nuclear chromosome IDs.

| Plant | Reference | Nuclear chromosomes | All declared/observed sequences | Supported gene-like loci | Parsed features |
| --- | --- | ---: | ---: | ---: | ---: |
| Arabidopsis thaliana | TAIR10 | 5 | 7 | 32,833 | 791,564 |
| Oryza sativa | IRGSP-1.0 | 12 | 63 | 38,993 | 551,530 |
| Zea mays | Zm-B73-REFERENCE-NAM-5.0 | 10 | 685 | 44,303 | 1,162,376 |
| Glycine max | Glycine_max_v2.1 | 20 | 1,190 | 57,147 | 1,328,989 |

These current model counts include supported gene-like source types (including ncRNA genes and pseudogenes), not only rows literally typed `gene`. The earlier literal-gene counts were 27,655 / 35,806 / 44,303 / 55,897 respectively; the independent full-archive test now checks all supported gene-like types explicitly.
Non-nuclear entries are intentionally retained, not silently discarded.

Source archives:

- [Arabidopsis TAIR10](https://ftp.ensemblgenomes.ebi.ac.uk/pub/plants/release-62/gff3/arabidopsis_thaliana/Arabidopsis_thaliana.TAIR10.62.gff3.gz)
- [Rice IRGSP-1.0](https://ftp.ensemblgenomes.ebi.ac.uk/pub/plants/release-62/gff3/oryza_sativa/Oryza_sativa.IRGSP-1.0.62.gff3.gz)
- [Maize B73 NAM 5.0](https://ftp.ensemblgenomes.ebi.ac.uk/pub/plants/release-62/gff3/zea_mays/Zea_mays.Zm-B73-REFERENCE-NAM-5.0.62.gff3.gz)
- [Soybean v2.1](https://ftp.ensemblgenomes.ebi.ac.uk/pub/plants/release-62/gff3/glycine_max/Glycine_max.Glycine_max_v2.1.62.gff3.gz)

## Portable fixtures and visible examples

`src/test/resources/chromosome-validation/` contains the portable validation fixtures for all four plants. Arabidopsis is additionally bundled in the application as complete nuclear Chr1–Chr5 resources; the other three examples remain deliberately small row-verified fixtures. Gene
coordinates, IDs, hierarchy and sequence lengths are retained from the source.
The extraction script excludes nonnumeric sequence IDs **only when creating
these explicitly nuclear-chromosome fixtures**, never when users import data.

`manifest.json` records source URLs, assemblies, selected gene IDs, per-sequence
lengths and SHA-256 hashes of both downloaded archives and extracted fixtures.
`expected.tsv` records the independently extracted expected coordinate extents.

The bundled catalogue contains four reviewed references: Arabidopsis TAIR10 (complete nuclear Chr1–Chr5), rice (12), maize (10), and soybean v2.1 (20). Arabidopsis is split into five chromosome-sized resources solely to keep each Git object below GitHub's 100 MB file limit; the chromosome feature rows are retained from the pinned source archive. Sequence
IDs and lengths are retained from the source, without aliases or manual overrides.

The older manually assembled excerpts and their untraceable expression values
have been removed from the application. Historical numerical and grammar fixtures
are retained only under `src/test/resources/legacy-regression`, explicitly marked
as illustrative. They exercise parser behaviour and are **not scientific evidence**.
They are excluded from the application JAR. The separate E-MTAB-7978 case now supplies measured expression with its own provenance manifest; historical illustrative matrices remain test-only.

## Reference acceptance audit (2026-10-15)

Three independent types of checks are required, rather than repeated runs alone:

1. **Reference identity:** verify the version and accession in the original
   annotation header against assembly records, with the correct reference material.
2. **File identity:** compare each downloaded archive with the provider's published
   BSD `sum` and block count; retain SHA-256 hashes to pin exact bytes locally.
3. **Biological record preservation:** match every bundled feature row to the
   original archive and compare all nuclear chromosome lengths; then import each
   complete annotation and compare its sequences and gene counts against an
   independent streaming inventory.

| Plant / reference material | Exact assembly accession | Nuclear chromosomes | Assembly record |
| --- | --- | ---: | --- |
| Arabidopsis Col-0 / TAIR10 | GCA_000001735.1 | 5 | [NCBI](https://www.ncbi.nlm.nih.gov/datasets/genome/GCA_000001735.1/) |
| Rice Nipponbare / IRGSP-1.0 | GCA_001433935.1 | 12 | [NCBI](https://www.ncbi.nlm.nih.gov/datasets/genome/GCA_001433935.1/) |
| Maize B73 / NAM 5.0 | GCA_902167145.1 | 10 | [NCBI](https://www.ncbi.nlm.nih.gov/datasets/genome/GCA_902167145.1/) |
| Soybean Williams 82 / Glycine_max_v2.1 | GCA_000004515.4 | 20 | [NCBI](https://www.ncbi.nlm.nih.gov/datasets/genome/GCA_000004515.4/) |

These are established, fixed reference assemblies, not a claim to use the newest
assembly available. Assembly and annotation versions are not interchangeable:
the annotation is explicitly Ensembl Plants release 62 for each named assembly.
The provider's release directory may contain several archives; checks use the
exact filenames recorded in the manifest.

`tools/audit_reference_datasets.py` fails on archive SHA changes, provider
checksum mismatches, accession/version mismatches, chromosome count/length
mismatches, any edited bundled feature row, or a main/test fixture discrepancy.
Its machine-readable results are shipped in
`src/main/resources/data/reference-provenance.json`.

## Automated checks

- `AdaptiveChromosomeTest`: real 5/12/10/20 chromosome excerpts, all sequence
  lengths and gene families; repeated species switches; empty inputs; synthetic
  duplicate/incomplete/invalid declarations; unannotated sequences; missing
  sequence declarations and protection against shrinking observed coordinates.
  Existing `SpeciesAgnosticTest` also covers headerless GTF and scaffold naming.
- `AdaptiveChromosomeUiTest`: actual JavaFX controls under Monocle; all chromosome
  choices, the count label, menu-to-dropdown synchronization and switching
  among 20, 12, 10 and 5 nuclear chromosomes. Snapshots go to `target/validation/`.
- `FullAnnotationValidationTest`: optional tests against the complete local
  archives; no network downloads occur inside tests.

The reference-only catalogue passed the complete Java validation suite with 0 failures, 0 errors and 0 skipped, including all four full annotation imports, the four shipped-data
integrity checks and actual JavaFX control tests. `clean verify` packaged the
executable JAR from a clean output directory. Archive inspection also confirmed
that none of the legacy fixtures or removed organelle examples were included in that application JAR. The new release separately includes the traceable E-MTAB-7978 matrix.
The UI test uses headless JavaFX,
so it does not certify platform-specific native file dialogs.

## Reproduce

With JDK 21 and Maven (or the Maven Wrapper):

```bash
# The small committed fixtures run without downloading genome annotations.
mvn -Pui-tests test

# Download source archives and regenerate the portable fixtures / examples.
python tools/build_chromosome_fixtures.py --cache /path/to/annotation-cache --update-examples
python tools/audit_reference_datasets.py --cache /path/to/annotation-cache

# Recheck all complete annotations and package the application.
mvn -Pui-tests -Dphytoscope.fullAnnotationDir=/path/to/annotation-cache verify
```

The full-archive tests run only when `phytoscope.fullAnnotationDir` is supplied.
The UI test runs only with `-Pui-tests`; this test dependency is not packaged
with the application. Windows users can substitute `mvnw.cmd` for `mvn`.

## Current release clarification (2026-10-16)

The table of four complete Ensembl release 62 imports above records the earlier
parser audit. It must not be confused with the current default Arabidopsis
example, which is **TAIR10 assembly / Araport11 original June 2016 annotation**
and contains 788,499 source feature rows and 33,997 gene/pseudogene records.
The other three bundled datasets remain small excerpts. Nuclear chromosome
counts, all sequence counts, supported gene counts and total feature rows are
different quantities and are reported separately.

Current validation also compares explicit source parent edges and exact feature
coordinates using `tools/validate_structure_inventory.py`. Unsupported source
feature types are counted in each report. Shared exon/CDS/UTR rows with multiple
transcript parents are retained under every explicit parent. Measured expression
has a separate manifest under `data/case-studies/arabidopsis-atlas`.

The complete bundled Araport11 model contains 37,898 supported gene-like loci: 33,997 `gene`/`pseudogene` records plus 3,901 `transposable_element_gene` records. The source manifest field `total_gene_like_features` historically counts only the first two types; it is not the application total.
