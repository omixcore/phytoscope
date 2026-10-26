# Supporting Information

PhytoScope for portable local queries of plant genome data

## Supplementary Methods

The software and evidence snapshot is commit `159ae0f79bd3681afc6c914db282c515190a3888` at https://github.com/omixcore/phytoscope. The code requires Java 21. Source accessions, versions, checksums, gene/transcript gold records, and the public expression matrix are supplied with this report. Full genomes, annotation archives and raw reads are fetched from their public repositories rather than duplicated in the submission package.

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
| Distinct GFF3 parent genes / matrix rows | 36,754 distinct parent genes / 34,914 matrix rows |
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


## Supporting Figure Legends

**Figure S1. Evaluation coverage.** Rows identify seven tested plant species; columns indicate annotation import, search/structure, selected sequence-gold and expression-matrix checks. A dash denotes a task not evaluated in that species.

**Figure S2. Structural tuple inventories.** Counts of recognized gene, transcript, exon, CDS and UTR relationship tuples in eight source inventories. Bars and exact labels show each count; the summary reports missing and extra records against source-derived references.

**Figure S3. Ricebean expression processing.** Assigned fragments are shown separately for the three public read libraries. The processing steps identify the fixed RefSeq transcript set, gene-level TPM matrix and identifier reconciliation. Source genotype IC426787 and reference cultivar VRB3 are labeled.

**Figure S4. Independent module checks.** Panels list task-specific counts for promoter sequences, domain-coordinate records, small protein-similarity classifications and expression identifiers. The protein panel is an RBCS-family smoke test. Separate denominators are retained for each check.
