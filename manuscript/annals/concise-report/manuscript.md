# PhytoScope for portable local queries of plant genome data

**Article type:** Concise Report

**Authors:** [AUTHOR NAMES IN FINAL ORDER]

**Affiliations:** [INSTITUTIONAL AFFILIATIONS]

**Correspondence:** [CORRESPONDING AUTHOR NAME AND EMAIL]

**ORCID:** [AUTHOR ORCIDS]

## Abstract

Public plant genomes often arrive as separate annotation, sequence, and expression files, while species-specific query interfaces remain unevenly available. PhytoScope provides a local desktop framework that connects these inputs through a reusable locus model. We evaluated the same import and query code across seven plant species. Independent source inventories matched 4,641,758 recognized structural tuples across eight annotation datasets, with no missing or extra tuples. Sequence reconstruction was checked at five coding loci in each of three additional plants, covering 25 coding transcripts. Public RNA sequencing reads from ricebean (*Vigna umbellata* (Thunb.) Ohwi & H.Ohashi) were quantified against a fixed reference transcript release; all 34,914 output gene identifiers matched its annotation. The reads and reference represent different genotypes, so this result establishes identifier compatibility. Together, the checks support a portable framework for importing public plant data, inspecting gene structures, exporting verified sequences, and reconnecting queries through saved projects.

**Keywords:** plant genomics; non-model species; genome annotation; sequence extraction; gene expression; reproducibility

**Graphical abstract summary:** PhytoScope connects user-supplied plant annotations, matching sequences, and expression matrices in a local locus workspace. Cross-species structural audits, independent sequence gold standards, and public expression identifier reconciliation test the same reusable workflow.

**Graphical abstract artwork:** Reuse Figure 1.

## Introduction

A public genome assembly is a valuable starting point for studying an underrepresented plant, but using its annotation can still require several disconnected files and tools. Curated resources such as The Arabidopsis Information Resource and Phytozome provide established entry points for selected species [1,2]. General browsers, including JBrowse 2 and the Integrative Genomics Viewer, support broad genomic visualization [3,4]. Researchers working with other taxa nevertheless need a practical way to connect local annotations, corresponding sequences, and expression tables without maintaining a species-specific database.

PhytoScope addresses this file-integration problem through a local desktop workspace. Species identity is supplied by the input files; the application uses a shared model for sequence identifiers, loci, transcripts, and structural features. We evaluated whether this model could query diverse public plant releases while retaining source coordinates and reconstructing selected sequences correctly. The report focuses on portability, source fidelity, and expression-data reconciliation, summarized in one figure (Figure 1).

## Materials and Methods

PhytoScope is a Java application with a JavaFX interface. It imports General Feature Format version 3 (GFF3) and Gene Transfer Format (GTF) annotations and links them to matching genomic FASTA and user-supplied expression matrices. RefSeq supplies release-specific reference records [5]; recognized gene, transcript, exon, coding-sequence (CDS), and untranslated-region (UTR) features and their parent relationships form the structural model [6]. Users can search identifiers or coordinates, inspect transcript structures, export sequences, display expression, and save or reopen file references, selected locus state, and results (Figure 1A–C). Sequence export requires the corresponding reference FASTA; expression display requires compatible identifiers and sample labels.

The reference cohort comprised thale cress (*Arabidopsis thaliana* (L.) Heynh.; TAIR10/Araport11), rice (*Oryza sativa* L.; IRGSP-1.0), maize (*Zea mays* L.; B73 NAM5), and soybean (*Glycine max* (L.) Merr.; v2.1). The additional cohort comprised ricebean (*Vigna umbellata* (Thunb.) Ohwi & H.Ohashi; GCF_029865625.1, cultivar VRB3), *Andrographis paniculata* (Burm.f.) Wall. ex Nees (GCF_009805555.1), and *Hibiscus syriacus* L. (GCF_006381635.1). Complete annotation imports and selected-locus queries used the same application code. Source versions and checksums are listed in Table S1 and the provenance manifests.

