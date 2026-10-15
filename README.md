# PhytoScope

A local JavaFX desktop browser for plant gene structures. Import GFF3/GTF
annotations, switch between sequences, inspect transcripts/exons/CDS/UTRs,
attach your own expression matrix, and export PNG or editable SVG figures.
Requires JDK 21. No external service is required while browsing local files.

## Verified reference examples

The application ships **four version-pinned reference annotation excerpts**.
Every nuclear chromosome is represented by two real gene families. These small
excerpts are not complete whole-genome annotations.

| Example ID | Plant / material | Assembly | Nuclear chromosomes |
| --- | --- | --- | ---: |
| `arabidopsis` | Arabidopsis thaliana Col-0 | TAIR10 | 5 |
| `rice` | Oryza sativa Nipponbare | IRGSP-1.0 | 12 |
| `maize` | Zea mays B73 | Zm-B73-REFERENCE-NAM-5.0 | 10 |
| `soybean` | Glycine max Williams 82 | Glycine_max_v2.1 | 20 |

All four come from the **Ensembl Plants release 62 GFF3 archives**. Assembly
accessions and nuclear chromosome counts were cross-checked with assembly
records, downloaded bytes were checked against provider checksums, and every
bundled feature row was matched to the original archive. Original sequence IDs,
coordinates and chromosome lengths are retained without alias conversion or
mixing annotation versions.

**No measured expression values are bundled.** Import your own traceable
expression matrix to populate the expression panel. The earlier organelle-only
mint and mixed/manual reference examples have been removed from the application.
Historical illustrative fixtures remain under test resources solely for parser
regression coverage; they are not scientific validation evidence and are not
packaged with the application.

See [validation and audit details](docs/chromosome-validation.md) and the
[machine-readable provenance](src/main/resources/data/reference-provenance.json).

## Run

```bash
# Windows
mvnw.cmd javafx:run

# Linux / macOS
sh mvnw javafx:run

# Build and run the executable JAR (Windows: substitute mvnw.cmd for sh mvnw)
sh mvnw clean package
java -jar target/phytoscope-0.1.0.jar
```

The first build downloads Maven and dependencies. JavaFX may print an
`unnamed module` warning when running the executable JAR.

## Browse your data

1. Use **Example datasets** to explore a reference, or **File → Import annotation**
   to open your GFF3, GFF, GTF or GFF2 file.
2. Select a sequence using the toolbar or Chromosome menu. Names and counts are
   rebuilt from the imported annotation, without a species-specific limit.
3. Search by gene ID, name, alias or description. Scroll to zoom, drag to pan,
   and click a gene to inspect its structure.
4. Use **Fit all** to fit the loaded genes, **Focus selected** for a single locus,
   or **View → Show full chromosome** for the complete declared sequence extent.
5. Import a matching expression matrix if available, then use **Export** to
   save the current tracks as PNG or SVG.

The toolbar counts **sequences present in the loaded file**, which may include
scaffolds and organelles. It does not infer missing chromosomes from a species
name. A partial annotation can contain fewer chromosomes than a whole genome.
Duplicate declarations are merged and observed bounds cannot be clipped by an
incomplete header. Dataset changes replace the old sequence list and selection.

## Input formats

Annotations use standard GFF3 `ID`/`Parent` attributes or GTF
`gene_id`/`transcript_id` attributes. Sequence lengths come from
`##sequence-region` directives; without them, bounds are inferred from observed
features. Arbitrary chromosome, scaffold and organelle names are supported.

Expression matrices are tab-, comma- or semicolon-delimited tables. The first
column contains identifiers matching the loaded genes; other columns are samples
or sample groups. Provide the measurement unit in a comment header. Loading a
new reference clears expression values from the previous dataset.

## Batch export

```bash
java -jar target/phytoscope-0.1.0.jar \
  --example maize --out figures --format svg

java -jar target/phytoscope-0.1.0.jar \
  --gff3 annotation.gff3 --expression expression.tsv \
  --loci gene1,gene2 --out figures --format both
```

Optional arguments: `--width`, `--height`, `--padding`, `--no-expression`.
Use `--help` for usage. The older `soybean-v2` ID remains an alias for `soybean`.

## Validate

With JDK 21 and Maven (or the Maven Wrapper):

```bash
# JavaFX control tests run without a native display using the test-only Monocle dependency.
mvn -Pui-tests test

# Reproduce official excerpts and verify them against source archives.
python tools/build_chromosome_fixtures.py --cache /path/to/cache --update-examples
python tools/audit_reference_datasets.py --cache /path/to/cache

# Validate the four complete annotations and build the executable JAR.
mvn -Pui-tests -Dphytoscope.fullAnnotationDir=/path/to/cache verify
```

Full genome archives are downloaded by the extraction script, not by unit tests.
See the audit document for source URLs, checks and interpretation of parser counts.
