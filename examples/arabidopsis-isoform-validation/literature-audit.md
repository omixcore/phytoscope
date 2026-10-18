# Novelty and functional interpretation audit

Checked 2026-10-18. No candidate passed independent validation, so there is no
candidate eligible for a validated-discovery claim. The strongest single-study
lead, Arabidopsis CIPK15, was additionally checked to avoid treating an existing
annotation as a discovery.

Queries included `AT5G01810 alternative splicing NAF`, `CIPK15 alternative
splicing isoform`, `AT5G01810.3`, and `CIPK15 NAF domain deletion`. Rice CIPK15
and Arabidopsis CIPK3 reports were not treated as evidence for Arabidopsis CIPK15.
Searches are not exhaustive, and lack of a relevant hit is not evidence of novelty.

| Source | Observation | Permitted inference |
| --- | --- | --- |
| [TAIR10 new gene models](https://www.arabidopsis.org/download/file?path=Genes%2FTAIR10_genome_release%2FTAIR10_gene_lists%2FTAIR10_new_gene_models.txt) | AT5G01810.3 appears in an earlier annotation release | The isoform is not a newly discovered sequence |
| [UniProt P92937](https://www.uniprot.org/uniprotkb/P92937/entry), pinned JSON in this directory | Kinase 12–266; NAF 299–323; multiple EST isoforms noted | Supports domain context, not a measured effect of the short isoform |
| [NCBI Gene 830556](https://www.ncbi.nlm.nih.gov/gene/830556) | CIPK15 identity and transcript records | Identifier provenance, not independent functional validation |
| [Reyes et al. 2026](https://doi.org/10.1093/nar/gkag400), Table S2 row 24784 | Published junction peptide from the Mergner source | Reanalysis of existing evidence, not a new peptide identification |

`source-article-gene-ids.txt` contains the 15 Arabidopsis locus IDs extracted from
the retrieved Reyes article HTML. None of the 13 candidate genes is explicitly
listed there. This narrow automated flag does not assess aliases, supplementary
tables, other publications or whether the underlying observation was previously
known. It must never be interpreted as a novelty classifier.

The structural hypothesis is that deleting the NAF region may change interaction
with CBL proteins while retaining the kinase sequence. We have not demonstrated
that interaction change, altered kinase activity, environmental regulation or
physiological consequences. A validated new biological finding remains outstanding.