Annotation audits independently enumerated feature type, explicit parent, sequence identifier, strand, and exact 1-based inclusive coordinates from source files. Eight inventories included the bundled Arabidopsis annotation, its full release-62 annotation, and excerpt/full annotations for rice, maize, and soybean. Missing and extra tuples were counted separately. A gffutils 0.13 comparator evaluated hierarchy extraction on four release-62 excerpts, using three alternating-order trials per species. Independent sequence gold records reconstructed genomic spans, spliced transcripts, and CDS directly from source intervals and matching FASTA, without calling the application parser. Five coding loci per additional plant supplied 25 coding transcripts, checked by SHA-256 digests.

Expression validation used three public ricebean seed-development libraries from genotype IC426787 at 5 days post-anthesis, deposited as PRJNA765494 by Verma and colleagues [7]. Salmon 2.8.0 [8] quantified SRR16122599, SRR16122608, and SRR16122607 from the Sequence Read Archive [9] against the 54,178-sequence RefSeq RNA FASTA associated with GCF_029865625.1. Gene-level transcripts per million (TPM) were obtained using Salmon’s gene-map option and the matching GFF3 parents. Validation checked matrix cells, column sums, and exact identifier overlap with the imported annotation. The reference is VRB3, whereas the reads are IC426787; no genotype comparison or differential-expression test was performed.

Supporting checks used independent interval reconstruction and bedtools [10] for promoter sequences, test-side alignment and BLASTP [11] for small protein panels, separate domain-coordinate reconstruction, expression-grouping arithmetic, and project save/restore regressions. Release identifiers, checksums, parameters, gold records, and archived test reports accompany the code, supporting reproducible computational provenance [12,13]. Supporting Information contains detailed commands, task-specific denominators, and the archived comparator results.

## Results

The evaluated annotations from all seven taxa were imported and queried without changing the parser or adding species-specific query branches. Complete GFF3 imports in the three additional plants supported sequence recognition, identifier search, and selected transcript-structure views, including loci on unplaced scaffolds (Figure 1A–C; Figure S1). Thus, a change of species was expressed through data files rather than a new application implementation.

Across the eight source inventories, all 4,641,758 recognized structural tuples matched exactly, with zero missing or extra tuples (Figure 1D; Table S2; Figure S2). These are feature/relationship records, not a count of distinct genes. The four-species gffutils comparison likewise reproduced the source-derived tuples in every archived trial. Timings describe different setup operations—application import and SQLite index construction—and were retained as execution records rather than a speed comparison.

The 15 selected loci produced 25 coding-transcript gold records: six in ricebean, 14 in Andrographis, and five in Hibiscus. Strand-aware genomic-span, transcript, and CDS hashes matched the independent reconstructions. Ricebean provided an informative format boundary. Its complete GFF3 contained 232 sequence identifiers, compared with 99 in the corresponding GTF. At LOC124833352 on NC_142174.1, the three-exon transcript XM_088100767.1 was 2,060 nucleotides long. GFF3 specified a 1,983-nucleotide CDS including the terminal stop codon; GTF specified a 1,980-nucleotide CDS and a separate three-base stop-codon feature. Both representations yielded the same 660-amino-acid protein. Terminal UTR intervals were labeled inferred because this annotation lacked explicit UTR rows (Figure 1C).

The re-quantified expression matrix contained 34,914 gene-level target identifiers and three sample columns. All identifiers matched the pinned annotation, with zero unmatched identifiers, missing cells, or invalid cells; each TPM column summed to one million within rounding (Figure 1D; Table S3; Figure S3). Figure 1C shows individual replicate TPM values for LOC148191553 as an expression-display example, separate from the sequence example. The result establishes release-specific identifier reconciliation of public reads, not biological equivalence between the source genotype and reference cultivar.

