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


## Expanded prior-annotation and literature audit (2026-10-18)

The 16 rows in the screen represent 13 genes and transcript models already
present in the pinned Araport11 annotation. This is a screen for candidate
domain-disrupting structures, **not** a discovery of new transcript sequences.
A targeted NCBI RefSeq and literature check found that multiple candidates have
independently catalogued transcript variants:

| Candidate gene | Public transcript evidence checked | Consequence for novelty claims |
|---|---|---|
| `AT1G21240` / WAK3 | NCBI lists `NM_101977.2` and `NM_001332490.1` as WAK3 splice variants ([RefSeq](https://www.ncbi.nlm.nih.gov/nuccore/NM_101977.2)). | Alternative transcript existence is already known; the exact domain-deletion model still needs isoform-specific protein evidence. |
| `AT5G01810` / CIPK15 | `AT5G01810.3` is an existing TAIR10/Araport transcript model; its one eligible junction peptide in the screen is from the Mergner acquisition. | Neither the transcript nor the peptide is newly discovered by PhytoScope. |
| `AT1G47290` / 3BETAHSD/D1 | NCBI lists `NM_103623.5` and `NM_179448.4` ([RefSeq](https://www.ncbi.nlm.nih.gov/nuccore/NM_103623)). The gene's enzyme activity and overexpression phenotypes were previously studied ([primary study](https://doi.org/10.1371/journal.pone.0085389)). | Existing gene-level biology does not validate the candidate isoform's specific domain-loss effect. |
| `AT3G57800` / bHLH family protein | NCBI lists `NM_115642.2` and `NM_180414.3` ([RefSeq](https://www.ncbi.nlm.nih.gov/nuccore/NM_115642.2)). | Alternative transcript existence is already catalogued. |
| `AT3G62090` / PIL2-PIF6 | NCBI reports four RefSeq mRNA variants ([gene record](https://www.ncbi.nlm.nih.gov/gene/825382)). Published work links PIF6 alternative splicing to seed dormancy/germination ([Penfield et al. 2010](https://doi.org/10.1007/s11103-009-9571-1); [Plant Cell 2012](https://doi.org/10.1105/tpc.112.103622)). | This is a **known-positive locus-level control**, not a new discovery. Exact mapping of the frozen `.2/.4` pair to historical constructs needs sequence-level reconciliation; see the [PIF6 case note](../../docs/case-study-pif6-known-positive.md). |
| `AT1G69530` / EXPA1 | NCBI lists five RefSeq mRNA splice variants ([RefSeq](https://www.ncbi.nlm.nih.gov/nuccore/NM_001334412.1)). | Alternative transcript existence is already catalogued. |
| `AT5G12480` / CPK7 | NCBI lists `NM_001085107.1` and `NM_121286.4` ([RefSeq](https://www.ncbi.nlm.nih.gov/nuccore/NM_001085107.1)). | Alternative transcript existence is already catalogued. |
| `AT1G76650` / CML38 | NCBI reports three RefSeq mRNA variants ([RefSeq](https://www.ncbi.nlm.nih.gov/nuccore/NM_001198484.1)). | Alternative transcript existence is already catalogued. |
| `AT5G56610` / phosphotyrosine protein phosphatase family | NCBI lists `NM_125044.4` and `NM_001037007.1` ([RefSeq](https://www.ncbi.nlm.nih.gov/nuccore/NM_125044.4)). | Alternative transcript existence is already catalogued. |
| `AT1G78320` / GSTU23 | NCBI's current reference record lists `NM_106481.4` ([gene record](https://www.ncbi.nlm.nih.gov/gene/844167)); the frozen screen itself uses the Araport11 transcript models. | No isoform-specific independent protein validation was found in the targeted check. |
| `AT5G62480` / GSTU9 | Targeted searches surfaced gene-level stress-response/expression literature, but not independent evidence for the exact domain-deletion transcript. | Gene-level stress associations cannot validate the candidate isoform. |
| `AT1G48590` | No direct independent transcript-level functional evidence was located in this targeted search. | A missing search hit is not evidence of novelty; retain as unvalidated. |
| `AT3G18750` / WNK6 | NCBI reports six RefSeq mRNA variants ([RefSeq](https://www.ncbi.nlm.nih.gov/nuccore/NM_001338355.1)). | Alternative transcript existence is already catalogued. |

This is a targeted audit, not an exhaustive systematic review. NCBI transcript
records establish annotation history, not translation or function of every
isoform. The PIF6 literature is especially important because it shows that
isoform-level biology at the locus is already known; it must not be presented
as a PhytoScope discovery. However, the precise mapping between the current
Araport11 `AT3G62090.4` model and historical PIF6 constructs must be checked
at sequence level before treating them as the same molecule.

**Updated conclusion:** no candidate in the 13-gene screen currently satisfies
the predeclared independent peptide threshold for a new discovery. CIPK15
remains a single-study lead; PIF6 provides known biological context, not
novelty. A new biological finding remains outstanding.
