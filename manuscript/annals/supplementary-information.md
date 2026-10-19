# Supplementary Information

## Supplementary Methods S1. Reproduction environment and source files

The code and test suite are maintained at https://github.com/omixcore/phytoscope. Before submission, replace the moving branch URL with a permanent release/archive DOI and record the exact commit hash used for the article. Species accessions, annotation versions, URLs, and integrity checks are defined in the repository manifests. The three non-model NCBI GFF3 downloads are checksum-verified at fetch time. The rice-bean RNA FASTA and GFF3 are pinned by SHA-256, and six paired FASTQ files are pinned by byte size and MD5 in `src/test/resources/non-model/vigna-umbellata/public-expression-manifest.json`.

### Reproduction commands

Use Java 21 for the application tests. Public reference files are fetched into a generated directory and are not required to be checked into the source repository.

```sh
python tools/fetch_vigna_reference.py --directory target/non-model/vigna-umbellata
python tools/fetch_additional_nonmodel_refs.py --directory target/additional-nonmodel
python tools/build_nonmodel_sequence_gold.py
./mvnw -B test
```

The dedicated rice-bean integration test is:

```sh
./mvnw -B -Dtest=NonModelVignaReferenceTest \
  -Dphytoscope.vignaReferenceDir=target/non-model/vigna-umbellata test
```

For the archived public expression calculation, use Salmon 2.8.0 and the exact RNA FASTA and GFF3 identified in the manifest:

```sh
python tools/requantify_public_expression.py \
  --rna-fasta target/non-model/vigna-umbellata/GCF_029865625.1_rna.fna \
  --gff target/non-model/vigna-umbellata/GCF_029865625.1_genomic.gff.gz \
  --reads-dir /path/to/fastq \
  --salmon /path/to/salmon \
  --output target/non-model/vigna-umbellata/expression \
  --threads 4 --fetch-reads --temporary-reads
```

The script validates fetched read size and MD5, transcript-to-gene mapping, matrix parsing, and output hashes. The checked-in `public-expression.tsv` is the resulting import fixture. It is explicitly not a differential-expression result.

## Table S1. Species and data coverage

| Taxon | Assembly / annotation | Data evaluated | Test scope |
|---|---|---|---|
| *Arabidopsis thaliana* | TAIR10/Araport11; Ensembl Plants release 62 | Full annotation, chromosome FASTA, selected expression and protein examples | Full recognized-feature tuple audits; promoter, CDS QC, homology, expression arithmetic, domain mapping; app regressions |
| *Oryza sativa* | IRGSP-1.0; Ensembl Plants release 62 | Full annotation and source excerpt | Full tuple audit; multi-chromosome parser comparison |
| *Zea mays* | B73 NAM5; Ensembl Plants release 62 | Full annotation and source excerpt | Full tuple audit; multi-chromosome parser comparison |
| *Glycine max* | v2.1; Ensembl Plants release 62 | Full annotation and source excerpt | Full tuple audit; multi-chromosome parser comparison |
| *Vigna umbellata* | GCF_029865625.1, Vumbellata_1.0; GCF_029865625.1-RS_2026_09 | Full GFF3 and GTF; matching chromosome FASTA and RNA FASTA; three public read libraries | Full imports; query/structure; independent selected-sequence gold; Salmon re-quantification and exact ID reconciliation |
| *Andrographis paniculata* | GCF_009805555.1; Annotation Release 100 | Full GFF3; selected scaffold FASTA | Full GFF3 import/search; selected-locus structure and sequence reconstruction |
| *Hibiscus syriacus* | GCF_006381635.1; Annotation Release 100 | Full GFF3; selected scaffold FASTA | Full GFF3 import/search; selected-locus structure and sequence reconstruction |

## Table S2. Full annotation tuple inventories

