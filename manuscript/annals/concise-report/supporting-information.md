# Supporting Information

PhytoScope for portable local queries of plant genome data

## Supplementary Methods

The software and supporting evidence are available at https://github.com/omixcore/phytoscope. The code requires Java 21. Source accessions, versions, checksums, gene/transcript gold records, and the public expression matrix are supplied with this report. Full genomes, annotation archives and raw reads are fetched from their public repositories rather than duplicated in the submission package.

The three additional RefSeq assemblies are GCF_029865625.1, GCF_009805555.1 and GCF_006381635.1. Official annotation checksums and accession-matched FASTA identify the tested releases. The eight structural inventories independently specify recognized feature types, explicit parents, sequence identifiers, strands and 1-based inclusive coordinates; unsupported feature types are reported separately. Five coding loci per additional species produce six ricebean, 14 Andrographis and five Hibiscus coding-transcript gold records, with independent genomic-span, spliced-transcript and CDS SHA-256 digests.

### Reproduction commands

Run in the pinned full repository:

```sh
python tools/fetch_vigna_reference.py --directory target/non-model/vigna-umbellata
python tools/fetch_additional_nonmodel_refs.py --directory target/additional-nonmodel
python tools/build_nonmodel_sequence_gold.py
./mvnw -B test
```

The ricebean integration test uses the fetched full reference inputs:

```sh
./mvnw -B -Dtest=NonModelVignaReferenceTest \
  -Dphytoscope.vignaReferenceDir=target/non-model/vigna-umbellata test
```

For the public expression calculation, use Salmon 2.8.0 and the exact RefSeq RNA FASTA/GFF3 named in `public-expression-manifest.json`:

```sh
python tools/requantify_public_expression.py \
  --rna-fasta target/non-model/vigna-umbellata/GCF_029865625.1_rna.fna \
  --gff target/non-model/vigna-umbellata/GCF_029865625.1_genomic.gff.gz \
  --reads-dir /path/to/fastq --salmon /path/to/salmon \
  --output target/non-model/vigna-umbellata/expression \
  --threads 4 --fetch-reads --temporary-reads
```

The script verifies read byte sizes and MD5 values, exact transcript-to-gene parents, and output hashes. Its mapping contains 54,178 reference transcripts and 36,754 distinct parent genes; the retained Salmon gene-level matrix has 34,914 rows. The manifest key `gene_map_count` counts distinct parent genes, not transcript-map file rows. No tximport length correction or differential-expression inference is applied.

The archived gffutils 0.13 comparison uses 10 Arabidopsis, 24 rice, 20 maize and 40 soybean loci, with three alternating-order fresh-process trials per species and source-derived tuple agreement. Hosted-runner import and SQLite index-build timings are retained in `benchmarks/results/2026-10-18/cross-species-extraction.md`; these operations differ and are not ranked as equivalent workflow timings. Independent promoter, domain, expression-grouping and project-restoration checks use the distinct references and denominators in Table S4.

## Supplementary Tables

Tables S1–S4 are supplied in Supporting_Tables.xlsx, one worksheet per table: species and data coverage (S1), structural tuple inventories (S2), public expression inputs and output (S3), and additional module checks (S4). Counts, accessions, denominators and scope limits are retained from the source tables.

## Supporting Figure Legend

Figure S1. Public-data evaluation and supporting checks. (A) Seven-species coverage of annotation import, search/structure, selected sequence-gold checks and expression-matrix validation. A dash indicates a task not evaluated in that species. (B) Eight structural inventories, with exact recognized-tuple counts on a shared linear bar scale; all 4,641,758 tuples matched, with zero missing or extra records. (C) Assigned-fragment counts for three public ricebean libraries and their quantification against the fixed RefSeq transcript release. The IC426787 read genotype differs from the VRB3 reference cultivar; this is technical import and identifier reconciliation. (D) Independent promoter, domain, small RBCS-family similarity and expression-identifier checks. Each task retains its own denominator; the protein panel is a family-level smoke test. Full source data, versions, checksums and bounded interpretations appear in Tables S1–S4 and the provenance manifests.
