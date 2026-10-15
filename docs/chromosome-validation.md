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

| Plant | Reference | Nuclear chromosomes | All declared/observed sequences | Parsed genes | Parsed features |
| --- | --- | ---: | ---: | ---: | ---: |
| Arabidopsis thaliana | TAIR10 | 5 | 7 | 27,655 | 791,564 |
| Oryza sativa | IRGSP-1.0 | 12 | 63 | 35,806 | 551,530 |
| Zea mays | Zm-B73-REFERENCE-NAM-5.0 | 10 | 685 | 44,303 | 1,162,376 |
| Glycine max | Glycine_max_v2.1 | 20 | 1,190 | 55,897 | 1,328,989 |

These are parser counts of features with type `gene`, not total biological gene
catalogue estimates (other annotation feature types may also represent loci).
Non-nuclear entries are intentionally retained, not silently discarded.

Source archives:

- [Arabidopsis TAIR10](https://ftp.ensemblgenomes.ebi.ac.uk/pub/plants/release-62/gff3/arabidopsis_thaliana/Arabidopsis_thaliana.TAIR10.62.gff3.gz)
- [Rice IRGSP-1.0](https://ftp.ensemblgenomes.ebi.ac.uk/pub/plants/release-62/gff3/oryza_sativa/Oryza_sativa.IRGSP-1.0.62.gff3.gz)
- [Maize B73 NAM 5.0](https://ftp.ensemblgenomes.ebi.ac.uk/pub/plants/release-62/gff3/zea_mays/Zea_mays.Zm-B73-REFERENCE-NAM-5.0.62.gff3.gz)
- [Soybean v2.1](https://ftp.ensemblgenomes.ebi.ac.uk/pub/plants/release-62/gff3/glycine_max/Glycine_max.Glycine_max_v2.1.62.gff3.gz)

## Portable fixtures and visible examples

`src/test/resources/chromosome-validation/` contains actual excerpts with two
complete gene families on each nuclear chromosome of all four plants. Gene
coordinates, IDs, hierarchy and sequence lengths are retained from the source.
The extraction script excludes nonnumeric sequence IDs **only when creating
these explicitly nuclear-chromosome fixtures**, never when users import data.

`manifest.json` records source URLs, assemblies, selected gene IDs, per-sequence
lengths and SHA-256 hashes of both downloaded archives and extracted fixtures.
`expected.tsv` records the independently extracted expected coordinate extents.

The bundled catalogue includes Arabidopsis (5 nuclear chromosomes), rice (12),
maize (10) and a separate soybean v2.1 panel (20). Rice retains its earlier
Chr1/Chr2 genes and expression IDs; the new Chr3–Chr12 rows use aliases `ChrN`
for source IDs `N`. No gene coordinates were fabricated. The older soybean GTF
example remains explicitly labelled as a three-chromosome excerpt; its assembly
and expression data are not mixed with the new soybean reference. Mint remains
an organelle panel (four sequences), not a nuclear genome example. New maize
and soybean v2.1 panels are annotation-only; no expression measurements were
invented, and loading them clears expression from the previous example.

## Automated checks

- `AdaptiveChromosomeTest`: real 5/12/10/20 chromosome excerpts, all sequence
  lengths and gene families; repeated species switches; empty inputs; synthetic
  duplicate/incomplete/invalid declarations; unannotated sequences; missing
  sequence declarations and protection against shrinking observed coordinates.
  Existing `SpeciesAgnosticTest` also covers headerless GTF and scaffold naming.
- `AdaptiveChromosomeUiTest`: actual JavaFX controls under Monocle; all chromosome
  choices, the count label, menu-to-dropdown synchronization and switching
  among 20, 12, 10, 5, 4 and 3 sequences. Snapshots go to `target/validation/`.
- `FullAnnotationValidationTest`: optional tests against the complete local
  archives; no network downloads occur inside tests.

Full verification completed with **92 tests, 0 failures, 0 errors, 0 skipped**;
Maven `verify` also packaged the executable JAR. The UI test uses headless JavaFX,
so it does not certify platform-specific native file dialogs.

## Reproduce

With JDK 21 and Maven (or the Maven Wrapper):

```bash
# The small committed fixtures run without downloading genome annotations.
mvn -Pui-tests test

# Download source archives and regenerate the portable fixtures / examples.
python tools/build_chromosome_fixtures.py --cache /path/to/annotation-cache --update-examples

# Recheck all complete annotations and package the application.
mvn -Pui-tests -Dphytoscope.fullAnnotationDir=/path/to/annotation-cache verify
```

The full-archive tests run only when `phytoscope.fullAnnotationDir` is supplied.
The UI test runs only with `-Pui-tests`; this test dependency is not packaged
with the application. Windows users can substitute `mvnw.cmd` for `mvn`.