| Inventory | Tuples | Missing | Extra |
|---|---:|---:|---:|
| Arabidopsis bundled annotation | 811,827 | 0 | 0 |
| Arabidopsis full release 62 | 791,557 | 0 | 0 |
| Maize source excerpt | 377 | 0 | 0 |
| Maize full annotation | 1,161,691 | 0 | 0 |
| Rice source excerpt | 347 | 0 | 0 |
| Rice full annotation | 550,921 | 0 | 0 |
| Soybean source excerpt | 839 | 0 | 0 |
| Soybean full annotation | 1,324,199 | 0 | 0 |
| **Total** | **4,641,758** | **0** | **0** |

Tuple fields include recognized gene, transcript, exon, CDS, and UTR type, explicit parent relation, sequence ID, strand, and exact coordinates. The validator documents ignored or unsupported source feature types separately.

## Table S3. Public expression quantification inputs and output

| Field | Value |
|---|---|
| BioProject | PRJNA765494 |
| Genotype / developmental stage | IC426787; 5 days post anthesis |
| Runs | SRR16122599, SRR16122608, SRR16122607 |
| Assembly / cultivar | GCF_029865625.1 / Vumbellata_1.0 / VRB3 |
| Annotation release | GCF_029865625.1-RS_2026_09 |
| Quantifier | Salmon 2.8.0; selective alignment; k-mer length 31 |
| Reference transcript count | 54,178; RefSeq RNA FASTA SHA-256 in public-expression-manifest.json |
| Gene-map rows / matrix rows | 36,754 map entries / 34,914 quantified gene rows |
| Exact annotation ID matches | 34,914 of 34,914 matrix IDs; 0 unmatched |
| Missing / invalid cells | 0 / 0 |
| TPM column sums | 1,000,000.000034; 999,999.999897; 999,999.999951 |
| Interpretive limit | Genotype differs from reference cultivar; import and identifier compatibility only; no differential-expression inference |

## Table S4. Additional module validation panels

| Function | Independent reference | Observed result | Scope limit |
|---|---|---|---|
| Promoter extraction | Direct 1-based inclusive FASTA slicing and reverse complement; bedtools 2.31.1 | 8/8 exact sequences, 2,200 bp each, both strands represented | Eight loci on one Arabidopsis chromosome |
| CDS sequence quality | Independent base-count/frame/start/stop oracle | Four RBCS CDS models (411, 561, 546, 546 nt) matched; no ambiguous bases flagged | Four models from one family |
| Protein similarity | Test-side Smith–Waterman and BLASTP 2.12.0+ | All 16 within-panel alignment metric checks; 12/12 non-self RBCS and 8/8 negative pair classifications agree; 20/20 in the cross-species mature-RBCS panel | Small one-family smoke test; not orthology, sensitivity, or specificity evaluation |
| Expression grouping | Expression Atlas E-MTAB-7978 source values | Selected means/medians reproduced; measured zeros retained | Single-assay group arithmetic, not differential expression |
| Domain mapping | Independent Python reconstruction | 16 domain/deletion coordinate records across 13 genes matched | Existing annotated models; not a curated multi-species accuracy set |
| Project persistence | JavaFX regression actions | Save, replace, restore, locus state and outputs verified in CI | Software behavior, not usability study |

## Supplementary Note S1. Data required from users and non-inferable properties

Sequence extraction requires a genome FASTA matching the annotation assembly. Coordinates, feature names, and parent relationships are those recorded in the imported annotation; they are not independent confirmation of model quality. Expression display requires a matrix and, for biological interpretation, a clear unit/normalization, sample identity, condition, replicate design, and source metadata. The software cannot infer functional roles, orthology, regulatory effects, differential expression, unannotated isoforms, or biological absence from missing feature rows. Unprovided annotation fields remain unknown; derived intervals are labeled as inferred.

## Supplementary Note S2. Fair-comparison boundary

The completed `gffutils==0.13` comparison is a scripted annotation hierarchy extraction baseline only. Manual file handling and JBrowse/IGV interactions were not run as matched end-to-end tasks. No participant study was conducted. Thus the manuscript reports no speed ranking, usability advantage, user time savings, reduced error rate, or user-value effect. A future comparison should freeze the same species, assembly, files, locus list, output specification, and independent gold standard, then record commands and applications, intermediate files, provenance fields, and outputs across counterbalanced runs.