Supporting module checks recovered eight of eight promoter sequences exactly, 16 domain-coordinate records across 13 genes, and agreement in the small protein-classification panel. Expression-grouping arithmetic and project restoration were also checked through their separate regressions (Table S4; Figure S4). These task-specific checks extend the locus workflow while keeping each result tied to its own reference and denominator.

## Discussion

The main contribution is a reusable query layer for researchers who possess public plant files but lack an integrated species query interface. The framework connects genome structure, sequence products, expression values, and project state around the same locus model. Its evaluation follows the information through that connection: source records are audited structurally, sequence products are independently reconstructed, and expression identifiers are joined to a fixed release. This makes the portability claim assessable through observable outputs rather than the number of supported menu items.

The validation scope also identifies where inputs constrain interpretation. Exact tuple agreement establishes fidelity to the recognized source fields, not the biological correctness of every annotation model. Sequence gold records cover selected loci rather than every transcript in each assembly. Missing annotation features require explicit source or inference labels, and expression values require appropriate reference matching and sample metadata. In the ricebean case, IC426787 reads and the VRB3 reference permit a technical import and identifier check; the experiment does not establish genotype-matched expression biology. Expression evidence currently centers on this one public species.

The narrow gffutils comparison provides a reproducible extraction baseline. A broader comparison with manual file handling and general browsers would need identical tasks, inputs, outputs, and execution conditions, as emphasized in computational benchmarking guidance [14]. Participant studies would be needed to assess ease of use or researcher time savings. Within the tested scope, the evidence supports a practical way to query dispersed public plant data locally while preserving the connection between source files and inspected outputs.

## Conclusion

PhytoScope supports a portable local workflow for plant gene queries. Cross-species structural audits, selected sequence gold standards, and exact-release expression identifier reconciliation provide complementary evidence for the tested datasets. A shared locus model allows these verified outputs to be accessed and revisited without building a new species-specific database.

## Acknowledgments

We acknowledge the researchers who generated the public reference and RNA sequencing datasets and the repositories that distribute them.

## Author Contributions

[AUTHOR CONTRIBUTIONS TO BE COMPLETED USING CREDIT ROLES]

## Competing Interests

[AUTHOR-VERIFIED COMPETING INTERESTS STATEMENT]

## Funding

[AUTHOR-VERIFIED FUNDING INFORMATION]

## Ethics Statement

This study analyzed public plant datasets and software behavior. It involved no new human-participant or animal research and no new biological sample collection.

## Data Availability

Software, frozen evidence, public expression values, source manifests, and reproduction scripts are available at https://github.com/omixcore/phytoscope. The application/evidence snapshot is commit 159ae0f79bd3681afc6c914db282c515190a3888. The expression reads are available under PRJNA765494; the matched reference files use GCF_029865625.1 and annotation release GCF_029865625.1-RS_2026_09. Additional assemblies are GCF_009805555.1 and GCF_006381635.1. Supporting Information identifies versions, input checksums, selected loci, and commands. [PERMANENT ARCHIVE DOI TO BE ADDED BY THE AUTHORS]

## References

1. T. Z. Berardini, L. Reiser, D. Li, et al., “The Arabidopsis Information Resource: Making and Mining the ‘Gold Standard’ Annotated Reference Plant Genome,” *Genesis* 53 (2015): 474–485. https://doi.org/10.1002/dvg.22877

2. D. M. Goodstein, S. Shu, R. Howson, et al., “Phytozome: A Comparative Platform for Green Plant Genomics,” *Nucleic Acids Research* 40 (2012): D1178–D1186. https://doi.org/10.1093/nar/gkr944

3. C. Diesh, G. J. Stevens, P. Xie, et al., “JBrowse 2: A Modular Genome Browser with Views of Synteny and Structural Variation,” *Genome Biology* 24 (2023): 74. https://doi.org/10.1186/s13059-023-02914-z

4. H. Thorvaldsdottir, J. T. Robinson, and J. P. Mesirov, “Integrative Genomics Viewer (IGV): High-Performance Genomics Data Visualization and Exploration,” *Briefings in Bioinformatics* 14 (2013): 178–192. https://doi.org/10.1093/bib/bbs017

5. N. A. O'Leary, M. W. Wright, J. R. Brister, et al., “Reference Sequence (RefSeq) Database at NCBI: Current Status, Taxonomic Expansion, and Functional Annotation,” *Nucleic Acids Research* 44 (2016): D733–D745. https://doi.org/10.1093/nar/gkv1189

6. K. Eilbeck, S. E. Lewis, C. J. Mungall, et al., “The Sequence Ontology: A Tool for the Unification of Genome Annotations,” *Genome Biology* 6 (2005): R44. https://doi.org/10.1186/gb-2005-6-5-r44

7. S. K. Verma, S. Mittal, Gayacharan, et al., “Transcriptome Analysis Reveals Key Pathways and Candidate Genes Controlling Seed Development and Size in Ricebean (*Vigna umbellata*),” *Frontiers in Genetics* 12 (2022): 791355. https://doi.org/10.3389/fgene.2021.791355

8. R. Patro, G. Duggal, M. I. Love, R. A. Irizarry, and C. Kingsford, “Salmon Provides Fast and Bias-Aware Quantification of Transcript Expression,” *Nature Methods* 14 (2017): 417–419. https://doi.org/10.1038/nmeth.4197

9. R. Leinonen, H. Sugawara, M. Shumway, and on behalf of the International Nucleotide Sequence Database Collaboration, “The Sequence Read Archive,” *Nucleic Acids Research* 39 (2011): D19–D21. https://doi.org/10.1093/nar/gkq1019

10. A. R. Quinlan, and I. M. Hall, “BEDTools: A Flexible Suite of Utilities for Comparing Genomic Features,” *Bioinformatics* 26 (2010): 841–842. https://doi.org/10.1093/bioinformatics/btq033

11. S. F. Altschul, W. Gish, W. Miller, E. W. Myers, and D. J. Lipman, “Basic Local Alignment Search Tool,” *Journal of Molecular Biology* 215 (1990): 403–410. https://doi.org/10.1016/S0022-2836(05)80360-2

12. G. K. Sandve, A. Nekrutenko, J. Taylor, and E. Hovig, “Ten Simple Rules for Reproducible Computational Research,” *PLOS Computational Biology* 9 (2013): e1003285. https://doi.org/10.1371/journal.pcbi.1003285

13. M. D. Wilkinson, M. Dumontier, I. J. Aalbersberg, et al., “The FAIR Guiding Principles for Scientific Data Management and Stewardship,” *Scientific Data* 3 (2016): 160018. https://doi.org/10.1038/sdata.2016.18

14. L. M. Weber, W. Saelens, R. Cannoodt, et al., “Essential Guidelines for Computational Method Benchmarking,” *Genome Biology* 20 (2019): 125. https://doi.org/10.1186/s13059-019-1738-8
## Figure Legend

**Figure 1. Local plant-data queries and independent validation in PhytoScope.** (A) User-provided annotation, matching reference FASTA, and expression data enter the workflow; plant symbols are schematic. (B) Sequence identifiers, gene records, transcripts, and structural features form a shared local model with project save/reopen operations. (C) The ricebean example LOC124833352 on NC_142174.1 shows a minus-strand three-exon transcript and its sequence lengths. CDS denotes coding sequence; UTR denotes untranslated region, with inferred terminal intervals labeled. The separate LOC148191553 example shows individual gene-level transcripts per million (TPM); R1–R3 correspond to SRR16122599, SRR16122608, and SRR16122607 at 5 days post-anthesis. (D) Structural tuple counts, selected-locus genomic-span/spliced-RNA/CDS hash checks, and expression identifier matches summarize separate validation tasks. The eight inventory counts share a linear bar scale. The expression workflow used IC426787 reads and the VRB3 reference transcript release. Checks indicate agreement with the specified source-derived references; task-specific denominators are not pooled. Full inputs and procedures appear in Supporting Information.
